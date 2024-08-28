package com.cloudsheeptech.shoppinglist.fragments.receipts_overview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.data.receipt.ApiReceipt
import com.cloudsheeptech.shoppinglist.data.receipt.DbReceipt
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

    private val _navigateToReceipt = MutableLiveData<Pair<Long, Long>>(Pair(-1L, -1L))
    val navigateToReceipt : LiveData<Pair<Long, Long>> get() = _navigateToReceipt

    private val _receipts = receiptRepository.readAllLive()
    val receipts : LiveData<List<DbReceipt>> get() = _receipts

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