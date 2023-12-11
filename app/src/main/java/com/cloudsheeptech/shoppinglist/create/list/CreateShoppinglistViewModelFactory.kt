package com.cloudsheeptech.shoppinglist.create.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cloudsheeptech.shoppinglist.list_overview.ListOverviewViewModel
import com.cloudsheeptech.shoppinglist.start.StartViewModel

class CreateShoppinglistViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateShoppinglistViewModel::class.java)) {
            return CreateShoppinglistViewModel() as T
        }
        throw IllegalArgumentException("Unknown class")
    }

}