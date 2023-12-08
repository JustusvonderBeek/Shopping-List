package com.cloudsheeptech.shoppinglist.start

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class StartViewModel() : ViewModel() {

    private val job = Job()
    private val recapScope = CoroutineScope(Dispatchers.IO + job)

    val inputText = MutableLiveData<String>()

    private val _navigateToApp = MutableLiveData<Boolean>(false)
    val navigateToApp : LiveData<Boolean> get() = _navigateToApp

    private val _hideKeyboard = MutableLiveData<Boolean>(false)
    val hideKeyboard : LiveData<Boolean> get() = _hideKeyboard

    private fun hideKeyboard() {
        _hideKeyboard.value = true
    }

    fun keyboardHidden() {
        _hideKeyboard.value = false
    }

    fun navigateToApp() {
        _navigateToApp.value = true
    }

    fun onAppNavigated() {
        _navigateToApp.value = false
    }
}