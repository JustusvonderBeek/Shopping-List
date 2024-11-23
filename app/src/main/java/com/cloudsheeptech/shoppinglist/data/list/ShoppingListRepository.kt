package com.cloudsheeptech.shoppinglist.data.list

import android.util.Log
import androidx.lifecycle.LiveData
import com.cloudsheeptech.shoppinglist.data.items.AppItem
import com.cloudsheeptech.shoppinglist.data.onlineUser.ListCreator
import com.cloudsheeptech.shoppinglist.data.recipe.ApiIngredient
import com.cloudsheeptech.shoppinglist.data.sharing.ShareUserPreview
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
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
            storedList = remoteApi.read(listId, createdBy)
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

    suspend fun update(list: ApiShoppingList) {
        localDataSource.update(list)
        try {
            updateListOnlineAndRetryOnFailure(list)
        } catch (ex: IllegalStateException) {
            val user = userRepository.read()
            list.createdBy.onlineId = user!!.OnlineID
            localDataSource.update(list)
            remoteApi.update(list)
        }
    }

    private suspend fun updateListOnlineAndRetryOnFailure(list: ApiShoppingList) {
        var success = remoteApi.update(list)
        if (!success) {
            updateListCreatedBy(list)
            success = remoteApi.update(list)
        }
        if (!success) {
            remoteApi.create(list)
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

    suspend fun insertItem(
        listId: Long,
        createdBy: Long,
        item: AppItem,
    ) {
        val updatedLocalList = localDataSource.insertItem(listId, createdBy, item)
        remoteApi.update(updatedLocalList)
    }

    suspend fun insertExistingItem(
        listId: Long,
        createdBy: Long,
        itemId: Long,
    ) {
        val updatedLocalList = localDataSource.insertExistingItem(listId, createdBy, itemId)
        remoteApi.update(updatedLocalList)
    }

    // TODO: Fix the signature of this function (ApiIngredients -> DbItems ??? )
    suspend fun addAll(
        listId: Long,
        createdBy: Long,
        ingredients: List<ApiIngredient>,
    ) {
        val updatedLocalList = localDataSource.addAll(listId, createdBy, ingredients)
        remoteApi.update(updatedLocalList)
    }

    suspend fun toggleItem(
        listId: Long,
        createdBy: Long,
        itemId: Long,
    ) {
        val updatedLocalList = localDataSource.toggleItem(listId, createdBy, itemId)
        remoteApi.update(updatedLocalList)
    }

    suspend fun updateItemCount(
        listId: Long,
        createdBy: Long,
        itemId: Long,
        quantity: Long,
    ) {
        val updatedLocalList = localDataSource.updateItemCount(listId, createdBy, itemId, quantity)
        try {
            updateListOnlineAndRetryOnFailure(updatedLocalList)
        } catch (ex: IllegalAccessError) {
            val user = userRepository.read()
            updatedLocalList.createdBy.onlineId = user!!.OnlineID
            localDataSource.update(updatedLocalList)
            remoteApi.update(updatedLocalList)
        }
    }

    suspend fun delete(
        listId: Long,
        createdBy: Long,
    ) {
        localDataSource.delete(listId, createdBy)
        remoteApi.deleteShoppingList(listId)
    }

    // TODO: Implement the functions below here or in other repos

    suspend fun updateCreatedByForOwnLists() {
        withContext(Dispatchers.IO) {
            val currUser = userRepository.read() ?: return@withContext
            // We don't need to update the list if the id is still 0
            if (currUser.OnlineID == 0L) {
                return@withContext
            }
            val allLists = localDataSource.readAll()
            allLists.forEach { list ->
                if (list.createdBy.onlineId == 0L) {
                    // This list can only be created by the local user, update
                    list.createdBy.onlineId = currUser.OnlineID
                    localDataSource.update(list)
                    remoteApi.create(list)
                }
            }
//            val mappings = mappingDao.getMappingsForList(listId, 0L)
//            if (mappings.isEmpty())
//                return@withContext
//            for (mapping in mappings) {
//                deleteMappingInDatabase(mapping.ItemID, mapping.ListID, 0L)
//                mapping.CreatedBy = createdBy
//                mapping.AddedBy = createdBy
//                mapping.ListID = moveToId
//                insertMappingInDatabase(mapping)
//            }
        }
    }

    private suspend fun deleteAllCheckedMappingsForListId(listId: Long) {
        withContext(Dispatchers.IO) {
//            mappingDao.deleteCheckedMappingsForListId(listId)
        }
    }

    private suspend fun deleteAllMappingsForListId(
        listId: Long,
        createdBy: Long,
    ) {
        withContext(Dispatchers.IO) {
//            mappingDao.deleteMappingsForListId(listId, createdBy)
        }
    }

    private suspend fun insertAllUserInfoInDatabase(users: List<ListCreator>) {
        withContext(Dispatchers.IO) {
            users.forEach { u ->
//                onlineUserDao.insertUser(u)
            }
        }
    }

    private suspend fun deleteUserInfoInDatabase(userId: Long) {
        withContext(Dispatchers.IO) {
//            onlineUserDao.deleteUser(userId)
        }
    }

    private suspend fun getUserInfoFromDatabase(userId: Long): ListCreator? {
        var onlineUser: ListCreator? = null
        withContext(Dispatchers.IO) {
//            val dbUser = onlineUserDao.getUser(userId) ?: return@withContext
//            onlineUser = dbUser
        }
        return onlineUser
    }

    private suspend fun getUserInfoFromOnline(userId: Long): ListCreator? {
        var userInfo: ListCreator? = null
        withContext(Dispatchers.IO) {
//            Networking.GET("v1/userinfo/$userId") { resp ->
//                if (resp.status != HttpStatusCode.OK) {
//                    Log.w("ShoppingListHandler", "User $userId not found")
//                    return@GET
//                }
//                val body = resp.bodyAsText(Charsets.UTF_8)
//                val decoded = json.decodeFromString<ListCreator>(body)
//                insertUserInfoInDatabase(decoded)
//                userInfo = decoded
//            }
        }
        return userInfo
    }

    private suspend fun getUserInfo(userId: Long): ListCreator? {
        var onlineUser: ListCreator? = null
        withContext(Dispatchers.IO) {
            val storedUser = getUserInfoFromDatabase(userId)
            if (storedUser != null) {
                onlineUser = storedUser
                return@withContext
            }
            val onlineInfo = getUserInfoFromOnline(userId) ?: return@withContext
            onlineUser = onlineInfo
        }
        return onlineUser
    }

    private suspend fun getAllSharedWithForListOffline(listId: Long): List<ShareUserPreview> {
        val sharedWith = mutableListOf<ShareUserPreview>()
        withContext(Dispatchers.IO) {
//            val sharedWithUsers = shareDao.getListSharedWith(listId)
//            if (sharedWithUsers.isEmpty())
//                return@withContext
//            sharedWithUsers.forEach {
//                val sharedWithUser = getUserInfoFromDatabase(it.SharedWith)
//                if (sharedWithUser != null)
//                    sharedWith.add(ShareUserPreview(it.SharedWith, sharedWithUser.Name, true))
//            }
        }
        return sharedWith
    }

    private suspend fun updateUserIdInItems() {
        withContext(Dispatchers.IO) {
//            val allItems = itemDao.getAllItems()
//            if (allItems.isEmpty())
//                return@withContext
//            val uninitializedItems = allItems.filter { it.id == 0L }
//            if (uninitializedItems.isEmpty())
//                return@withContext
//            uninitializedItems.forEach {
//                it.ID = AppUserLocalDataSource.getUser()!!.OnlineID
//                updateItemInDatabase(it)
//            }
        }
    }

    private suspend fun updateUserIdInLists() {
        withContext(Dispatchers.IO) {
//            val lists = listDao.getShoppingLists()
//            if (lists.isEmpty())
//                return@withContext
//            val uninitializedLists = lists.filter { it.createdBy == 0L }
//            if (uninitializedLists.isEmpty())
//                return@withContext
//            val updatedCreator = ListCreator(AppUserLocalDataSource.getUser()!!.OnlineID, AppUserLocalDataSource.getUser()!!.Username)
//            uninitializedLists.forEach {
//                it.CreatedByID = updatedCreator.ID
//                it.CreatedByName = updatedCreator.Name
//                updateListInDatabase(it)
//            }
        }
    }

    // ------------------------------------------------------------------------------
    // Posting and Getting Lists from Online
    // ------------------------------------------------------------------------------

    suspend fun GetAllSharedUsersForList(listId: Long): List<ShareUserPreview> {
        var userPreview = listOf<ShareUserPreview>()
        withContext(Dispatchers.IO) {
            val matchingUser = getAllSharedWithForListOffline(listId)
            userPreview = matchingUser
        }
        return userPreview
    }

    fun ClearCheckedItemsInList(listId: Long) {
        Log.d("ShoppingListHandler", "Clearing all checked items for list $listId")
//        localCoroutine.launch {
//            deleteAllCheckedMappingsForListId(listId)
// //            postShoppingListOnline(listId, AppUserLocalDataSource.getUser()!!.OnlineID)
//        }
    }
}
