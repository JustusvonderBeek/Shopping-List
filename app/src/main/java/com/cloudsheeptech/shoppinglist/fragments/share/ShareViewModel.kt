package com.cloudsheeptech.shoppinglist.fragments.share

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.handling.ShoppingListHandler

class ShareViewModel(val database : ShoppingListDatabase, private val listId : Long) : ViewModel() {

    private val listHandler = ShoppingListHandler(database)

    val searchName = MutableLiveData<String>("")

    fun searchUser() {
        if (searchName.value == "")
            return

    }

    fun shareList(sharedWithId : Long) {
        listHandler.ShareShoppingListOnline(listId, sharedWithId)
    }

    fun unshareList() {
        listHandler.UnshareShoppingListOnline(listId)
    }

}