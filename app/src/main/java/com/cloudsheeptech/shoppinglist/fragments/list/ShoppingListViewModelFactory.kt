package com.cloudsheeptech.shoppinglist.fragments.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase

class ShoppingListViewModelFactory(val database : ShoppingListDatabase, val shoppingListId : Long, val creatorId : Long) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShoppinglistViewModel::class.java)) {
            return ShoppinglistViewModel(database, shoppingListId, creatorId) as T
        }
        throw IllegalArgumentException("Unknown class")
    }

}