package com.cloudsheeptech.shoppinglist.create.list

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cloudsheeptech.shoppinglist.data.User
import com.cloudsheeptech.shoppinglist.database.ShoppingListDatabase

class CreateShoppinglistViewModelFactory(val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateShoppinglistViewModel::class.java)) {
            return CreateShoppinglistViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown class")
    }

}