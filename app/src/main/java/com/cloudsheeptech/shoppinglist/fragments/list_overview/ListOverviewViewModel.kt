package com.cloudsheeptech.shoppinglist.fragments.list_overview

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cloudsheeptech.shoppinglist.data.Item
import com.cloudsheeptech.shoppinglist.data.ListMapping
import com.cloudsheeptech.shoppinglist.data.ShoppingList
import com.cloudsheeptech.shoppinglist.data.ShoppingListWire
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.handling.ShoppingListHandler
import com.cloudsheeptech.shoppinglist.network.Networking
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.Instant
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
    private val itemDao = database.itemDao()
    private val userDao = database.userDao()
    private val itemMappingDao = database.mappingDao()
    private val listHandler = ShoppingListHandler(database)

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
    val shoppingList = shoppingListDao.getShoppingListsLive()

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
                    Log.d("ListOverviewViewModel", "Creating new list")
                    val newList = ShoppingList(list.ListId, list.Name, list.CreatedBy, list.LastEdited)
                    shoppingListDao.insertList(newList)
                    continue
                }
                if (list.LastEdited != localList.LastEdited) {
                    // Convert string into date and compare which one is newer
                    try {
//                        Log.d("ListOverviewViewModel", "Format: ${formatter.parse(list.LastEdited)}")
                        val convertedOnline = Instant.parse(list.LastEdited)
//                        Log.d("ListOverviewViewModel", "Converted online parsed")
                        val convertedLocal = Instant.parse(localList.LastEdited)
//                        Log.d("ListOverviewViewModel", "Local parsed")
                        if (convertedOnline.isAfter(convertedLocal)) {
                            Log.d("ListOverviewViewModel", "Online list ${list.ListId} is newer than local list! Updating")
                            val convertedOnlineToLocal = ShoppingList(list.ListId, list.Name, list.CreatedBy, list.LastEdited)
                            shoppingListDao.updateList(convertedOnlineToLocal)
                            // Fill the list with items
                            for (item in list.Items) {
                                var dbItem = itemDao.getItemFromName(item.Name)
                                if (dbItem == null) {
                                    dbItem = Item(0, item.Name, item.Icon)
                                    itemDao.insertItem(dbItem)
                                    dbItem = itemDao.getItemFromName(item.Name)
                                }
                                var dbMapping = itemMappingDao.getMappingForItemAndList(dbItem!!.ID, list.ListId)
                                if (dbMapping.isEmpty()) {
                                    val mapping = ListMapping(0, dbItem.ID, list.ListId, item.Quantity, item.Checked, list.CreatedBy.ID)
                                    itemMappingDao.insertMapping(mapping)
                                } else {
                                    val mapping = dbMapping[0]
                                    mapping.Quantity = item.Quantity
                                    mapping.Checked = item.Checked
                                    itemMappingDao.updateMapping(mapping)
                                }
                            }
                        }
                    } catch (ex : Exception) {
                        Log.w("ListOverviewViewModel", "Failed to merge lists: $ex")
                    }
                } else {
                    Log.d("ListOverviewViewModel", "Both lists are the same")
                }
            }
        }
    }

    private suspend fun updateListOverview() {
        withContext(Dispatchers.IO) {
            listHandler.GetAllShoppingLists()
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