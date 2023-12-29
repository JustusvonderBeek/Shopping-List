package com.cloudsheeptech.shoppinglist.list_overview

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cloudsheeptech.shoppinglist.create.user.StartViewModel

class ListOverviewViewModelFactory(val application: Application) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StartViewModel::class.java)) {
            return ListOverviewViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown class")
    }
}