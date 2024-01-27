package com.cloudsheeptech.shoppinglist.data.handling

import android.util.Log
import com.cloudsheeptech.shoppinglist.data.Item
import com.cloudsheeptech.shoppinglist.data.ItemWire
import com.cloudsheeptech.shoppinglist.data.ListMapping
import com.cloudsheeptech.shoppinglist.data.ShoppingList
import com.cloudsheeptech.shoppinglist.data.ShoppingListWire
import com.cloudsheeptech.shoppinglist.data.User
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
import kotlin.random.Random
import kotlin.random.nextUInt

class ShoppingListHandler(val database : ShoppingListDatabase) {

    private val job = Job()
    private val localCoroutine = CoroutineScope(Dispatchers.Main + job)

    private val listDao = database.shoppingListDao()
    private val itemDao = database.itemListDao()
    private val mappingDao = database.mappingDao()

    private fun assembleShoppingList(name : String) : ShoppingList {
        val now = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        val createdBy = AppUser.getUser()
        // Try our best to still assign a non 0 ID and bet on random to be different for a while
        val listId = Random.nextUInt().toLong()
        val list = ShoppingList(ID=listId, Name=name, CreatedBy = createdBy, LastEdited = now)
        Log.d("ShoppingListHandler", "Created List: $list")
        return list
    }

    private suspend fun insertShoppingListIntoDatabase(list : ShoppingList) : Long {
        var insertedListId = list.ID
        withContext(Dispatchers.IO) {
            var existingList = listDao.getShoppingList(insertedListId)
            while (existingList.value != null) {
                insertedListId = Random.nextUInt().toLong()
                existingList = listDao.getShoppingList(insertedListId)
            }
            list.ID = insertedListId
            listDao.insertList(list)
            Log.d("ShoppingListHandler", "Inserted list ${list.ID} into database")
        }
        return insertedListId
    }

    private suspend fun updateListIdInDatabase(list : ShoppingList) {
        withContext(Dispatchers.IO) {
            listDao.updateList(list)
            Log.d("ShoppingListHandler", "Updated list ${list.ID} in database")
        }
    }

    private suspend fun convertItemToItemWire(item : ListMapping) : ItemWire? {
        val convertedItem = ItemWire(Name="", Icon="", Quantity = 1L, Checked = false)
        withContext(Dispatchers.IO) {
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

    private suspend fun convertShoppingListToWireFormat(list : ShoppingList) : ShoppingListWire {
        val convertedList = ShoppingListWire(ListId = list.ID, Name = list.Name, CreatedBy = list.CreatedBy.ID, LastEdited = list.LastEdited, Items = mutableListOf())
        withContext(Dispatchers.IO) {
            // Retrieve the list
            val itemsInList = mutableListOf<ItemWire>()
            val itemsMapped = mappingDao.getMappingsForList(list.ID)
            for (item in itemsMapped) {
                val itemConvertedToWire = convertItemToItemWire(item) ?: continue
                itemsInList.add(itemConvertedToWire)
            }
            convertedList.Items = itemsInList
        }
        return convertedList
    }

    private suspend fun postShoppingListOnline(list : ShoppingList) {
        withContext(Dispatchers.IO) {
            val listInWireFormat = convertShoppingListToWireFormat(list)
            val serializedList = Json.encodeToString(listInWireFormat)
            Networking.POST("v1/list", serializedList) { resp ->
                if (resp.status != HttpStatusCode.Created) {
                    Log.w("ShoppingListHandler", "Posting Shopping List online failed")
                    return@POST
                }
                // We don't expect anything from online
                // The list ID is handled locally by us
            }
            updateListIdInDatabase(list)
        }
    }

    fun PostShoppingListOnline(list : ShoppingList) {
        
    }

    // Creating a new shopping list with the given name
    // Includes storing the list locally
    // Includes pushing the list to the server online (if possible)
    fun CreateNewShoppingList(name : String) {
        val list = assembleShoppingList(name)
        localCoroutine.launch {
            list.ID = insertShoppingListIntoDatabase(list)
            postShoppingListOnline(list)
        }
    }

    fun DeleteShoppingList() {

    }

    private suspend fun createMappingsInDatabase(mappings : List<ListMapping>) {
        withContext(Dispatchers.IO) {
            for (mapping in mappings) {
                mappingDao.insertMapping(mapping)
            }
        }
    }

    private suspend fun createItemInDatabase(name: String, icon : String) : Item {
        var item = Item(0L, name, icon)
        withContext(Dispatchers.IO) {
            itemDao.insertItem(item)
            // If this below fails, we got a bigger problem in the code
            item = itemDao.getItemFromName(name)!!
        }
        return item
    }

    private suspend fun convertItemWireToItem(itemWire: ItemWire) : Item {
        var item: Item
        withContext(Dispatchers.IO) {
            item = itemDao.getItemFromName(itemWire.Name) ?: createItemInDatabase(
                itemWire.Name,
                itemWire.Icon
            )
        }
        return item
    }

    private suspend fun convertItemToListMapping(list : ShoppingListWire, itemWire: ItemWire) : ListMapping {
        val mapping = ListMapping(0L, ItemID = 0L, ListID = list.ListId, Quantity = itemWire.Quantity, Checked = itemWire.Checked, AddedBy = list.CreatedBy)
        withContext(Dispatchers.IO) {
            val databaseItem = itemDao.getItemFromName(itemWire.Name) ?: return@withContext
            mapping.ItemID = databaseItem.ID
        }
        return mapping
    }

    private suspend fun convertShoppingListWireToLocalFormat(list : ShoppingListWire) : Triple<ShoppingList, List<ListMapping>, List<Item>> {
        val shoppingList = ShoppingList(list.ListId, list.Name, User(list.CreatedBy), list.LastEdited)
        val mappings = mutableListOf<ListMapping>()
        val items = mutableListOf<Item>()
        withContext(Dispatchers.IO) {
            for (item in list.Items) {
                val convertedItem = convertItemWireToItem(item)
                items.add(convertedItem)
                val convertedMapping = convertItemToListMapping(list, item)
                mappings.add(convertedMapping)
            }
        }
        return Triple(shoppingList, mappings, items)
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

    fun GetShoppingList(listId : Long) {
        localCoroutine.launch {
            val onlineList = getShoppingListFromOnline(listId) ?: return@launch
            // Automatically creates the items if not existing
            val convertedTriple = convertShoppingListWireToLocalFormat(onlineList)
            val list = convertedTriple.first
            val mappings = convertedTriple.second
            // Items already inserted, no need to process them here
            insertShoppingListIntoDatabase(list)
            createMappingsInDatabase(mappings)
        }
    }

}