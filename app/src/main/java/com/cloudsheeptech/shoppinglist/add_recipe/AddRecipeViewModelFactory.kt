package com.cloudsheeptech.shoppinglist.add_recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class AddRecipeViewModelFactory() : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddRecipeViewModel::class.java)) {
            return AddRecipeViewModel() as T
        }
        throw IllegalArgumentException("Unknown class")
    }

}