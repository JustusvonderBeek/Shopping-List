package com.cloudsheeptech.shoppinglist.fragments.list_overview

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.cloudsheeptech.shoppinglist.ShoppingListApplication
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListRepository
import com.cloudsheeptech.shoppinglist.data.user.AppUserLocalDataSource
import com.cloudsheeptech.shoppinglist.data.user.AppUserRemoteDataSource
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import com.cloudsheeptech.shoppinglist.fragments.create.user.StartViewModel
import com.cloudsheeptech.shoppinglist.network.Networking
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/*
* This class is the main HUB of the application, taking care of user initialization etc.
* When no user is found, navigate to the user creation and only allow navigating back if a user is found
 */
@HiltViewModel
class ListOverviewViewModel @Inject constructor(
    private val listRepo: ShoppingListRepository,
    private val userRepo: AppUserRepository
) : ViewModel() {

    private val job = Job()
    private val vmCoroutine = CoroutineScope(Dispatchers.Main + job)

    // -----------------------------------------------
    // Navigation variables

    private  val _createList = MutableLiveData<Boolean>(false)
    val createList : LiveData<Boolean> get() = _createList

    private val _navigateList = MutableLiveData<Pair<Long, Long>>(Pair(-1, -1))
    val navigateList : LiveData<Pair<Long, Long>> get() = _navigateList

    private val _navigateUser = MutableLiveData<Boolean>(false)
    val navigateUser : LiveData<Boolean> get() = _navigateUser
    private val _refreshing = MutableLiveData<Boolean>(false)

    private val _navigateConfig = MutableLiveData<Boolean>(false)
    val navigateConfig : LiveData<Boolean> get() = _navigateConfig

    // UI State changes

    val refreshing : LiveData<Boolean> get() = _refreshing

    // Data

    val jsonSerializer = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
    }
    val user = userRepo.readLive()
    val shoppingList = listRepo.readAllLive()   // We only require the name and creator name

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
            userRepo.delete()
        }
    }

    private suspend fun removeItemsAndListsFromDatabase() {
        withContext(Dispatchers.IO) {
//            shoppingListDao.reset()
//            itemDao.deleteAll()
//            itemMappingDao.clearAll()
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

    private suspend fun updateListOverview() {
        withContext(Dispatchers.IO) {
            listRepo.readAllRemote()
        }
        withContext(Dispatchers.Main) {
            _refreshing.value = false
        }
    }

    // -----------------------------------------------

    fun navigateToShoppingList(id : Long, from : Long) {
        _navigateList.value = Pair(id, from)
    }

    fun onShoppingListNavigated() {
        _navigateList.value = Pair(-1, -1)
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

    fun navigateConfig() {
        _navigateConfig.value = true
    }

    fun onConfigNavigated() {
        _navigateConfig.value = false
    }

}