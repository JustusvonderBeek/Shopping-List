package com.cloudsheeptech.shoppinglist.fragments.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class EditViewModelFactory() : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditViewModel::class.java)) {
            return EditViewModel() as T
        }
        throw IllegalArgumentException("Unknown class")
    }
}