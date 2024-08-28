package com.cloudsheeptech.shoppinglist.fragments.receipts_overview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.data.receipt.ReceiptRepository
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import javax.inject.Inject

@HiltViewModel
class ReceiptsOverviewViewModel @Inject constructor(
    private val receiptRepository: ReceiptRepository,
    private val userRepository: AppUserRepository,
) : ViewModel() {

    private val job = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + job)

    private val _navigateToCreateReceipt = MutableLiveData<Boolean>(false)
    val navigateToCreateReceipt : LiveData<Boolean> get() = _navigateToCreateReceipt

    fun createReceipt() {
        _navigateToCreateReceipt.value = true
    }

    fun onCreateReceiptNavigate() {
        _navigateToCreateReceipt.value = false
    }

}