package com.cloudsheeptech.shoppinglist.fragments.create.list

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListRepository
import com.cloudsheeptech.shoppinglist.data.user.AppUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import javax.inject.Inject

@HiltViewModel
class CreateShoppinglistViewModel @Inject constructor(
    private val shoppingListRepository: ShoppingListRepository
) : ViewModel() {

    private val job = Job()
    private val vmCoroutine = CoroutineScope(Dispatchers.Main + job)

    val title = MutableLiveData<String>("")

    private val _navigateBack = MutableLiveData<Boolean>(false)
    val navigateBack : LiveData<Boolean> get() = _navigateBack

    private val _navigateToCreatedList = MutableLiveData<Long>(-1)
    val navigateToCreatedList : LiveData<Long> get() = _navigateToCreatedList

//    private val listHandler = ShoppingListRepository(ShoppingListDatabase.getInstance(application))
//    private val user = AppUserLocalDataSource.getUser()
    private val user = AppUser(0, 0, "", "", OffsetDateTime.now())

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