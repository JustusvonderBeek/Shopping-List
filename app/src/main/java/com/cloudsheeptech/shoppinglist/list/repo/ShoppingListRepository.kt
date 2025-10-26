package com.cloudsheeptech.shoppinglist.list.repo

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.cloudsheeptech.shoppinglist.data.recipe.ApiIngredient
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import com.cloudsheeptech.shoppinglist.list.model.AppItem
import com.cloudsheeptech.shoppinglist.list.model.ItemToggleStatus
import com.cloudsheeptech.shoppinglist.list.model.ListCreator
import com.cloudsheeptech.shoppinglist.list.model.QuantityType
import com.cloudsheeptech.shoppinglist.list.model.ShoppingList
import com.cloudsheeptech.shoppinglist.list.model.ShoppingListOperation
import com.cloudsheeptech.shoppinglist.list.model.ShoppingListPK
import com.cloudsheeptech.shoppinglist.list.model.UserNotAuthenticatedException
import com.cloudsheeptech.shoppinglist.list.util.ShoppingListCreatedByUtil
import io.ktor.client.network.sockets.SocketTimeoutException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.EmptyCoroutineContext

/**
 * This class implements the main handling of app wide shopping lists.
 * This includes the creation of new lists in a displayable format
 * and updating individual parts of the list
 */
@Singleton
class ShoppingListRepository
    @Inject
    constructor(
        private val listUtil: ShoppingListCreatedByUtil,
        private val localDataSource: ShoppingListLocalDataSource,
        private val remoteDataSource: ShoppingListRemoteDataSource,
        private val userRepository: AppUserRepository,
    ) {
        init {
            CoroutineScope(Dispatchers.Main + Job()).launch {
                Log.d("ShoppingListRepository", "Starting updating process")
                updateCreatedByToCurrentId()
            }
        }

        // ------------------------------------------------------------------------------
        // Creation of a new list
        // ------------------------------------------------------------------------------

        suspend fun create(title: String): ShoppingList {
            var newList =
                ShoppingList(
                    listId = 0L, // Created by DB
                    createdBy = ListCreator(0, ""),
                    title = title,
                    synchronized = OffsetDateTime.now(),
                    items = mutableListOf(),
                )
            listUtil.updateListToCurrentUser(newList)

            val createdByBeforeOnlineOperation = newList.createdBy
            newList = localDataSource.create(newList)
            Log.i("ShoppingListRepository", "Stored list $newList offline, creating online next...")
            try {
                var success = remoteDataSource.create(newList)
                if (!success) {
                    Log.i(
                        "ShoppingListRepository",
                        "Failed to create list online, might be because a new user was created. Trying again with updated id...",
                    )
                    listUtil.updateListToCurrentUser(newList)
                    if (newList.createdBy.onlineId != createdByBeforeOnlineOperation.onlineId) {
                        localDataSource.updateCreatedByForOwnLists(
                            createdByBeforeOnlineOperation.onlineId,
                            newList.createdBy.onlineId,
                        )
                        Log.i("ShoppingListRepository", "Updated createdBy to new id ${newList.createdBy.onlineId}")
                    }
                    success = remoteDataSource.create(newList)
                    if (!success) {
                        Log.e(
                            "ShoppingListRepository",
                            "Failed to create new list with new createdBy ${newList.createdBy.onlineId}. Is the user created online?",
                        )
                    }
                }
                if (success) {
                    Log.d("ShoppingListRepository", "Successfully create list $newList online")
                }
            } catch (ex: IllegalAccessException) {
                Log.w("ShoppingListRepository", "Ex: $ex")
            } catch (ex: UserNotAuthenticatedException) {
                Log.w("ShoppingListRepository", "User not authenticated: $ex")
            }
            return newList
        }

        suspend fun read(
            listId: Long,
            createdBy: Long,
        ): ShoppingList? =
            withContext(Dispatchers.IO) {
                localDataSource.read(listId, createdBy)
            }

        suspend fun readAllRemote() {
            try {
                val allRemoteLists = remoteDataSource.readAll()
                // TODO: Write an integration method with the locally stored lists
                allRemoteLists.forEach { remoteList ->
                    // Create if not exists, update if exists
                    val exists =
                        localDataSource.exists(remoteList.listId, remoteList.createdBy.onlineId)
                    Log.d(
                        "ShoppingListRepository",
                        "List ${remoteList.listId} from ${remoteList.createdBy.onlineId} exists: $exists",
                    )
                    if (!exists) {
                        localDataSource.create(remoteList)
                    } else {
                        localDataSource.create(remoteList)
                    }
                }
            } catch (ex: Exception) {
                Log.e("ShoppingListRepository", "Failed to read all remote lists: $ex")
            }
        }

        // This is only relevant for the overview, therefore the basic infos (title, creator) suffice
        fun readAllLive(): LiveData<List<ShoppingList>> = localDataSource.readAllLive()

        fun readAllListItemsLive(
            listId: Long,
            createdBy: Long,
        ): LiveData<List<AppItem>> = localDataSource.readAllListItemsLive(listId, createdBy).asLiveData(EmptyCoroutineContext, 5000L)

        suspend fun exist(
            listId: Long,
            createdBy: Long,
        ): Boolean =
            withContext(Dispatchers.IO) {
                localDataSource.exists(listId, createdBy)
            }

        /**
         * Idea of this function would be after the synchronisation with online to perform update
         * I dont know if this interface is needed for this or should be private
         * Even for testing I would prefer the direct methods like addItem
         */
        suspend fun update(updateOperations: List<ShoppingListOperation>): ShoppingList? {
            val updatedList = localDataSource.applyUpdates(updateOperations)
            if (updatedList == null) {
                Log.e("ShoppingListRepository", "Failed to update list")
                return null
            }
            try {
                updateListOnlineAndRetryOnFailure(updatedList)
            } catch (ex: IllegalStateException) {
                Log.e("ShoppingListRepository", "Ex: $ex")
            } catch (ex: UserNotAuthenticatedException) {
                Log.e("ShoppingListRepository", "User not authenticated: $ex")
            } catch (ex: IllegalArgumentException) {
                Log.e("ShoppingListRepository", "Failed to update list: $ex")
            } catch (ex: SocketTimeoutException) {
                Log.e("ShoppingListRepository", "Timeout while updating list: $ex")
            }
            return updatedList
        }

        private suspend fun updateListOnlineAndRetryOnFailure(list: ShoppingList) {
            var success = remoteDataSource.update(list)
            if (!success) {
                Log.i("ShoppingListRepository", "Updating list $list online failed, trying with updated userId")
                listUtil.updateListToCurrentUser(list)
                success = remoteDataSource.update(list)
            }
            if (!success) {
                Log.i("ShoppingListRepository", "Updating list failed, trying to create list online")
                success = remoteDataSource.create(list)
            }
            if (success) {
                Log.i("ShoppingListRepository", "The list ${list.listId} was updated online")
            } else {
                Log.i("ShoppingListRepository", "Updating the list ${list.listId} online failed")
            }
        }

        suspend fun resetCreatedBy() {
            withContext(Dispatchers.IO) {
                val user = userRepository.read() ?: throw IllegalStateException("user null after login")
                localDataSource.updateCreatedByForOwnLists(0L, user.OnlineID)
            }
        }

        suspend fun updateCreatedByToCurrentId() {
            val currUser = userRepository.read() ?: throw IllegalStateException("user null after login")
            if (currUser.OnlineID == 0L) {
                return
            }
            localDataSource.updateCreatedByForOwnLists(0L, currUser.OnlineID)
            val allLists = localDataSource.readAll()
            allLists.forEach { list -> remoteDataSource.create(list) }
        }

        suspend fun resetCreatedByForOwnLists() {
            withContext(Dispatchers.IO) {
                val user = userRepository.read() ?: throw IllegalStateException("user null after login")
                localDataSource.updateCreatedByForOwnLists(user.OnlineID, 0L)
            }
        }

        // ---------------- Operations -------------------

        suspend fun insertItem(
            listPK: ShoppingListPK,
            item: AppItem,
        ) {
            try {
                val addItemOperation = ShoppingListOperation.AddItem(item, listPK)
                val updatedLocalList = localDataSource.update(addItemOperation)
                if (updatedLocalList == null) {
                    throw IllegalArgumentException("failed to update list, maybe because list doesn't exist?")
                }
                updateListOnlineAndRetryOnFailure(updatedLocalList)
            } catch (ex: IllegalArgumentException) {
                Log.e("ShoppingListRepository", "List $listPK not found: $ex")
            } catch (ex: IllegalStateException) {
                Log.e("ShoppingListRepository", "User null after login screen: $ex")
                throw ex
            } catch (ex: Exception) {
                Log.e("ShoppingListRepository", "Failed to insert item ${item.id}: $ex")
            }
        }

        suspend fun insertExistingItem(
            itemId: Long,
            listPk: ShoppingListPK,
        ) {
            try {
                val addItemOperation = ShoppingListOperation.AddItemById(itemId, listPk)
                val updatedLocalList = localDataSource.update(addItemOperation)
                if (updatedLocalList == null) {
                    Log.e("ShoppingListRepository", "Failed to add item $itemId")
                    return
                }
                updateListOnlineAndRetryOnFailure(updatedLocalList)
            } catch (ex: Exception) {
                Log.e("ShoppingListRepository", "Failed to add item $itemId: $ex")
            }
        }

        suspend fun removeItem(
            itemId: Long,
            listPK: ShoppingListPK,
        ) {
            try {
                val removeItemOperation = ShoppingListOperation.RemoveItemById(itemId, listPK)
                val updatedLocalList = localDataSource.update(removeItemOperation)
                if (updatedLocalList == null) {
                    Log.e("ShoppingListRepository", "Failed to remove item $itemId")
                    return
                }
                updateListOnlineAndRetryOnFailure(updatedLocalList)
            } catch (ex: Exception) {
                Log.e("ShoppingListRepository", "Failed to remove item $itemId: $ex")
            }
        }

        suspend fun updateItemCount(
            listPK: ShoppingListPK,
            itemId: Long,
            quantity: Long,
            quantityType: QuantityType? = null,
        ): Boolean {
            try {
                val quantityOperation = ShoppingListOperation.ChangeQuantityOfItem(itemId, quantity, quantityType, listPK)
                val updatedLocalList =
                    localDataSource.update(quantityOperation)
                if (updatedLocalList == null) {
                    Log.e("ShoppingListRepository", "Failed to update quantity for item $itemId")
                    return false
                }
                updateListOnlineAndRetryOnFailure(updatedLocalList)
                return true
            } catch (ex: IllegalArgumentException) {
                Log.e("ShoppingListRepository", "List $listPK not found")
            } catch (ex: UserNotAuthenticatedException) {
                Log.e("ShoppingListRepository", "User not authenticated: $ex")
            } catch (ex: Exception) {
                Log.e("ShoppingListRepository", "Failed to update quantity for item $itemId: $ex")
            }
            return false
        }

        // TODO: Fix the signature of this function (ApiIngredients -> DbItems ??? )
        suspend fun addAll(
            listId: Long,
            createdBy: Long,
            ingredients: List<ApiIngredient>,
        ) {
//            val updatedLocalList = localDataSource.addAll(listId, createdBy, ingredients)
//            update(updatedLocalList)
        }

        suspend fun toggleItem(
            itemId: Long,
            listPk: ShoppingListPK,
        ): Boolean {
            try {
                val toggleOperation =
                    ShoppingListOperation.SetItemCheckedStatus(
                        itemId,
                        ItemToggleStatus.TOGGLE,
                        listPk,
                    )
                val updatedLocalList = localDataSource.update(toggleOperation)
                if (updatedLocalList == null) {
                    Log.e("ShoppingListRepository", "Failed to toggle item $itemId in list $listPk")
                    return false
                }
                updateListOnlineAndRetryOnFailure(updatedLocalList)
                return true
            } catch (ex: IllegalArgumentException) {
                Log.e("ShoppingListRepository", "List $listPk not found")
            } catch (ex: UserNotAuthenticatedException) {
                Log.e("ShoppingListRepository", "User not authenticated: $ex")
            } catch (ex: Exception) {
                Log.e("ShoppingListRepository", "Failed to toggle item: $ex")
            }
            return false
        }

        suspend fun updateTitle(
            title: String,
            listPk: ShoppingListPK,
        ): Boolean {
            val updateListTitleOperation =
                ShoppingListOperation.RenameList(
                    title,
                    listPk,
                )
            val updatedList = localDataSource.update(updateListTitleOperation)
            if (updatedList == null) {
                Log.e("ShoppingListRepository", "Failed to update list title for list $listPk")
                return false
            }
            try {
                updateListOnlineAndRetryOnFailure(updatedList)
                return true
            } catch (ex: Exception) {
                Log.e("ShoppingListRepository", "Failed to update list title online: $ex")
            }
            return false
        }

        suspend fun delete(
            listId: Long,
            createdBy: Long,
        ) {
            try {
                localDataSource.delete(listId, createdBy)
                remoteDataSource.deleteShoppingList(listId, createdBy)
            } catch (ex: Exception) {
                Log.e("ShoppingListRepository", "Failed to delete list: $ex")
            }
        }

        suspend fun deleteAllCheckedItems(
            listId: Long,
            createdBy: Long,
        ) {
            try {
//                val updatedList = localDataSource.remoteCheckedItems(listId, createdBy)
//                remoteDataSource.update(updatedList)
            } catch (ex: Exception) {
                Log.e("ShoppingListRepository", "Failed to delete all checked items: $ex")
            }
        }

        suspend fun deleteAll() {
            localDataSource.deleteAll()
            remoteDataSource.deleteAll()
        }
    }
