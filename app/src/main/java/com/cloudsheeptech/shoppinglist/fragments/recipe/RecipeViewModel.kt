package com.cloudsheeptech.shoppinglist.fragments.recipe

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
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