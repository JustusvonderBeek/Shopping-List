package com.cloudsheeptech.shoppinglist.data.list

import android.util.Log
import com.cloudsheeptech.shoppinglist.data.typeConverter.OffsetDateTimeSerializer
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import com.cloudsheeptech.shoppinglist.network.Networking
import com.cloudsheeptech.shoppinglist.network.UrlProviderEnum
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingListRemoteDataSource
@Inject
constructor(
    private val networking: Networking,
    private val userRepository: AppUserRepository,
) {
    private val json =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = false
            serializersModule =
                SerializersModule {
                    contextual(OffsetDateTime::class, OffsetDateTimeSerializer())
                }
        }

    suspend fun create(list: ApiShoppingList): Boolean {
        var success = false
        withContext(Dispatchers.IO) {
            try {
                val encodedList = json.encodeToString(list)
                Log.d("ShoppingListRemoteDataSource", "Encoded List:\n$encodedList")
                networking.POST(
                    UrlProviderEnum.BASE_SHOPPING_LIST_URL.url,
                    encodedList,
                ) { response ->
                    if (response.status == HttpStatusCode.BadRequest) {
                        return@POST
                    }
                    if (response.status != HttpStatusCode.Created) {
                        Log.e("ShoppingListRemoteDataSource", "The list was not created online")
                        return@POST
                    }
                    success = true
                }
            } catch (ex: SerializationException) {
                Log.e(
                    "ShoppingListRemoteDataSource",
                    "Cannot serialize the list into a string: $ex",
                )
                return@withContext
            } catch (ex: IllegalAccessError) {
                Log.e("ShoppingListRemoteDataSource", "Error: $ex")
            }
        }
        // Because we store the List ID  + User Online, the user can decide what ID the list has
        // Therefore, we are not interested in whatever ID the server assigned to the
        // posted list. Simply respond with success or failure
        return success
    }

    suspend fun read(
        listId: Long,
        createdBy: Long,
    ): ApiShoppingList? {
        var retrievedRemoteList: ApiShoppingList? = null
        withContext(Dispatchers.IO) {
            networking.GET("${UrlProviderEnum.SHOPPING_LIST_READ.url}/$listId?createdBy=$createdBy") { response ->
                if (response.status != HttpStatusCode.OK) {
                    Log.e(
                        "ShoppingListRemoteDataSource",
                        "Failed to read list $listId createdBy $createdBy from remote",
                    )
                    return@GET
                }
                val rawBody = response.bodyAsText(Charsets.UTF_8)
                if (rawBody.isEmpty()) {
                    Log.e("ShoppingListRemoteDataSource", "Remote did not return any list body")
                    return@GET
                }
                val onlineList = json.decodeFromString<ApiShoppingList>(rawBody)
                retrievedRemoteList = onlineList
            }
        }
        return retrievedRemoteList
    }

    suspend fun readAll(): List<ApiShoppingList> {
        val allRemoteLists = mutableListOf<ApiShoppingList>()
        withContext(Dispatchers.IO) {
            networking.GET(UrlProviderEnum.BASE_SHOPPING_LIST_URL.url) { response ->
                if (response.status != HttpStatusCode.OK) {
                    Log.e("ShoppingListRemoteDataSource", "Failed to read all lists from remote")
                    return@GET
                }
                val rawBody = response.bodyAsText(Charsets.UTF_8)
                if (rawBody.isEmpty() || rawBody == "null") {
                    Log.e("ShoppingListRemoteDataSource", "Remote did not return any list body")
                    return@GET
                }
                Log.d("ShoppingListRemoteDataSource", "Received: $rawBody")
                val decodedOnlineLists = json.decodeFromString<List<ApiShoppingList>>(rawBody)
                allRemoteLists.addAll(decodedOnlineLists)
            }
        }
        return allRemoteLists
    }

    suspend fun update(updatedList: ApiShoppingList): Boolean {
        var success = false
        withContext(Dispatchers.IO) {
            try {
                val encodedList = json.encodeToString(updatedList)
                networking.PUT(
                    "${UrlProviderEnum.BASE_SHOPPING_LIST_URL.url}/${updatedList.listId}?createdBy=${updatedList.createdBy.onlineId}",
                    encodedList,
                ) { response ->
                    if (response.status == HttpStatusCode.BadRequest) {
                        return@PUT
                    }
                    if (response.status != HttpStatusCode.OK) {
                        Log.e(
                            "ShoppingListRemoteDataSource",
                            "Remote did not process updating list successfully",
                        )
                        return@PUT
                    }
                    success = true
                }
            } catch (ex: SerializationException) {
                Log.w(
                    "ShoppingListRemoteDataSource",
                    "Cannot serialize the list into a string: $ex",
                )
            } catch (ex: IllegalAccessException) {
                Log.w("ShoppingListRemoteDataSource", "Error: $ex")
            }
        }
        return success
    }

    // Because we can only delete the lists that we created ourselves, the createdBy
    // parameter is implicitly given and we don't need to include it here
    suspend fun deleteShoppingList(listId: Long): Boolean {
        var success = false
        withContext(Dispatchers.IO) {
            networking.DELETE("${UrlProviderEnum.BASE_SHOPPING_LIST_URL.url}/$listId") { response ->
                if (response.status != HttpStatusCode.OK) {
                    Log.e("ShoppingListRemoteDataSource", "Failed to delete list $listId at remote")
                    return@DELETE
                }
                success = true
            }
        }
        return success
    }

    suspend fun deleteAll(): Boolean {
        var success = false
        withContext(Dispatchers.IO) {
            networking.DELETE("${UrlProviderEnum.BASE_SHOPPING_LIST_URL.url}/lists") { response ->
                if (response.status != HttpStatusCode.OK) {
                    Log.e("ShoppingListRemoteDataSource", "Failed to delete all lists at remote")
                    return@DELETE
                }
                success = true
            }
        }
        return success
    }
}
