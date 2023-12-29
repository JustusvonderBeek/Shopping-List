package com.cloudsheeptech.shoppinglist.create.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cloudsheeptech.shoppinglist.data.User
import com.cloudsheeptech.shoppinglist.database.ShoppingListDatabase

class CreateShoppinglistViewModelFactory(val user: User, val database : ShoppingListDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateShoppinglistViewModel::class.java)) {
            return CreateShoppinglistViewModel(user, database) as T
        }
        throw IllegalArgumentException("Unknown class")
    }

}