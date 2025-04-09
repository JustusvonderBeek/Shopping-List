package com.cloudsheeptech.shoppinglist.fragments.recipes_overview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.data.recipe.DbRecipe
import com.cloudsheeptech.shoppinglist.data.recipe.RecipeRepository
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class RecipesOverviewViewModel @Inject constructor(
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
    val receipts: LiveData<List<DbRecipe>> get() = _receipts

    private val _refreshing = MutableLiveData<Boolean>(false)
    val refreshing: LiveData<Boolean> get() = _refreshing

    fun updateAllRecipes() {
        vmScope.launch {
            recipeRepository.readAllOnline()
            withContext(Dispatchers.Main) {
                _refreshing.value = false
            }
        }
    }

    fun navigateToReceipt(id: Long, createdBy: Long, title: String) {
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