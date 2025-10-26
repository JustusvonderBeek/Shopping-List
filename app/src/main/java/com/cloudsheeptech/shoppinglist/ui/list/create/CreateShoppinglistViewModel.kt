package com.cloudsheeptech.shoppinglist.ui.list.create

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
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

@HiltViewModel
class CreateShoppinglistViewModel
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

        private val _editTitle = MutableLiveData<Boolean>(listIdToEdit > 0L)
        val editTitle: LiveData<Boolean> get() = _editTitle

        private val _navigateBack = MutableLiveData<BackNavigation>(BackNavigation.NONE)
        val navigateBack: LiveData<BackNavigation> get() = _navigateBack

        private val _navigateToCreatedList = MutableLiveData<Long>(-1)
        val navigateToCreatedList: LiveData<Long> get() = _navigateToCreatedList

        fun create() {
            Log.d("CreateShoppinglistViewModel", "Creating list pressed")
            if (title.value == null || title.value!!.isEmpty()) {
                return
            }
            // In case the user is not correctly initialized only the ID should be 0
            // Updating the ID is handled by the list handler
            // Storing the list to database and posting it online handled by this function
//        listHandler.CreateNewShoppingList(title.value!!)
            vmCoroutine.launch {
                if (listIdToEdit > 0L) {
                    val user = userRepository.read() ?: throw IllegalStateException("user is null")
                    shoppingListRepository.updateTitle(listIdToEdit, user.OnlineID, title.value!!)
                    withContext(Dispatchers.Main) {
                        navigateBack(true)
                    }
                    return@launch
                }
                Log.d("CreateShoppinglistViewModel", "ListID is 0, creating new list with title '${title.value}'")
                shoppingListRepository.create(title.value!!)
                withContext(Dispatchers.Main) {
                    navigateBack(false)
                }
            }
        }

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

        fun navigateCreatedList() {
            Log.d("CreateShoppingListViewModel", "Navigating to list")
            _navigateToCreatedList.value = 1
        }

        fun onCreatedListNavigated() {
            _navigateToCreatedList.value = -1
        }
    }
