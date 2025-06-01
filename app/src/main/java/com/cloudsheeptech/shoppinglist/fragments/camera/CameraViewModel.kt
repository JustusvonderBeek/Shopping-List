package com.cloudsheeptech.shoppinglist.fragments.camera

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
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

        private var _selectPhotos = MutableLiveData(false)
        val selectPhotos: LiveData<Boolean>
            get() = _selectPhotos

        private var _navigateUp = MutableLiveData(false)
        val navigateUp: LiveData<Boolean>
            get() = _navigateUp

        private val selectedImages = mutableListOf<String>()
        private val _imagePaths = MutableLiveData(emptyList<String>())
        val imagePaths: LiveData<List<String>> get() = _imagePaths

        fun getImagePaths(): List<String> = selectedImages

        suspend fun clearTemporaryImages() {
            withContext(Dispatchers.IO) {
                val nonRemovedPaths = mutableListOf<String>()
                for (image in imagePaths.value!!) {
                    val deleted = binaryFileHandler.deleteImage(image)
                    if (!deleted) {
                        nonRemovedPaths.add(image)
                    }
                }
                _imagePaths.value = nonRemovedPaths
            }
        }

        fun takePhoto() {
            _takePhoto.value = true
        }

        fun addPhotoPath(imagePath: String) {
            val appendedPath = _imagePaths.value?.toMutableList()
            appendedPath?.add(imagePath)
            _imagePaths.value = appendedPath
            appendedPath?.let { paths ->
                setSelectedImages(paths.map { path -> path.toUri() })
            }
        }

        fun setSelectedImages(imagePaths: List<Uri>) {
            selectedImages.clear()
            selectedImages.addAll(imagePaths.map { it.toString() })
            Log.d("CameraViewModel", "Selected images: $selectedImages")
        }

        fun onPhotoTaken() {
            _takePhoto.value = false
        }

        fun selectPhotos() {
            _selectPhotos.value = true
        }

        fun onPhotosSelected() {
            _selectPhotos.value = false
        }

        fun finish() {
            _navigateUp.value = true
        }

        fun onFinished() {
            _navigateUp.value = false
        }
    }
