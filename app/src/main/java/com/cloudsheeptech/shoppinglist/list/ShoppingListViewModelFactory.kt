package com.cloudsheeptech.shoppinglist.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cloudsheeptech.shoppinglist.datastructures.Shoppinglist

class ShoppingListViewModelFactory(val shoppinglist: Shoppinglist) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShoppinglistViewModel::class.java)) {
            return ShoppinglistViewModel(shoppinglist) as T
        }
        throw IllegalArgumentException("Unknown class")
    }

}