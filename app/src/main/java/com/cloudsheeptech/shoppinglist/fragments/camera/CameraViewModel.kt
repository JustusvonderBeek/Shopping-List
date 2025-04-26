package com.cloudsheeptech.shoppinglist.fragments.camera

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CameraViewModel
    @Inject
    constructor() : ViewModel() {
        private var _takePhoto = MutableLiveData(false)
        val takePhoto: LiveData<Boolean>
            get() = _takePhoto

        fun takePhoto() {
            _takePhoto.value = true
        }

        fun onPhotoTaken() {
            _takePhoto.value = false
        }
    }
