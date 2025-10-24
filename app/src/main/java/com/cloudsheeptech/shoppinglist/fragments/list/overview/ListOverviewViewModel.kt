package com.cloudsheeptech.shoppinglist.fragments.list.overview

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import com.cloudsheeptech.shoppinglist.list.repo.ShoppingListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/*
* This class is the main HUB of the application, taking care of user initialization etc.
* When no user is found, navigate to the user creation and only allow navigating back if a user is found
 */
@HiltViewModel
class ListOverviewViewModel
    @Inject
    constructor(
        private val shoppingListRepository: ShoppingListRepository,
        private val userRepo: AppUserRepository,
    ) : ViewModel() {
        private val job = Job()
        private val coroutineScope = CoroutineScope(Dispatchers.Main + job)

        // -----------------------------------------------
        // Navigation variables

        private val _createList = MutableLiveData<Boolean>(false)
        val createList: LiveData<Boolean> get() = _createList

        private val _navigateList = MutableLiveData<Triple<Long, Long, String>>(Triple(-1, -1, ""))
        val navigateList: LiveData<Triple<Long, Long, String>> get() = _navigateList

        private val _navigateUser = MutableLiveData<Boolean>(false)
        val navigateUser: LiveData<Boolean> get() = _navigateUser
        private val _refreshing = MutableLiveData<Boolean>(false)

        private val _navigateConfig = MutableLiveData<Boolean>(false)
        val navigateConfig: LiveData<Boolean> get() = _navigateConfig

        // UI State changes
        val refreshing: LiveData<Boolean> get() = _refreshing

        // Data
        val user = userRepo.readLive()
        val shoppingList = shoppingListRepository.readAllLive() // We only require the name and creator name

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
            navigateToCreateList()
        }

        fun removeUser() {
            coroutineScope.launch {
                try {
                    shoppingListRepository.resetCreatedByForOwnLists()
                    userRepo.delete()
                } catch (ex: Exception) {
                    Log.e("ListOverviewViewModel", "Failed to remove current user: $ex")
                }
            }
        }

        private suspend fun removeItemsAndListsFromDatabase() {
            withContext(Dispatchers.IO) {
                shoppingListRepository.deleteAll()
            }
        }

        fun clearDatabase() {
            coroutineScope.launch {
                try {
                    removeItemsAndListsFromDatabase()
                } catch (ex: Exception) {
                    Log.e("ListOverviewViewModel", "Failed to clear database: $ex")
                }
            }
        }

        fun updateAllLists() {
            Log.d("ListOverviewViewModel", "Updating all list for this user")
            coroutineScope.launch {
                withContext(Dispatchers.Main) {
                    _refreshing.value = true
                }
                // Launch the update for the own lists
                updateAllListsFromRemote()
                withContext(Dispatchers.Main) {
                    _refreshing.value = false
                }
            }
        }

        private suspend fun updateAllListsFromRemote() {
            withContext(Dispatchers.IO) {
                shoppingListRepository.readAllRemote()
            }
        }

        // -----------------------------------------------

        fun navigateToShoppingList(
            id: Long,
            from: Long,
            title: String,
        ) {
            _navigateList.value = Triple(id, from, title)
        }

        fun onShoppingListNavigated() {
            _navigateList.value = Triple(-1, -1, "")
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

        fun navigateConfig() {
            _navigateConfig.value = true
        }

        fun onConfigNavigated() {
            _navigateConfig.value = false
        }
    }
