package com.cloudsheeptech.shoppinglist.fragments.config

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.cloudsheeptech.shoppinglist.BuildConfig
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ConfigViewModel @Inject constructor(
    private val userRepository: AppUserRepository
): ViewModel() {

    private val _appVersion = MutableLiveData<String>("Version: ${BuildConfig.VERSION_NAME}")
    val appVersion : LiveData<String> get() = _appVersion

    val remoteUrl = MutableLiveData<String>("https://10.0.2.2:46152")

    private val user = userRepository.readLive()
    val username : LiveData<String> get() = user.map { x -> x.Username }
}