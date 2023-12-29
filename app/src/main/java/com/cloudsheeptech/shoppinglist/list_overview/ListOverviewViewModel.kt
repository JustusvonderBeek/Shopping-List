package com.cloudsheeptech.shoppinglist.list_overview

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.data.Item
import com.cloudsheeptech.shoppinglist.data.User
import com.cloudsheeptech.shoppinglist.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.datastructures.ItemListWithName
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

    private val user = User()
    private val database = ShoppingListDatabase.getInstance(application.applicationContext)
    private val shoppingListDao = database.shoppingListDao()

    private  val _createList = MutableLiveData<Boolean>(false)
    val createList : LiveData<Boolean> get() = _createList

    private val _navigateList = MutableLiveData<Long>(-1)
    val navigateList : LiveData<Long> get() = _navigateList

    private val _navigateUser = MutableLiveData<Boolean>(false)
    val navigateUser : LiveData<Boolean> get() = _navigateUser

    val shoppingList = shoppingListDao.getShoppingLists()

    init {
        checkInitialized()
    }

    private fun checkInitialized() {
        Log.i("ListOverviewViewModel", "Checking if already initialized")
        vmCoroutine.launch {
           loadUser()
        }
    }

    fun createNewList() {
//        Log.d("ListOverviewViewModel", "Creating new list")
//        val list = Item((shoppingList.size() + 1).toLong(), "New List", "")
//        shoppingList.addItem(list)
        navigateToCreateList()
    }

    private suspend fun loadUser() {
        val result = withContext(Dispatchers.IO) {
            try {
                val userfile = File(getApplication<Application>().filesDir, "user.json")
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
                Log.i("ListOverviewViewModel", "Load user $user from disk")
            } catch (ex : Exception) {
                Log.w("ListOverviewViewModel", "Failed to write username to file: $ex")
            }
            return@withContext true
        }
        if (!result) {
            Log.d("ListOverviewViewModel", "Failed to load user. Okay for now")
            navigateToCreateUser()
        }
    }

    fun removeUser() {
        vmCoroutine.launch {
            deleteUserFromDisk()
        }
    }

    private suspend fun deleteUserFromDisk() {
        withContext(Dispatchers.IO) {
            try {
                val userfile = File(getApplication<Application>().filesDir, "username.json")
                if (!userfile.exists()) {
                    Log.d("ListOverviewViewModel", "Found no user at ${userfile.absolutePath}")
                    return@withContext false
                }
                userfile.delete()
            } catch (ex : Exception) {
                Log.w("ListOverviewViewModel", "Failed to write username to file: $ex")
            }
        }
    }

    fun navigateToShoppingList(id : Long) {
        _navigateList.value = id
    }

    fun onShoppingListNavigated() {
        _navigateList.value = -1
    }

    private fun navigateToCreateList() {
        _createList.value = true
    }

    fun onCreateListNavigated() {
        _createList.value = false
    }

    private fun navigateToCreateUser() {
        _navigateUser.value = true
    }

    fun onCreateUserNavigated() {
        _navigateUser.value = false
    }

}