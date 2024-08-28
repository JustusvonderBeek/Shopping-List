package com.cloudsheeptech.shoppinglist.data.receipt

import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import com.google.protobuf.api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import javax.inject.Inject

class ReceiptLocalDataSource @Inject constructor(
    private val database: ShoppingListDatabase,
    private val userRepository: AppUserRepository,
) {

    private val receiptDao = database.receiptDao()

    private fun DbReceipt.toApiReceipt() : ApiReceipt {
        val apiReceipt = ApiReceipt(
            onlineId = this.id,
            name = this.name,
            createdBy = this.createdBy,
            createdAt = this.createdAt,
            lastUpdated = this.lastUpdated,
            ingredients = emptyList(),
            description = emptyList(),
        )
        return apiReceipt
    }

    private fun ApiReceipt.toDbReceipt() : DbReceipt {
        val dbReceipt = DbReceipt(
            id = this.onlineId,
            name = this.name,
            createdBy = this.createdBy,
            createdAt = this.createdAt,
            lastUpdated = this.lastUpdated,
        )
        return dbReceipt
    }

    suspend fun create(name: String, icon: String?) : ApiReceipt {
        val user = userRepository.read() ?: throw IllegalStateException("user null after login")
        val dbReceipt = DbReceipt(
            id = 0L,
            name = name,
            createdBy = user.OnlineID,
            createdAt = OffsetDateTime.now(),
            lastUpdated = OffsetDateTime.now(),
        )
        withContext(Dispatchers.IO) {
            val receiptId = receiptDao.insert(dbReceipt)
            dbReceipt.id = receiptId
        }
        return dbReceipt.toApiReceipt()
    }

    suspend fun read(receiptId: Long, createdBy: Long) : ApiReceipt? {
        var storedReceipt : ApiReceipt? = null
        withContext(Dispatchers.IO) {
            val dbReceipt = receiptDao.get(receiptId, createdBy)
            storedReceipt = dbReceipt?.toApiReceipt()
        }
        return storedReceipt
    }

    suspend fun update(receipt: ApiReceipt) {
        withContext(Dispatchers.IO) {
            val dbReceipt = receipt.toDbReceipt()
            receiptDao.update(dbReceipt)
        }
    }

    suspend fun delete(receiptId: Long, createdBy: Long) {
        withContext(Dispatchers.IO) {
            receiptDao.delete(receiptId, createdBy)
        }
    }

}