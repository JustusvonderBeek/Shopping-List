package com.cloudsheeptech.shoppinglist.data.handling

import android.util.Log
import com.cloudsheeptech.shoppinglist.data.Serializer.OffsetDateTimeSerializer
import com.cloudsheeptech.shoppinglist.data.ShoppingList
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.network.Networking
import com.cloudsheeptech.shoppinglist.data.user.AppUserHandler
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.time.OffsetDateTime

class OnlineListHandler(database: ShoppingListDatabase) : ListHandler(database) {

    private val json = Json {
        serializersModule = SerializersModule {
            contextual(OffsetDateTime::class, OffsetDateTimeSerializer())
        }
    }

    override suspend fun storeShoppingList(list: ShoppingList) : Long {
        var listId = 0L
        withContext(Dispatchers.IO) {
//            if (list.CreatedByID == 0L) {
//                Log.d("ShoppingListHandler", "List was created offline and needs to be converted")
//                // TODO: Move this into the user
//                if (AppUserHandler.UserId == 0L) {
//                    AppUserHandler.PostUserOnlineAsync(null)
//                    if (AppUserHandler.UserId == 0L) {
//                        Log.i("ShoppingListHandler", "Failed to create user online: Cannot complete request")
//                        return@withContext
//                    }
//                }
//                list.CreatedByID = AppUserHandler.UserId
////                updatedCreatedByForAllLists()
//            }
            val listInWireFormat = list.toShoppingListWire(database)
            val serializedList = json.encodeToString(listInWireFormat)
            Networking.POST("v1/list", serializedList) { resp ->
                if (resp.status != HttpStatusCode.Created) {
                    Log.w("ShoppingListHandler", "Posting Shopping List online failed")
                    return@POST
                }
                // We don't expect anything from online
                listId = list.ID
            }
        }
        // Because we store the List ID  + User Online, the user can decide what ID the list has
        // Therefore, we are not interested in whatever ID the server assigned to the
        // posted list. Simply respond with success or failure
        return listId
    }

    override suspend fun retrieveShoppingList(listId: Long, createdBy: Long): ShoppingList? {
        var shoppingList : ShoppingList? = null
        withContext(Dispatchers.IO) {
            Networking.GET("v1/list/$listId") { resp ->
                if (resp.status != HttpStatusCode.OK) {
                    Log.d("OnlineListHandler", "Failed to retrieve list $listId")
                    return@GET
                }
                try {
                    val body = resp.bodyAsText(Charsets.UTF_8)
                    val decoded = json.decodeFromString<ShoppingList>(body)
                    shoppingList = decoded
                } catch (ex: SerializationException) {
                    Log.e("OnlineListHandler", "Failed to deserialize the received content!")
                    return@GET
                }
            }
        }
        return shoppingList
    }

    override suspend fun updateLastEditedNow(listId: Long, createdBy: Long): ShoppingList? {
        TODO("Not yet implemented")
    }

    override suspend fun deleteShoppingList(listId: Long, createdBy: Long) {
        TODO("Not yet implemented")
    }
}