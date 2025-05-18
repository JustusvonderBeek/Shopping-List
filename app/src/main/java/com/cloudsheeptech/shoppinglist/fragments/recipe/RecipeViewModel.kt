package com.cloudsheeptech.shoppinglist.fragments.recipe

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.cloudsheeptech.shoppinglist.data.list.DbShoppingList
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListRepository
import com.cloudsheeptech.shoppinglist.data.recipe.ApiIngredient
import com.cloudsheeptech.shoppinglist.data.recipe.RecipeImage
import com.cloudsheeptech.shoppinglist.data.recipe.RecipeRepository
import com.cloudsheeptech.shoppinglist.data.sharing.recipe.RecipeShareRepository
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.max

@HiltViewModel
class RecipeViewModel
    @Inject
    constructor(
        private val recipeRepository: RecipeRepository,
        private val recipeShareRepository: RecipeShareRepository,
        private val userRepository: AppUserRepository,
        private val listRepository: ShoppingListRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val job = Job()
        private val vmScope = CoroutineScope(Dispatchers.Main + job)

        private var recipeId: Long = savedStateHandle["receiptId"] ?: -1L
        private var createdBy: Long = savedStateHandle["createdBy"] ?: -1L
        private val listPickerRecipeId: Long = savedStateHandle["recipeIdForSelectedList"] ?: -1L
        val title = MutableLiveData<String>("Rezept")
        val portionsText = MutableLiveData("Portions")

        private var selectedIngredients: List<ApiIngredient> = emptyList()
        private var selectedList: Pair<Long, Long> = Pair(-1L, -1L)

        private val _shoppingLists = listRepository.readAllLive()
        val shoppingLists: LiveData<List<DbShoppingList>> get() = _shoppingLists

        var recipe = recipeRepository.readLive(recipeId, createdBy)

        private var imageLocations = MutableLiveData<List<RecipeImage>>(emptyList())
        private val _images: MutableLiveData<List<String>> =
            MutableLiveData(emptyList<String>())
        val images: LiveData<List<String>> get() = _images

        private val _portions = MutableLiveData<Int>(2)
        val portions: LiveData<Int> get() = _portions

        @Suppress("ktlint:standard:backing-property-naming")
        private var _ingredients: LiveData<List<ApiIngredient>> =
            recipe.map { recipe -> recipe.ingredients }

        // TODO: Clean up this mess
        val ingredientWithPortionsApplied =
            MediatorLiveData<List<ApiIngredient>>().apply {
                addSource(_ingredients) { ingredients ->
                    val mappedIngredients =
                        ingredients.map { ingredient ->
                            return@map ingredient.copy(
                                quantity = ingredient.quantity * (portions.value ?: 1),
                            )
                        }
                    value = mappedIngredients
                }
                addSource(_portions) { portion ->
                    val mappedIngredients =
                        recipe.value?.ingredients?.map { ingredient ->
                            return@map ingredient.copy(quantity = ingredient.quantity * portion)
                        }
                    value = mappedIngredients
                }
            }

        private val _confirmDelete = MutableLiveData<Boolean>(false)
        val confirmDelete: LiveData<Boolean> get() = _confirmDelete

        private val _navigateToEdit = MutableLiveData<Pair<Long, Long>>(Pair(-1L, -1L))
        val navigateToEdit: LiveData<Pair<Long, Long>> get() = _navigateToEdit

        private val _navigateToShare = MutableLiveData<Pair<Long, Long>>(-1L to -1L)
        val navigateToShare: LiveData<Pair<Long, Long>> get() = _navigateToShare

        private val _navigateToSelectList = MutableLiveData<Boolean>(false)
        val navigateToSelectList: LiveData<Boolean> get() = _navigateToSelectList

        private val _navigateToCreateList = MutableLiveData<Boolean>(false)
        val navigateToCreateList: LiveData<Boolean> get() = _navigateToCreateList

        private val _navigateUp = MutableLiveData<Boolean>(false)
        val navigateUp: LiveData<Boolean> get() = _navigateUp

        private val _toastMessage = MutableLiveData(Pair("", -1))
        val toastMessage: LiveData<Pair<String, Int>> get() = _toastMessage

        fun setRecipeIds(
            recipeId: Long,
            createdBy: Long,
        ) {
            this.recipeId = recipeId
            this.createdBy = createdBy
            this.ingredientWithPortionsApplied.removeSource(this._ingredients)
            this.recipe = recipeRepository.readLive(this@RecipeViewModel.recipeId, createdBy)
            this._ingredients = this.recipe.map { recipe -> recipe.ingredients }
            this.ingredientWithPortionsApplied.addSource(this._ingredients) { ingredients ->
                val mappedIngredients =
                    ingredients.map { ingredient ->
                        return@map ingredient.copy(
                            quantity = ingredient.quantity * (portions.value ?: 1),
                        )
                    }
                this.ingredientWithPortionsApplied.value = mappedIngredients
            }
            vmScope.launch {
                var locationsTest =
                    recipeRepository.readAllImageLocations(recipeId, createdBy)
                withContext(Dispatchers.Main) {
                    _images.postValue(locationsTest.map { it.fileLocation })
                }
            }
        }

        fun setTitle(title: String) {
            this.title.value = title
        }

        fun setPortionsText(text: String) {
            this.portionsText.value = text
        }

        fun removeRecipe() {
            _confirmDelete.value = true
        }

        fun onDeleteConfirmed() {
            _confirmDelete.value = false
            vmScope.launch {
                // Differentiate between own and shared recipe
                val user = userRepository.read() ?: throw IllegalStateException("user null after login")
                if (createdBy != user.OnlineID) {
                    Log.i("RecipeViewModel", "Recipe is shared, deleting share and local recipe")
                    recipeShareRepository.delete(recipeId, createdBy, user.OnlineID)
                    recipeRepository.delete(recipeId, createdBy)
                } else {
                    recipeRepository.delete(recipeId, createdBy)
                }
                withContext(Dispatchers.Main) {
                    navigateUp()
                }
            }
        }

        fun onDeleteCanceled() {
            _confirmDelete.value = false
        }

        fun addRecipeToShoppingList() {
            Log.d("RecipeViewModel", "Adding items to viewmodel pressed")
            // First we need to know which list, then we can add the items into the list
            Log.d("RecipeViewModel", "Would add: ${recipe.value?.ingredients}")
            selectedIngredients = ingredientWithPortionsApplied.value ?: emptyList()
            navigateToSelectList()
        }

        fun increasePortions() {
            _portions.value = _portions.value?.plus(1)
        }

        fun decreasePortions() {
            _portions.value = max(1, _portions.value?.minus(1) ?: 1)
        }

        fun editReceipt() {
            _navigateToEdit.value = Pair(recipeId, createdBy)
        }

        fun shareRecipe() {
            _navigateToShare.value = recipeId to createdBy
        }

        private fun navigateToSelectList() {
            _navigateToSelectList.value = true
        }

        fun selectList(
            listId: Long,
            createdBy: Long,
        ) {
            selectedList = Pair(listId, createdBy)
            vmScope.launch {
                Log.d(
                    "RecipeViewModel",
                    "Adding ${selectedIngredients.size} items from $recipeId by $createdBy to list $listId",
                )
                listRepository.addAll(listId, createdBy, selectedIngredients)
                val list = listRepository.read(listId, createdBy) ?: return@launch
                makeToast(selectedIngredients.size, list.title)
                withContext(Dispatchers.Main) {
                    navigateUp()
                }
            }
        }

        fun onSelectListNavigated() {
            _navigateToSelectList.value = false
        }

        fun createList() {
            navigateToCreateList()
        }

        private fun navigateToCreateList() {
            _navigateToCreateList.value = true
        }

        fun onCreateListNavigated() {
            _navigateToCreateList.value = false
        }

        fun navigatedToEditWord() {
            _navigateToEdit.value = Pair(-1L, -1L)
        }

        fun onShareNavigated() {
            _navigateToShare.value = -1L to -1L
        }

        fun navigateUp() {
            _navigateUp.value = true
        }

        fun onUpNavigated() {
            _navigateUp.value = false
        }

        private fun makeToast(
            items: Int,
            list: String,
        ) {
            _toastMessage.value = Pair(list, items)
        }

        fun onToastMessageShown() {
            _toastMessage.value = Pair("", -1)
        }
    }
