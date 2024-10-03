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
import javax.inject.Inject

@HiltViewModel
class RecipesOverviewViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val userRepository: AppUserRepository,
) : ViewModel() {

    private val job = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + job)

    private val _navigateToCreateReceipt = MutableLiveData<Boolean>(false)
    val navigateToCreateReceipt : LiveData<Boolean> get() = _navigateToCreateReceipt

    private val _navigateToReceipt = MutableLiveData<Pair<Long, Long>>(Pair(-1L, -1L))
    val navigateToReceipt : LiveData<Pair<Long, Long>> get() = _navigateToReceipt

    private val _receipts = recipeRepository.readAllLive()
    val receipts : LiveData<List<DbRecipe>> get() = _receipts

    fun navigateToReceipt(id: Long, createdBy: Long) {
        _navigateToReceipt.value = Pair(id, createdBy)
    }

    fun onReceiptNavigated() {
        _navigateToReceipt.value = Pair(-1, -1)
    }

    fun createReceipt() {
        _navigateToCreateReceipt.value = true
    }

    fun onCreateReceiptNavigate() {
        _navigateToCreateReceipt.value = false
    }

}