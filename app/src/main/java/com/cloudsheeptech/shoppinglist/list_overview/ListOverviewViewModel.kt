package com.cloudsheeptech.shoppinglist.list_overview

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.data.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.Exception

class ListOverviewViewModel(application : Application) : AndroidViewModel(application) {

    private val job = Job()
    private val vmCoroutine = CoroutineScope(Dispatchers.Main + job)

    private val _init = MutableLiveData<Boolean>(true)
    val init : LiveData<Boolean> get()= _init

    val username = MutableLiveData<String>("")

    init {
        checkInitialized()
    }

    private fun checkInitialized() {
        Log.i("ListOverviewViewModel", "Checking if already initialized")
        vmCoroutine.launch {
           loadUser()
        }
    }

    private suspend fun loadUser() {
        val result = withContext(Dispatchers.IO) {
            try {
                val userfile = File(getApplication<Application>().filesDir, "username.json")
                if (!userfile.exists()) {
                    Log.d("ListOverviewViewModel", "Found no user at ${userfile.absolutePath}")
                    return@withContext false
                }
                val reader = userfile.reader(Charsets.UTF_8)
                val content = reader.readText()
                val jsonSerializer = Json {
                    encodeDefaults = true
                    ignoreUnknownKeys = false
                }
                val user = jsonSerializer.decodeFromString<User>(content)
                if (user.ID == 0L) {
                    Log.w("ListOverviewViewModel", "Found user with ID == 0! Incorrect state! Deleting and setting up new user")
                    userfile.delete()
                    return@withContext false
                }
                reader.close()
            } catch (ex : Exception) {
                Log.w("ListOverviewViewModel", "Failed to write username to file: $ex")
            }
            return@withContext true
        }
        if (!result) {
            Log.d("ListOverviewViewModel", "Showing fragment to set username")
            navigateToStart()
        }
    }

    private fun navigateToStart() {
        _init.value = false
    }

    fun onStartNavigated() {
        _init.value = true
    }

}