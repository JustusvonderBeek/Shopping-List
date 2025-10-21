package com.cloudsheeptech.shoppinglist.fragments.list.detail

import android.content.res.Resources
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.items.AppItem
import com.cloudsheeptech.shoppinglist.data.items.ItemClassifier
import com.cloudsheeptech.shoppinglist.data.list.DbShoppingList
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListRepository
import com.cloudsheeptech.shoppinglist.data.uiPreference.Ordering
import com.cloudsheeptech.shoppinglist.data.uiPreference.OrderingUtil
import com.cloudsheeptech.shoppinglist.data.uiPreference.UIPreference
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ShoppingListDetailViewModel
    @Inject
    constructor(
        database: ShoppingListDatabase,
        val shoppingListRepository: ShoppingListRepository,
        private val appUserRepository: AppUserRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        // TODO: Replace with repository abstraction
        private val listDao = database.shoppingListDao()
        private val itemDao = database.itemDao()
        private val mappingDao = database.mappingDao()
        private val preferenceDao = database.preferenceDao()

        val itemName = MutableLiveData<String>("")

        private val shoppingListId: Long = savedStateHandle["ListID"]!!
        private val createdBy: Long = savedStateHandle["CreatedBy"]!!
        val title = MutableLiveData<String>(savedStateHandle["Title"]!!)

        private val job = Job()
        private val localCoroutine = CoroutineScope(Dispatchers.Main + job)

        // UI State

        private val _ordering = MutableLiveData<Ordering>(Ordering.DEFAULT)
        val ordering: LiveData<Ordering> get() = _ordering

        val preferences = preferenceDao.getPreferencesForListLive(shoppingListId)

        private val _refreshing = MutableLiveData<Boolean>(false)
        val refreshing: LiveData<Boolean> get() = _refreshing

        private val _emptyList = MutableLiveData<Boolean>(true)
        val emptyList: LiveData<Boolean> get() = _emptyList

        // Navigation
        private val _navigateUp = MutableLiveData<Boolean>(false)
        val navigateUp: LiveData<Boolean> get() = _navigateUp

        private val _navigateShare = MutableLiveData<Pair<Long, Long>>(-1L to -1L)
        val navigateShare: LiveData<Pair<Long, Long>> get() = _navigateShare

        private val _hideKeyboard = MutableLiveData<Boolean>(false)
        val hideKeyboard: LiveData<Boolean> get() = _hideKeyboard

        private val _confirmDelete = MutableLiveData<Boolean>(false)
        val confirmDelete: LiveData<Boolean> get() = _confirmDelete

        private val _confirmClear = MutableLiveData<Boolean>(false)
        val confirmClear: LiveData<Boolean> get() = _confirmClear

        private val _renameList = MutableLiveData<Pair<String, Long>>(Pair("", -1L))
        val renameList: LiveData<Pair<String, Long>> get() = _renameList

        private val _allItemsChecked = mappingDao.getIsListFinishedLive(shoppingListId, createdBy)
        val allItemsChecked: LiveData<Int> get() = _allItemsChecked

        private val _finished = MutableLiveData<Boolean>(false)
        val finished: LiveData<Boolean> get() = _finished

        private val _scrollDown = MutableLiveData<Int>(-1)
        val scrollDown: LiveData<Int> get() = _scrollDown

        // ---

        // The items in this list
        private val itemsInList =
            shoppingListRepository.readAllListItemsLive(this.shoppingListId, this.createdBy)

        private val _previewItems = MutableLiveData<List<AppItem>>()
        val previewItems: LiveData<List<AppItem>> get() = _previewItems

        private val _listInformation = listDao.getShoppingListLive(shoppingListId, createdBy)
        val listInformation: LiveData<DbShoppingList> get() = _listInformation

        val orderedItemsInList = MediatorLiveData<List<AppItem>>()

        init {
            orderedItemsInList.addSource(_ordering, { value ->
                val orderedList = getSortedList(orderedItemsInList.value ?: emptyList(), value)
                if (orderedList != orderedItemsInList.value) {
                    orderedItemsInList.value = orderedList
                    updateListEmpty()
                }
            })
            orderedItemsInList.addSource(itemsInList, { value ->
                val orderedList = getSortedList(value, _ordering.value!!)
                if (orderedList != orderedItemsInList.value) {
                    orderedItemsInList.value = orderedList
                    updateListEmpty()
                }
            })
        }

        private fun getSortedList(
            listToSort: List<AppItem>,
            ordering: Ordering,
        ): List<AppItem> {
            Log.d("ShoppingListViewModel", "Ordering called")
            return when (ordering) {
                Ordering.DEFAULT -> {
                    listToSort
                }

                Ordering.CHECKED_LAST -> {
                    val sorted =
                        listToSort.sortedWith(
                            compareBy<AppItem>({ it.checked }, { it.name }),
                        )
                    sorted
                }

                Ordering.ALPHABETICAL -> {
                    val sorted =
                        listToSort.sortedBy {
                            it.name
                        }
                    sorted
                }

                Ordering.ALPHABETICAL_REVERSE -> {
                    val sorted =
                        listToSort
                            .sortedBy {
                                it.name
                            }.reversed()
                    sorted
                }

                Ordering.SUPERMARKET_ODER -> {
                    val sorted =
                        listToSort.sortedWith(
                            compareBy(
                                { ItemClassifier.convertStringToItemClass(it.name) },
                                { it.name },
                            ),
                        )
                    sorted
                }
            }
        }

        private fun updateListEmpty() {
            if (this.orderedItemsInList.value == null) {
                Log.d("ShoppingListViewModel", "Cannot update list because value is null")
                return
            }
            if (this.orderedItemsInList.value!!.isEmpty()) {
                Log.d("ShoppingListViewModel", "List is empty")
                _emptyList.value = true
                return
            }
            Log.d("ShoppingListViewModel", "List is not empty")
            _emptyList.value = false
        }

        // ----

        private fun createNewItemWithName(name: String): AppItem {
            val user = appUserRepository.read() ?: throw IllegalStateException("user null after login")
            val trimmedName = name.trim()
            return AppItem(
                id = 0L, // Generated by the database
                name = trimmedName,
                icon = "ic_item",
                quantity = 1L,
                checked = false,
                addedBy = user.OnlineID,
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
                shoppingListRepository.insertItem(shoppingListId, createdBy, item)
                val lastPosition = itemsInList.value?.size ?: -1
                withContext(Dispatchers.Main) {
                    hideKeyboard()
                    clearItemPreview()
                    scrollDown(lastPosition - 1)
                }
            }
        }

        fun toggleItem(itemId: Long) {
            Log.d("ShoppinListViewModel", "Toggle item $itemId")
            localCoroutine.launch {
                val migratedToNewId =
                    shoppingListRepository.toggleItem(shoppingListId, createdBy, itemId)
                withContext(Dispatchers.Main) {
                    _finished.value = false
                    if (migratedToNewId) {
                        navigateUp()
                    }
                }
            }
        }

        fun increaseItemCount(
            itemId: Int,
            quantity: Long = 1L,
        ) {
            Log.d("ShoppingListViewModel", "List has ${orderedItemsInList.hasActiveObservers()}")
            localCoroutine.launch {
                val migratedToNewId =
                    shoppingListRepository.updateItemCount(
                        shoppingListId,
                        createdBy,
                        itemId.toLong(),
                        quantity,
                    )
                if (migratedToNewId) {
                    withContext(Dispatchers.Main) {
                        navigateUp()
                    }
                }
            }
        }

        fun decreaseItemCount(itemId: Int) {
            localCoroutine.launch {
                val migratedToNewId =
                    shoppingListRepository.updateItemCount(
                        shoppingListId,
                        createdBy,
                        itemId.toLong(),
                        -1L,
                    )
                if (migratedToNewId) {
                    withContext(Dispatchers.Main) {
                        navigateUp()
                    }
                }
            }
        }

        fun updateShoppinglist() {
            _refreshing.value = true
            viewModelScope.launch {
                // Already updates the list in the database in case it is newer
                shoppingListRepository.read(shoppingListId, createdBy)
                withContext(Dispatchers.Main) {
                    _refreshing.value = false
                }
            }
        }

        fun showItemPreview(enteredName: String) {
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

        private suspend fun loadMatchingItems(name: String) {
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
                shoppingListRepository.insertExistingItem(shoppingListId, createdBy, itemId)
                val lastPosition = itemsInList.value?.size ?: -1
                withContext(Dispatchers.Main) {
                    scrollDown(lastPosition - 1)
                }
            }
        }

        fun addTappedItem(itemId: Long) {
            localCoroutine.launch {
                addItemFromPreviewToList(itemId)
            }
            _finished.value = false
        }

        fun shareThisList() {
            navigateToShare()
        }

        private suspend fun updateOrCreatePreferenceInDatabase(ordering: Ordering) {
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

        private fun updateListPreference(order: Ordering) {
            localCoroutine.launch {
                updateOrCreatePreferenceInDatabase(order)
            }
        }

        fun resetOrdering() {
            Log.d("ShoppingListViewModel", "Reset ordering called")
            _ordering.value = Ordering.DEFAULT
            updateListPreference(_ordering.value!!)
        }

        fun setOrderingInDatabase(
            order: String,
            resources: Resources,
        ) {
            val orderEnum = OrderingUtil.orderingStringToEnum(resources, order)
            if (orderEnum == _ordering.value) {
                Log.d("ShoppingListViewModel", "Ordering already applied")
                return
            }
            updateListPreference(orderEnum)
        }

        fun setOrdering(order: Ordering) {
            if (order == _ordering.value) {
                Log.d("ShoppingListViewModel", "Ordering already applied")
                return
            }
            Log.d("ShoppingListViewMode", "Setting order to $order")
            _ordering.value = order
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

        fun renameThisList() {
            _renameList.value = Pair(this.title.value!!, this.shoppingListId)
        }

        fun onListRenamed() {
            _renameList.value = Pair("", -1L)
        }

        fun onDeleteConfirmed() {
            _confirmDelete.value = false
            viewModelScope.launch {
                shoppingListRepository.delete(shoppingListId, createdBy)
                withContext(Dispatchers.Main) {
                    navigateUp()
                }
            }
        }

        fun onDeleteCanceled() {
            _confirmDelete.value = false
        }

        fun clearAllCheckedItems() {
            _confirmClear.value = true
            resetFinished()
        }

        fun onClearAllItemsPositive() {
            _confirmClear.value = false
            viewModelScope.launch {
                shoppingListRepository.deleteAllCheckedItems(shoppingListId, createdBy)
                withContext(Dispatchers.Main) {
                    resetFinished()
                }
            }
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
            _navigateShare.value = shoppingListId to createdBy
        }

        fun onShareNavigated() {
            _navigateShare.value = -1L to -1L
        }

        private fun hideKeyboard() {
            _hideKeyboard.value = true
        }

        fun keyboardHidden() {
            _hideKeyboard.value = false
        }

        private fun scrollDown(position: Int) {
            _scrollDown.value = position
        }

        fun onViewScrolledDown() {
            _scrollDown.value = -1
        }
    }
