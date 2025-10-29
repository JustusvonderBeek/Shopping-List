package com.cloudsheeptech.shoppinglist.ui.list.create

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.list.model.ShoppingListPK
import com.cloudsheeptech.shoppinglist.list.repo.ShoppingListRepository
import com.cloudsheeptech.shoppinglist.user.repo.AppUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CreateShoppingListViewModel
    @Inject
    constructor(
        private val shoppingListRepository: ShoppingListRepository,
        private val userRepository: AppUserRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val job = Job()
        private val vmCoroutine = CoroutineScope(Dispatchers.Main + job)

        private val titleToEdit = savedStateHandle["titleToEdit"] ?: ""
        private val listIdToEdit = savedStateHandle["listId"] ?: -1L
        val title = MutableLiveData<String>(titleToEdit)

        private val _navigateToEditTitle = MutableLiveData<Boolean>(listIdToEdit > 0L)
        val navigateToEditTitle: LiveData<Boolean> get() = _navigateToEditTitle

        private val _navigateBack = MutableLiveData<BackNavigation>(BackNavigation.NONE)
        val navigateBack: LiveData<BackNavigation> get() = _navigateBack

        private val _navigateToCreatedList = MutableLiveData<Long>(-1)
        val navigateToCreatedList: LiveData<Long> get() = _navigateToCreatedList

        // -------------------- List Functions -----------------------

        fun create() {
            Log.d("CreateShoppingListViewModel", "Creating list pressed")
            if (title.value.isNullOrEmpty()) {
                Log.w(
                    "CreateShoppingListViewModel",
                    "Cannot create/update new list with empty title",
                )
                return
            }
            // In case the user is not correctly initialized only the ID should be 0
            // Updating the ID is handled by the list handler
            // Storing the list to database and posting it online handled by this function
//        listHandler.CreateNewShoppingList(title.value!!)
            vmCoroutine.launch {
                if (listIdToEdit > 0L) {
                    val user = userRepository.read() ?: throw IllegalStateException("user is null")
                    try {
                        shoppingListRepository.updateTitle(
                            ShoppingListPK(listIdToEdit, user.OnlineID),
                            title.value!!,
                        )
                        withContext(Dispatchers.Main) {
                            // Bug/Problem: When navigating back the title is still the old one
                            // because the data is stale at this point. Fix this problem and we can
                            // also navigate back to the list only
                            navigateBack(true)
                        }
                        return@launch
                    } catch (ex: Exception) {
                        Log.e("CreateShoppingListViewModel", "Failed to update list title: $ex")
                        return@launch
                    }
                }
                Log.d(
                    "CreateShoppinglistViewModel",
                    "ListID is 0, creating new list with title '${title.value}'",
                )
                val user =
                    userRepository.read() ?: throw IllegalStateException("user null after login")
                val beforeOnlineId = user.OnlineID
                try {
                    val newList = shoppingListRepository.create(title.value!!)
                    val navigateToOverview = beforeOnlineId != newList.createdBy.onlineId
                    withContext(Dispatchers.Main) {
                        // Navigate to the overview, in case the user has changed
                        navigateBack(navigateToOverview)
                    }
                } catch (ex: Exception) {
                    Log.e(
                        "CreateShoppingListViewModel",
                        "Failed to create list ${title.value}: $ex",
                    )
                }
            }
        }

        // ----------------------- Navigation Functions ------------------------

        fun navigateBack(toOverview: Boolean) {
            _navigateBack.value =
                if (toOverview) {
                    BackNavigation.TO_OVERVIEW
                } else {
                    BackNavigation.TO_LIST
                }
        }

        fun onBackNavigated() {
            _navigateBack.value = BackNavigation.NONE
        }

        fun navigateToCreatedList() {
            Log.d("CreateShoppingListViewModel", "Navigating to list")
            _navigateToCreatedList.value = 1
        }

        fun onCreatedListNavigated() {
            _navigateToCreatedList.value = -1
        }
    }
