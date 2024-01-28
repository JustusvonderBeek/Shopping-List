package com.cloudsheeptech.shoppinglist.fragments.list

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.cloudsheeptech.shoppinglist.data.Item
import com.cloudsheeptech.shoppinglist.data.ItemWithQuantity
import com.cloudsheeptech.shoppinglist.data.ListMapping
import com.cloudsheeptech.shoppinglist.data.ListShare
import com.cloudsheeptech.shoppinglist.data.ShoppingList
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.handling.ShoppingListHandler
import com.cloudsheeptech.shoppinglist.network.Networking
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ShoppinglistViewModel(val database: ShoppingListDatabase, private val shoppingListId : Long) : ViewModel() {

    private val listDao = database.shoppingListDao()
    private val itemDao = database.itemDao()
    private val mappingDao = database.mappingDao()
    private val listHandler = ShoppingListHandler(database)

    val itemName = MutableLiveData<String>("")
    val title = MutableLiveData<String>("Liste")

    // UI State

    private val _refreshing = MutableLiveData<Boolean>(false)
    val refreshing : LiveData<Boolean> get() = _refreshing

    // Navigation
    private val _navigateToAddWord = MutableLiveData<Boolean>(false)
    val navigateToAdd : LiveData<Boolean> get() = _navigateToAddWord

    private val _navigateToEditWord = MutableLiveData<Int>(-1)
    val navigateToEdit : LiveData<Int> get() = _navigateToEditWord

    private val _hideKeyboard = MutableLiveData<Boolean>(false)
    val hideKeyboard : LiveData<Boolean> get() = _hideKeyboard

    private val _toggleItem = MutableLiveData<Int>()
    val toggleItem : LiveData<Int> get() = _toggleItem

    private val _shoppinglist = liveData<List<ItemWithQuantity>> {
        val listLive = listDao.getShoppingListLive(shoppingListId)
        val mappingLive = mappingDao.getMappingsForListLive(shoppingListId)
        if (mappingLive.value == null)
            return@liveData
        val itemList = mutableListOf<ItemWithQuantity>()
        for(mapping in mappingLive.value!!) {
            withContext(Dispatchers.IO) {
                val itemWithQuant = listHandler.mappingToItemWithQuantity(mapping) ?: return@withContext
                itemList.add(itemWithQuant)
            }
        }
        emit(itemList)
    }

    val shoppinglist : LiveData<List<ItemWithQuantity>> get() = _shoppinglist
    val mappedItemIds = mappingDao.getMappingsForListLive(shoppingListId)

    private val _previewItems = MutableLiveData<List<Item>>()
    val previewItems : LiveData<List<Item>> get() = _previewItems

    private val _listInformation = listDao.getShoppingListLive(shoppingListId)
    val listInformation : LiveData<ShoppingList> get() = _listInformation

    // ----

    fun updateShoppinglist() {
        _refreshing.value = true
        listHandler.GetShoppingList(shoppingListId)
        _refreshing.value = false
    }

    fun addItem() {
        Log.d("ShoppinglistViewModel", "Adding new item to list")
        if (itemName.value == null || itemName.value!!.isEmpty()) {
            Log.i("ShoppinglistViewModel", "Do not add empty item")
            return
        }
        val item = Item(ID = 0, Name=itemName.value!!, Icon = "ic_item")
        listHandler.AddItemAndAddToShoppingList(item, shoppingListId)
        hideKeyboard()
        clearItemNameInput()
    }

    fun toggleItem(itemId : Long) {
        Log.d("ShoppinListViewModel", "Toggle item $itemId")
        listHandler.ToggleItemInShoppingList(itemId, shoppingListId)
    }


    fun increaseItemCount(itemId : Int, quantity : Long = 1L) {
        listHandler.IncreaseItemCountInShoppingList(itemId.toLong(), shoppingListId, quantity)
    }

    fun decreaseItemCount(itemId : Int) {
        listHandler.DecreaseItemCountInShoppingList(itemId.toLong(), shoppingListId)
    }

    private fun pushListToServer() {
        Log.d("ShoppinglistViewModel", "Pushing list with ${_shoppinglist.value!!.size} to server")
        listHandler.PostShoppingListOnline(shoppingListId)
    }

//    fun reloadItemsInList(itemIds : List<ListMapping>) {
//        // Takes the list of currently contained IDs and updates the items in the shopping list
//        val ids : List<Triple<Long, Long, Boolean>> = itemIds.map { map -> Triple(map.ItemID, map.Quantity, map.Checked) }
//        scope.launch {
//            loadItemsInListFromDatabase(ids)
//        }
//    }
//
//    private suspend fun loadItemsInListFromDatabase(itemIds : List<Triple<Long, Long, Boolean>>) {
//         withContext(Dispatchers.IO) {
//            Log.d("ShoppinglistViewModel", "Loading the current items for list $shoppingListId from the database")
//            val items = databaseDao.getItems(itemIds.map { it.first })
//            val zipped = mutableListOf<ItemWithQuantity>()
//            for (item in items) {
//                val quant = itemIds.find { s -> s.first == item.ID }
//                zipped.add(ItemWithQuantity(item.ID, item.Name, item.Icon, quant!!.second, quant.third, 0L))
//            }
//            withContext(Dispatchers.Main) {
//                _shoppinglist.value = zipped
//            }
//        }
//    }

//    fun showItemPreview(enteredName : String) {
//        Log.d("ShoppinglistViewModel", "User entered: $enteredName")
//        if (enteredName.isEmpty()) {
//            _previewItems.value = emptyList()
//            return
//        }
//        scope.launch {
//            loadMatchingItems(enteredName)
//        }
//    }

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
//
//    fun addTappedItem(id : Long) {
//        Log.d("ShoppinglistViewModel", "Adding item with ID $id")
//        scope.launch {
//            val item = itemDao.getItem(id) ?: return@launch
//            Log.d("ShoppinglistViewModel", "Found item to add")
//            addItemToList(item)
//        }
//    }

    private suspend fun loadItemFromDatabase(id : Long) : Item? {
        val item = withContext(Dispatchers.IO) {
            return@withContext itemDao.getItem(id)
        }
        return item
    }

//    fun shareThisList() {
//        scope.launch {
//            shareListOnline()
//        }
//    }

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
//                val body = resp.bodyAsText(Charsets.UTF_8)
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

    private fun clearItemNameInput() {
        itemName.value = ""
    }

    private fun hideKeyboard() {
        _hideKeyboard.value = true
    }

    fun keyboardHidden() {
        _hideKeyboard.value = false
    }

}