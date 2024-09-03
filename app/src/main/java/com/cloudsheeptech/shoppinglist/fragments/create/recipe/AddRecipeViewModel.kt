package com.cloudsheeptech.shoppinglist.fragments.create.recipe

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.SingleEvent
import com.cloudsheeptech.shoppinglist.data.items.DbItem
import com.cloudsheeptech.shoppinglist.data.receipt.ApiDescription
import com.cloudsheeptech.shoppinglist.data.receipt.ReceiptRepository
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import com.cloudsheeptech.shoppinglist.datastructures.ItemListWithName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AddRecipeViewModel @Inject constructor(
    private val receiptRepository: ReceiptRepository,
    private val userRepository: AppUserRepository,
) : ViewModel() {

    private val job = Job()
    private val addVmScope = CoroutineScope(Dispatchers.IO + job)

    val dbItemListWithName = MutableLiveData<ItemListWithName<DbItem>>()

    val receiptName = MutableLiveData<String>()
    val receiptDescription = MutableLiveData<String>()

    private val handledToast = MutableLiveData<SingleEvent<String>>()
    val toast : LiveData<SingleEvent<String>>
        get() = handledToast

    private val _navigateUp = MutableLiveData<Boolean>(false)
    val navigateUp : LiveData<Boolean> get() = _navigateUp

    fun create() {
        val currentTitle = receiptName.value ?: return
        val currentDescription = receiptDescription.value ?: return
        addVmScope.launch {
            val receipt = receiptRepository.create(currentTitle, "ic_receipt")
            receipt.description = listOf(ApiDescription(1, currentDescription))
            receiptRepository.update(receipt)
//            if (word.value != null && translation.value != null) {
//                vocabulary.postVocabulary(word.value!!, translation.value!!)
//                resetValues()
//            }
            withContext(Dispatchers.Main) {
                navigateUp()
            }
        }
    }

    private fun navigateUp() {
        _navigateUp.value = true
    }

    fun onUpNavigated() {
        _navigateUp.value = false
    }
}