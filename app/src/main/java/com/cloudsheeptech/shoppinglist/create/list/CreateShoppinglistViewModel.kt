package com.cloudsheeptech.shoppinglist.create.list

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.data.ShoppingList
import com.cloudsheeptech.shoppinglist.data.User
import com.cloudsheeptech.shoppinglist.database.ShoppingListDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateShoppinglistViewModel(database : ShoppingListDatabase) : ViewModel() {

    private val job = Job()
    private val createSLCoroutine = CoroutineScope(Dispatchers.Main + job)

    val title = MutableLiveData<String>("")
    val description = MutableLiveData<String>("")
    val image = MutableLiveData<String>()

    private val _navigateBack = MutableLiveData<Boolean>(false)
    val navigateBack : LiveData<Boolean> get() = _navigateBack

    private val _navigateToCreatedList = MutableLiveData<Long>(-1)
    val navigateToCreatedList : LiveData<Long> get() = _navigateToCreatedList

    private val shoppingListDao = database.shoppingListDao()

    fun create() {
        Log.d("CreateShoppinglistViewModel", "Creating list pressed")
        if (title.value == null || title.value!!.isEmpty()) {
            return
        }
        if (description.value == null)
            description.value = ""
        val creator = User(ID = 100, Username = "TestNutzer", Password = "")
        val newShoppingList = ShoppingList(ID=0, Title = title.value!!, Description = description.value!!, Image = "", Creator = creator)
        createSLCoroutine.launch {
            storeShoppingListDatabase(newShoppingList)
        }
    }

    private suspend fun storeShoppingListDatabase(list : ShoppingList) {
        withContext(Dispatchers.IO) {
            shoppingListDao.insertList(list)
            Log.d("CreateShoppingListViewModel", "Stored list to database")
        }
        withContext(Dispatchers.Main) {
            navigateBack()
        }
    }

    fun navigateBack() {
        _navigateBack.value = true
    }

    fun onBackNavigated() {
        _navigateBack.value = false
    }

    fun navigateCreatedList() {
        _navigateToCreatedList.value = 1
    }

    fun onCreatedListNavigated() {
        _navigateToCreatedList.value = -1
    }
}