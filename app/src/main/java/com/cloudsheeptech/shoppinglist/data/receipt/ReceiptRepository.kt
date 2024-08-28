package com.cloudsheeptech.shoppinglist.data.receipt

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

    suspend fun readOnline(receiptId: Long, createdBy: Long) : ApiReceipt? {
        return remoteDataSource.read(receiptId, createdBy)
    }

    suspend fun update(receipt: ApiReceipt) {
        localDataSource.update(receipt)
        remoteDataSource.update(receipt)
    }

    suspend fun delete(receiptId: Long, createdBy: Long) {
        remoteDataSource.delete(receiptId, createdBy)
        localDataSource.delete(receiptId, createdBy)
    }

}