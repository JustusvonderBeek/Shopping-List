package com.cloudsheeptech.shoppinglist.ui.recipe.overview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.recipe.model.DbRecipe
import com.cloudsheeptech.shoppinglist.recipe.model.RecipeImage
import com.cloudsheeptech.shoppinglist.recipe.repo.RecipeRepository
import com.cloudsheeptech.shoppinglist.user.repo.AppUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class RecipesOverviewViewModel
    @Inject
    constructor(
        private val recipeRepository: RecipeRepository,
        private val userRepository: AppUserRepository,
    ) : ViewModel() {
        private val job = Job()
        private val vmScope = CoroutineScope(Dispatchers.Main + job)

        private val _navigateToCreateReceipt = MutableLiveData<Boolean>(false)
        val navigateToCreateReceipt: LiveData<Boolean> get() = _navigateToCreateReceipt

        private val _navigateToReceipt = MutableLiveData(Triple(-1L, -1L, ""))
        val navigateToReceipt: LiveData<Triple<Long, Long, String>> get() = _navigateToReceipt

        private val _receipts = recipeRepository.readAllLive()
        private val _recipesWithImages = recipeRepository.readAllLiveWithImages()
        val receipts: LiveData<List<Pair<DbRecipe, RecipeImage?>>> get() = _receipts
        val recipesWithImages: LiveData<List<Pair<DbRecipe, List<RecipeImage>>>> get() = _recipesWithImages

        private val _refreshing = MutableLiveData<Boolean>(false)
        val refreshing: LiveData<Boolean> get() = _refreshing

        fun updateAllRecipes() {
            vmScope.launch {
                recipeRepository.readAllOwnAndSharedRecipesOnline()
                withContext(Dispatchers.Main) {
                    _refreshing.value = false
                }
            }
        }

        fun navigateToReceipt(
            id: Long,
            createdBy: Long,
            title: String,
        ) {
            _navigateToReceipt.value = Triple(id, createdBy, title)
        }

        fun onReceiptNavigated() {
            _navigateToReceipt.value = Triple(-1, -1, "")
        }

        fun createReceipt() {
            _navigateToCreateReceipt.value = true
        }

        fun onCreateReceiptNavigate() {
            _navigateToCreateReceipt.value = false
        }
    }
