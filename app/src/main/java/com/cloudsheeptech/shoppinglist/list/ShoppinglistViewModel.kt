package com.cloudsheeptech.shoppinglist.list

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.data.AppUser
import com.cloudsheeptech.shoppinglist.data.Item
import com.cloudsheeptech.shoppinglist.data.ItemWire
import com.cloudsheeptech.shoppinglist.data.ItemWithQuantity
import com.cloudsheeptech.shoppinglist.data.ListMapping
import com.cloudsheeptech.shoppinglist.data.ListShare
import com.cloudsheeptech.shoppinglist.data.ShoppingList
import com.cloudsheeptech.shoppinglist.data.ShoppingListWire
import com.cloudsheeptech.shoppinglist.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.datastructures.ItemListWithName
import com.cloudsheeptech.shoppinglist.network.Networking
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.random.Random

class ShoppinglistViewModel(val list: ItemListWithName<Item>, val database: ShoppingListDatabase, val shoppingListId : Long) : ViewModel() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val databaseDao = database.itemListDao()
    private val mappingDao = database.mappingDao()
    private val listDao = database.shoppingListDao()
    private val itemDao = database.itemListDao()

    private val _refreshing = MutableLiveData<Boolean>()
    val refreshing : LiveData<Boolean>
        get() = _refreshing

    val itemName = MutableLiveData<String>("")
    val title = MutableLiveData<String>("Liste")

    // Navigation
    private val _navigateToAddWord = MutableLiveData<Boolean>(false)
    val navigateToAdd : LiveData<Boolean> get() = _navigateToAddWord
    private val _navigateToEditWord = MutableLiveData<Int>(-1)
    val navigateToEdit : LiveData<Int> get() = _navigateToEditWord
    private val _hideKeyboard = MutableLiveData<Boolean>(false)
    val hideKeyboard : LiveData<Boolean> get() = _hideKeyboard

    private val _toggleItem = MutableLiveData<Int>()
    val toggleItem : LiveData<Int> get() = _toggleItem

    private val _shoppinglist = MutableLiveData<List<ItemWithQuantity>>()
    val shoppinglist : LiveData<List<ItemWithQuantity>> get() = _shoppinglist
    val mappedItemIds = mappingDao.getMappingsForListLive(shoppingListId)

    private val _previewItems = MutableLiveData<List<Item>>()
    val previewItems : LiveData<List<Item>> get() = _previewItems

    private val _listInformation = listDao.getShoppingList(shoppingListId)
    val listInformation : LiveData<ShoppingList> get() = _listInformation

    // ----

    init {
        _refreshing.value = false
//        if (shoppingListId > 0)
//            shoppingListData = databaseDao.getItemLive(shoppingListId)
    }

    private suspend fun updateLocalList(list : ShoppingListWire) {
        withContext(Dispatchers.IO) {
            for (item in list.Items) {
                var dbItem = itemDao.getItemFromName(item.Name)
                if (dbItem == null) {
                    dbItem = Item(ID = 0, Name=item.Name, Icon = item.Icon)
                }
                addItemToDatabase(dbItem)
                setItemInList(dbItem, item.Quantity, item.Checked)
            }
        }
        withContext(Dispatchers.Main) {
            title.value = list.Name
        }
    }

    fun updateShoppinglist() {
        scope.launch {
            withContext(Dispatchers.Main) {
                _refreshing.value = true
            }
            Networking.GET("v1/list/$shoppingListId") { resp ->
                Log.d("ShoppinglistViewModel", "Got response with updated list")
                try {
                    if (resp.status == HttpStatusCode.NotFound) {
                        Log.w("ShoppinglistViewModel", "Queried resource not found")
                        return@GET
                    }
                    if (resp.status == HttpStatusCode.Unauthorized) {
                        Log.w("ShoppinglistViewModel", "Unauthorized. Try repeating the request")
                        return@GET
                    }
                    if (resp.status != HttpStatusCode.OK) {
                        Log.w("ShoppinglistViewModel", "Request was not successful! HTTP: ${resp.status}")
                        return@GET
                    }
                    val body = resp.bodyAsText(Charsets.UTF_8)
                    val list = Json.decodeFromString<ShoppingListWire>(body)
                    Log.d("ShoppinglistViewModel", "Got list: $list")
                    updateLocalList(list)
                } catch (ex : NoTransformationFoundException) {
                    Log.w("ShoppinglistViewModel", "The received data is in incorrect format!")
                    return@GET
                }
            }
            withContext(Dispatchers.Main) {
                _refreshing.value = false
            }
        }
    }

    fun addItem() {
        Log.d("ShoppinglistViewModel", "Adding new item to list")
        if (itemName.value == null || itemName.value!!.isEmpty()) {
            Log.i("ShoppinglistViewModel", "Do not add empty item")
            return
        }
        val item = Item(ID = 0, Name=itemName.value!!, Icon = "ic_item")
        scope.launch {
            val databaseItem = addItemToDatabase(item)
            addItemToList(databaseItem)
            pushListToServer()
        }
        hideKeyboard()
        clearItemNameInput()
    }

    private suspend fun setItemInList(item : Item, quantity: Long = 1L, checked: Boolean = false) {
        withContext(Dispatchers.IO) {
            val itemsInList = mappingDao.getMappingForItemAndList(item.ID, shoppingListId)
            var rnd = Random.nextLong()
            if (!itemsInList.isEmpty()) {
                rnd = itemsInList[0].ID
            }
            val mapping = ListMapping(rnd, item.ID, shoppingListId, quantity, checked, AppUser.ID)
            mappingDao.updateMapping(mapping)
        }
    }

    private suspend fun addItemToList(item : Item, quantity : Long = 1L, checked : Boolean = false) {
        withContext(Dispatchers.IO) {
            // Check if the item is already in the list
            val itemsInList = mappingDao.getMappingForItemAndList(item.ID, shoppingListId)
            if (itemsInList.isNotEmpty()) {
                Log.d("ShoppinglistViewModel", "The item is already in the list")
                if (quantity != 1L) {
                    setItemQuantity(item.ID.toInt(), quantity)
                } else {
                    increaseItemCount(item.ID.toInt(), quantity)
                }
                return@withContext
            }
            val rnd = Random.nextLong()
            val mapping = ListMapping(rnd, item.ID, shoppingListId, quantity, checked, AppUser.ID)
            mappingDao.insertMapping(mapping)
            Log.d("ShoppinglistViewModel", "Added mapping $mapping for item")
        }
    }

    private suspend fun pushItemToServer(item : Item) : Item {
        var responseItem = item
        withContext(Dispatchers.IO) {
            val encoded = Json.encodeToString(item)
            Networking.POST("v1/item", encoded) { resp ->
                if (resp.status != HttpStatusCode.Created) {
                    Log.w("ShoppinglistViewModel", "Failed to push item to server")
                    return@POST
                }
                Log.i("ShoppinglistViewModel", "Pushed item ${item.ID} to server")
                val body = resp.bodyAsText(Charsets.UTF_8)
                responseItem = Json.decodeFromString<Item>(body)
                Log.d("ShoppinglistViewModel", "Got item ID ${responseItem.ID} back")
            }
        }
        return responseItem
    }

    private suspend fun addItemToDatabase(item : Item) : Item {
        val returnItem = withContext(Dispatchers.IO) {
            // Check if an item with the same name already exists
            val possibleItem = itemDao.getItemFromName(item.Name)
            if (possibleItem != null) {
                return@withContext possibleItem
            }
//            val updateItem = pushItemToServer(item)
            val currentId = databaseDao.getCurrentId() + 1
//            if (updateItem.ID == 0L) {
//                currentId += 1
//            } else {
//                currentId = updateItem.ID
//            }
//            Log.d("ShoppinglistViewModel", "Current ID is: $currentId")
            item.ID = currentId
            databaseDao.insertItem(item)
            Log.d("ShoppinglistViewModel", "Added item $item into database")
//            pushItemToServer(item)
            return@withContext item
        }
        return returnItem
    }

    private suspend fun createWireList() : ShoppingListWire {
        val list = withContext(Dispatchers.IO) {
            val nowFormatted = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            val wireList = ShoppingListWire(shoppingListId, title.value!!, AppUser.ID, nowFormatted, mutableListOf())
            val itemsInList = mappingDao.getMappingsForList(shoppingListId)
            if (itemsInList.isEmpty())
                return@withContext wireList
            for (item in itemsInList) {
                val dbItem = itemDao.getItem(item.ItemID) ?: continue
                val converted = ItemWire(dbItem.Name, dbItem.Icon, item.Quantity, item.Checked)
                wireList.Items.add(converted)
            }
            return@withContext wireList
        }
        return list
    }

    private suspend fun pushListToServer() {
        Log.d("ShoppinglistViewModel", "Pushing list with ${_shoppinglist.value!!.size} to server")

        withContext(Dispatchers.IO) {
            val wireList = createWireList()
            //            val itemsInList = mappingDao.getMappingsForList(shoppingListId)
//            if (itemsInList.isEmpty())
//                return@withContext
            val encoded = Json.encodeToString(wireList)
            Networking.POST("v1/list", encoded) { resp ->
                Log.d("ShoppinglistViewModel", "Got an answer with updated mapping ids")
                if (resp.status != HttpStatusCode.Created) {
                    Log.w("ShoppinglistViewModel", "Failed to push list to server")
                    return@POST
                }
                Log.i("ShoppinglistViewModel", "Pushed list to server")
            }
        }
    }

    fun clearAll() {
        scope.launch {
            clearItemDatabase()
        }
    }

    private suspend fun clearItemDatabase() {
        withContext(Dispatchers.IO) {
            databaseDao.deleteAll()
        }
    }

    fun reloadItemsInList(itemIds : List<ListMapping>) {
        // Takes the list of currently contained IDs and updates the items in the shopping list
        val ids : List<Triple<Long, Long, Boolean>> = itemIds.map { map -> Triple(map.ItemID, map.Quantity, map.Checked) }
        scope.launch {
            loadItemsInListFromDatabase(ids)
        }
    }

    private suspend fun loadItemsInListFromDatabase(itemIds : List<Triple<Long, Long, Boolean>>) {
         withContext(Dispatchers.IO) {
            Log.d("ShoppinglistViewModel", "Loading the current items for list $shoppingListId from the database")
            val items = databaseDao.getItems(itemIds.map { it.first })
            val zipped = mutableListOf<ItemWithQuantity>()
            for (item in items) {
                val quant = itemIds.find { s -> s.first == item.ID }
                zipped.add(ItemWithQuantity(item.ID, item.Name, item.Icon, quant!!.second, quant.third, 0L))
            }
            withContext(Dispatchers.Main) {
                _shoppinglist.value = zipped
            }
        }
    }

    fun checkItem(itemId : Int) {
        Log.d("ShoppinglistViewModel", "Toggle item $itemId")
        scope.launch {
            toggleItem(itemId)
        }
    }

    private suspend fun toggleItem(itemId: Int) {
        withContext(Dispatchers.IO) {
            val mapping = getMapping(itemId.toLong()) ?: return@withContext
            mapping.Checked = mapping.Checked xor true
            setMapping(mapping)
        }
    }

    fun setItemQuantity(itemId : Int, quantity: Long = 1L) {
        scope.launch {
            val mapping = getMapping(itemId.toLong()) ?: return@launch
            mapping.Quantity = quantity
            setMapping(mapping)
        }
    }

    fun increaseItemCount(itemId : Int, quantity : Long = 1L) {
        scope.launch {
            Log.d("ShoppinglistViewModel", "Tapped on item with ID: $itemId")
            val mapping = getMapping(itemId.toLong()) ?: return@launch
            mapping.Quantity += quantity
            setMapping(mapping)

        }
    }

    fun decreaseItemCount(itemId : Int) {
        scope.launch {
            Log.d("ShoppinglistViewModel", "Tapped on item with ID: $itemId")
            val mapping = getMapping(itemId.toLong()) ?: return@launch
            mapping.Quantity -= 1
            if (mapping.Quantity == 0L) {
                // Remove the mapping
                removeMapping(mapping)
                return@launch
            }
            setMapping(mapping)
        }
    }

    private suspend fun getMapping(itemId : Long) : ListMapping? {
        val m = withContext(Dispatchers.IO) {
            val mapping = mappingDao.getMappingForItemAndList(itemId, shoppingListId)
            if (mapping.isEmpty()) {
                return@withContext null
            }
            return@withContext mapping.first()
        }
        return m
    }

    private suspend fun setMapping(mapping : ListMapping) {
        withContext(Dispatchers.IO) {
            Log.d("ShoppinglistViewModel", "Updating mapping for $mapping")
            mappingDao.updateMapping(mapping)
        }
    }

    private suspend fun removeMapping(mapping: ListMapping) {
        withContext(Dispatchers.IO) {
            mappingDao.deleteMapping(mapping.ID)
        }
    }

    fun showItemPreview(enteredName : String) {
        Log.d("ShoppinglistViewModel", "User entered: $enteredName")
        if (enteredName.isEmpty()) {
            _previewItems.value = emptyList()
            return
        }
        scope.launch {
            loadMatchingItems(enteredName)
        }
    }

    fun clearItemPreview() {
        itemName.value = ""
    }

    private suspend fun loadMatchingItems(name : String) {
        withContext(Dispatchers.IO) {
            val items = itemDao.getItemsFromName(name)
            Log.d("ShoppinglistViewModel", "Got ${items.size} from database")
            withContext(Dispatchers.Main) {
                _previewItems.value = items
            }
        }
    }

    fun addTappedItem(id : Long) {
        Log.d("ShoppinglistViewModel", "Adding item with ID $id")
        scope.launch {
            val item = itemDao.getItem(id) ?: return@launch
            Log.d("ShoppinglistViewModel", "Found item to add")
            addItemToList(item)
        }
    }

    private suspend fun loadItemFromDatabase(id : Long) : Item? {
        val item = withContext(Dispatchers.IO) {
            return@withContext itemDao.getItem(id)
        }
        return item
    }

    fun shareThisList() {
        scope.launch {
            shareListOnline()
        }
    }

    private suspend fun shareListOnline() {
        withContext(Dispatchers.IO) {
            Log.d("ShoppinglistViewModel", "Sharing list $shoppingListId online")
            val sharedList = ListShare(0, shoppingListId, -1)
            val encoded = Json.encodeToString(sharedList)
            Networking.POST("v1/share/$shoppingListId", encoded) { resp ->
                if (resp.status == HttpStatusCode.BadRequest) {
                    Log.d("ShoppinglistViewModel", "Internal error, made bad request")
                    return@POST
                }
                val body = resp.bodyAsText(Charsets.UTF_8)
            }
        }
    }

    fun onEditWordNavigated() {
        _navigateToEditWord.value = -1
    }

    fun navigateToAddWord() {
        _navigateToAddWord.value = true
    }

    fun onAddWordNavigated() {
        _navigateToAddWord.value = false
    }

    fun clearItemNameInput() {
        itemName.value = ""
    }

    private fun hideKeyboard() {
        _hideKeyboard.value = true
    }

    fun keyboardHidden() {
        _hideKeyboard.value = false
    }

}