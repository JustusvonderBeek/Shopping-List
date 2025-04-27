package com.cloudsheeptech.shoppinglist.fragments.camera

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.data.recipe.BinaryFileHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CameraViewModel
    @Inject
    constructor(
        private val binaryFileHandler: BinaryFileHandler,
    ) : ViewModel() {
        private var _takePhoto = MutableLiveData(false)
        val takePhoto: LiveData<Boolean>
            get() = _takePhoto

        private var _navigateUp = MutableLiveData(false)
        val navigateUp: LiveData<Boolean>
            get() = _navigateUp

        private val imagePaths = mutableListOf<String>()

        fun getImagePaths(): List<String> = imagePaths

        suspend fun clearTemporaryImages() {
            withContext(Dispatchers.IO) {
                val nonRemovedPaths = mutableListOf<String>()
                for (image in imagePaths) {
                    val deleted = binaryFileHandler.deleteImage(image)
                    if (!deleted) {
                        nonRemovedPaths.add(image)
                    }
                }
                imagePaths.clear()
                imagePaths.addAll(nonRemovedPaths)
        }
    }

        fun takePhoto() {
            _takePhoto.value = true
        }

        fun addPhotoPath(imagePath: String) {
        imagePaths.add(imagePath)
    }

        fun onPhotoTaken() {
            _takePhoto.value = false
        }

        fun finish() {
            _navigateUp.value = true
    }

    fun onFinished() {
        _navigateUp.value = false
    }
    }
