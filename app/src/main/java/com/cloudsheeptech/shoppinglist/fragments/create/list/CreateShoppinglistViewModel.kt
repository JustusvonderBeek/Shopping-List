package com.cloudsheeptech.shoppinglist.fragments.create.list

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListRepository
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
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
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val job = Job()
        private val vmCoroutine = CoroutineScope(Dispatchers.Main + job)

        private val titleToEdit = savedStateHandle["titleToEdit"] ?: ""
        private val listIdToEdit = savedStateHandle["listId"] ?: -1L
        val title = MutableLiveData<String>(titleToEdit)

        private val _editTitle = MutableLiveData<Boolean>(listIdToEdit > 0L)
        val editTitle: LiveData<Boolean> get() = _editTitle

        private val _navigateBack = MutableLiveData<Boolean>(false)
        val navigateBack: LiveData<Boolean> get() = _navigateBack

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
                if (listIdToEdit == -1L) {
                    val user = userRepository.read()
                    if (user == null || user.OnlineID == 0L) {
                        return@launch
                    }
                    shoppingListRepository.updateTitle(listIdToEdit, user.OnlineID, title.value!!)
                    withContext(Dispatchers.Main) {
                        navigateBack()
                    }
                    return@launch
                }
                shoppingListRepository.create(title.value!!)
                withContext(Dispatchers.Main) {
                    navigateBack()
                }
            }
        }

        fun navigateBack() {
            _navigateBack.value = true
        }

        fun onBackNavigated() {
            _navigateBack.value = false
        }

        fun navigateCreatedList() {
            Log.d("CreateShoppingListViewModel", "Navigating to list")
            _navigateToCreatedList.value = 1
        }

        fun onCreatedListNavigated() {
            _navigateToCreatedList.value = -1
        }
    }
