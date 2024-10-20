package com.cloudsheeptech.shoppinglist.fragments.list

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.cloudsheeptech.shoppinglist.R
import com.cloudsheeptech.shoppinglist.data.items.DbItem
import com.cloudsheeptech.shoppinglist.data.items.ItemClassifier
import com.cloudsheeptech.shoppinglist.data.items.AppItem
import com.cloudsheeptech.shoppinglist.data.itemToListMapping.ListMapping
import com.cloudsheeptech.shoppinglist.data.list.DbShoppingList
import com.cloudsheeptech.shoppinglist.data.UIPreference
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.items.ApiItem
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListRepository
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ShoppinglistViewModel @Inject constructor(
    val database: ShoppingListDatabase,
    private val listRepo: ShoppingListRepository,
    private val userRepo: AppUserRepository,
    savedStateHandle: SavedStateHandle,
//    private val shoppingListId : Long,
//    private val createdBy : Long
) : ViewModel() {

    private val listDao = database.shoppingListDao()
    private val itemDao = database.itemDao()
    private val mappingDao = database.mappingDao()
    private val preferenceDao = database.preferenceDao()
//    private val listHandler = ShoppingListRepository(database)

    val itemName = MutableLiveData<String>("")
    val title = MutableLiveData<String>("Liste")

    private val shoppingListId : Long = savedStateHandle["ListID"]!!
    private val createdBy : Long = savedStateHandle["CreatedBy"]!!

    private val job = Job()
    private val localCoroutine = CoroutineScope(Dispatchers.Main + job)

    // UI State

    // TODO: Allow user to select the ordering
    enum class ORDERING(val position : Int) {
        DEFAULT(0),
        ALPHABETICAL(1),
        ALPHABETICAL_REVERSE(2),
        CHECKED_LAST(3),
        SUPERMARKET_ODER(4),
    }

    private val _ordering = MutableLiveData<ORDERING>(ORDERING.DEFAULT)
    val ordering : LiveData<ORDERING> get() = _ordering

    val preferences = preferenceDao.getPreferencesForListLive(shoppingListId)

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
    // TODO: Would love to move this into its own mediator class
    private val itemsInList = MediatorLiveData<List<AppItem>>()

    private val _previewItems = MutableLiveData<List<DbItem>>()
    val previewItems : LiveData<List<DbItem>> get() = _previewItems

    private val _listInformation = listDao.getShoppingListLive(shoppingListId, createdBy)
    val listInformation : LiveData<DbShoppingList> get() = _listInformation

    val orderedItemsInList = itemsInList.switchMap {
        Log.d("ShoppingListViewModel", "Ordering called")
        liveData {
            when (ordering.value!!) {
                ORDERING.DEFAULT -> {
                    emit(it)
                }
                ORDERING.CHECKED_LAST -> {
                    val sorted = it.sortedWith(
                        compareBy({it.checked}, {it.name})
                    )
                    emit(sorted)
                }
                ORDERING.ALPHABETICAL -> {
                    val sorted = it.sortedBy {
                        it.name
                    }
                    emit(sorted)
                }
                ORDERING.ALPHABETICAL_REVERSE-> {
                    val sorted = it.sortedBy {
                        it.name
                    }.reversed()
                    emit(sorted)
                }
                ORDERING.SUPERMARKET_ODER -> {
                    val sorted = it.sortedWith(
                        compareBy({ ItemClassifier.convertStringToItemClass(it.name)}, {it.name})
                    )
                    emit(sorted)
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
        val quantityItems = mutableListOf<AppItem>()
        itemsInList.addSource(liveItems) {
            mappings.forEachIndexed { index, listMapping ->
                // Order might not match
                val matchingItem = liveItems.value!!.first { x -> x.id == listMapping.ItemID }
                val quantItem = combineMappingAndItemToItemWithQuantity(listMapping, matchingItem)
                quantityItems.add(quantItem)
            }
            itemsInList.value = quantityItems
        }
    }

    private fun combineMappingAndItemToItemWithQuantity(mapping: ListMapping, dbItem : DbItem) : AppItem {
        return AppItem(mapping.ItemID, dbItem.name, dbItem.icon, mapping.Quantity, mapping.Checked, mapping.AddedBy)
    }

    private fun createNewItemWithName(name: String) : AppItem {
        val user = userRepo.read() ?: throw IllegalStateException("user null after login")
        return AppItem(
            id = 0L, // Generated by the database
            name = name,
            icon = "ic_item",
            quantity = 1L,
            checked = false,
            addedBy = user.OnlineID
        )
    }

    fun addItem() {
        Log.d("ShoppinglistViewModel", "Adding new item to list")
        if (itemName.value == null || itemName.value!!.isEmpty()) {
            Log.i("ShoppinglistViewModel", "Do not add empty item")
            return
        }
        localCoroutine.launch {
            val item = createNewItemWithName(itemName.value!!)
            listRepo.insertItem(shoppingListId, createdBy, item)
            withContext(Dispatchers.Main) {
                hideKeyboard()
                clearItemNameInput()
            }
        }
    }

    fun toggleItem(itemId : Long) {
        Log.d("ShoppinListViewModel", "Toggle item $itemId")
        localCoroutine.launch {
            listRepo.toggleItem(shoppingListId, createdBy, itemId)
            withContext(Dispatchers.Main) {
                _finished.value = false
            }
        }
    }


    fun increaseItemCount(itemId : Int, quantity : Long = 1L) {
        localCoroutine.launch {
            listRepo.updateItemCount(shoppingListId, createdBy, itemId.toLong(), quantity)
        }
//        listHandler.IncreaseItemCountInShoppingList(itemId.toLong(), shoppingListId, createdBy, quantity)
    }

    fun decreaseItemCount(itemId : Int) {
        localCoroutine.launch {
            listRepo.updateItemCount(shoppingListId, createdBy, itemId.toLong(), -1L)
        }
//        listHandler.DecreaseItemCountInShoppingList(itemId.toLong(), shoppingListId, createdBy)
    }

    fun updateShoppinglist() {
        _refreshing.value = true
//        listHandler.GetShoppingList(shoppingListId, createdBy)
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
//            Log.d("ShoppinglistViewModel", "Found item to add")
//            listHandler.AddItemToShoppingList(item, shoppingListId, createdBy)
            listRepo.insertExistingItem(shoppingListId, createdBy, itemId)
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
//        listHandler.ShareShoppingListOnline(shoppingListId, -1)
        navigateToShare()
    }

    private suspend fun updateOrCreatePreferenceInDatabase(ordering: ORDERING) {
        withContext(Dispatchers.IO) {
            var preference = preferenceDao.getPreferenceForList(shoppingListId)
            if (preference == null) {
                preference = UIPreference(0, shoppingListId, ordering)
            } else {
                preference.Ordering = ordering
            }
            preferenceDao.insertPreference(preference)
        }
    }

    private fun updateListPreference() {
        localCoroutine.launch {
            updateOrCreatePreferenceInDatabase(_ordering.value!!)
        }
    }

    fun resetOrdering() {
        Log.d("ShoppingListViewModel", "Reset ordering called")
        _ordering.value = ORDERING.DEFAULT
        updateListPreference()
    }

    private fun convertStringToOrder(orderString: String, context: Context) : ORDERING {
        val r = context.resources
        return when (orderString) {
            r.getString(R.string.ordering_default_key) -> ORDERING.DEFAULT
            r.getString(R.string.ordering_alphabet_key) -> ORDERING.ALPHABETICAL
            r.getString(R.string.ordering_alphabet_rev_key) -> ORDERING.ALPHABETICAL_REVERSE
            r.getString(R.string.ordering_checked_key) -> ORDERING.CHECKED_LAST
            r.getString(R.string.ordering_supermarket_key) -> ORDERING.SUPERMARKET_ODER
            else -> ORDERING.DEFAULT
        }
    }

    fun setOrdering(order : String, context : Context) {
        val orderEnum = convertStringToOrder(order, context)
        if (orderEnum == _ordering.value) {
            Log.d("ShoppingListViewModel", "Ordering already applied")
            return
        }
        Log.d("ShoppingListViewModel", "Setting order to $orderEnum")
        _ordering.value = orderEnum
        updateListPreference()
    }

    fun setOrdering(order : ORDERING, context: Context) {
        if (order == _ordering.value) {
            Log.d("ShoppingListViewModel", "Ordering already applied")
            return
        }
        Log.d("ShoppingListViewMode", "Setting order to $order")
        _ordering.value = order
        updateListPreference()
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
        viewModelScope.launch {
            listRepo.delete(shoppingListId, createdBy)
            withContext(Dispatchers.Main) {
                navigateUp()
            }
        }
        // TODO: Difference between own and shared list:
        // Shared list -> delete offline and sharing
        // Own list -> delete list offline and online + sharing
//        listHandler.DeleteShoppingList(shoppingListId, createdBy)
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
//        listHandler.ClearCheckedItemsInList(shoppingListId)
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