package com.cloudsheeptech.shoppinglist.list_overview

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.cloudsheeptech.shoppinglist.data.ShoppingList
import com.cloudsheeptech.shoppinglist.data.ShoppingListWire
import com.cloudsheeptech.shoppinglist.data.User
import com.cloudsheeptech.shoppinglist.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.network.Networking
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import kotlin.Exception

/*
* This class is the main HUB of the application, taking care of user initialization etc.
* When no user is found, navigate to the user creation and only allow navigating back if a user is found
 */
class ListOverviewViewModel(application : Application) : AndroidViewModel(application) {

    private val job = Job()
    private val vmCoroutine = CoroutineScope(Dispatchers.Main + job)

    // -----------------------------------------------

    private val database = ShoppingListDatabase.getInstance(application.applicationContext)
    private val shoppingListDao = database.shoppingListDao()
    private val itemDao = database.itemListDao()
    private val userDao = database.userDao()
    private val itemMappingDao = database.mappingDao()

    // Navigation variables

    private  val _createList = MutableLiveData<Boolean>(false)
    val createList : LiveData<Boolean> get() = _createList

    private val _navigateList = MutableLiveData<Long>(-1)
    val navigateList : LiveData<Long> get() = _navigateList

    private val _navigateUser = MutableLiveData<Boolean>(false)
    val navigateUser : LiveData<Boolean> get() = _navigateUser
    private val _refreshing = MutableLiveData<Boolean>(false)

    // UI State changes

    val refreshing : LiveData<Boolean> get() = _refreshing

    // Data

    val jsonSerializer = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
    }
    val user = userDao.getUserLive()
    val shoppingList = shoppingListDao.getShoppingLists()

    // -----------------------------------------------

    init {
        checkInitialized()
    }

    private fun checkInitialized() {
        if (user.value == null) {
            Log.d("ListOverviewViewModel", "User is not initialized. Creating user")
            navigateToCreateUser()
        }
    }

    fun createNewList() {
//        Log.d("ListOverviewViewModel", "Creating new list")
//        val list = Item((shoppingList.size() + 1).toLong(), "New List", "")
//        shoppingList.addItem(list)
        navigateToCreateList()
    }

    fun removeUser() {
        vmCoroutine.launch {
            deleteUserFromDatabase()
        }
    }

    private suspend fun deleteUserFromDatabase() {
        withContext(Dispatchers.IO) {
            userDao.resetUser()
        }
    }

    private suspend fun removeItemsAndListsFromDatabase() {
        withContext(Dispatchers.IO) {
            shoppingListDao.reset()
            itemDao.deleteAll()
            itemMappingDao.clearAll()
        }
    }

    fun clearDatabase() {
        vmCoroutine.launch {
            removeItemsAndListsFromDatabase()
        }
    }

    fun updateAllLists() {
        Log.d("ListOverviewViewModel", "Updating all list for this user")
        _refreshing.value = true
        vmCoroutine.launch {
            // Launch the update for the own lists
            updateListOverview()
        }
    }

    private suspend fun mergeUpdatedAndExistingLists(onlineList : List<ShoppingListWire>) {
        withContext(Dispatchers.IO) {
            val finalList = mutableListOf<ShoppingList>()
            for (list in onlineList) {
                // Compare online and local list and take what is more recent
                val localList = shoppingList.value!!.find { x -> x.ID == list.ListId }
                if (localList == null) {
                    // TODO: Create new list
                    val newList = ShoppingList(list.ListId, list.Name, User(list.CreatedBy), list.LastEdited)
                    shoppingListDao.insertList(newList)
                    continue
                }
                if (list.LastEdited != localList.LastEdited) {
                    // Convert string into date and compare which one is newer
                    val formatter = SimpleDateFormat(DateTimeFormatter.ISO_INSTANT.toString())
                    val convertedOnline = formatter.parse(list.LastEdited)!!
                    val convertedLocal = formatter.parse(localList.LastEdited)!!
                    if (convertedOnline.after(convertedLocal)) {
                        Log.d("ListOverviewViewModel", "Online list ${list.ListId} is newer than local list! Updating")
                        val responsibleUser = User(list.CreatedBy)
                        val convertedOnlineToLocal = ShoppingList(list.ListId, list.Name, responsibleUser, list.LastEdited)
                        shoppingListDao.updateList(convertedOnlineToLocal)
                    }
                } else {
                    Log.d("ListOverviewViewModel", "Both lists are the same")
                }
            }
        }
    }

    private suspend fun updateListOverview() {
        withContext(Dispatchers.IO) {
            Networking.GET("v1/lists/${user.value!!.ID}") { resp ->
                if (resp.status != HttpStatusCode.OK) {
                    Log.w("ListOverviewViewModel", "Fetching lists failed")
                    return@GET
                }
                try {
                    val body = resp.bodyAsText(Charsets.UTF_8)
                    val lists = Json.decodeFromString<List<ShoppingListWire>>(body)
                    mergeUpdatedAndExistingLists(lists)
                } catch (ex : Exception) {
                    Log.w("ListOverviewViewModel", "Failed to process server response: $ex")
                    return@GET
                }
            }
        }
        withContext(Dispatchers.Main) {
            _refreshing.value = false
        }
    }

    // -----------------------------------------------

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
//        if (user.value != null) {
//
//        } else {
//            Log.d("ListOverviewViewModel", "User is still null")
//        }
    }

}