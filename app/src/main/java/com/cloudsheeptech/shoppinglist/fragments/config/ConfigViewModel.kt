package com.cloudsheeptech.shoppinglist.fragments.config

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.cloudsheeptech.shoppinglist.BuildConfig
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListRepository
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ConfigViewModel @Inject constructor(
    private val userRepository: AppUserRepository,
    private val shoppingListRepository: ShoppingListRepository,
): ViewModel() {

    private val job = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + job)

    private val _appVersion = MutableLiveData<String>("Version: ${BuildConfig.VERSION_NAME}")
    val appVersion : LiveData<String> get() = _appVersion

    val remoteUrl = MutableLiveData<String>("https://10.0.2.2:46152")

    private val user = userRepository.readLive()
    val username : LiveData<String> get() = user.map { x -> x.Username }

    // We want to create the user if not already online, aka. id = 0, otherwise delete
    private val _offlineUser = user.map { x -> x.OnlineID == 0L }
    val offlineUser : LiveData<Boolean> get() = _offlineUser

    fun toggleUserOnline() {
        Log.d("ConfigViewModel", "Toggling the current user status")
        if (offlineUser.value == true) {
            vmScope.launch {
                createUserOnlineAndResetOwnLists()
            }
        } else {
            vmScope.launch {
                resetUserOnlineAndOwnLists()
            }
        }
    }

    // TODO: All this fails when the user was created online but the list not!
    // So prevent that another function can create the user online, but not update the list
    private suspend fun createUserOnlineAndResetOwnLists() {
        val currentUser = userRepository.read() ?: return
        if (currentUser.OnlineID != 0L) {
            Log.w("ConfigViewModel", "The current user is already registered online")
            // Should we still run the conversion from lists with createdBy = 0L to currentId?
            return
        }
        userRepository.createOnline()
        val updatedUser = userRepository.read() ?: return
        if (updatedUser.OnlineID == 0L) {
            Log.w("ConfigViewModel", "Failed to create the user online")
            return
        }
        // Additionally creates the lists online
        shoppingListRepository.updateCreatedByToCurrentId()
    }

    private suspend fun resetUserOnlineAndOwnLists() {
        val currentUser = userRepository.read() ?: return
        if (currentUser.OnlineID == 0L) {
            Log.w("ConfigViewModel", "The current user is not registered online")
            return
        }
        shoppingListRepository.resetCreatedBy()
        userRepository.delete()
    }

}