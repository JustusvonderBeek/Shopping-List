package com.cloudsheeptech.shoppinglist.data.handling

import android.util.Log
import com.cloudsheeptech.shoppinglist.data.Serializer.OffsetDateTimeSerializer
import com.cloudsheeptech.shoppinglist.data.ShoppingList
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.network.Networking
import com.cloudsheeptech.shoppinglist.user.AppUser
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
            if (list.CreatedBy == 0L) {
                Log.d("ShoppingListHandler", "List was created offline and needs to be converted")
                if (AppUser.UserId == 0L) {
                    AppUser.PostUserOnlineAsync(null)
                    if (AppUser.UserId == 0L) {
                        Log.i("ShoppingListHandler", "Failed to create user online: Cannot complete request")
                        return@withContext
                    }
                }
                list.CreatedBy = AppUser.UserId
//                updatedCreatedByForAllLists()
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
        }
        // Because we store the List ID  + User Online, the user can decide what ID the list has
        // Therefore, we are not interested in whatever ID the server assigned to the
        // posted list. Simply respond with success or failure
        return listId
    }

    override suspend fun retrieveShoppingList(listId: Long, createdBy: Long): ShoppingList? {
        TODO("Not yet implemented")
    }

    override suspend fun updateLastEditedNow(listId: Long, createdBy: Long): ShoppingList? {
        TODO("Not yet implemented")
    }

    override suspend fun deleteShoppingList(listId: Long, createdBy: Long) {
        TODO("Not yet implemented")
    }
}