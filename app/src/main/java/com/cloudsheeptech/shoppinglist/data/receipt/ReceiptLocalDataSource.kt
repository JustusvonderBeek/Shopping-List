package com.cloudsheeptech.shoppinglist.data.receipt

import android.util.Log
import androidx.lifecycle.LiveData
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.receiptItemAndDescriptionMapping.ReceiptDescriptionMapping
import com.cloudsheeptech.shoppinglist.data.receiptItemAndDescriptionMapping.ReceiptItemMapping
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import javax.inject.Inject

class ReceiptLocalDataSource @Inject constructor(
    private val database: ShoppingListDatabase,
    private val userRepository: AppUserRepository,
) {

    private val receiptDao = database.receiptDao()
    private val itemDao = database.itemDao()
    private val receiptItemDao = database.receiptItemDao()
    private val receiptDescriptionDao = database.receiptDescriptionDao()

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

    fun readLive(receiptId: Long, createdBy: Long) : Flow<ApiReceipt> {
        return combine(
            receiptDao.getFlow(receiptId, createdBy),
            receiptItemDao.readFlow(receiptId, createdBy),
            receiptDescriptionDao.readFlow(receiptId, createdBy),
        ) { baseReceipt : DbReceipt , receiptItems : List<ReceiptItemMapping>, receiptDescriptions : List<ReceiptDescriptionMapping>  ->
            val convertedItems = receiptItems.map { x ->
                val dbItem = itemDao.getItem(x.itemId)
                ApiIngredient(x.itemId, dbItem!!.name, dbItem.icon, x.quantity, x.quantityType)
            }
            val orderedDescriptions = receiptDescriptions.sortedBy { x -> x.descriptionOrder }
            val convertedDescription = orderedDescriptions.map { x ->
                ApiDescription(x.description)
            }
            ApiReceipt(
                onlineId = baseReceipt.id,
                name = baseReceipt.name,
                createdBy = baseReceipt.createdBy,
                createdAt = baseReceipt.createdAt,
                lastUpdated = baseReceipt.lastUpdated,
                ingredients = convertedItems,
                description = convertedDescription
            )
        }
    }

    fun readAllLive() : LiveData<List<DbReceipt>> {
        return receiptDao.getAllLive()
    }

    suspend fun update(receipt: ApiReceipt) {
        Log.d("ReceiptLocalDataSource", "Updating: $receipt")
        withContext(Dispatchers.IO) {
            val dbReceipt = receipt.toDbReceipt()
            receiptDao.update(dbReceipt)
            receipt.ingredients.forEach { ingredient ->
                val convertedIngredient = ReceiptItemMapping(
                    id = ingredient.id,
                    receiptId = receipt.onlineId,
                    createdBy = receipt.createdBy,
                    itemId = ingredient.id,
                    quantity = ingredient.quantity,
                    quantityType = ingredient.quantityType
                )
                receiptItemDao.update(convertedIngredient)
            }
            receipt.description.forEachIndexed { index, description ->
                val convertedDesc = ReceiptDescriptionMapping(
                    id = 0L,
                    receiptId = receipt.onlineId,
                    createdBy = receipt.createdBy,
                    description = description.step,
                    descriptionOrder = index
                )
                receiptDescriptionDao.insert(convertedDesc)
            }
        }
    }

    suspend fun delete(receiptId: Long, createdBy: Long) {
        withContext(Dispatchers.IO) {
            receiptDao.delete(receiptId, createdBy)
        }
    }

}