package com.cloudsheeptech.shoppinglist.fragments.recipe

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.data.Recipe
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecipeViewModel() : ViewModel() {

    private val job = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + job)

    val recipe = MutableLiveData<Recipe>()

    private val _navigateToEdit = MutableLiveData<Int>(-1)
    val navigateToEdit : LiveData<Int> get() = _navigateToEdit

    fun removeRecipe() {

    }

    fun addRecipeToShoppingList() {

    }

    fun editWord() {
        _navigateToEdit.value = recipe.value!!.ID.toInt()
    }

    fun navigatedToEditWord() {
        _navigateToEdit.value = -1
    }

}