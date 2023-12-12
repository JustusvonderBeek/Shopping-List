package com.cloudsheeptech.shoppinglist.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.data.Item
import com.cloudsheeptech.shoppinglist.data.ShoppingList
import com.cloudsheeptech.shoppinglist.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.datastructures.ItemListWithName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShoppinglistViewModel(val list: ItemListWithName<Item>, val database: ShoppingListDatabase, val shoppingListId : Long) : ViewModel() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val slDatabaseDao = database.shoppingListDao()

    private val _refreshing = MutableLiveData<Boolean>()
    val refreshing : LiveData<Boolean>
        get() = _refreshing

    // Navigation
    private val _navigateToAddWord = MutableLiveData<Boolean>(false)
    val navigateToAdd : LiveData<Boolean> get() = _navigateToAddWord
    private val _navigateToEditWord = MutableLiveData<Int>(-1)
    val navigateToEdit : LiveData<Int> get() = _navigateToEditWord

    private val _shoppinglist = MutableLiveData<MutableList<Item>>()
    val shoppinglist : LiveData<MutableList<Item>> get() = _shoppinglist

    lateinit var shoppingListData : LiveData<ShoppingList>

    // ----

    init {
        _refreshing.value = false
        if (shoppingListId > 0)
            shoppingListData = slDatabaseDao.getShoppingList(shoppingListId)
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

    fun editWord(id : Int) {
//        val oldWord = vocabulary.wordList[id]
//        _navigateToEditWord.value = oldWord.ID
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

}