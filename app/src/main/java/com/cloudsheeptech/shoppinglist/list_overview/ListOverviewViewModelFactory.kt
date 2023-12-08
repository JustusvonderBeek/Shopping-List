package com.cloudsheeptech.shoppinglist.list_overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cloudsheeptech.shoppinglist.start.StartViewModel

class ListOverviewViewModelFactory() : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StartViewModel::class.java)) {
            return ListOverviewViewModel() as T
        }
        throw IllegalArgumentException("Unknown class")
    }
}