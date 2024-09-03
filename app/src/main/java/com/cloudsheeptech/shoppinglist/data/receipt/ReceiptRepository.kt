package com.cloudsheeptech.shoppinglist.data.receipt

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptRepository @Inject constructor(
    private val localDataSource: ReceiptLocalDataSource,
    private val remoteDataSource: ReceiptRemoteDataSource,
    private val userRepository: AppUserRepository,
)  {

   suspend fun create(name: String, icon: String?) : ApiReceipt {
       val receipt = localDataSource.create(name, icon)
       remoteDataSource.create(receipt)
       return receipt
   }

   suspend fun read(receiptId: Long, createdBy: Long) : ApiReceipt? {
       val receipt = localDataSource.read(receiptId, createdBy) ?: return null
       return receipt
   }

    fun readLive(receiptId: Long, createdBy: Long) : LiveData<ApiReceipt> {
        val localReceipt = localDataSource.readLive(receiptId, createdBy)
        return localReceipt.asLiveData()
    }

    // TODO: Fix the different list type
    fun readAllLive() : LiveData<List<DbReceipt>> {
        return localDataSource.readAllLive()
    }

    suspend fun readOnline(receiptId: Long, createdBy: Long) : ApiReceipt? {
        return remoteDataSource.read(receiptId, createdBy)
    }

    suspend fun update(receipt: ApiReceipt) {
        localDataSource.update(receipt)
        remoteDataSource.update(receipt)
    }

    suspend fun insertDescription(receiptId: Long, createdBy: Long, order: Int, description: String) {
        localDataSource.insertDescription(receiptId, createdBy, order, description)
        val localReceipt = localDataSource.read(receiptId, createdBy) ?: return
        remoteDataSource.update(localReceipt)
    }

    suspend fun updateDescription(receiptId: Long, createdBy: Long, order: Int, description: String) {
        localDataSource.updateDescription(receiptId, createdBy, order, description)
        val localReceipt = localDataSource.read(receiptId, createdBy) ?: return
        remoteDataSource.update(localReceipt)
    }

    suspend fun deleteDescription(receiptId: Long, createdBy: Long, order: Int) {
        localDataSource.deleteDescription(receiptId, createdBy, order)
        val localReceipt = localDataSource.read(receiptId, createdBy) ?: return
        remoteDataSource.update(localReceipt)
    }

    suspend fun delete(receiptId: Long, createdBy: Long) {
        remoteDataSource.delete(receiptId, createdBy)
        localDataSource.delete(receiptId, createdBy)
    }

}