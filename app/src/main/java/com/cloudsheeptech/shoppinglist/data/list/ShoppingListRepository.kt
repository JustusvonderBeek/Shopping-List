package com.cloudsheeptech.shoppinglist.data.list

import android.util.Log
import androidx.lifecycle.LiveData
import com.cloudsheeptech.shoppinglist.data.items.AppItem
import com.cloudsheeptech.shoppinglist.data.onlineUser.ListCreator
import com.cloudsheeptech.shoppinglist.data.recipe.ApiIngredient
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import com.cloudsheeptech.shoppinglist.exception.UserNotAuthenticatedException
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
        val newListId = localDataSource.create(newList)
        newList.listId = newListId
        try {
            val success = remoteApi.create(newList)
            if (!success) {
                updateListCreatedBy(newList)
                if (newList.createdBy.onlineId != createdByBeforeOnlineOperation.onlineId) {
                    localDataSource.delete(newListId, createdByBeforeOnlineOperation.onlineId)
                    localDataSource.create(newList)
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

    suspend fun read(
        listId: Long,
        createdBy: Long,
    ): ApiShoppingList? {
        var storedList: ApiShoppingList? = null
        storedList = localDataSource.read(listId, createdBy)
        // Only update the list when we have nothing stored locally
        if (storedList == null) {
            try {
                storedList = remoteApi.read(listId, createdBy)
            } catch (ex: IllegalAccessException) {
                Log.w("ShoppingListRepository", "Ex: $ex")
            } catch (ex: UserNotAuthenticatedException) {
                Log.w("ShoppingListRepository", "User not authenticated: $ex")
            }
        }
        return storedList
    }

    suspend fun readAllOwn(): List<ApiShoppingList> = localDataSource.readAll()

    suspend fun readAllRemote() {
        val allRemoteLists = remoteApi.readAll()
        // TODO: Write an integration method with the locally stored lists
        allRemoteLists.forEach { remoteList ->
            // Create if not exists, update if exists
            val exists = localDataSource.exists(remoteList.listId, remoteList.createdBy.onlineId)
            Log.d(
                "ShoppingListRepository",
                "List ${remoteList.listId} from ${remoteList.createdBy.onlineId} exists: $exists",
            )
            if (!exists) {
                localDataSource.create(remoteList)
            } else {
                localDataSource.update(remoteList)
            }
        }
    }

    // This is only relevant for the overview, therefore the basic infos (title, creator) suffice
    fun readAllLive(): LiveData<List<DbShoppingList>> = localDataSource.readAllLive()

    fun readAllListItemsLive(listId: Long): LiveData<List<AppItem>> {
        return localDataSource.readAllListItemsLive(listId)
    }

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
        localDataSource.update(list)
        var migratedListToNewId = false
        try {
            updateListOnlineAndRetryOnFailure(list)
            if (list.createdBy.onlineId != onlineIdBeforeUpdate) {
                migratedListToNewId = true
                localDataSource.updateCreatedByForList(list.listId, list.createdBy.onlineId)
            }
        } catch (ex: IllegalStateException) {
            Log.w("ShoppingListRepository", "Ex: $ex")
        } catch (ex: UserNotAuthenticatedException) {
            Log.w("ShoppingListRepository", "User not authenticated: $ex")
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
        val updatedLocalList = localDataSource.insertItem(listId, createdBy, item)
        update(updatedLocalList)
    }

    suspend fun insertExistingItem(
        listId: Long,
        createdBy: Long,
        itemId: Long,
    ) {
        val updatedLocalList = localDataSource.insertExistingItem(listId, createdBy, itemId)
        update(updatedLocalList)
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
        val updatedLocalList = localDataSource.toggleItem(listId, createdBy, itemId)
        val migratedToNewId = update(updatedLocalList)
        return migratedToNewId
    }

    suspend fun updateItemCount(
        listId: Long,
        createdBy: Long,
        itemId: Long,
        quantity: Long,
    ): Boolean {
        val updatedLocalList = localDataSource.updateItemCount(listId, createdBy, itemId, quantity)
        val migratedToNewId = update(updatedLocalList)
        return migratedToNewId
    }

    suspend fun delete(
        listId: Long,
        createdBy: Long,
    ) {
        localDataSource.delete(listId, createdBy)
        remoteApi.deleteShoppingList(listId)
    }

    suspend fun deleteAll() {
        localDataSource.deleteAll()
        remoteApi.deleteAll()
    }
}
