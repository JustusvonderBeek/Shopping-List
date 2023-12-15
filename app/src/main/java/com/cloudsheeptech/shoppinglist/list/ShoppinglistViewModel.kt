package com.cloudsheeptech.shoppinglist.list

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.data.Item
import com.cloudsheeptech.shoppinglist.data.ListMapping
import com.cloudsheeptech.shoppinglist.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.datastructures.ItemListWithName
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


    private val _shoppinglist = MutableLiveData<List<Item>>()
    val shoppinglist : LiveData<List<Item>> get() = _shoppinglist
    // TODO: Fix the list id in case another list than the default one is loaded
    val mappedItemIds = mappingDao.getMappingsForListLive(0)

    var itemName = MutableLiveData<String>("")
    var title = MutableLiveData<String>("Test")

    // ----

    init {
        _refreshing.value = false
//        if (shoppingListId > 0)
//            shoppingListData = databaseDao.getItemLive(shoppingListId)
    }

    fun updateVocabulary() {
        scope.launch {
            withContext(Dispatchers.Main) {
                _refreshing.value = false
            }
//            vocabulary.updateVocabulary()
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
            val mapping = ListMapping(rnd, item.ID, 0, 1)
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

    fun editWord(id : Int) {
//        val oldWord = vocabulary.wordList[id]
//        _navigateToEditWord.value = oldWord.ID
    }

    fun reloadItemsInList(itemIds : List<ListMapping>) {
        // Takes the list of currently contained IDs and updates the items in the shopping list
        val ids = itemIds.map { map -> map.ItemID }
        scope.launch {
            loadItemsInListFromDatabase(ids)
        }
    }

    private suspend fun loadItemsInListFromDatabase(itemIds : List<Long>) {
        withContext(Dispatchers.IO) {
            Log.d("ShoppinglistViewModel", "Loading the current items from the database")
            val items = databaseDao.getItems(itemIds)
            withContext(Dispatchers.Main) {
                _shoppinglist.value = items
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