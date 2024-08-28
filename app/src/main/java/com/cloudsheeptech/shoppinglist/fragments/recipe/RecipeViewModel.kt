package com.cloudsheeptech.shoppinglist.fragments.recipe

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.data.receipt.ApiReceipt
import com.cloudsheeptech.shoppinglist.data.receipt.ReceiptRepository
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
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

    private val _navigateToEdit = MutableLiveData<Int>(-1)
    val navigateToEdit : LiveData<Int> get() = _navigateToEdit

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

    fun editWord() {
//        _navigateToEdit.value = recipe!!.onlineId.toInt()
    }

    fun navigatedToEditWord() {
        _navigateToEdit.value = -1
    }

    fun navigateUp() {
        _navigateUp.value = true
    }

    fun onUpNavigated() {
        _navigateUp.value = false
    }

}