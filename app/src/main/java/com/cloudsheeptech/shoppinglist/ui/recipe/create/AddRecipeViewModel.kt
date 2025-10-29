package com.cloudsheeptech.shoppinglist.ui.recipe.create

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.list.model.DbItem
import com.cloudsheeptech.shoppinglist.recipe.model.ApiDescription
import com.cloudsheeptech.shoppinglist.recipe.repo.RecipeRepository
import com.cloudsheeptech.shoppinglist.user.repo.AppUserRepository
import com.cloudsheeptech.shoppinglist.util.SingleEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AddRecipeViewModel
    @Inject
    constructor(
        private val recipeRepository: RecipeRepository,
        private val userRepository: AppUserRepository,
    ) : ViewModel() {
        private val job = Job()
        private val addVmScope = CoroutineScope(Dispatchers.IO + job)

        val dbItemListWithName = MutableLiveData<List<DbItem>>()

        val receiptName = MutableLiveData<String>()
        val receiptDescription = MutableLiveData<String>()

        private val handledToast = MutableLiveData<SingleEvent<String>>()
        val toast: LiveData<SingleEvent<String>>
            get() = handledToast

        private val _selectImage = MutableLiveData<Boolean>(false)
        val selectImage: LiveData<Boolean> get() = _selectImage
        private val _navigateUp = MutableLiveData<Boolean>(false)
        val navigateUp: LiveData<Boolean> get() = _navigateUp

        fun create() {
            val currentTitle = receiptName.value ?: return
            val currentDescription = receiptDescription.value ?: return
            addVmScope.launch {
                val recipe =
                    recipeRepository.create(currentTitle, 2, emptyList(), emptyList(), emptyList())
                recipe.description = listOf(ApiDescription(1, currentDescription))
                recipeRepository.update(recipe, emptyList())
                withContext(Dispatchers.Main) {
                    navigateUp()
                }
            }
        }

        fun selectImage() {
            this._selectImage.value = true
        }

        fun onImageSelected() {
            this._selectImage.value = false
        }

        private fun navigateUp() {
            _navigateUp.value = true
        }

        fun onUpNavigated() {
            _navigateUp.value = false
        }
    }
