package com.cloudsheeptech.shoppinglist.fragments.create.user

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cloudsheeptech.shoppinglist.data.user.AppUserHandler
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase

class StartViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ShoppingListDatabase.getInstance(application.applicationContext)
    private val userDao = database.userDao()

    // UI State

    private val _hideKeyboard = MutableLiveData<Boolean>(false)
    val hideKeyboard : LiveData<Boolean> get() = _hideKeyboard

    val user = userDao.getUserLive()
    val inputText = MutableLiveData<String>()

    // -----------------------------------------------

    init {
        if (user.value != null) {
            Log.i("StartViewModel", "User already exists. Navigate back")
        }
    }

    fun pushUsername() {
        hideKeyboard()
        if (inputText.value == null)
            return
        if (inputText.value!!.isEmpty())
            return
        AppUserHandler.new(inputText.value!!)
        AppUserHandler.PostUserOnline(getApplication<Application>().applicationContext)
    }

    // -----------------------------------------------

    private fun hideKeyboard() {
        _hideKeyboard.value = true
    }

    fun keyboardHidden() {
        _hideKeyboard.value = false
    }
}