package com.cloudsheeptech.shoppinglist.data.list

import android.util.Log
import com.cloudsheeptech.shoppinglist.data.items.DbItem
import com.cloudsheeptech.shoppinglist.data.items.ApiItem
import com.cloudsheeptech.shoppinglist.data.items.AppItem
import com.cloudsheeptech.shoppinglist.data.ListCreator
import com.cloudsheeptech.shoppinglist.data.itemToListMapping.ListMapping
import com.cloudsheeptech.shoppinglist.data.ListShareDatabase
import com.cloudsheeptech.shoppinglist.data.typeConverter.OffsetDateTimeSerializer
import com.cloudsheeptech.shoppinglist.data.ShareUserPreview
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.time.OffsetDateTime
import javax.inject.Inject

/**
* This class implements the main handling of app wide shopping lists.
* This includes the creation of new lists in a displayable format
* and the deserialization from an API list to into the individual parts
* that can be stored by the application.
 */
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

    }

    suspend fun update(list: ApiShoppingList) {
        localDataSource.update(list)
        remoteApi.update(list)
    }

    suspend fun delete(listId: Long, createdBy: Long) {
        localDataSource.delete(listId, createdBy)
        remoteApi.deleteShoppingList(listId)
    }

    private suspend fun updateListInDatabase(list : DbShoppingList) : Boolean {
        var updated = false
//        withContext(Dispatchers.IO) {
//            val existingList = listDao.getShoppingList(list.listId, list.createdBy)
//            if (existingList == null) {
//                Log.i("ShoppingListHandler", "List ${list.listId} cannot be found but should exist")
//                listDao.insertList(list)
//                updated = true
//                return@withContext
//            }
//            try {
//                // Compare last edited value
//                if (existingList.lastUpdated.isAfter(list.lastUpdated))
//                    return@withContext
//                listDao.updateList(list)
//                updated = true
//                Log.d("ShoppingListHandler", "Updated list ${list.listId} in database")
//            } catch (ex : DateTimeParseException) {
//                updated = false
//                Log.w("ShoppingListHandler", "Failed to parse time (${list.lastUpdated}) and (${existingList.lastUpdated}): $ex")
//            } catch (ex : Exception) {
//                // Most likely because the instant failed to parse. Don't updated in this case
//                updated = false
//                Log.w("ShoppingListHandler", "Failed to update list: $ex")
//            }
//        }
        return updated
    }

    // Helper function
    private suspend fun insertMappingInDatabase(mapping : ListMapping) {
        withContext(Dispatchers.IO) {
            insertOrRemoveMappingsInDatabase(listOf(mapping))
        }
    }

    private suspend fun insertOrRemoveMappingsInDatabase(mappings : List<ListMapping>, remove : Boolean = false) {
        withContext(Dispatchers.IO) {
            // Check if the mapping is new or does exist
//            if (mappings.isEmpty())
//                return@withContext
//            if (remove)
//                mappingDao.deleteMappingsForListId(mappings.first().ListID, mappings.first().CreatedBy)
//            for (mapping in mappings) {
//                val existingMappings = mappingDao.getMappingForItemAndList(mapping.ItemID, mapping.ListID, mapping.CreatedBy)
//                if (mapping.ID == 0L || existingMappings.isEmpty()) {
//                    mappingDao.insertMapping(mapping)
//                    continue
//                }
//                mapping.ID = existingMappings.first().ID
//                updateMappingInDatabase(mapping)
//            }
        }
    }

    private suspend fun toggleMappingInDatabase(itemId : Long, listId : Long, createdBy: Long) {
        withContext(Dispatchers.IO) {
//            val mapping = mappingDao.getMappingForItemAndList(itemId, listId, createdBy)
//            if (mapping.isEmpty())
//                return@withContext
//            val toggleMapping = mapping.first()
//            toggleMapping.Checked = toggleMapping.Checked xor true
//            updateMappingInDatabase(toggleMapping)
        }
    }

    private suspend fun increaseItemCountInDatabase(itemId : Long, listId : Long, createdBy: Long, count : Long) {
        Log.d("ShoppingListHandler", "Executing increase count")
        withContext(Dispatchers.IO) {
//            val mapping = mappingDao.getMappingForItemAndList(itemId, listId, createdBy)
//            if (mapping.isEmpty())
//                return@withContext
//            val incMapping = mapping.first()
//            incMapping.Quantity += count
//            if (incMapping.Quantity == 0L) {
//                deleteMappingInDatabase(itemId, listId, createdBy)
//                return@withContext
//            }
//            updateMappingInDatabase(incMapping)
        }
    }

    private suspend fun updateMappingInDatabase(mapping: ListMapping) {
        withContext(Dispatchers.IO) {
//            val existingMapping = mappingDao.getMapping(mapping.ID)
//            if (existingMapping == null) {
//                mappingDao.insertMapping(mapping)
//                return@withContext
//            }
//            mappingDao.updateMapping(mapping)
        }
    }

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

    private suspend fun deleteMappingInDatabase(item : Long, list : Long, createdBy: Long) {
        withContext(Dispatchers.IO) {
//            val existingMappings = mappingDao.getMappingForItemAndList(item, list, createdBy)
//            if (existingMappings.isEmpty())
//                return@withContext
//            for(existingMapping in existingMappings) {
//                mappingDao.deleteMapping(existingMapping.ID)
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

    private suspend fun insertItemInDatabase(dbItem : DbItem) : Long {
        var itemId = 0L
        withContext(Dispatchers.IO) {
            itemId = insertItemsInDatabase(listOf(dbItem)).first()
        }
        return itemId
    }

    private suspend fun insertItemsInDatabase(dbItems : List<DbItem>) : List<Long> {
        val itemIds = mutableListOf<Long>()
        withContext(Dispatchers.IO) {
//            for(item in dbItems) {
//                val existingItem = itemDao.getItemFromName(item.name)
//                if (item.id == 0L && existingItem == null) {
//                    itemIds.add(itemDao.insertItem(item))
//                    continue
//                }
//                if (item.id == 0L)
//                    item.id = existingItem!!.id
//                updateItemInDatabase(item)
//                itemIds.add(item.id)
//            }
        }
        return itemIds
    }

    private suspend fun updateItemInDatabase(dbItem : DbItem) {
        withContext(Dispatchers.IO) {
//            val existingItem = itemDao.getItem(dbItem.id)
//            if (existingItem == null) {
//                itemDao.insertItem(dbItem)
//                return@withContext
//            }
//            itemDao.updateItem(dbItem)
        }
    }

    private suspend fun findNextFreeListId(list: DbShoppingList, newUserId : Long) : Long {
        var nextId = list.listId
        withContext(Dispatchers.IO) {
//            val updateListWithIdExists = listDao.getShoppingList(list.listId, newUserId)
//            if (updateListWithIdExists != null) {
//                // The list exists, so create a new ID for this list
//                val existingLists = listDao.getLatestListId() ?: return@withContext
//                nextId = existingLists + 1
//                return@withContext
//            }
//            // The list with update userId does not exist, therefore its fine to simply update the
//            // list here
//            return@withContext
        }
        return nextId
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

    private suspend fun insertUserInfoInDatabase(user : ListCreator) {
        return insertAllUserInfoInDatabase(listOf(user))
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
    // Conversion of Lists from and to Wire Format
    // ------------------------------------------------------------------------------

    private suspend fun listMappingToItemWire(item : ListMapping) : ApiItem? {
//        val convertedItem = ApiItem(Name="", Icon="", Quantity = 1L, checked = false, addedBy = item.AddedBy)
        withContext(Dispatchers.IO) {
//            // The database item being null should NEVER happen!
//            val databaseItem = itemDao.getItem(item.ItemID) ?: return@withContext null
//            convertedItem.Name = databaseItem.name
//            convertedItem.Icon = databaseItem.icon
//            convertedItem.Quantity = item.Quantity
//            convertedItem.checked = item.Checked
//            convertedItem.addedBy = item.AddedBy
        }
//        if (convertedItem.Name == "") {
//            return null
//        }
//        return convertedItem
        return null
    }

    suspend fun mappingToItemWithQuantity(mapping: ListMapping) : AppItem? {
        var appItem : AppItem? = null
        withContext(Dispatchers.IO) {
//            val databaseItem = itemDao.getItem(mapping.ItemID) ?: return@withContext
//            appItem = AppItem(id=databaseItem.id, name=databaseItem.name, icon=databaseItem.icon, quantity=mapping.Quantity, checked = mapping.Checked, addedBy = mapping.AddedBy)
        }
        return appItem
    }

    private suspend fun shoppingListToWire(list : DbShoppingList) : ApiShoppingList {
        val convertedList = ApiShoppingList(listId = list.listId, title = list.title, createdBy = ListCreator(list.createdBy, list.createdByName), createdAt = OffsetDateTime.now(), lastUpdated = list.lastUpdated, items = mutableListOf())
        withContext(Dispatchers.IO) {
            // Retrieve the list
//            val itemsInList = mutableListOf<ApiItem>()
//            val itemsMapped = mappingDao.getMappingsForList(list.listId, list.createdBy)
//            for (item in itemsMapped) {
//                val itemConvertedToWire = listMappingToItemWire(item) ?: continue
//                itemsInList.add(itemConvertedToWire)
//            }
//            convertedList.items = itemsInList
        }
        return convertedList
    }

    private fun itemToMapping(dbItem: DbItem, list : Long, createdBy : Long) : ListMapping {
//        return ListMapping(ID = 0L, item.ID, list, Quantity = 1L, Checked = false, CreatedBy = createdBy, AddedBy = AppUserLocalDataSource.getUser()!!.OnlineID)
        return ListMapping(ID = 0L, dbItem.id, list, Quantity = 1L, Checked = false, CreatedBy = createdBy, AddedBy = -1)
    }

//    private suspend fun itemWireToItemAndCreateIfNotExists(apiItem: ApiItem) : DbItem {
//        var dbItem: DbItem
//        withContext(Dispatchers.IO) {
////            val dbItem = itemDao.getItemFromName(apiItem.Name)
////            if (dbItem == null) {
////                Log.d("ShoppingListHandler", "Item ${apiItem.Name} not found")
////                dbItem = DbItem(0, apiItem.Name, apiItem.Icon)
////                dbItem.id = insertItemInDatabase(dbItem)
////                return@withContext
////            }
////            dbItem = dbItem
//        }
//        return dbItem
//    }

    private suspend fun itemWireToListMapping(list : ApiShoppingList, apiItem: ApiItem) : ListMapping {
        val mapping = ListMapping(0L, ItemID = 0L, ListID = list.listId, Quantity = apiItem.quantity, Checked = apiItem.checked, CreatedBy = list.createdBy.onlineId, AddedBy = list.createdBy.onlineId)
        withContext(Dispatchers.IO) {
            // The case where we do not have the ID should never happen because
            // all items should be inserted by this point
//            val databaseItem = itemDao.getItemFromName(apiItem.Name) ?: return@withContext
//            mapping.ItemID = databaseItem.id
        }
        return mapping
    }

    private suspend fun shoppingListWireToLocal(list : ApiShoppingList) : Triple<DbShoppingList, List<ListMapping>, List<DbItem>> {
        val dbShoppingList = DbShoppingList(list.listId, list.title, list.createdBy.onlineId, list.createdBy.username, list.lastUpdated)
        val mappings = mutableListOf<ListMapping>()
        val dbItems = mutableListOf<DbItem>()
        withContext(Dispatchers.IO) {
            for (item in list.items) {
//                val convertedItem = itemWireToItemAndCreateIfNotExists(item)
//                dbItems.add(convertedItem)
//                val convertedMapping = itemWireToListMapping(list, item)
//                mappings.add(convertedMapping)
            }
        }
        return Triple(dbShoppingList, mappings, dbItems)
    }

    // ------------------------------------------------------------------------------
    // Posting and Getting Lists from Online
    // ------------------------------------------------------------------------------

    private suspend fun postShoppingListOnline(listId : Long, from : Long) : DbShoppingList? {
        var updatedList : DbShoppingList? = null
        withContext(Dispatchers.IO) {
//            val list = listDao.getShoppingList(listId, from)
//            if (list == null) {
//                Log.i("ShoppingListHandler", "List $listId does not exist. Cannot push online")
//                return@withContext
//            }
//            updatedList = postShoppingListOnline(list)
        }
        return updatedList
    }

    private suspend fun postShoppingListOnline(list : DbShoppingList) : DbShoppingList {
        var updatedList = list
        withContext(Dispatchers.IO) {
            if (list.createdBy == 0L) {
                Log.d("ShoppingListHandler", "List was created offline and needs to be converted")
//                if (AppUserLocalDataSource.getUser()!!.OnlineID == 0L) {
//                    AppUserLocalDataSource.PostUserOnlineAsync(null)
//                    if (AppUserLocalDataSource.getUser()!!.OnlineID == 0L) {
//                        Log.i("ShoppingListHandler", "Failed to create user online: Cannot complete request")
//                        return@withContext
//                    }
//                }
//                list.CreatedByID = AppUserLocalDataSource.getUser()!!.OnlineID
                updatedCreatedByForAllLists()
            }
            val listInWireFormat = shoppingListToWire(list)
            val serializedList = json.encodeToString(listInWireFormat)
//            Networking.POST("v1/list", serializedList) { resp ->
//                if (resp.status != HttpStatusCode.Created) {
//                    Log.w("ShoppingListHandler", "Posting Shopping List online failed")
//                    return@POST
//                }
//                // We don't expect anything from online
//            }
        }
        return updatedList
    }

    private suspend fun deleteShoppingListOnline(listId : Long) {
        withContext(Dispatchers.IO) {
//            Networking.DELETE("v1/list/$listId", "") { resp ->
//                if (resp.status != HttpStatusCode.OK) {
//                    Log.w("ShoppingListHandler", "Failed to remove list $listId")
//                    return@DELETE
//                }
//                Log.d("ShoppingListHandler"," Removed list $listId online")
//            }
        }
    }

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

    private suspend fun getShoppingListFromOnline(listId : Long, createdBy: Long?) : ApiShoppingList? {
        var onlineList : ApiShoppingList? = null
        withContext(Dispatchers.IO) {
            var requestUrl = "v1/list?listId=$listId"
            if (createdBy != null) {
                requestUrl = "$requestUrl&createdBy=$createdBy"
            }
//            Networking.GET(requestUrl) { resp ->
//                if (resp.status != HttpStatusCode.OK) {
//                    Log.w("ShoppingListHandler", "Failed to GET list $listId ($createdBy) from online")
//                    return@GET
//                }
//                val body = resp.bodyAsText(Charsets.UTF_8)
//                val decoded = json.decodeFromString<ShoppingListWire>(body)
//                onlineList = decoded
//            }
        }
        return onlineList
    }

    private suspend fun getShoppingListsFromOnline(lists : List<Pair<Long, Long>>) : List<ApiShoppingList> {
        val onlineLists = mutableListOf<ApiShoppingList>()
        withContext(Dispatchers.IO) {
            for((listId, createdBy) in lists) {
                val onlineList = getShoppingListFromOnline(listId, createdBy)
                if (onlineList != null)
                    onlineLists.add(onlineList)
            }
            Log.d("ShoppingListHandler", "Retrieved ${onlineLists.size}/${lists.size} lists successfully")
        }
        return onlineLists
    }

    private suspend fun getOwnAndSharedShoppingListsFromOnline() : List<ApiShoppingList> {
        val onlineLists = mutableListOf<ApiShoppingList>()
        withContext(Dispatchers.IO) {
//            Networking.GET("v1/lists/${AppUserLocalDataSource.getUser()!!.OnlineID}") { resp ->
//                if (resp.status != HttpStatusCode.OK) {
//                    Log.w("ShoppingListHandler", "Failed to GET all list from online")
//                    return@GET
//                }
//                val body = resp.bodyAsText(Charsets.UTF_8)
//                val decodedShoppingLists = json.decodeFromString<List<ShoppingListWire>>(body)
//                onlineLists.addAll(decodedShoppingLists)
//                Log.d("ShoppingListHandler", "Retrieved ${onlineLists.size} lists successfully")
//            }
        }
        return onlineLists
    }

    private suspend fun getAllShoppingListsFromOnline() {
        withContext(Dispatchers.IO) {
            val onlineLists = getOwnAndSharedShoppingListsFromOnline()
            Log.d("ShoppingListHandler", "Got lists: $onlineLists")
            for (list in onlineLists) {
                // Automatically creates the items if not existing
                val (localList, mappings, _) = shoppingListWireToLocal(list)
                val updated = updateListInDatabase(localList)
                if (updated) {
                    insertOrRemoveMappingsInDatabase(mappings, true)
                }
            }
        }
    }

    // ------------------------------------------------------------------------------
    // The Public API
    // ------------------------------------------------------------------------------

    fun PostShoppingListOnline(listId : Long, from : Long) {
//        localCoroutine.launch {
//            postShoppingListOnline(listId, from)
//        }
    }

    fun PostShoppingListOnline(list : DbShoppingList) {
//        localCoroutine.launch {
//            postShoppingListOnline(list)
//        }
    }

    // Creating a new shopping list with the given name
    // Stores the list locally
    // Stores the list online (if possible)
    // Returns the newly created list
    fun CreateNewShoppingList(name : String) {
//        localCoroutine.launch {
//            val list = newShoppingList(name)
//            val updatedList = postShoppingListOnline(list)
//            insertShoppingListIntoDatabase(updatedList)
//        }
        // Cannot return the list here because we have the asynchronous operations before
    }

    fun DeleteShoppingList(listId : Long, from : Long) {
//        localCoroutine.launch {
//            val list = getShoppingListFromDatabase(listId, from) ?: return@launch
//            DeleteShoppingList(list)
//        }
    }

    fun DeleteShoppingList(list : DbShoppingList) {
//        localCoroutine.launch {
//            deleteShoppingListFromDatabase(list)
////            if (list.CreatedByID != AppUserLocalDataSource.getUser()!!.OnlineID) {
////                // Delete sharing online
////                requestUnshareList(list.ID, list.CreatedByID)
////            } else {
////                // Should include the unsharing of the list
////                deleteShoppingListOnline(list.ID)
////            }
//        }
    }

    fun AddItemToShoppingList(dbItem : DbItem, list : Long, createdBy: Long) : Int {
//        localCoroutine.launch {
//            val mapping = itemToMapping(dbItem, list, createdBy)
//            insertMappingInDatabase(mapping)
////            updateLastEditedInDatabase(list, AppUserLocalDataSource.getUser()!!.OnlineID)
////            postShoppingListOnline(list, AppUserLocalDataSource.getUser()!!.OnlineID)
//        }
        return 0
    }

    fun AddItemAndAddToShoppingList(dbItem : DbItem, list : Long, createdBy: Long) {
//        localCoroutine.launch {
//            val insertedId = insertItemInDatabase(dbItem)
//            dbItem.id = insertedId
//            AddItemToShoppingList(dbItem, list, createdBy)
//        }
    }

    fun DeleteItemFromShoppingList(dbItem : DbItem, list : Long, createdBy: Long) {
//        localCoroutine.launch {
//            deleteMappingInDatabase(dbItem.id, list, createdBy)
//            updateLastEditedInDatabase(list, createdBy)
//            postShoppingListOnline(list, createdBy)
//        }
    }

    fun ToggleItemInShoppingList(itemId : Long, listId : Long, createdBy: Long) {
//        localCoroutine.launch {
//            updateLastEditedInDatabase(listId, createdBy)
//            toggleMappingInDatabase(itemId, listId, createdBy)
//            postShoppingListOnline(listId, createdBy)
//        }
    }

    fun IncreaseItemCountInShoppingList(itemId : Long, listId : Long, createdBy: Long, count : Long = 1) {
//        localCoroutine.launch {
//            updateLastEditedInDatabase(listId, createdBy)
//            increaseItemCountInDatabase(itemId, listId, createdBy, count)
//            postShoppingListOnline(listId, createdBy)
//        }
    }

    fun DecreaseItemCountInShoppingList(itemId : Long, listId : Long, createdBy: Long, count : Long = 1) {
//        localCoroutine.launch {
//            updateLastEditedInDatabase(listId, createdBy)
//            increaseItemCountInDatabase(itemId, listId, createdBy, -1 * count)
//            postShoppingListOnline(listId, createdBy)
//        }
    }

    fun GetShoppingList(listId : Long, createdBy: Long) {
        // Automatically updating the list in the database
        // No need to return the list
//        localCoroutine.launch {
//            val onlineList = getShoppingListFromOnline(listId, createdBy) ?: return@launch
//            // Automatically creates the items if not existing
//            val (list, mappings, _) = shoppingListWireToLocal(onlineList)
////            insertItemsInDatabase(items)
//            val updated = updateListInDatabase(list)
//            if (updated) {
//                // Only update the mappings in case we received a newer list
//                insertOrRemoveMappingsInDatabase(mappings, true)
//            }
//        }
    }

    fun GetAllShoppingLists() {
//        localCoroutine.launch {
//            getAllShoppingListsFromOnline()
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