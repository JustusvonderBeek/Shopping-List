package com.cloudsheeptech.shoppinglist.list.repo

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.cloudsheeptech.shoppinglist.list.model.AppItem
import com.cloudsheeptech.shoppinglist.list.model.ItemToggleStatus
import com.cloudsheeptech.shoppinglist.list.model.ListCreator
import com.cloudsheeptech.shoppinglist.list.model.QuantityType
import com.cloudsheeptech.shoppinglist.list.model.ShoppingList
import com.cloudsheeptech.shoppinglist.list.model.ShoppingListOperation
import com.cloudsheeptech.shoppinglist.list.model.ShoppingListPK
import com.cloudsheeptech.shoppinglist.list.model.UserNotAuthenticatedException
import com.cloudsheeptech.shoppinglist.list.util.ShoppingListCreatedByUtil
import com.cloudsheeptech.shoppinglist.sharing.repo.OnlineUserRepository
import com.cloudsheeptech.shoppinglist.user.repo.AppUserRepository
import io.ktor.client.network.sockets.SocketTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

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
        private val onlineUserRepository: OnlineUserRepository,
    ) {
        // ----------------------------- Core Functions --------------------------------

        suspend fun read(listPk: ShoppingListPK): ShoppingList? =
            withContext(Dispatchers.IO) {
                localDataSource.read(listPk.listId, listPk.createdBy)
            }

        fun readAllLive(): LiveData<List<ShoppingList>> = localDataSource.readAllLive()

        // TODO: Fully write and test this function
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
                        // TODO: Think about how this updated can be made, what data
                        // is received here and how to update the existing lists, if at all
                        localDataSource.create(remoteList)
                    }
                }
            } catch (ex: Exception) {
                Log.e("ShoppingListRepository", "Failed to read all remote lists: $ex")
            }
        }

        fun readAllListItemsLive(listPk: ShoppingListPK): LiveData<List<AppItem>> =
            localDataSource
                .readAllListItemsLive(listPk.listId, listPk.createdBy)
                .asLiveData(Dispatchers.IO)

        suspend fun exist(listPk: ShoppingListPK): Boolean =
            withContext(Dispatchers.IO) {
                localDataSource.exists(listPk.listId, listPk.createdBy)
            }

        /**
         * Idea of this function would be after the synchronisation with online to perform update
         * I dont know if this interface is needed for this or should be private
         * Even for testing I would prefer the direct methods like addItem
         * Necessary for a list of online operations
         */
        suspend fun update(updateOperations: List<ShoppingListOperation>): ShoppingList? {
            var updatedList: ShoppingList? = null
            for (update in updateOperations) {
                updatedList = localDataSource.update(update)
                if (updatedList == null) {
                    // TODO: How to proceed from here? Is the list missing?
                    // Is the updated somehow failed?
                    // Do I simply have to pull the latest version of the list from online?
                    Log.e("ShoppingListRepository", "Failed to update list")
                    return null
                }
                try {
                    updateListOnlineAndRetryOnFailure(update)
                } catch (ex: IllegalStateException) {
                    Log.e("ShoppingListRepository", "Ex: $ex")
                } catch (ex: UserNotAuthenticatedException) {
                    Log.e("ShoppingListRepository", "User not authenticated: $ex")
                } catch (ex: IllegalArgumentException) {
                    Log.e("ShoppingListRepository", "Failed to update list: $ex")
                } catch (ex: SocketTimeoutException) {
                    Log.e("ShoppingListRepository", "Timeout while updating list: $ex")
                } catch (ex: Exception) {
                    Log.e("ShoppingListRepository", "Failed to perform $update: $ex")
                }
            }
            return updatedList
        }

        private suspend fun updateListOnlineAndRetryOnFailure(
            list: ShoppingListOperation,
            retryCount: Int = 1,
        ): Boolean {
            for (i in retryCount downTo 0) {
                try {
                    val success = remoteDataSource.update(list)
                    if (success) {
                        Log.i("ShoppingListRepository", "Successfully updated list online")
                        return success
                    }
                } catch (ex: IllegalArgumentException) {
                    Log.e("ShoppingListRepository", "Failed to update list online due to wrong input: $ex")
                } catch (ex: Exception) {
                    Log.e("ShoppingListRepository", "Failed to update list online: $ex")
                }
            }
            return false
        }

        suspend fun deleteAll() {
            localDataSource.deleteAll()
            remoteDataSource.deleteAll()
        }

        // ---------------- Convenience Operations -------------------

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
                        "Failed to create list online, might be because the user was created online. Trying again with updated id...",
                    )
                    listUtil.updateListToCurrentUser(newList)
                    if (newList.createdBy.onlineId != createdByBeforeOnlineOperation.onlineId) {
                        localDataSource.updateCreatedByForOwnLists(
                            createdByBeforeOnlineOperation.onlineId,
                            newList.createdBy.onlineId,
                        )
                        Log.i(
                            "ShoppingListRepository",
                            "Updated createdBy to new id ${newList.createdBy.onlineId}",
                        )
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

        suspend fun syncListOnline(listPk: ShoppingListPK): ShoppingList? =
            try {
                // TODO: Implement
                null
            } catch (ex: Exception) {
                Log.e("ShoppingListRepository", "Failed to sync list online: $ex")
                null
            }

        suspend fun renameList(
            listPk: ShoppingListPK,
            newTitle: String,
        ): ShoppingList? {
            return try {
                val updateListTitleOperation =
                    ShoppingListOperation.RenameList(
                        listPk,
                        newTitle,
                    )
                val updatedLocalList = localDataSource.update(updateListTitleOperation)
                if (updatedLocalList == null) {
                    Log.e("ShoppingListRepository", "Failed to update list title for list $listPk")
                    return null
                }
                val success = updateListOnlineAndRetryOnFailure(updateListTitleOperation)
                if (success) {
                    Log.i("ShoppingListRepository", "Successfully updated list title $newTitle of list $listPk online")
                } else {
                    Log.w("ShoppingListRepository", "Failed to set title $newTitle of list $listPk online")
                }
                updatedLocalList
            } catch (ex: Exception) {
                Log.e("ShoppingListRepository", "Failed to update list title to $newTitle: $ex")
                null
            }
        }

        suspend fun insertItem(
            listPk: ShoppingListPK,
            item: AppItem,
        ): ShoppingList? {
            return try {
                val addItemOperation = ShoppingListOperation.AddItem(listPk, item)
                val updatedLocalList =
                    localDataSource.update(addItemOperation)
                if (updatedLocalList == null) {
                    Log.e("ShoppingListRepository", "Failed to add item $item")
                    return null
                }
                val success = updateListOnlineAndRetryOnFailure(addItemOperation)
                if (success) {
                    Log.i("ShoppingListRepository", "Added item $item successfully to list $listPk online")
                } else {
                    Log.w("ShoppingListRepository", "Failed to add item $item into list $listPk online")
                }
                updatedLocalList
            } catch (ex: IllegalArgumentException) {
                Log.e("ShoppingListRepository", "List $listPk not found: $ex")
                null
            } catch (ex: IllegalStateException) {
                Log.e("ShoppingListRepository", "User null after login screen: $ex")
                null
            } catch (ex: Exception) {
                Log.e("ShoppingListRepository", "Failed to insert item ${item.name}: $ex")
                null
            }
        }

        suspend fun insertExistingItem(
            listPk: ShoppingListPK,
            itemName: String,
        ): ShoppingList? {
            return try {
                val addItemOperation = ShoppingListOperation.AddItemByName(listPk, itemName)
                val updatedLocalList = localDataSource.update(addItemOperation)
                if (updatedLocalList == null) {
                    Log.e("ShoppingListRepository", "Failed to add item $itemName")
                    return null
                }
                val success = updateListOnlineAndRetryOnFailure(addItemOperation)
                if (success) {
                    Log.i("ShoppingListRepository", "Successfully added item $itemName into list $listPk online online")
                } else {
                    Log.w("ShoppingListRepository", "Failed to add item $itemName into list $listPk online")
                }
                updatedLocalList
            } catch (ex: Exception) {
                Log.e("ShoppingListRepository", "Failed to add item $itemName: $ex")
                null
            }
        }

        suspend fun removeItemByName(
            listPk: ShoppingListPK,
            itemName: String,
        ): ShoppingList? {
            return try {
                val removeItemOperation = ShoppingListOperation.RemoveItemByName(listPk, itemName)
                val updatedLocalList = localDataSource.update(removeItemOperation)
                if (updatedLocalList == null) {
                    Log.e("ShoppingListRepository", "Failed to remove item $itemName")
                    return null
                }
                val success = updateListOnlineAndRetryOnFailure(removeItemOperation)
                if (success) {
                    Log.i("ShoppingListRepository", "Successfully removed item $itemName from list $listPk online")
                } else {
                    Log.w("ShoppingListRepository", "Failed to remove item $itemName from list $listPk online")
                }
                updatedLocalList
            } catch (ex: Exception) {
                Log.e("ShoppingListRepository", "Failed to remove item $itemName: $ex")
                null
            }
        }

        suspend fun setItemQuantity(
            listPk: ShoppingListPK,
            itemName: String,
            quantity: Long,
            quantityType: QuantityType? = null,
        ): ShoppingList? {
            return try {
                val quantityOperation =
                    ShoppingListOperation.ChangeQuantityOfItem(
                        listPk,
                        itemName,
                        quantity,
                        quantityType,
                    )
                val updatedLocalList =
                    localDataSource.update(quantityOperation)
                if (updatedLocalList == null) {
                    Log.e("ShoppingListRepository", "Failed to update quantity for item $itemName")
                    return null
                }
                val success = updateListOnlineAndRetryOnFailure(quantityOperation)
                if (success) {
                    Log.i(
                        "ShoppingListRepository",
                        "Successfully set quantity $quantity (${quantityType ?: "---"}) of $itemName in list $listPk online",
                    )
                } else {
                    Log.w(
                        "ShoppingListRepository",
                        "Failed to set quantity $quantity (${quantityType ?: "---"}) of $itemName in list $listPk online",
                    )
                }
                updatedLocalList
            } catch (ex: IllegalArgumentException) {
                Log.e("ShoppingListRepository", "List $listPk not found: $ex")
                null
            } catch (ex: UserNotAuthenticatedException) {
                Log.e("ShoppingListRepository", "User not authenticated: $ex")
                null
            } catch (ex: Exception) {
                Log.e("ShoppingListRepository", "Failed to set quantity for item $itemName: $ex")
                null
            }
        }

        suspend fun setItemToggle(
            listPk: ShoppingListPK,
            itemName: String,
            toggleStatus: ItemToggleStatus = ItemToggleStatus.TOGGLE,
        ): ShoppingList? {
            return try {
                val toggleOperation =
                    ShoppingListOperation.SetItemCheckedStatus(
                        listPk,
                        itemName,
                        toggleStatus,
                    )
                val updatedLocalList = localDataSource.update(toggleOperation)
                if (updatedLocalList == null) {
                    Log.e("ShoppingListRepository", "Failed to toggle item $itemName in list $listPk")
                    return null
                }
                val success = updateListOnlineAndRetryOnFailure(toggleOperation)
                if (success) {
                    Log.i(
                        "ShoppingListRepository",
                        "Successfully set toggle $toggleStatus of $itemName in list $listPk online",
                    )
                } else {
                    Log.w(
                        "ShoppingListRepository",
                        "Failed to set toggle $toggleStatus of $itemName in list $listPk online",
                    )
                }
                updatedLocalList
            } catch (ex: IllegalArgumentException) {
                Log.e("ShoppingListRepository", "List $listPk not found: $ex")
                null
            } catch (ex: UserNotAuthenticatedException) {
                Log.e("ShoppingListRepository", "User not authenticated: $ex")
                null
            } catch (ex: Exception) {
                Log.e("ShoppingListRepository", "Failed to toggle item: $ex")
                null
            }
        }

        suspend fun removeAllCheckedItemsFromList(listPk: ShoppingListPK): ShoppingList? {
            return try {
                val currentList = localDataSource.read(listPk.listId, listPk.createdBy)
                if (currentList == null) {
                    Log.e("ShoppingListRepository", "Skipping removing checked items from list $listPk because the list cannot be found")
                    return null
                }
                val checkedItems = currentList.items.filter { item -> item.checked }
                val removeOperations = mutableListOf<ShoppingListOperation>()
                for (item in checkedItems) {
                    removeOperations.add(
                        ShoppingListOperation.RemoveItemByName(
                            listPk,
                            item.name,
                        ),
                    )
                }
                return update(removeOperations)
            } catch (ex: Exception) {
                Log.e("ShoppingListRepository", "Failed to remove all checked items from list $listPk: $ex")
                null
            }
        }

        suspend fun deleteList(listPk: ShoppingListPK): Boolean {
            return try {
                val removeOperation =
                    ShoppingListOperation.Delete(
                        listPk,
                    )
                val updatedLocalList = localDataSource.update(removeOperation)
                if (updatedLocalList != null) {
                    Log.e("ShoppingListRepository", "Failed to delete list $listPk")
                    return false
                }
                val success = remoteDataSource.update(removeOperation)
                if (success) {
                    Log.i(
                        "ShoppingListRepository",
                        "Successfully deleted list $listPk online",
                    )
                    return true
                } else {
                    Log.w(
                        "ShoppingListRepository",
                        "Failed to delete list $listPk online",
                    )
                }
                false
            } catch (ex: Exception) {
                Log.e("ShoppingListRepository", "Failed to delete list: $listPk: $ex")
                false
            }
        }

        suspend fun updateCreatedByToCurrentId() {
            val currUser = userRepository.read() ?: throw IllegalStateException("user null after login")
            if (currUser.OnlineID == 0L) {
                Log.i("ShoppingListRepository", "Skipping update of createdBy because local user onlineId is 0")
                return
            }
            try {
                localDataSource.updateCreatedByForOwnLists(0L, currUser.OnlineID)
                // TODO: Careful with this and the operation semantics. It could break the IDs and update statements...?
                val allLists = localDataSource.readAll()
                allLists.forEach { list -> remoteDataSource.create(list) }
            } catch (ex: Exception) {
                Log.e("ShoppingListRepository", "Failed to update createdBy to currentId: $ex")
            }
        }

        suspend fun resetCreatedByForOwnLists() {
            withContext(Dispatchers.IO) {
                val user = userRepository.read() ?: throw IllegalStateException("user null after login")
                try {
                    localDataSource.updateCreatedByForOwnLists(user.OnlineID, 0L)
                } catch (ex: Exception) {
                    Log.e("ShoppingListRepository", "Failed to reset created by to 0: $ex")
                }
            }
        }
    }
