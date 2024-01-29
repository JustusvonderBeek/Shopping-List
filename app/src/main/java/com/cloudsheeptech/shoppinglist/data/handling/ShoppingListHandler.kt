package com.cloudsheeptech.shoppinglist.data.handling

import android.util.Log
import com.cloudsheeptech.shoppinglist.data.Item
import com.cloudsheeptech.shoppinglist.data.ItemWire
import com.cloudsheeptech.shoppinglist.data.ItemWithQuantity
import com.cloudsheeptech.shoppinglist.data.ListCreator
import com.cloudsheeptech.shoppinglist.data.ListMapping
import com.cloudsheeptech.shoppinglist.data.ListShare
import com.cloudsheeptech.shoppinglist.data.ShoppingList
import com.cloudsheeptech.shoppinglist.data.ShoppingListWire
import com.cloudsheeptech.shoppinglist.data.User
import com.cloudsheeptech.shoppinglist.data.UserWire
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.network.Networking
import com.cloudsheeptech.shoppinglist.user.AppUser
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/*
* This class implements the main handling of shopping lists and other datastructures
* used in this app.
* To make the handling more clear and understandable, make the functionality and name
* of each method match. Don't introduce any side-effects in methods which might not be
* clear to the user.
* This, especially, concerns uploading lists to the server, creating items, or storing
* objects into the database.
 */
class ShoppingListHandler(val database : ShoppingListDatabase) {

    private val job = Job()
    private val localCoroutine = CoroutineScope(Dispatchers.Main + job)

    private val listDao = database.shoppingListDao()
    private val itemDao = database.itemDao()
    private val mappingDao = database.mappingDao()
    private val onlineUserDao = database.onlineUserDao()

    // ------------------------------------------------------------------------------
    // Creation of a new list + Insertion + Update
    // ------------------------------------------------------------------------------

    private fun newShoppingList(name : String) : ShoppingList {
        val now = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        val createdBy = AppUser.getUser()
        // Let the database assign the unique ID (0 = DB assign)
        val list = ShoppingList(ID=0, Name=name, CreatedBy = ListCreator(createdBy.ID, createdBy.Username), LastEdited = now)
        Log.d("ShoppingListHandler", "Created List: $list")
        return list
    }

    private suspend fun insertShoppingListIntoDatabase(list : ShoppingList) : Long {
        var insertedListId = list.ID
        withContext(Dispatchers.IO) {
            // Differentiate between new and existing list
            if (insertedListId == 0L) {
                insertedListId = listDao.insertList(list)
                Log.d("ShoppingListHandler", "Inserted list $insertedListId into database")
                return@withContext
            }
            // In this case, the ID is already existing and set in the variable returned
            updateListInDatabase(list)
        }
        return insertedListId
    }

    private fun setLastEditedNowInShoppingList(list : ShoppingList) : ShoppingList {
        val now = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        list.LastEdited = now
        return list
    }

    private suspend fun updateLastEditedInDatabase(list : ShoppingList) {
        withContext(Dispatchers.IO) {
            val updatedList = setLastEditedNowInShoppingList(list)
            updateListInDatabase(updatedList)
        }
    }

    private suspend fun updateLastEditedInDatabase(listId : Long) {
        withContext(Dispatchers.IO) {
            val list = listDao.getShoppingList(listId) ?: return@withContext
            updateLastEditedInDatabase(list)
        }
    }

    private suspend fun updateListInDatabase(list : ShoppingList) : Boolean {
        var updated = false
        withContext(Dispatchers.IO) {
            val existingList = listDao.getShoppingList(list.ID)
            if (existingList == null) {
                Log.i("ShoppingListHandler", "List ${list.ID} cannot be found but should exist")
                listDao.insertList(list)
                updated = true
                return@withContext
            }
            try {
                // Compare last edited value
                if (list.LastEdited == "") {
                    return@withContext
                }
                val lastEditedNewList = Instant.parse(list.LastEdited)
                val lastEditedExistingList = Instant.parse(existingList.LastEdited)
                if (lastEditedExistingList.isAfter(lastEditedNewList))
                    return@withContext
                listDao.updateList(list)
                updated = true
                Log.d("ShoppingListHandler", "Updated list ${list.ID} in database")
            } catch (ex : DateTimeParseException) {
                updated = false
                Log.w("ShoppingListHandler", "Failed to parse time (${list.LastEdited}) and (${existingList.LastEdited}): $ex")
            } catch (ex : Exception) {
                // Most likely because the instant failed to parse. Don't updated in this case
                updated = false
                Log.w("ShoppingListHandler", "Failed to update list: $ex")
            }
        }
        return updated
    }

    private suspend fun deleteShoppingListFromDatabase(list : ShoppingList) {
        withContext(Dispatchers.IO) {
            deleteAllMappingsForListId(list.ID)
            listDao.deleteList(list.ID)
            Log.i("ShoppingListHandler", "Deleted list ${list.Name}")
        }
    }

    // Helper function
    private suspend fun insertMappingInDatabase(mapping : ListMapping) {
        withContext(Dispatchers.IO) {
            insertMappingsInDatabase(listOf(mapping))
        }
    }

    private suspend fun insertMappingsInDatabase(mappings : List<ListMapping>) {
        withContext(Dispatchers.IO) {
            // Check if the mapping is new or does exist
            for (mapping in mappings) {
                val existingMappings = mappingDao.getMappingForItemAndList(mapping.ItemID, mapping.ListID)
                if (mapping.ID == 0L && existingMappings.isEmpty()) {
                    mappingDao.insertMapping(mapping)
                    return@withContext
                }
                mapping.ID = existingMappings.first().ID
                updateMappingInDatabase(mapping)
            }
        }
    }

    private suspend fun toggleMappingInDatabase(itemId : Long, listId : Long) {
        withContext(Dispatchers.IO) {
            val mapping = mappingDao.getMappingForItemAndList(itemId, listId)
            if (mapping.isEmpty())
                return@withContext
            val toggleMapping = mapping.first()
            toggleMapping.Checked = toggleMapping.Checked xor true
            updateMappingInDatabase(toggleMapping)
        }
    }

    private suspend fun increaseItemCountInDatabase(itemId : Long, listId : Long, count : Long) {
        withContext(Dispatchers.IO) {
            val mapping = mappingDao.getMappingForItemAndList(itemId, listId)
            if (mapping.isEmpty())
                return@withContext
            val incMapping = mapping.first()
            incMapping.Quantity += count
            if (incMapping.Quantity == 0L) {
                deleteMappingInDatabase(itemId, listId)
                return@withContext
            }
            updateMappingInDatabase(incMapping)
        }
    }

    private suspend fun updateMappingInDatabase(mapping: ListMapping) {
        withContext(Dispatchers.IO) {
            val existingMapping = mappingDao.getMapping(mapping.ID)
            if (existingMapping == null) {
                mappingDao.insertMapping(mapping)
                return@withContext
            }
            mappingDao.updateMapping(mapping)
        }
    }

    private suspend fun deleteMappingInDatabase(item : Long, list : Long) {
        withContext(Dispatchers.IO) {
            val existingMappings = mappingDao.getMappingForItemAndList(item, list)
            if (existingMappings.isEmpty())
                return@withContext
            for(existingMapping in existingMappings) {
                mappingDao.deleteMapping(existingMapping.ID)
            }
        }
    }

    private suspend fun deleteAllMappingsForListId(listId : Long) {
        withContext(Dispatchers.IO) {
            mappingDao.deleteMappingsForListId(listId)
        }
    }

    private suspend fun insertItemInDatabase(item : Item) : Long {
        var itemId = 0L
        withContext(Dispatchers.IO) {
            itemId = insertItemsInDatabase(listOf(item)).first()
        }
        return itemId
    }

    private suspend fun insertItemsInDatabase(items : List<Item>) : List<Long> {
        val itemIds = mutableListOf<Long>()
        withContext(Dispatchers.IO) {
            for(item in items) {
                val existingItem = itemDao.getItemFromName(item.Name)
                if (item.ID == 0L && existingItem == null) {
                    itemIds.add(itemDao.insertItem(item))
                    continue
                }
                if (item.ID == 0L)
                    item.ID = existingItem!!.ID
                updateItemInDatabase(item)
                itemIds.add(item.ID)
            }
        }
        return itemIds
    }

    private suspend fun updateItemInDatabase(item : Item) {
        withContext(Dispatchers.IO) {
            val existingItem = itemDao.getItem(item.ID)
            if (existingItem == null) {
                itemDao.insertItem(item)
                return@withContext
            }
            itemDao.updateItem(item)
        }
    }

    private suspend fun updatedCreatedByForAllLists() {
        withContext(Dispatchers.IO) {
            if (AppUser.ID == 0L)
                return@withContext
            val allShoppingLists = listDao.getShoppingLists()
            if (allShoppingLists.isEmpty())
                return@withContext
            val uninitializedLists = allShoppingLists.filter { list -> list.CreatedBy.ID == 0L }
            for (list in uninitializedLists) {
                list.CreatedBy.ID = AppUser.ID
                updateListInDatabase(list)
            }
        }
    }

    private suspend fun insertUserInfoInDatabase(user : UserWire) {
        withContext(Dispatchers.IO) {
            onlineUserDao.insertUser(user)
        }
    }

    private suspend fun deleteUserInfoInDatabase(userId: Long) {
        withContext(Dispatchers.IO) {
            onlineUserDao.deleteUser(userId)
        }
    }

    private suspend fun getUserInfoFromDatabase(userId : Long) : UserWire? {
        var onlineUser : UserWire? = null
        withContext(Dispatchers.IO) {
            val dbUser = onlineUserDao.getUser(userId) ?: return@withContext
            onlineUser = dbUser
        }
        return onlineUser
    }


    private suspend fun getUserInfoFromOnline(userId : Long) : UserWire? {
        var userInfo : UserWire? = null
        withContext(Dispatchers.IO) {
            Networking.GET("v1/userinfo/$userId") { resp ->
                if (resp.status != HttpStatusCode.OK) {
                    Log.w("ShoppingListHandler", "User $userId not found")
                    return@GET
                }
                val body = resp.bodyAsText(Charsets.UTF_8)
                val decoded = Json.decodeFromString<UserWire>(body)
                insertUserInfoInDatabase(decoded)
                userInfo = decoded
            }
        }
        return userInfo
    }

    private suspend fun getUserInfo(userId: Long) : UserWire? {
        var onlineUser : UserWire? = null
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

    // ------------------------------------------------------------------------------
    // Conversion of Lists from and to Wire Format
    // ------------------------------------------------------------------------------

    private suspend fun listMappingToItemWire(item : ListMapping) : ItemWire? {
        val convertedItem = ItemWire(Name="", Icon="", Quantity = 1L, Checked = false)
        withContext(Dispatchers.IO) {
            // The database item being null should NEVER happen!
            val databaseItem = itemDao.getItem(item.ItemID) ?: return@withContext null
            convertedItem.Name = databaseItem.Name
            convertedItem.Icon = databaseItem.Icon
            convertedItem.Quantity = item.Quantity
            convertedItem.Checked = item.Checked
        }
        if (convertedItem.Name == "") {
            return null
        }
        return convertedItem
    }

    suspend fun mappingToItemWithQuantity(mapping: ListMapping) : ItemWithQuantity? {
        var itemWithQuantity : ItemWithQuantity? = null
        withContext(Dispatchers.IO) {
            val databaseItem = itemDao.getItem(mapping.ItemID) ?: return@withContext
            itemWithQuantity = ItemWithQuantity(ID=databaseItem.ID, Name=databaseItem.Name, IconPath=databaseItem.Icon, Quantity=mapping.Quantity, Checked = mapping.Checked, AddedBy = mapping.AddedBy)
        }
        return itemWithQuantity
    }

    private suspend fun shoppingListToWire(list : ShoppingList) : ShoppingListWire {
        val convertedList = ShoppingListWire(ListId = list.ID, Name = list.Name, CreatedBy = list.CreatedBy, LastEdited = list.LastEdited, Items = mutableListOf())
        withContext(Dispatchers.IO) {
            // Retrieve the list
            val itemsInList = mutableListOf<ItemWire>()
            val itemsMapped = mappingDao.getMappingsForList(list.ID)
            for (item in itemsMapped) {
                val itemConvertedToWire = listMappingToItemWire(item) ?: continue
                itemsInList.add(itemConvertedToWire)
            }
            convertedList.Items = itemsInList
        }
        return convertedList
    }

    private fun itemToMapping(item: Item, list : Long) : ListMapping {
        return ListMapping(ID = 0L, item.ID, list, Quantity = 1L, Checked = false, AddedBy = AppUser.ID)
    }

    private suspend fun itemWireToItemAndCreateIfNotExists(itemWire: ItemWire) : Item {
        var item: Item
        withContext(Dispatchers.IO) {
            val dbItem = itemDao.getItemFromName(itemWire.Name)
            if (dbItem == null) {
                item = Item(0, itemWire.Name, itemWire.Icon)
                item.ID = insertItemInDatabase(item)
                return@withContext
            }
            item = dbItem
        }
        return item
    }

    private suspend fun itemWireToListMapping(list : ShoppingListWire, itemWire: ItemWire) : ListMapping {
        val mapping = ListMapping(0L, ItemID = 0L, ListID = list.ListId, Quantity = itemWire.Quantity, Checked = itemWire.Checked, AddedBy = list.CreatedBy.ID)
        withContext(Dispatchers.IO) {
            // The case where we do not have the ID should never happen because
            // all items should be inserted by this point
            val databaseItem = itemDao.getItemFromName(itemWire.Name) ?: return@withContext
            mapping.ItemID = databaseItem.ID
        }
        return mapping
    }

    private suspend fun shoppingListWireToLocal(list : ShoppingListWire) : Triple<ShoppingList, List<ListMapping>, List<Item>> {
        val shoppingList = ShoppingList(list.ListId, list.Name, list.CreatedBy, list.LastEdited)
        val mappings = mutableListOf<ListMapping>()
        val items = mutableListOf<Item>()
        withContext(Dispatchers.IO) {
            for (item in list.Items) {
                val convertedItem = itemWireToItemAndCreateIfNotExists(item)
                items.add(convertedItem)
                val convertedMapping = itemWireToListMapping(list, item)
                mappings.add(convertedMapping)
            }
        }
        return Triple(shoppingList, mappings, items)
    }

    // ------------------------------------------------------------------------------
    // Posting and Getting Lists from Online
    // ------------------------------------------------------------------------------

    private suspend fun postShoppingListOnline(listId : Long) : Boolean {
        var success = false
        withContext(Dispatchers.IO) {
            val list = listDao.getShoppingList(listId)
            if (list == null) {
                Log.i("ShoppingListHandler", "List $listId does not exist. Cannot push online")
                return@withContext
            }
            success = postShoppingListOnline(list)
        }
        return success
    }

    private suspend fun postShoppingListOnline(list : ShoppingList) : Boolean {
        var success = false
        withContext(Dispatchers.IO) {
            // Pushing the user to the server is done by the networking stack
            val listInWireFormat = shoppingListToWire(list)
            val serializedList = Json.encodeToString(listInWireFormat)
            Networking.POST("v1/list", serializedList, { resp ->
                if (resp.status != HttpStatusCode.Created) {
                    Log.w("ShoppingListHandler", "Posting Shopping List online failed")
                    return@POST
                }
                // We don't expect anything from online
                // The list ID is handled locally by us
                success = true
            }, {
                // This is triggered in case we freshly updated the user
                if (AppUser.ID == 0L)
                    return@POST it
                updatedCreatedByForAllLists()
                val decoded = Json.decodeFromString<ShoppingListWire>(it)
                decoded.CreatedBy = ListCreator(AppUser.ID, AppUser.Username)
                return@POST Json.encodeToString(decoded)
            })
        }
        return success
    }

    private suspend fun shareListOnline(listId : Long, sharedWithId: Long) : Boolean {
        var success = false
        withContext(Dispatchers.IO) {
            val sharedListObject = ListShare(listId, sharedWithId)
            val serialized = Json.encodeToString(sharedListObject)
            Networking.POST("v1/share/$listId", serialized) { resp ->
                if (resp.status != HttpStatusCode.Created) {
                    Log.w("ShoppingListHandler", "Failed to share list $listId online")
                    return@POST
                }
                success = true
                Log.d("ShoppingListHandler", "List $listId shared online with $sharedWithId")
            }
        }
        return success
    }


    private suspend fun getShoppingListFromOnline(listId : Long) : ShoppingListWire? {
        var onlineList : ShoppingListWire? = null
        withContext(Dispatchers.IO) {
            Networking.GET("v1/list/$listId") { resp ->
                if (resp.status != HttpStatusCode.OK) {
                    Log.w("ShoppingListHandler", "Failed to GET list $listId from online")
                    return@GET
                }
                val body = resp.bodyAsText(Charsets.UTF_8)
                val decoded = Json.decodeFromString<ShoppingListWire>(body)
                onlineList = decoded
            }
        }
        return onlineList
    }

    private suspend fun getShoppingListsFromOnline(lists : List<Long>) : List<ShoppingListWire> {
        val onlineLists = mutableListOf<ShoppingListWire>()
        withContext(Dispatchers.IO) {
            for(listId in lists) {
                val onlineList = getShoppingListFromOnline(listId)
                if (onlineList != null)
                    onlineLists.add(onlineList)
            }
            Log.d("ShoppingListHandler", "Retrieved ${onlineLists.size}/${lists.size} lists successfully")
        }
        return onlineLists
    }

    private suspend fun getOwnAndSharedShoppingListsFromOnline() : List<ShoppingListWire> {
        val onlineLists = mutableListOf<ShoppingListWire>()
        withContext(Dispatchers.IO) {
            Networking.GET("v1/lists/${AppUser.ID}") { resp ->
                if (resp.status != HttpStatusCode.OK) {
                    Log.w("ShoppingListHandler", "Failed to GET all list from online")
                    return@GET
                }
                val body = resp.bodyAsText(Charsets.UTF_8)
                val decodedShoppingLists = Json.decodeFromString<List<ShoppingListWire>>(body)
                onlineLists.addAll(decodedShoppingLists)
                Log.d("ShoppingListHandler", "Retrieved ${onlineLists.size} lists successfully")
            }
        }
        return onlineLists
    }

    private suspend fun getAllShoppingListsFromOnline() {
        withContext(Dispatchers.IO) {
            val onlineLists = getOwnAndSharedShoppingListsFromOnline()
            for (list in onlineLists) {
                // Automatically creates the items if not existing
                val (localList, mappings, _) = shoppingListWireToLocal(list)
                val updated = updateListInDatabase(localList)
                if (updated) {
                    insertMappingsInDatabase(mappings)
                }
            }
        }
    }

    // ------------------------------------------------------------------------------
    // The Public API
    // ------------------------------------------------------------------------------

    fun PostShoppingListOnline(listId : Long) {
        localCoroutine.launch {
            postShoppingListOnline(listId)
        }
    }

    fun PostShoppingListOnline(list : ShoppingList) {
        localCoroutine.launch {
            postShoppingListOnline(list)
        }
    }

    // Creating a new shopping list with the given name
    // Stores the list locally
    // Stores the list online (if possible)
    // Returns the newly created list
    fun CreateNewShoppingList(name : String) {
        val list = newShoppingList(name)
        localCoroutine.launch {
            list.ID = insertShoppingListIntoDatabase(list)
            val success = postShoppingListOnline(list)
        }
        // Cannot return the list here because we have the asynchronous operations before
    }

    fun DeleteShoppingList(list : ShoppingList) {
        localCoroutine.launch {
            deleteShoppingListFromDatabase(list)
            postShoppingListOnline(list)
        }
    }

    fun AddItemToShoppingList(item : Item, list : Long) {
        localCoroutine.launch {
            val mapping = itemToMapping(item, list)
            insertMappingInDatabase(mapping)
            updateLastEditedInDatabase(list)
            postShoppingListOnline(list)
        }
    }

    fun AddItemAndAddToShoppingList(item : Item, list : Long) {
        localCoroutine.launch {
            val insertedId = insertItemInDatabase(item)
            item.ID = insertedId
            AddItemToShoppingList(item, list)
        }
    }

    fun DeleteItemFromShoppingList(item : Item, list : Long) {
        localCoroutine.launch {
            deleteMappingInDatabase(item.ID, list)
            updateLastEditedInDatabase(list)
            postShoppingListOnline(list)
        }
    }

    fun ToggleItemInShoppingList(itemId : Long, listId : Long) {
        localCoroutine.launch {
            updateLastEditedInDatabase(listId)
            toggleMappingInDatabase(itemId, listId)
            postShoppingListOnline(listId)
        }
    }

    fun IncreaseItemCountInShoppingList(itemId : Long, listId : Long, count : Long = 1) {
        localCoroutine.launch {
            updateLastEditedInDatabase(listId)
            increaseItemCountInDatabase(itemId, listId, count)
            postShoppingListOnline(listId)
        }
    }

    fun DecreaseItemCountInShoppingList(itemId : Long, listId : Long, count : Long = 1) {
        localCoroutine.launch {
            updateLastEditedInDatabase(listId)
            increaseItemCountInDatabase(itemId, listId, -1 * count)
            postShoppingListOnline(listId)
        }
    }

    fun GetShoppingList(listId : Long) {
        // Automatically updating the list in the database
        // No need to return the list
        localCoroutine.launch {
            val onlineList = getShoppingListFromOnline(listId) ?: return@launch
            // Automatically creates the items if not existing
            val (list, mappings, _) = shoppingListWireToLocal(onlineList)
//            insertItemsInDatabase(items)
            val updated = updateListInDatabase(list)
            if (updated) {
                // Only update the mappings in case we received a newer list
                insertMappingsInDatabase(mappings)
            }
        }
    }

    fun GetAllShoppingLists() {
        localCoroutine.launch {
            getAllShoppingListsFromOnline()
        }
    }

    fun ShareShoppingListOnline(listId: Long, sharedWithId : Long) {
        Log.d("ShoppingListHandler", "Sharing list $listId with $sharedWithId")
        localCoroutine.launch {
            postShoppingListOnline(listId)
            shareListOnline(listId, sharedWithId)
        }
    }

}