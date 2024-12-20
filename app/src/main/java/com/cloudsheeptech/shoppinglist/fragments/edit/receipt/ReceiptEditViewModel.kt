package com.cloudsheeptech.shoppinglist.fragments.edit.receipt

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.data.recipe.ApiDescription
import com.cloudsheeptech.shoppinglist.data.recipe.ApiIngredient
import com.cloudsheeptech.shoppinglist.data.recipe.RecipeRepository
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
class ReceiptEditViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val userRepository: AppUserRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val job = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + job)

    private val receiptId : Long = savedStateHandle["receiptId"]!!
    private val createdBy : Long = savedStateHandle["createdBy"]!!

    private val _navigateUp = MutableLiveData<Boolean>(false)
    val navigateUp : LiveData<Boolean> get() = _navigateUp

    private val _store = MutableLiveData<Boolean>(false)
    val store : LiveData<Boolean> get() = _store

    private val receipt = recipeRepository.readLive(receiptId, createdBy)

    val title = MutableLiveData<String>("")

    val receiptDescription = MutableLiveData<List<ApiDescription>>(emptyList())
//    val receiptDescription : LiveData<List<ReceiptDescription>> get() = _receiptDescriptionList

    private val _receiptIngredientList = MutableLiveData<List<ApiIngredient>>(emptyList())
    val receiptIngredients : LiveData<List<ApiIngredient>> get() = _receiptIngredientList

    init {
        // The user should be able to modify the existing receipt, therefore load ingredients
        // and descriptions at the start
        if (receiptId != -1L && createdBy != -1L) {
            vmScope.launch {
                val storedReceipt = recipeRepository.read(receiptId, createdBy) ?: return@launch
                Log.d("ReceiptEditViewModel", "Loaded: $storedReceipt")
                withContext(Dispatchers.Main) {
                    receiptDescription.value = storedReceipt.description
                    _receiptIngredientList.value = storedReceipt.ingredients
                    title.value = storedReceipt.name
                }
            }
        } else {
            Log.d("ReceiptEditViewModel", "Creating new receipt")
        }
    }

    fun setImages() {

    }

    fun addItem() {
        val newIngredient = ApiIngredient(
            id = 0L,
            name = "",
            icon = "",
            quantity = 0,
            quantityType = "",
        )
        _receiptIngredientList.value = _receiptIngredientList.value!! + newIngredient
    }

    fun addDescription() {
        val emptyDescription = ApiDescription(
            order = receiptDescription.value!!.size + 1,
            step = "",
        )
        receiptDescription.value = receiptDescription.value!! + emptyDescription
    //        descriptions += 1
//        _addDescriptionView.value = descriptions
//        vmScope.launch {
//            descriptions = receipt.value?.description?.size ?: 1
//            receiptRepository.insertDescription(receiptId, createdBy, descriptions, "")
//        }
    }

    fun deleteDescription(order: Int) {
        Log.d("ReceiptEditViewModel", "Filtering $order description")
        receiptDescription.value = receiptDescription.value?.filter { x -> x.order != order }
    }

    fun changeIngredientQuantity(ingredient: Long, quantity: Int) {
        Log.d("ReceiptEditViewModel", "Changing quantity of ingredient $ingredient by $quantity")
        val changedIngredients = _receiptIngredientList.value?.map { ing ->
            if (ing.id == ingredient) {
                ing.quantity = max(1, ing.quantity.plus(quantity))
            }
            ing
        } ?: return
        _receiptIngredientList.value = changedIngredients
    }

    fun deleteIngredient(itemId: Long) {
        Log.d("ReceiptEditViewModel", "Filtering $itemId ingredient")
        _receiptIngredientList.value = receiptIngredients.value?.filter { x -> x.id != itemId }
    }

    fun storeUpdate() {
        if (title.value == null || title.value!!.isEmpty()) {
            Log.e("ReceiptEditViewModel", "Title cannot be empty!")
            // TODO: Make toast
            return
        }
        // Differentiate between completely new receipt and existing one
        if (receiptId == -1L && createdBy == -1L) {
            vmScope.launch {
                val newReceipt = recipeRepository.create(title.value!!, "")
                newReceipt.ingredients = _receiptIngredientList.value?.filter { x -> x.name.isNotEmpty() } ?: emptyList()
                newReceipt.description = receiptDescription.value?.filter { x -> x.step.isNotEmpty() } ?: emptyList()
                recipeRepository.update(newReceipt)
                withContext(Dispatchers.Main) {
                    navigateUp()
                }
            }
        } else {
            vmScope.launch {
                val updatedReceipt = recipeRepository.read(receiptId, createdBy) ?: return@launch
                updatedReceipt.name = title.value ?: "Title"
                updatedReceipt.ingredients = _receiptIngredientList.value?.filter { x -> x.name.isNotEmpty() } ?: emptyList()
                updatedReceipt.description = receiptDescription.value?.filter { x -> x.step.isNotEmpty() } ?: emptyList()
                recipeRepository.update(updatedReceipt)
                withContext(Dispatchers.Main) {
                    navigateUp()
                }
            }
        }
    }

    fun navigateUp() {
        _navigateUp.value = true
    }

    fun onUpNavigated() {
        _navigateUp.value = false
    }

}