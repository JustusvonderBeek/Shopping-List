package com.cloudsheeptech.shoppinglist.fragments.recipe

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListRepository
import com.cloudsheeptech.shoppinglist.data.receipt.ApiIngredient
import com.cloudsheeptech.shoppinglist.data.receipt.ReceiptRepository
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
class RecipeViewModel @Inject constructor(
    private val receiptRepository: ReceiptRepository,
    private val userRepository: AppUserRepository,
    private val listRepository: ShoppingListRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val job = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + job)

    private val receiptId : Long = savedStateHandle["receiptId"]!!
    private val createdBy : Long = savedStateHandle["createdBy"]!!

    val receipt = receiptRepository.readLive(receiptId, createdBy)

    private val _portions = MutableLiveData<Int>(2)
    val portions : LiveData<Int> get() = _portions

    private val _ingredients = receipt.switchMap { rec ->
        liveData {
            emit(rec.ingredients)
        }
    }
//    private val _ingredients = MediatorLiveData<List<ApiIngredient>>()
    val ingredients : LiveData<List<ApiIngredient>> get() = _ingredients

    private val _navigateToEdit = MutableLiveData<Pair<Long, Long>>(Pair(-1L, -1L))
    val navigateToEdit : LiveData<Pair<Long, Long>> get() = _navigateToEdit

    private val _navigateUp = MutableLiveData<Boolean>(false)
    val navigateUp : LiveData<Boolean> get() = _navigateUp

    init {
//        _ingredients.addSource(portions) { portion ->
//            val mappedIngredients = receipt.value?.ingredients?.map { x ->
//                x.quantity *= portion
//                x
//            } ?: emptyList()
//            _ingredients.value = mappedIngredients
//        }
//        _portions.value = 2
    }

    // TODO: Include a question if the receipt should really be deleted
    fun removeRecipe() {
        vmScope.launch {
            receiptRepository.delete(receiptId, createdBy)
            withContext(Dispatchers.Main) {
                navigateUp()
            }
        }
    }

    fun addRecipeToShoppingList() {
        Log.d("RecipeViewModel", "Adding items to viewmodel pressed")
        // First we need to know which list, then we can add the items into the list
        Log.d("RecipeViewModel", "Would add: ${receipt.value?.ingredients}")
        vmScope.launch {
            // TODO: Ask the user which list he wants to use, or create a new one
            listRepository.addAll(1, 7259303, receipt!!.value!!.ingredients)
            withContext(Dispatchers.Main) {
                navigateUp()
            }
        }
    }

    fun increasePortions() {
        _portions.value = _portions.value?.plus(1)
    }

    fun decreasePortions() {
        _portions.value = max(1, _portions.value?.minus(1) ?: 1)
    }

    fun editReceipt() {
        _navigateToEdit.value = Pair(receiptId, createdBy)
    }

    fun navigatedToEditWord() {
        _navigateToEdit.value = Pair(-1L, -1L)
    }

    fun navigateUp() {
        _navigateUp.value = true
    }

    fun onUpNavigated() {
        _navigateUp.value = false
    }

}