package com.cloudsheeptech.shoppinglist.fragments.create.list

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.handling.ShoppingListHandler
import com.cloudsheeptech.shoppinglist.data.user.AppUserHandler

class CreateShoppinglistViewModel(application: Application) : AndroidViewModel(application) {
    val title = MutableLiveData<String>("")

    private val _navigateBack = MutableLiveData<Boolean>(false)
    val navigateBack : LiveData<Boolean> get() = _navigateBack

    private val _navigateToCreatedList = MutableLiveData<Long>(-1)
    val navigateToCreatedList : LiveData<Long> get() = _navigateToCreatedList

    private val listHandler = ShoppingListHandler(ShoppingListDatabase.getInstance(application))
    private val user = AppUserHandler.getUser()

    fun create() {
        Log.d("CreateShoppinglistViewModel", "Creating list pressed")
        if (title.value == null || title.value!!.isEmpty()) {
            return
        }
        // In case the user is not correctly initialized only the ID should be 0
        // Updating the ID is handled by the list handler
        // Storing the list to database and posting it online handled by this function
        listHandler.CreateNewShoppingList(title.value!!)
        navigateBack()
    }

    fun navigateBack() {
        _navigateBack.value = true
    }

    fun onBackNavigated() {
        _navigateBack.value = false
    }

    fun navigateCreatedList() {
        Log.d("CreateShoppingListViewModel", "Navigating to list")
        _navigateToCreatedList.value = 1
    }

    fun onCreatedListNavigated() {
        _navigateToCreatedList.value = -1
    }
}