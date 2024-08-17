package com.cloudsheeptech.shoppinglist.fragments.create.user

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.user.AppUser
import com.cloudsheeptech.shoppinglist.data.user.AppUserLocalDataSource
import com.cloudsheeptech.shoppinglist.data.user.AppUserRemoteDataSource
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import com.cloudsheeptech.shoppinglist.network.Networking
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class StartViewModel @Inject constructor(private val userRepository: AppUserRepository) : ViewModel() {

    // UI State
    private val job = Job()
    private val asyncScope = CoroutineScope(Dispatchers.Main + job)
    private val _hideKeyboard = MutableLiveData<Boolean>(false)
    val hideKeyboard : LiveData<Boolean> get() = _hideKeyboard

    val user = userRepository.readLive()
    val inputText = MutableLiveData<String>()
    private val _disableButton = MutableLiveData<Boolean>()
    val disableButton : LiveData<Boolean> get() = _disableButton

    // -----------------------------------------------

    init {
        if (user.value != null) {
            Log.d("StartViewModel", "User initialized!")
        }
    }

//    fun injectUserRepo(userRepository: AppUserRepository) {
//        this.userRepository = userRepository
//    }

    fun pushUsername() {
        hideKeyboard()
        if (inputText.value == null)
            return
        if (inputText.value!!.isEmpty())
            return
        disableButton()
        asyncScope.launch {
            userRepository.create(inputText.value!!)
            withContext(Dispatchers.Main) {
                enableButton()
            }
        }
    }

    // -----------------------------------------------

    private fun disableButton() {
        _disableButton.value = true
    }

    private fun enableButton() {
        _disableButton.value = false
    }

    private fun hideKeyboard() {
        _hideKeyboard.value = true
    }

    fun keyboardHidden() {
        _hideKeyboard.value = false
    }
}