package com.cloudsheeptech.shoppinglist.list

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.data.Item
import com.cloudsheeptech.shoppinglist.data.ItemWithQuantity
import com.cloudsheeptech.shoppinglist.data.ListMapping
import com.cloudsheeptech.shoppinglist.data.ShoppingList
import com.cloudsheeptech.shoppinglist.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.datastructures.ItemListWithName
import com.cloudsheeptech.shoppinglist.network.Networking
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class ShoppinglistViewModel(val list: ItemListWithName<Item>, val database: ShoppingListDatabase, val shoppingListId : Long) : ViewModel() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val databaseDao = database.itemListDao()
    private val mappingDao = database.mappingDao()
    private val listDao = database.shoppingListDao()

    private val _refreshing = MutableLiveData<Boolean>()
    val refreshing : LiveData<Boolean>
        get() = _refreshing

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

    private val _listInformation = listDao.getShoppingList(shoppingListId)
    val listInformation : LiveData<ShoppingList> get() = _listInformation

    val itemName = MutableLiveData<String>("")
    var title = MutableLiveData<String>("Liste")

    // ----

    init {
        _refreshing.value = false
//        if (shoppingListId > 0)
//            shoppingListData = databaseDao.getItemLive(shoppingListId)
    }

    fun updateShoppinglist() {
        scope.launch {
            withContext(Dispatchers.Main) {
                _refreshing.value = true
            }
            // TODO: Implement refreshing of this concrete list
            Networking.GET("list/$shoppingListId") { resp ->
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
                    val body = resp.body<List<ItemWithQuantity>>()
                    // TODO: Update the current list with what was found online
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
        val item = Item(ID = 0, Name=itemName.value!!, ImagePath = "ic_item")
        scope.launch {
            addItemToDatabase(item)
        }
        hideKeyboard()
        clearItemNameInput()
    }

    private suspend fun addItemToDatabase(item : Item) {
        withContext(Dispatchers.IO) {
            var currentId = databaseDao.getCurrentId()
            currentId += 1
//            Log.d("ShoppinglistViewModel", "Current ID is: $currentId")
            item.ID = currentId
            databaseDao.insertItem(item)
            Log.d("ShoppinglistViewModel", "Added item $item into database")
            val rnd = Random.nextLong()
            val mapping = ListMapping(rnd, item.ID, shoppingListId, 1, false)
            mappingDao.insertMapping(mapping)
            Log.d("ShoppinglistViewModel", "Added mapping $mapping for item")
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
                zipped.add(ItemWithQuantity(item.ID, item.Name, item.ImagePath, quant!!.second, quant.third, 0L))
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

    fun increaseItemCount(itemId : Int) {
        scope.launch {
            Log.d("ShoppinglistViewModel", "Tapped on item with ID: $itemId")
            val mapping = getMapping(itemId.toLong()) ?: return@launch
            mapping.Quantity += 1
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