package com.cloudsheeptech.shoppinglist.data.list

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.cloudsheeptech.shoppinglist.data.items.DbItem
import com.cloudsheeptech.shoppinglist.data.items.ApiItem
import com.cloudsheeptech.shoppinglist.data.items.AppItem
import com.cloudsheeptech.shoppinglist.data.ListCreator
import com.cloudsheeptech.shoppinglist.data.itemToListMapping.ListMapping
import com.cloudsheeptech.shoppinglist.data.sharing.ListShareDatabase
import com.cloudsheeptech.shoppinglist.data.typeConverter.OffsetDateTimeSerializer
import com.cloudsheeptech.shoppinglist.data.sharing.ShareUserPreview
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
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
class ShoppingListRepository @Inject constructor(
    private val localDataSource: ShoppingListLocalDataSource,
    private val remoteApi: ShoppingListRemoteDataSource,
    private val userRepository: AppUserRepository,
) {

    private val json = Json {
        ignoreUnknownKeys = false
        encodeDefaults = true
        serializersModule = SerializersModule {
            contextual(OffsetDateTime::class, OffsetDateTimeSerializer())
        }
    }

    // ------------------------------------------------------------------------------
    // Creation of a new list + Insertion + Update
    // ------------------------------------------------------------------------------

    suspend fun create(title: String) : ApiShoppingList {
        val now = OffsetDateTime.now()
        val user = userRepository.read() ?: throw IllegalStateException("user null after login screen")
        val newList = ApiShoppingList(
            listId = 0L,
            title = title,
            createdBy = ListCreator(user.OnlineID, user.Username),
            createdAt = now,
            lastUpdated = now,
            items = mutableListOf(),
        )
        val newListId = localDataSource.create(newList)
        newList.listId = newListId
        remoteApi.create(newList)
        return newList
    }

    suspend fun read(listId: Long, createdBy: Long) : ApiShoppingList? {
        var storedList : ApiShoppingList? = null
        storedList = localDataSource.read(listId, createdBy)
        // Only update the list when we have nothing stored locally
        if (storedList == null) {
            storedList = remoteApi.read(listId, createdBy)
        }
        return storedList
    }

    suspend fun readAllRemote() {
        val allRemoteLists = remoteApi.readAll()
        // TODO: Write an integration method with the locally stored lists
        allRemoteLists.forEach { remoteList ->
            // Create if not exists, update if exists
            val exists = localDataSource.exists(remoteList.listId, remoteList.createdBy.onlineId)
            Log.d("ShoppingListRepository", "List ${remoteList.listId} from ${remoteList.createdBy.onlineId} exists: $exists")
            if (!exists) {
                localDataSource.create(remoteList)
            } else {
                localDataSource.update(remoteList)
            }
        }
    }

    // This is only relevant for the overview, therefore the basic infos (title, creator) suffice
    fun readAllLive() : LiveData<List<DbShoppingList>> {
        return localDataSource.readAllLive()
    }

    suspend fun exist(listId: Long, createdBy: Long) : Boolean {
        var exists = false
        withContext(Dispatchers.IO) {
            exists = localDataSource.exists(listId, createdBy)
        }
        return exists
    }

    suspend fun update(list: ApiShoppingList) {
        localDataSource.update(list)
        remoteApi.update(list)
    }

    suspend fun insertItem(listId: Long, createdBy: Long, item: AppItem) {
        val updatedLocalList = localDataSource.insertItem(listId, createdBy, item)
        remoteApi.update(updatedLocalList)
    }

    suspend fun insertExistingItem(listId: Long, createdBy: Long, itemId: Long) {
        val updatedLocalList = localDataSource.insertExistingItem(listId, createdBy, itemId)
        remoteApi.update(updatedLocalList)
    }

    suspend fun toggleItem(listId: Long, createdBy: Long, itemId: Long) {
        val updatedLocalList = localDataSource.toggleItem(listId, createdBy, itemId)
        remoteApi.update(updatedLocalList)
    }

    suspend fun updateItemCount(listId: Long, createdBy: Long, itemId: Long, quantity: Long) {
        val updatedLocalList = localDataSource.updateItemCount(listId, createdBy, itemId, quantity)
        remoteApi.update(updatedLocalList)
    }

    suspend fun delete(listId: Long, createdBy: Long) {
        localDataSource.delete(listId, createdBy)
        remoteApi.deleteShoppingList(listId)
    }

    // TODO: Implement the functions below here or in other repos

    private suspend fun updateCreatedByForMappings(listId : Long, moveToId : Long, createdBy: Long) {
        withContext(Dispatchers.IO) {
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

    private suspend fun resetCreatedByForMappings(list: DbShoppingList) {
        withContext(Dispatchers.IO) {
//            val mappings = mappingDao.getMappingsForList(list.listId, list.createdBy)
//            if (mappings.isEmpty())
//                return@withContext
//            for (mapping in mappings) {
//                mapping.CreatedBy = 0L
//                mapping.AddedBy = 0L
//                deleteMappingInDatabase(mapping.ItemID, mapping.ListID, list.createdBy)
//                insertMappingInDatabase(mapping)
//            }
        }
    }

    private suspend fun deleteAllCheckedMappingsForListId(listId: Long) {
        withContext(Dispatchers.IO) {
//            mappingDao.deleteCheckedMappingsForListId(listId)
        }
    }

    private suspend fun deleteAllMappingsForListId(listId : Long, createdBy: Long) {
        withContext(Dispatchers.IO) {
//            mappingDao.deleteMappingsForListId(listId, createdBy)
        }
    }


    private suspend fun updateCreatedByForList(list : DbShoppingList, createdBy: Long) {
        if (createdBy == 0L || list.createdBy != 0L)
            return
        withContext(Dispatchers.IO) {
            // In case the user create new lists before updating the old ones,
            // find the next secure listID to "move" the list to
//            val newListId = findNextFreeListId(list, createdBy)
//            Log.d("ShoppingListHandler", "Found $newListId as next free list id")
//            // Update the item mappings, then the list itself
//            Log.d("ShoppingListHandler", "Updating mappings for list ${list.listId}")
//            updateCreatedByForMappings(list.listId, newListId, createdBy)
//            Log.d("ShoppingListHandler", "Deleting list ${list.listId} from database")
//            deleteShoppingListFromDatabase(list)
//            list.createdBy = createdBy
////            list.CreatedByName = AppUserLocalDataSource.getUser()!!.Username
//            list.listId = newListId
//            // This is not really an edit from the user
////            existingList.LastEdited = OffsetDateTime.now()
//            Log.d("ShoppingListHandler", "Inserting list $list")
//            insertShoppingListIntoDatabase(list)
        }
    }

    private suspend fun resetCreatedByForList(list: DbShoppingList) {
        if (list.createdBy == 0L)
            return
        withContext(Dispatchers.IO) {
            resetCreatedByForMappings(list)
            // TODO: Include the sharing as well. This is relevant when pushing the new lists
//            deleteShoppingListFromDatabase(list)
//            list.createdBy = 0L
//            insertShoppingListIntoDatabase(list)
        }
    }

    suspend fun updatedCreatedByForAllLists() {
        Log.d("ShoppingListHandler", "Updating all lists in database that were created offline")
        withContext(Dispatchers.IO) {
//            if (AppUserLocalDataSource.getUser()!!.OnlineID == 0L)
//                return@withContext
//            val allShoppingLists = listDao.getShoppingLists()
//            if (allShoppingLists.isEmpty())
//                return@withContext
//            val uninitializedLists = allShoppingLists.filter { list -> list.createdBy == 0L }
//            Log.d("ShoppingListHandler", "Found ${uninitializedLists.size} lists to update")
//            for (list in uninitializedLists) {
//                updateCreatedByForList(list, AppUserLocalDataSource.getUser()!!.OnlineID)
//            }
        }
    }

    suspend fun resetCreatedByForOwnLists(createdBy: Long) {
        if (createdBy == 0L)
            return
        withContext(Dispatchers.IO) {
//            val allLists = listDao.getShoppingLists()
//            if (allLists.isEmpty())
//                return@withContext
//            val ownLists = allLists.filter { l -> l.createdBy == createdBy }
//            Log.d("ShoppingListHandler", "Found ${ownLists.size} own lists to reset")
//            for (list in ownLists) {
//                resetCreatedByForList(list)
//            }
        }
    }

    private suspend fun insertAllUserInfoInDatabase(users : List<ListCreator>) {
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

    private suspend fun getUserInfoFromDatabase(userId : Long) : ListCreator? {
        var onlineUser : ListCreator? = null
        withContext(Dispatchers.IO) {
//            val dbUser = onlineUserDao.getUser(userId) ?: return@withContext
//            onlineUser = dbUser
        }
        return onlineUser
    }


    private suspend fun getUserInfoFromOnline(userId : Long) : ListCreator? {
        var userInfo : ListCreator? = null
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

    private suspend fun getUserInfo(userId: Long) : ListCreator? {
        var onlineUser : ListCreator? = null
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

    private suspend fun getMatchingUsersFromOnline(name: String) : List<ListCreator> {
        var users = emptyList<ListCreator>()
        withContext(Dispatchers.IO) {
//            Networking.GET("v1/users/$name") { resp ->
//                if (resp.status != HttpStatusCode.OK) {
//                    Log.w("ShoppingListHandler", "Failed to find users for query $name")
//                    return@GET
//                }
//                val body = resp.bodyAsText(Charsets.UTF_8)
//                val decoded = json.decodeFromString<List<ListCreator>>(body)
//                users = decoded
//            }
        }
        return users
    }

    private suspend fun getAllSharedWithForListOffline(listId: Long) : List<ShareUserPreview> {
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

    private suspend fun createSharingInDatabase(userId: Long, listId: Long) {
        withContext(Dispatchers.IO) {
            val share = ListShareDatabase(0, listId, userId)
//            shareDao.insertShared(share)
        }
    }

    private suspend fun deleteSharingInDatabase(userId: Long, listId: Long) {
        withContext(Dispatchers.IO) {
//            shareDao.deleteForUser(userId, listId)
        }
    }

    private suspend fun deleteAllSharingForListInDatabase(listId: Long) {
        withContext(Dispatchers.IO) {
//            shareDao.deleteAllFromList(listId)
        }
    }

    // ------------------------------------------------------------------------------
    // Posting and Getting Lists from Online
    // ------------------------------------------------------------------------------


    private suspend fun shareListOnline(listId : Long, sharedWithId: Long) : Boolean {
        var success = false
        withContext(Dispatchers.IO) {
//            val sharedListObject = ListShare(listId, AppUserLocalDataSource.getUser()!!.OnlineID, sharedWithId, OffsetDateTime.now())
//            val serialized = json.encodeToString(sharedListObject)
//            Networking.POST("v1/share/$listId", serialized) { resp ->
//                if (resp.status != HttpStatusCode.Created) {
//                    Log.w("ShoppingListHandler", "Failed to share list $listId online")
//                    return@POST
//                }
//                success = true
//                Log.d("ShoppingListHandler", "List $listId shared online with $sharedWithId")
//            }
        }
        return success
    }

    private suspend fun unshareListOnline(listId: Long) {
        withContext(Dispatchers.IO) {
//            val unshareObject = ListShare(listId, AppUserLocalDataSource.getUser()!!.OnlineID, -1, OffsetDateTime.now())
//            val serialized = json.encodeToString(unshareObject)
//            Networking.DELETE("v1/share/$listId", serialized) { resp ->
//                if (resp.status != HttpStatusCode.OK) {
//                    Log.w("ShoppingListHandler", "Failed to unshare list $listId online")
//                    return@DELETE
//                }
//            }
        }
    }

    private suspend fun unshareListForUserOnline(userId: Long, listId: Long) {
        withContext(Dispatchers.IO) {
//            val unshareObject = ListShare(listId, AppUserLocalDataSource.getUser()!!.OnlineID, userId, OffsetDateTime.now())
//            val serialized = json.encodeToString(unshareObject)
//            Networking.DELETE("v1/share/$listId", serialized) { resp ->
//                if (resp.status != HttpStatusCode.OK) {
//                    Log.w("ShoppingListHandler", "Failed to unshare list $listId online")
//                    return@DELETE
//                }
//            }
        }
    }

    private suspend fun requestUnshareList(listId: Long, createdBy: Long) {
        withContext(Dispatchers.IO) {
//            val unshareObject = ListShare(listId, createdBy, AppUserLocalDataSource.getUser()!!.OnlineID, OffsetDateTime.now())
//            val serialized = json.encodeToString(unshareObject)
//            Networking.DELETE("v1/share/$listId", serialized) { resp ->
//                if (resp.status != HttpStatusCode.OK) {
//                    Log.w("ShoppingListHandler", "Failed to request unshare list $listId")
//                    return@DELETE
//                }
//                Log.d("ShoppingListHandler", "Unshared list $listId")
//            }
        }
    }

    fun DeleteItemFromShoppingList(dbItem : DbItem, list : Long, createdBy: Long) {
//        localCoroutine.launch {
//            deleteMappingInDatabase(dbItem.id, list, createdBy)
//            updateLastEditedInDatabase(list, createdBy)
//            postShoppingListOnline(list, createdBy)
//        }
    }

    suspend fun SearchUsersOnline(name : String) : List<ListCreator> {
        var users = emptyList<ListCreator>()
        withContext(Dispatchers.IO) {
            val matchingUsers = getMatchingUsersFromOnline(name) ?: return@withContext
            insertAllUserInfoInDatabase(matchingUsers)
            users = matchingUsers
        }
        return users
    }

    suspend fun GetAllSharedUsersForList(listId: Long) : List<ShareUserPreview> {
        var userPreview = listOf<ShareUserPreview>()
        withContext(Dispatchers.IO) {
            val matchingUser = getAllSharedWithForListOffline(listId) ?: return@withContext
            userPreview = matchingUser
        }
        return userPreview
    }

    fun ShareShoppingListOnline(listId: Long, sharedWithId : Long) {
        Log.d("ShoppingListHandler", "Sharing list $listId with $sharedWithId")
//        localCoroutine.launch {
////            postShoppingListOnline(listId, AppUserLocalDataSource.getUser()!!.OnlineID)
//            createSharingInDatabase(sharedWithId, listId)
//            shareListOnline(listId, sharedWithId)
//        }
    }

    fun UnshareShoppingListOnline(listId : Long) {
        Log.d("ShoppingListHandler", "Unsharing list $listId")
//        localCoroutine.launch {
//            unshareListOnline(listId)
//            deleteAllSharingForListInDatabase(listId)
//        }
    }

    fun UnshareShoppingListForUserOnline(userId : Long, listId : Long) {
        Log.d("ShoppingListHandler", "Unsharing list $listId for user $userId")
//        localCoroutine.launch {
//            unshareListForUserOnline(userId, listId)
//            deleteSharingInDatabase(userId, listId)
//        }
    }

    fun ClearCheckedItemsInList(listId: Long) {
        Log.d("ShoppingListHandler", "Clearing all checked items for list $listId")
//        localCoroutine.launch {
//            deleteAllCheckedMappingsForListId(listId)
////            postShoppingListOnline(listId, AppUserLocalDataSource.getUser()!!.OnlineID)
//        }
    }

}