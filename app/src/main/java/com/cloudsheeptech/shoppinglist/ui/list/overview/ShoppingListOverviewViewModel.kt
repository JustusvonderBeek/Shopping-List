package com.cloudsheeptech.shoppinglist.ui.list.overview

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.list.repo.ShoppingListRepository
import com.cloudsheeptech.shoppinglist.user.repo.AppUserRepository
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
class ShoppingListOverviewViewModel
    @Inject
    constructor(
        private val shoppingListRepository: ShoppingListRepository,
        private val userRepo: AppUserRepository,
    ) : ViewModel() {
        // ------------------- Async variables -------------------
        private val job = Job()
        private val vmCoroutine = CoroutineScope(Dispatchers.Main + job)

        // ------------------- Navigation variables -------------------

        private val _createList = MutableLiveData<Boolean>(false)
        val createList: LiveData<Boolean> get() = _createList

        // ListId, CreatedBy, Title
        private val _navigateToList = MutableLiveData<Triple<Long, Long, String>>(Triple(-1, -1, ""))
        val navigateToList: LiveData<Triple<Long, Long, String>> get() = _navigateToList

        private val _refreshing = MutableLiveData<Boolean>(false)
        val refreshing: LiveData<Boolean> get() = _refreshing

        // TODO: Is a configuration really necessary after debugging? In production? I guess not
        private val _navigateToConfig = MutableLiveData<Boolean>(false)
        val navigateToConfig: LiveData<Boolean> get() = _navigateToConfig

        // ------------------- Data -------------------
        val user = userRepo.readLive()
        val allShoppingLists = shoppingListRepository.readAllLive()

        // --------------------- Lists Handling --------------------------

        fun createNewList() {
            navigateToCreateList()
        }

        fun updateAllLists() {
            Log.d("ListOverviewViewModel", "Updating all list for this user")
            vmCoroutine.launch {
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

        // --------------------- Drop Down Menu Handling --------------------------

        fun removeUser() {
            vmCoroutine.launch {
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
            vmCoroutine.launch {
                try {
                    removeItemsAndListsFromDatabase()
                } catch (ex: Exception) {
                    Log.e("ListOverviewViewModel", "Failed to clear database: $ex")
                }
            }
        }

        // ------------------- Navigation functions -------------------

        fun navigateToShoppingList(
            id: Long,
            from: Long,
            title: String,
        ) {
            _navigateToList.value = Triple(id, from, title)
        }

        fun onShoppingListNavigated() {
            _navigateToList.value = Triple(-1, -1, "")
        }

        private fun navigateToCreateList() {
            _createList.value = true
        }

        fun onCreateListNavigated() {
            _createList.value = false
        }

        fun navigateConfig() {
            _navigateToConfig.value = true
        }

        fun onConfigNavigated() {
            _navigateToConfig.value = false
        }
    }
