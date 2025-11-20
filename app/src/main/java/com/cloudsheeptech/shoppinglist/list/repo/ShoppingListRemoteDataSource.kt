package com.cloudsheeptech.shoppinglist.list.repo

import android.util.Log
import com.cloudsheeptech.shoppinglist.list.api.ShoppingListApi
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
        private val shoppingListApi: ShoppingListApi,
        private val pendingOperationRepository: ShoppingListOperationRepository,
        private val userRepository: AppUserRepository,
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

        suspend fun create(list: ShoppingList): Boolean {
            return withContext(Dispatchers.IO) {
                try {
                    val result = shoppingListApi.create(list)
                    // TODO: I need to adapt the server to return the correct response
                    if (!result.isSuccessful) {
                        Log.e(
                            "ShoppingListRemoteDataSource",
                            "Failed to create list online: ${result.message()}",
                        )
                        return@withContext false
                    }
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

        suspend fun executePendingOperations(): Boolean {
            return withContext(Dispatchers.IO) {
                try {
                    val pendingOperations = pendingOperationRepository.getAllPendingOperations()
                    val response = shoppingListApi.performOperations(pendingOperations)
                    if (response.code() != HttpStatusCode.OK.value) {
                        Log.e("ShoppingListRemoteDataSource", "Failed to execute operations successfully online")
                        return@withContext false
                    }
                    Log.i("ShoppingListRemoteDataSource", "Successfully performed operations online")
                    return@withContext true
//                    for (operation in pendingOperations) {
//                        val convertedOp =
//                            ShoppingListOperationConversionUtil.shoppingListApiOperationToShoppingListOperation(
//                                operation,
//                            )
//                        val success = update(convertedOp)
//                        if (!success) {
//                            return@withContext false
//                        }
//                    }
                } catch (ex: Exception) {
                    Log.e("ShoppingListRemoteDataSource", "Unknown error while performing operation: $ex")
                }
                return@withContext false
            }
        }

        suspend fun update(operation: ShoppingListOperation): Boolean =
            withContext(Dispatchers.IO) {
                try {
                    when (operation) {
                        is ShoppingListOperation.Create -> {
//                            val pendingOperation =
//                                ShoppingListOperationConversionUtil.shoppingListOperationToApiOperation(
//                                    operation,
//                                )
                            val opId = pendingOperationRepository.insert(operation)
                            if (opId < 0L) {
                                Log.e("ShoppingListRemoteDataSource", "Failed to insert operation, continue online...")
                            }
                            executePendingOperations()
//                            val response = shoppingListApi.performOperation(operation = pendingOperation)
//                            if (response.code() != HttpStatusCode.Created.value) {
//                                Log.i(
//                                    "ShoppingListRemoteDataSource",
//                                    "Failed to create list online, trying again with updated user onlineId",
//                                )
//
//                                val currentUser = userRepository.read() ?: throw IllegalStateException("user null after login")
//
//                                if (currentUser.OnlineID == operation.creator.onlineId) {
//                                    Log.e(
//                                        "ShoppingListRemoteDataSource",
//                                        "OnlineID did not change since last request, failed to create list online",
//                                    )
//                                    return@withContext false
//                                }
//                                Log.i(
//                                    "ShoppingListRemoteDataSource",
//                                    "OnlineId of user changed during last operation, trying again with new id",
//                                )
//
//                                operation.creator.onlineId = currentUser.OnlineID
//                                val updatedOperation =
//                                    ShoppingListOperationConversionUtil.shoppingListOperationToApiOperation(
//                                        operation,
//                                    )
//                                pendingOperationRepository.updateOperation(opId, updatedOperation)
//
//                                val response = shoppingListApi.performOperation(operation = updatedOperation)
//
//                                if (response.code() == HttpStatusCode.Created.value) {
//                                    Log.e("ShoppingListRemoteDataSource", "Failed to create list ${operation.title} online")
//                                    return@withContext false
//                                }
//
//                                pendingOperationRepository.deleteAllPendingOperations()
//                            }
                            Log.i("ShoppingListRemoteDataSource", "Successfully created list ${operation.title} online")
                            return@withContext true
                        }
                        is ShoppingListOperation.AddItem -> {
                            val response = shoppingListApi.addItem(operation.listPk.listId, operation.item)
                            response.status == HttpStatusCode.OK
                        }
                        is ShoppingListOperation.AddItemByName -> {
                            // TODO: Update to retrieve correct item
                            val itemToAdd = AppItem("", "", 1, QuantityType.PIECES, false, 1L, opCount = 0)
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
                        is ShoppingListOperation.Delete -> {
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
