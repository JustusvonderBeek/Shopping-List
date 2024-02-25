package com.cloudsheeptech.shoppinglist.fragments.list

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import com.cloudsheeptech.shoppinglist.data.Item
import com.cloudsheeptech.shoppinglist.data.ItemWithQuantity
import com.cloudsheeptech.shoppinglist.data.ListMapping
import com.cloudsheeptech.shoppinglist.data.ShoppingList
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.handling.ShoppingListHandler
import com.cloudsheeptech.shoppinglist.user.AppUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.DEFAULT_CONCURRENCY_PROPERTY_NAME
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShoppinglistViewModel(val database: ShoppingListDatabase, private val shoppingListId : Long, private val createdBy : Long) : ViewModel() {

    private val listDao = database.shoppingListDao()
    private val itemDao = database.itemDao()
    private val mappingDao = database.mappingDao()
    private val listHandler = ShoppingListHandler(database)

    val itemName = MutableLiveData<String>("")
    val title = MutableLiveData<String>("Liste")

    private val job = Job()
    private val localCoroutine = CoroutineScope(Dispatchers.Main + job)

    // UI State

    // TODO: Allow user to select the ordering
    enum class ORDERING {
        DEFAULT,
        CHECKED_LAST,
        ALPHABETICAL,
        ALPHABETICAL_REVERSE,
        SUPERMARKET_ODER,
    }

    private val _ordering = MutableLiveData<ORDERING>(ORDERING.CHECKED_LAST)
    val ordering : LiveData<ORDERING> get() = _ordering

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

    private val _allItemsChecked = mappingDao.getIsListFinishedLive(shoppingListId, createdBy)
    val allItemsChecked : LiveData<Int> get() = _allItemsChecked

    private val _finished = MutableLiveData<Boolean>(false)
    val finished : LiveData<Boolean> get() = _finished

    // ---

    // The items in this list
    private val itemsInList = MediatorLiveData<List<ItemWithQuantity>>()

    private val _previewItems = MutableLiveData<List<Item>>()
    val previewItems : LiveData<List<Item>> get() = _previewItems

    private val _listInformation = listDao.getShoppingListLive(shoppingListId, createdBy)
    val listInformation : LiveData<ShoppingList> get() = _listInformation

    val orderedItemsInList = itemsInList.switchMap {
        Log.d("ShoppingListViewModel", "Ordering called")
        liveData {
            when (ordering.value!!) {
                ORDERING.DEFAULT -> {
                    emit(it)
                }
                ORDERING.CHECKED_LAST -> {
                    val sorted = it.sortedWith(
                        compareBy({it.Checked}, {it.Name})
                    )
                    emit(sorted)
                }
                ORDERING.ALPHABETICAL -> {
                    val sorted = it.sortedBy {
                        it.Name
                    }
                    emit(sorted)
                }
                ORDERING.ALPHABETICAL_REVERSE-> {
                    val sorted = it.sortedBy {
                        it.Name
                    }.reversed()
                    emit(sorted)
                }
                ORDERING.SUPERMARKET_ODER -> {
                    // TODO: Make later
                    emit(it)
                }
            }
        }
    }

    init {
        itemsInList.addSource(ordering) {
            val mappings = mappingDao.getMappingsForListLive(shoppingListId, createdBy)
            itemsInList.addSource(mappings) { m ->
                fetchItemsForList(m)
            }
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
        listHandler.AddItemAndAddToShoppingList(item, shoppingListId, createdBy)
        hideKeyboard()
        clearItemNameInput()
    }

    fun toggleItem(itemId : Long) {
        Log.d("ShoppinListViewModel", "Toggle item $itemId")
        listHandler.ToggleItemInShoppingList(itemId, shoppingListId, createdBy)
        _finished.value = false
    }


    fun increaseItemCount(itemId : Int, quantity : Long = 1L) {
        listHandler.IncreaseItemCountInShoppingList(itemId.toLong(), shoppingListId, createdBy, quantity)
    }

    fun decreaseItemCount(itemId : Int) {
        listHandler.DecreaseItemCountInShoppingList(itemId.toLong(), shoppingListId, createdBy)
    }

    private fun pushListToServer() {
        Log.d("ShoppinglistViewModel", "Pushing list with ${itemsInList.value?.size} to server")
        listHandler.PostShoppingListOnline(shoppingListId, createdBy)
    }

    fun updateShoppinglist() {
        _refreshing.value = true
        listHandler.GetShoppingList(shoppingListId, createdBy)
        _refreshing.value = false
    }


    fun showItemPreview(enteredName : String) {
//        Log.d("ShoppinglistViewModel", "User entered: $enteredName")
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
            listHandler.AddItemToShoppingList(item, shoppingListId, createdBy)
        }
    }

    fun AddTappedItem(itemId : Long) {
        Log.d("ShoppinglistViewModel", "Adding item with ID $itemId")
        localCoroutine.launch {
            addItemFromPreviewToList(itemId)
        }
        _finished.value = false
    }

    fun shareThisList() {
        // TODO: Let user decide who to share the list with
        // For now use '-1' == 'all'
//        listHandler.ShareShoppingListOnline(shoppingListId, -1)
        navigateToShare()
    }

    fun resetOrdering() {
        _ordering.value = ORDERING.DEFAULT
    }

    private fun convertStringToOrder(orderString: String) : ORDERING {
        return when (orderString) {
            "Default" -> ORDERING.DEFAULT
            "Alphabetical" -> ORDERING.ALPHABETICAL
            "Reversed Alphabetical" -> ORDERING.ALPHABETICAL_REVERSE
            "Checked Last" -> ORDERING.CHECKED_LAST
            "Supermarket Order" -> ORDERING.SUPERMARKET_ODER
            else -> ORDERING.DEFAULT
        }
    }

    fun setOrdering(order : String) {
        val orderEnum = convertStringToOrder(order)
        Log.d("ShoppingListViewModel", "Setting order to $orderEnum")
        _ordering.value = orderEnum
    }

    fun listFinished() {
        Log.d("ShoppingListViewModel", "Clicked on list finished okay")
        _finished.value = true
    }

    private fun resetFinished() {
        Log.d("ShoppingListViewModel", "Resetting finished")
        _finished.value = false
    }

    fun deleteThisList() {
        _confirmDelete.value = true
    }

    fun onDeleteConfirmed() {
        _confirmDelete.value = false
        // TODO: Difference between own and shared list:
        // Shared list -> delete offline and sharing
        // Own list -> delete list offline and online + sharing
        listHandler.DeleteShoppingList(shoppingListId, createdBy)
        navigateUp()
    }

    fun onDeleteCanceled() {
        _confirmDelete.value = false
    }

    fun clearAllCheckedItems() {
        _confirmClear.value = true
        resetFinished()
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

    private fun navigateUp() {
        _navigateUp.value = true
    }

    private fun navigateToShare() {
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