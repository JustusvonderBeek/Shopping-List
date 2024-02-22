package com.cloudsheeptech.shoppinglist.fragments.create.user

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cloudsheeptech.shoppinglist.user.AppUser
import com.cloudsheeptech.shoppinglist.data.DatabaseUser
import com.cloudsheeptech.shoppinglist.data.User
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.network.Networking
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.Exception
import kotlin.random.Random

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
        AppUser.new(inputText.value!!)
        AppUser.PostUserOnline(getApplication<Application>().applicationContext)
    }

    // -----------------------------------------------

    private fun hideKeyboard() {
        _hideKeyboard.value = true
    }

    fun keyboardHidden() {
        _hideKeyboard.value = false
    }
}