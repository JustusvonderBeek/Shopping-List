package com.cloudsheeptech.shoppinglist.list.repo

import android.util.Log
import com.cloudsheeptech.shoppinglist.list.api.AppApiProvider
import com.cloudsheeptech.shoppinglist.list.model.AppItem
import com.cloudsheeptech.shoppinglist.list.model.QuantityType
import com.cloudsheeptech.shoppinglist.list.model.ShoppingList
import com.cloudsheeptech.shoppinglist.list.model.ShoppingListOperation
import com.cloudsheeptech.shoppinglist.network.Networking
import com.cloudsheeptech.shoppinglist.network.UrlProviderEnum
import com.cloudsheeptech.shoppinglist.user.repo.AppUserRepository
import com.cloudsheeptech.shoppinglist.util.OffsetDateTimeFormatHandler
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
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
        private val appApiProvider: AppApiProvider,
    ) {
        private val json =
            Json {
                encodeDefaults = true
                ignoreUnknownKeys = false
                // Set empty/null values to the default values
                coerceInputValues = true
                serializersModule =
                    SerializersModule {
                        contextual(OffsetDateTime::class, OffsetDateTimeFormatHandler())
                    }
            }

        private val shoppingListApi = appApiProvider.shoppingListApi

        suspend fun create(list: ShoppingList): Boolean {
            return withContext(Dispatchers.IO) {
                try {
                    val result = shoppingListApi.create(list)
                    if (!result.result.lowercase().equals("success")) {
                        Log.e(
                            "ShoppingListRemoteDataSource",
                            "Failed to create list online: ${result.errorMessage}",
                        )
                        return@withContext false
                    }
                    // TODO: Test and if it works, remove this code here
//                    val encodedList = json.encodeToString(list)
//                    Log.d("ShoppingListRemoteDataSource", "Encoded List:\n$encodedList")
//                    networking.POST(
//                        UrlProviderEnum.BASE_SHOPPING_LIST_URL.url,
//                        encodedList,
//                    ) { response ->
//                        if (response.status == HttpStatusCode.BadRequest) {
//                            return@POST
//                        }
//                        if (response.status != HttpStatusCode.Created) {
//                            Log.e("ShoppingListRemoteDataSource", "The list was not created online")
//                            return@POST
//                        }
//                        success = true
//                    }
                    return@withContext true
                } catch (ex: Exception) {
                    Log.e("ShoppingListRemoteDataSource", "Failed to create list online: $ex")
                }
                return@withContext false
            }
        }

        suspend fun read(
            listId: Long,
            createdBy: Long,
        ): ShoppingList? {
            var retrievedRemoteList: ShoppingList? = null
            withContext(Dispatchers.IO) {
                networking.get("${UrlProviderEnum.BASE_SHOPPING_LIST_URL.url}/$listId?createdBy=$createdBy") { response ->
                    if (response.status != HttpStatusCode.OK) {
                        Log.e(
                            "ShoppingListRemoteDataSource",
                            "Failed to read list $listId createdBy $createdBy from remote",
                        )
                        return@get
                    }
                    val rawBody = response.bodyAsText(Charsets.UTF_8)
                    if (rawBody.isEmpty()) {
                        Log.e("ShoppingListRemoteDataSource", "Remote did not return any list body")
                        return@get
                    }
                    val onlineList = json.decodeFromString<ShoppingList>(rawBody)
                    retrievedRemoteList = onlineList
                }
            }
            return retrievedRemoteList
        }

        suspend fun readAll(): List<ShoppingList> {
            return withContext(Dispatchers.IO) {
                val lists = shoppingListApi.readAllRemote()
                if (lists == null) {
                    Log.e("ShoppingListRemoteDataSource", "Failed to read all remote lists")
                    return@withContext listOf()
                }
                return@withContext lists
//                networking.get(UrlProviderEnum.BASE_SHOPPING_LIST_URL.url) { response ->
//                    if (response.status != HttpStatusCode.OK) {
//                        Log.e("ShoppingListRemoteDataSource", "Failed to read all lists from remote")
//                        return@get
//                    }
//                    try {
//                        val rawBody = response.bodyAsText(Charsets.UTF_8)
//                        if (rawBody.isEmpty() || rawBody == "null") {
//                            Log.i("ShoppingListRemoteDataSource", "No remote lists found")
//                            return@get
//                        }
//                        Log.d("ShoppingListRemoteDataSource", "Received: $rawBody")
//                        val decodedOnlineLists = json.decodeFromString<List<ShoppingList>>(rawBody)
//                        allRemoteLists.addAll(decodedOnlineLists)
//                    } catch (ex: SerializationException) {
//                        Log.e("ShoppingListRemoteDataSource", "Cannot decode remote lists $ex")
//                    } catch (ex: IllegalArgumentException) {
//                        Log.e("ShoppingListRemoteDataSource", "Received lists in wrong format: $ex")
//                    }
//                }
            }
        }

        suspend fun update(operation: ShoppingListOperation): Boolean =
            withContext(Dispatchers.IO) {
                try {
                    when (operation) {
                        is ShoppingListOperation.AddItem -> {
                            val response = shoppingListApi.addItem(operation.listPk.listId, operation.item)
                            response.status == HttpStatusCode.OK
                        }
                        is ShoppingListOperation.AddItemById -> {
                            // TODO: Update to retrieve correct item
                            val itemToAdd = AppItem(0L, "", "", 1, QuantityType.PIECES, false, 1L)
                            val response = shoppingListApi.addItem(operation.listPk.listId, itemToAdd)
                            response.status == HttpStatusCode.OK
                        }
                        is ShoppingListOperation.ChangeQuantityOfItem -> {
                            val response =
                                shoppingListApi.changeQuantityItem(
                                    operation.listPk.listId,
                                    operation.quantityType!!,
                                )
                            response.status == HttpStatusCode.OK
                        }
                        is ShoppingListOperation.Create -> {
                        }
                        is ShoppingListOperation.Delete -> {
                        }
                        is ShoppingListOperation.RemoveItemById -> {
                        }
                        is ShoppingListOperation.RemoveItemByName -> {
                        }
                        is ShoppingListOperation.RenameList -> {
                            val response = shoppingListApi.updateTitle(operation.listPk.listId, operation.newName)
                            response.status == HttpStatusCode.OK
                        }
                        is ShoppingListOperation.SetItemCheckedStatus -> {
                        }
                    }
//                    val encodedList = json.encodeToString(operation)
//                    networking.PUT(
//                        "${UrlProviderEnum.BASE_SHOPPING_LIST_URL.url}/${operation.listId}?createdBy=${operation.createdBy.onlineId}",
//                        encodedList,
//                    ) { response ->
//                        if (response.status == HttpStatusCode.BadRequest) {
//                            return@PUT
//                        }
//                        if (response.status != HttpStatusCode.OK) {
//                            Log.e(
//                                "ShoppingListRemoteDataSource",
//                                "Remote did not process updating list successfully",
//                            )
//                            return@PUT
//                        }
//                        success = true
//                    }
                } catch (ex: SerializationException) {
                    Log.w(
                        "ShoppingListRemoteDataSource",
                        "Cannot serialize the list into a string: $ex",
                    )
                } catch (ex: IllegalAccessException) {
                    Log.w("ShoppingListRemoteDataSource", "Error: $ex")
                } catch (ex: Exception) {
                    Log.e("ShoppingListRemoteDataSource", "Failed to update list: $ex")
                }
                false
            }

        // This will delete the sharing if any existed
        suspend fun deleteShoppingList(
            listId: Long,
            createdBy: Long,
        ): Boolean {
            var success = false
            withContext(Dispatchers.IO) {
                networking.DELETE("${UrlProviderEnum.BASE_SHOPPING_LIST_URL.url}/$listId?createdBy=$createdBy") { response ->
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
                networking.DELETE(UrlProviderEnum.BASE_SHOPPING_LIST_URL.url) { response ->
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
