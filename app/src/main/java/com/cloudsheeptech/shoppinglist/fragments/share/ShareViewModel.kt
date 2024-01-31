package com.cloudsheeptech.shoppinglist.fragments.share

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.data.ListCreator
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.handling.ShoppingListHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShareViewModel(val database : ShoppingListDatabase, private val listId : Long) : ViewModel() {

    private val job = Job()
    private val localCoroutine = CoroutineScope(Dispatchers.Main + job)

    private val listHandler = ShoppingListHandler(database)

    val searchName = MutableLiveData<String>("")
    private val _searchedUsers = MutableLiveData<List<ListCreator>>()
    val searchedUsers : LiveData<List<ListCreator>> get() = _searchedUsers

    private val _navigateUp = MutableLiveData<Boolean>(false)
    val navigateUp : LiveData<Boolean> get() = _navigateUp

    init {
        _searchedUsers.value = emptyList()
    }

    private suspend fun searchUsersFromOnlineAndDatabase(name : String) : List<ListCreator> {
        var users = emptyList<ListCreator>()
        withContext(Dispatchers.IO) {
            val onlineUsers = listHandler.SearchUsersOnline(name) ?: return@withContext
            users = onlineUsers
        }
        return users
    }

    private suspend fun updateListCreators(list : List<ListCreator>) {
        withContext(Dispatchers.Main) {
            _searchedUsers.value = list
        }
    }

    fun searchUser() {
        if (searchName.value!!.isEmpty()) {
            Log.d("ShareViewModel", "Empty string, clear preview")
            _searchedUsers.value = emptyList()
            return
        }
        localCoroutine.launch {
            val creators = searchUsersFromOnlineAndDatabase(searchName.value!!)
            updateListCreators(creators)
        }
    }

    fun shareList(sharedWithId : Long) {
        listHandler.ShareShoppingListOnline(listId, sharedWithId)
        navigateUp()
    }

    fun unshareList() {
        listHandler.UnshareShoppingListOnline(listId)
    }

    fun navigateUp() {
        _navigateUp.value = true
    }

    fun onUpNavigated() {
        _navigateUp.value = false
    }
}