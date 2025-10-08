package com.cloudsheeptech.shoppinglist.data.list

import android.util.Log
import androidx.lifecycle.LiveData
import com.cloudsheeptech.shoppinglist.data.items.AppItem
import com.cloudsheeptech.shoppinglist.data.onlineUser.ListCreator
import com.cloudsheeptech.shoppinglist.data.recipe.ApiIngredient
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import com.cloudsheeptech.shoppinglist.exception.UserNotAuthenticatedException
import io.ktor.client.network.sockets.SocketTimeoutException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class implements the main handling of app wide shopping lists.
 * This includes the creation of new lists in a displayable format
 * and the deserialization from an API list to into the individual parts
 * that can be stored by the application.
 */
@Singleton
class ShoppingListRepository
    @Inject
    constructor(
        private val localDataSource: ShoppingListLocalDataSource,
        private val remoteApi: ShoppingListRemoteDataSource,
        private val userRepository: AppUserRepository,
    ) {
        // TODO: This should only ever happen once after the creation of a new user
        init {
            CoroutineScope(Dispatchers.Main + Job()).launch {
                Log.d("ShoppingListRepository", "Starting updating process")
                updateCreatedByToCurrentId()
            }
        }

        // ------------------------------------------------------------------------------
        // Creation of a new list + Insertion + Update
        // ------------------------------------------------------------------------------

        private fun updateListCreatedBy(list: ApiShoppingList) {
            if (list.createdBy.onlineId != 0L) {
                return
            }
            val user =
                userRepository.read() ?: throw IllegalStateException("user null after login screen")
            list.createdBy.onlineId = user.OnlineID
            list.createdBy.username = user.Username
            list.items.map { item -> item.addedBy = user.OnlineID }
        }

        suspend fun create(title: String): ApiShoppingList {
            val now = OffsetDateTime.now()
            val newList =
                ApiShoppingList(
                    listId = 0L,
                    title = title,
                    createdBy = ListCreator(0, ""),
                    createdAt = now,
                    lastUpdated = now,
                    items = mutableListOf(),
                    1L,
                )
            updateListCreatedBy(newList)
            val createdByBeforeOnlineOperation = newList.createdBy
            val newListId = localDataSource.createOrUpdate(newList)
            newList.listId = newListId
            try {
                val success = remoteApi.create(newList)
                if (!success) {
                    updateListCreatedBy(newList)
                    if (newList.createdBy.onlineId != createdByBeforeOnlineOperation.onlineId) {
                        localDataSource.updateCreatedByForList(
                            newList.createdBy.onlineId,
                            createdByBeforeOnlineOperation.onlineId,
                        )
                    }
                    remoteApi.create(newList)
                }
            } catch (ex: IllegalAccessException) {
                Log.w("ShoppingListRepository", "Ex: $ex")
            } catch (ex: UserNotAuthenticatedException) {
                Log.w("ShoppingListRepository", "User not authenticated: $ex")
            }
            return newList
        }

        private suspend fun createRemote(list: ApiShoppingList): Boolean {
            try {
                return remoteApi.create(list)
            } catch (ex: IllegalArgumentException) {
                Log.w("ShoppingListRepository", "List already exists: $ex")
            } catch (ex: Exception) {
                Log.w("ShoppingListRepository", "Failed to create list: $ex")
            }
            return false
        }

        suspend fun read(
            listId: Long,
            createdBy: Long,
        ): ApiShoppingList? {
            var latestList: ApiShoppingList? = null
            try {
                val storedList = localDataSource.read(listId, createdBy)
                val remoteList = remoteApi.read(listId, createdBy)
                if (storedList == null && remoteList == null) {
                    Log.e("ShoppingListRepository", "List $listId from $createdBy not found")
                    return null
                } else if (storedList == null && remoteList != null) {
                    latestList = remoteList
                    Log.d(
                        "ShoppingListRepository",
                        "List $listId from $createdBy not found locally. Creating new list",
                    )
                    createRemote(latestList)
                    return latestList
                } else if (storedList != null && remoteList == null) {
                    latestList = storedList
                    Log.d(
                        "ShoppingListRepository",
                        "List $listId from $createdBy not found online. Update skipped",
                    )
                    return latestList
                } else if (storedList != null && remoteList != null) {
                    latestList = compareAndGetLatestList(storedList, remoteList)
                }
                // We don't want to propagate an update we just received back online
                localDataSource.update(latestList!!)
                Log.d(
                    "ShoppingListRepository",
                    "Updated List $listId from $createdBy to latest version ${latestList.version}",
                )
            } catch (ex: IllegalAccessException) {
                Log.w("ShoppingListRepository", "User not allowed to acces list: $ex")
            } catch (ex: UserNotAuthenticatedException) {
                Log.w("ShoppingListRepository", "User not authenticated: $ex")
            } catch (ex: Exception) {
                Log.e("ShoppingListRepository", "Unknown error while reading list: $ex")
            }
            return latestList
        }

        /**
         * @return The list with the highest version. If both versions are equal list1 is returned
         */
        private fun compareAndGetLatestList(
            list1: ApiShoppingList,
            list2: ApiShoppingList,
        ): ApiShoppingList =
            when (list1.compare(list2)) {
                -1 -> list2
                else -> list1
            }

        suspend fun readAllOwn(): List<ApiShoppingList> = localDataSource.readAll()

        suspend fun readAllRemote() {
            try {
                val allRemoteLists = remoteApi.readAll()
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
                        localDataSource.createOrUpdate(remoteList)
                    } else {
                        localDataSource.update(remoteList)
                    }
                }
            } catch (ex: Exception) {
                Log.e("ShoppingListRepository", "Failed to read all remote lists: $ex")
            }
        }

        // This is only relevant for the overview, therefore the basic infos (title, creator) suffice
        fun readAllLive(): LiveData<List<DbShoppingList>> = localDataSource.readAllLive()

        fun readAllListItemsLive(
            listId: Long,
            createdBy: Long,
        ): LiveData<List<AppItem>> = localDataSource.readAllListItemsLive(listId, createdBy)

        suspend fun exist(
            listId: Long,
            createdBy: Long,
        ): Boolean {
            var exists = false
            withContext(Dispatchers.IO) {
                exists = localDataSource.exists(listId, createdBy)
            }
            return exists
        }

        suspend fun update(list: ApiShoppingList): Boolean {
            val onlineIdBeforeUpdate = list.createdBy.onlineId
            list.version++
            var migratedListToNewId = false
            try {
                localDataSource.update(list)
                updateListOnlineAndRetryOnFailure(list)
                if (list.createdBy.onlineId != onlineIdBeforeUpdate) {
                    migratedListToNewId = true
                    localDataSource.updateCreatedByForList(list.listId, list.createdBy.onlineId)
                }
            } catch (ex: IllegalStateException) {
                Log.e("ShoppingListRepository", "Ex: $ex")
            } catch (ex: UserNotAuthenticatedException) {
                Log.e("ShoppingListRepository", "User not authenticated: $ex")
            } catch (ex: IllegalArgumentException) {
                Log.e("ShoppingListRepository", "Failed to update list: $ex")
            } catch (ex: SocketTimeoutException) {
                Log.e("ShoppingListRepository", "Timeout while updating list: $ex")
            }
            return migratedListToNewId
        }

        private suspend fun updateListOnlineAndRetryOnFailure(list: ApiShoppingList) {
            var success = remoteApi.update(list)
            if (!success) {
                updateListCreatedBy(list)
                success = remoteApi.update(list)
            }
            if (!success) {
                success = remoteApi.create(list)
            }
            if (success) {
                Log.i("ShoppingListRepository", "The list ${list.listId} was updated online")
            } else {
                Log.i("ShoppingListRepository", "Updating the list ${list.listId} online failed")
            }
        }

        suspend fun resetCreatedBy() {
            localDataSource.resetCreatedBy()
        }

        suspend fun updateCreatedByToCurrentId() {
            val currUser = userRepository.read()
            if (currUser == null) {
                Log.d("ShoppingListRepository", "user is null")
                return
            }
            if (currUser.OnlineID == 0L) {
                return
            }
            localDataSource.updateCreatedById(0L)
            val allLists = localDataSource.readAll()
            allLists.forEach { list -> remoteApi.create(list) }
        }

        suspend fun resetCreatedByForOwnLists() {
            localDataSource.resetCreatedBy()
        }

        suspend fun resetAddedByForOwnLists() {
            val currUser = userRepository.read() ?: return
            if (currUser.OnlineID == 0L) {
                Log.d("ShoppingListRepository", "OnlineID is 0")
                return
            }
            localDataSource.resetAddedBy(currUser.OnlineID)
        }

        suspend fun insertItem(
            listId: Long,
            createdBy: Long,
            item: AppItem,
        ) {
            try {
                val updatedLocalList = localDataSource.insertItem(listId, createdBy, item)
//                update(updatedLocalList)
                updateListOnlineAndRetryOnFailure(updatedLocalList)
            } catch (ex: IllegalArgumentException) {
                Log.e("ShoppingListRepository", "List $listId from $createdBy not found")
            } catch (ex: IllegalStateException) {
                Log.e("ShoppingListRepository", "User null after login screen")
                throw ex
            }
        }

        suspend fun insertExistingItem(
            listId: Long,
            createdBy: Long,
            itemId: Long,
        ) {
            val updatedLocalList = localDataSource.insertExistingItem(listId, createdBy, itemId)
//            update(updatedLocalList)
            updateListOnlineAndRetryOnFailure(updatedLocalList)
        }

        suspend fun removeItem(
            listId: Long,
            createdBy: Long,
            itemId: Long,
        ) {
            try {
                val updatedLocalList = localDataSource.removeItem(listId, createdBy, itemId)
//                update(updatedLocalList)
                updateListOnlineAndRetryOnFailure(updatedLocalList)
            } catch (ex: Exception) {
            }
        }

        // TODO: Fix the signature of this function (ApiIngredients -> DbItems ??? )
        suspend fun addAll(
            listId: Long,
            createdBy: Long,
            ingredients: List<ApiIngredient>,
        ) {
            val updatedLocalList = localDataSource.addAll(listId, createdBy, ingredients)
            update(updatedLocalList)
        }

        suspend fun toggleItem(
            listId: Long,
            createdBy: Long,
            itemId: Long,
        ): Boolean {
            try {
                val updatedLocalList = localDataSource.toggleItem(listId, createdBy, itemId)
                updateListOnlineAndRetryOnFailure(updatedLocalList)
                //                val migratedToNewId = update(updatedLocalList)
//                return migratedToNewId
            } catch (ex: IllegalArgumentException) {
                Log.e("ShoppingListRepository", "List $listId from $createdBy not found")
            } catch (ex: UserNotAuthenticatedException) {
                Log.e("ShoppingListRepository", "User not authenticated: $ex")
            } catch (ex: Exception) {
                Log.e("ShoppingListRepository", "Failed to toggle item: $ex")
            }
            return false
        }

        suspend fun updateItemCount(
            listId: Long,
            createdBy: Long,
            itemId: Long,
            quantity: Long,
        ): Boolean {
            try {
                val updatedLocalList =
                    localDataSource.updateItemCount(listId, createdBy, itemId, quantity)
//                val migratedToNewId = update(updatedLocalList)
                updateListOnlineAndRetryOnFailure(updatedLocalList)
//                return migratedToNewId
            } catch (ex: IllegalArgumentException) {
                Log.e("ShoppingListRepository", "List $listId from $createdBy not found")
            } catch (ex: UserNotAuthenticatedException) {
                Log.e("ShoppingListRepository", "User not authenticated: $ex")
            }
            return false
        }

        suspend fun updateTitle(
            listId: Long,
            createdBy: Long,
            title: String,
        ): Boolean {
            localDataSource.updateTitle(listId, createdBy, title)
            if (createdBy == 0L) {
                Log.i("ShoppingListRepository", "User not registered online, skipping remote update")
                return true
            }
            try {
                val updatedList =
                    read(listId, createdBy) ?: throw IllegalArgumentException("list does not exist")
                updateListOnlineAndRetryOnFailure(updatedList)
                return true
            } catch (ex: Exception) {
                Log.e("ShoppingListRepository", "Failed to update list remote: $ex")
            }
            return false
        }

        suspend fun delete(
            listId: Long,
            createdBy: Long,
        ) {
            try {
                localDataSource.delete(listId, createdBy)
                remoteApi.deleteShoppingList(listId, createdBy)
            } catch (ex: Exception) {
                Log.e("ShoppingListRepository", "Failed to delete list: $ex")
            }
        }

        suspend fun deleteAllCheckedItems(
            listId: Long,
            createdBy: Long,
        ) {
            try {
                val updatedList = localDataSource.remoteCheckedItems(listId, createdBy)
                remoteApi.update(updatedList)
            } catch (ex: Exception) {
                Log.e("ShoppingListRepository", "Failed to delete all checked items: $ex")
            }
        }

        suspend fun deleteAll() {
            localDataSource.deleteAll()
            remoteApi.deleteAll()
        }
    }
