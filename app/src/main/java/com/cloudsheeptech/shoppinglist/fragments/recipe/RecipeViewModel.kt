package com.cloudsheeptech.shoppinglist.fragments.recipe

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListRepository
import com.cloudsheeptech.shoppinglist.data.receipt.ApiDescription
import com.cloudsheeptech.shoppinglist.data.receipt.ReceiptRepository
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

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

    private val _navigateToEdit = MutableLiveData<Pair<Long, Long>>(Pair(-1L, -1L))
    val navigateToEdit : LiveData<Pair<Long, Long>> get() = _navigateToEdit

    private val _navigateUp = MutableLiveData<Boolean>(false)
    val navigateUp : LiveData<Boolean> get() = _navigateUp

    init {
//        vmScope.launch {
//            recipe = receiptRepository.read(receiptId, createdBy)
//        }
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

    fun editReceipt() {
        _navigateToEdit.value = Pair(receiptId, createdBy)
//        val updatedReceipt = receipt.value!!
//        updatedReceipt.description += listOf(ApiDescription(updatedReceipt.description.size + 1, "new step"))
//        vmScope.launch {
//            receiptRepository.update(updatedReceipt)
//        }
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