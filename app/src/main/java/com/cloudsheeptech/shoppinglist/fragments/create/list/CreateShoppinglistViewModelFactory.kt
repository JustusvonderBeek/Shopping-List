package com.cloudsheeptech.shoppinglist.fragments.create.list

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListRepository

class CreateShoppinglistViewModelFactory(val shoppingListRepository: ShoppingListRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateShoppinglistViewModel::class.java)) {
            return CreateShoppinglistViewModel(shoppingListRepository) as T
        }
        throw IllegalArgumentException("Unknown class")
    }

}