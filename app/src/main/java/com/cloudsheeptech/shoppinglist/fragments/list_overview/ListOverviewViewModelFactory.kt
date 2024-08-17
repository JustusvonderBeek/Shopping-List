package com.cloudsheeptech.shoppinglist.fragments.list_overview

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListRepository
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import com.cloudsheeptech.shoppinglist.fragments.create.user.StartViewModel

class ListOverviewViewModelFactory(val application: Application, val listRepo: ShoppingListRepository, val userRepo: AppUserRepository) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StartViewModel::class.java)) {
            return ListOverviewViewModel(listRepo, userRepo) as T
        }
        throw IllegalArgumentException("Unknown class")
    }
}