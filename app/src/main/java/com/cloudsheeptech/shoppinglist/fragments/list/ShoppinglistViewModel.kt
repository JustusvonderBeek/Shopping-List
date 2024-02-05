package com.cloudsheeptech.shoppinglist.fragments.list

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.data.Item
import com.cloudsheeptech.shoppinglist.data.ItemWithQuantity
import com.cloudsheeptech.shoppinglist.data.ListMapping
import com.cloudsheeptech.shoppinglist.data.ShoppingList
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.handling.ShoppingListHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.DEFAULT_CONCURRENCY_PROPERTY_NAME
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShoppinglistViewModel(val database: ShoppingListDatabase, private val shoppingListId : Long) : ViewModel() {

    private val listDao = database.shoppingListDao()
    private val itemDao = database.itemDao()
    private val mappingDao = database.mappingDao()
    private val listHandler = ShoppingListHandler(database)

    val itemName = MutableLiveData<String>("")
    val title = MutableLiveData<String>("Liste")

    private val job = Job()
    private val localCoroutine = CoroutineScope(Dispatchers.Main + job)

    // UI State

    private val _refreshing = MutableLiveData<Boolean>(false)
    val refreshing : LiveData<Boolean> get() = _refreshing

    // Navigation
    private val _navigateUp = MutableLiveData<Boolean>(false)
    val navigateUp : LiveData<Boolean> get() = _navigateUp

    private val _navigateShare = MutableLiveData<Long>(-1)
    val navigateShare : LiveData<Long> get() = _navigateShare

    private val _hideKeyboard = MutableLiveData<Boolean>(false)
    val hideKeyboard : LiveData<Boolean> get() = _hideKeyboard

    private val _confirmDelete = MutableLiveData<Boolean>(false)
    val confirmDelete : LiveData<Boolean> get() = _confirmDelete

    private val _confirmClear = MutableLiveData<Boolean>(false)
    val confirmClear : LiveData<Boolean> get() = _confirmClear

    // ---

    private val itemsMappedToList = mappingDao.getMappingsForListLive(shoppingListId)
    // The items in this list
    val itemsInList = MediatorLiveData<List<ItemWithQuantity>>()

    private val _previewItems = MutableLiveData<List<Item>>()
    val previewItems : LiveData<List<Item>> get() = _previewItems

    private val _listInformation = listDao.getShoppingListLive(shoppingListId)
    val listInformation : LiveData<ShoppingList> get() = _listInformation

    init {
        itemsInList.addSource(itemsMappedToList) { mappings ->
            // When the mappings change, re-fetch the items
            fetchItemsForList(mappings)
        }
    }

    // ----

    private fun fetchItemsForList(mappings : List<ListMapping>) {
        if (mappings.isEmpty()) {
            itemsInList.value = emptyList()
            return
        }
        val itemIds = mappings.map { it -> it.ItemID }
        val liveItems = itemDao.getItemsLive(itemIds)
        val quantityItems = mutableListOf<ItemWithQuantity>()
        itemsInList.addSource(liveItems) {
            mappings.forEachIndexed { index, listMapping ->
                // Order might not match
                val matchingItem = liveItems.value!!.first { x -> x.ID == listMapping.ItemID }
                val quantItem = combineMappingAndItemToItemWithQuantity(listMapping, matchingItem)
                quantityItems.add(quantItem)
            }
            itemsInList.value = quantityItems
        }
    }

    private fun combineMappingAndItemToItemWithQuantity(mapping: ListMapping, item : Item) : ItemWithQuantity {
        return ItemWithQuantity(mapping.ItemID, item.Name, item.Icon, mapping.Quantity, mapping.Checked, mapping.AddedBy)
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
        Log.d("ShoppinglistViewModel", "Pushing list with ${itemsInList.value?.size} to server")
        listHandler.PostShoppingListOnline(shoppingListId)
    }

    fun updateShoppinglist() {
        _refreshing.value = true
        listHandler.GetShoppingList(shoppingListId)
        _refreshing.value = false
    }


    fun showItemPreview(enteredName : String) {
        Log.d("ShoppinglistViewModel", "User entered: $enteredName")
        if (enteredName.isEmpty()) {
            _previewItems.value = emptyList()
            return
        }
        localCoroutine.launch {
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

    private suspend fun addItemFromPreviewToList(itemId: Long) {
        withContext(Dispatchers.IO) {
            val item = itemDao.getItem(itemId) ?: return@withContext
//            Log.d("ShoppinglistViewModel", "Found item to add")
            listHandler.AddItemToShoppingList(item, shoppingListId)
        }
    }

    fun AddTappedItem(itemId : Long) {
        Log.d("ShoppinglistViewModel", "Adding item with ID $itemId")
        localCoroutine.launch {
            addItemFromPreviewToList(itemId)
        }
    }

    fun shareThisList() {
        // TODO: Let user decide who to share the list with
        // For now use '-1' == 'all'
//        listHandler.ShareShoppingListOnline(shoppingListId, -1)
        navigateToShare()
    }

    fun deleteThisList() {
        _confirmDelete.value = true
    }

    fun onDeleteConfirmed() {
        _confirmDelete.value = false
        listHandler.DeleteShoppingList(shoppingListId)
        navigateUp()
    }

    fun onDeleteCanceled() {
        _confirmDelete.value = false
    }

    fun clearAllCheckedItems() {
        _confirmClear.value = true
    }

    fun onClearAllItemsPositiv() {
        _confirmClear.value = false
        listHandler.ClearCheckedItemsInList(shoppingListId)
    }

    fun onClearAllItemsNegative() {
        _confirmClear.value = false
    }

    fun onUpNavigated() {
        _navigateUp.value = false
    }

    fun navigateUp() {
        _navigateUp.value = true
    }

    fun navigateToShare() {
        _navigateShare.value = shoppingListId
    }

    fun onShareNavigated() {
        _navigateShare.value = -1
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