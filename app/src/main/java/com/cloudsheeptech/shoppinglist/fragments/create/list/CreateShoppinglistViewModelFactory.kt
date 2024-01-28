package com.cloudsheeptech.shoppinglist.fragments.create.list

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cloudsheeptech.shoppinglist.data.User
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.handling.ShoppingListHandler

class CreateShoppinglistViewModelFactory(val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateShoppinglistViewModel::class.java)) {
            return CreateShoppinglistViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown class")
    }

}