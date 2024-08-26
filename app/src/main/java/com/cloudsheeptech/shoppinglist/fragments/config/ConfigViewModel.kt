package com.cloudsheeptech.shoppinglist.fragments.config

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.cloudsheeptech.shoppinglist.BuildConfig
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfigViewModel @Inject constructor(
    private val userRepository: AppUserRepository
): ViewModel() {

    private val job = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + job)

    private val _appVersion = MutableLiveData<String>("Version: ${BuildConfig.VERSION_NAME}")
    val appVersion : LiveData<String> get() = _appVersion

    val remoteUrl = MutableLiveData<String>("https://10.0.2.2:46152")

    private val user = userRepository.readLive()
    val username : LiveData<String> get() = user.map { x -> x.Username }

    // We want to create the user if not already online, aka. id = 0, otherwise delete
    private val _buttonStatus = user.map { x -> x.OnlineID == 0L }
    val buttonStatus : LiveData<Boolean> get() = _buttonStatus

    fun toggleUserOnline() {
        Log.d("ConfigViewModel", "Toggling the current user status")
        vmScope.launch {
            userRepository.createOnline()
        }
    }


}