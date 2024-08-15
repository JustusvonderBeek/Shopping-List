package com.cloudsheeptech.shoppinglist.fragments.create.user

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository

class StartViewModelFactory(private val userRepository: AppUserRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StartViewModel::class.java)) {
            return StartViewModel(userRepository) as T
        }
        throw IllegalArgumentException("Unknown class")
    }
}