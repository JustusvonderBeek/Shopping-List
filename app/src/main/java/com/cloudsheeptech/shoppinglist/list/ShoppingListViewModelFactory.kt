package com.cloudsheeptech.shoppinglist.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cloudsheeptech.shoppinglist.data.Item
import com.cloudsheeptech.shoppinglist.datastructures.ItemListWithName

class ShoppingListViewModelFactory(val itemListWithName: ItemListWithName<Item>) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShoppinglistViewModel::class.java)) {
            return ShoppinglistViewModel(itemListWithName) as T
        }
        throw IllegalArgumentException("Unknown class")
    }

}