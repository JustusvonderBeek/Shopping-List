package com.cloudsheeptech.shoppinglist.data.recipe

import android.util.Log
import androidx.lifecycle.LiveData
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.items.DbItem
import com.cloudsheeptech.shoppinglist.data.items.ItemRepository
import com.cloudsheeptech.shoppinglist.data.receiptItemAndDescriptionMapping.ReceiptDescriptionMapping
import com.cloudsheeptech.shoppinglist.data.receiptItemAndDescriptionMapping.ReceiptItemMapping
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import javax.inject.Inject

class RecipeLocalDataSource @Inject constructor(
    private val database: ShoppingListDatabase,
    private val userRepository: AppUserRepository,
    private val itemRepository: ItemRepository,
) {

    private val receiptDao = database.receiptDao()
    private val itemDao = database.itemDao()
    private val receiptItemDao = database.receiptItemDao()
    private val receiptDescriptionDao = database.receiptDescriptionDao()

    private fun DbRecipe.toApiReceipt() : ApiRecipe {
        val apiRecipe = ApiRecipe(
            onlineId = this.id,
            name = this.name,
            createdBy = this.createdBy,
            createdAt = this.createdAt,
            lastUpdated = this.lastUpdated,
            ingredients = emptyList(),
            description = emptyList(),
        )
        return apiRecipe
    }

    private fun ApiRecipe.toDbReceipt() : DbRecipe {
        val dbRecipe = DbRecipe(
            id = this.onlineId,
            name = this.name,
            createdBy = this.createdBy,
            createdAt = this.createdAt,
            lastUpdated = this.lastUpdated,
        )
        return dbRecipe
    }

    suspend fun create(name: String, icon: String?) : ApiRecipe {
        val user = userRepository.read() ?: throw IllegalStateException("user null after login")
        val dbRecipe = DbRecipe(
            id = 0L,
            name = name,
            createdBy = user.OnlineID,
            createdAt = OffsetDateTime.now(),
            lastUpdated = OffsetDateTime.now(),
        )
        withContext(Dispatchers.IO) {
            val receiptId = receiptDao.insert(dbRecipe)
            dbRecipe.id = receiptId
        }
        return dbRecipe.toApiReceipt()
    }

    suspend fun read(receiptId: Long, createdBy: Long) : ApiRecipe? {
        var storedReceipt : ApiRecipe? = null
        withContext(Dispatchers.IO) {
            val dbReceipt = receiptDao.get(receiptId, createdBy) ?: return@withContext
            storedReceipt = dbReceipt.toApiReceipt()
            val storedDescriptions = receiptDescriptionDao.read(receiptId, createdBy)
            storedReceipt!!.description = storedDescriptions.map { x -> ApiDescription(x.descriptionOrder, x.description) }
            val storedIngredients = receiptItemDao.readAllForReceipt(receiptId, createdBy)
            storedReceipt!!.ingredients = storedIngredients.map { ingredient ->
                val storedItem = itemDao.getItem(ingredient.itemId)
                ApiIngredient(ingredient.itemId, storedItem!!.name, storedItem.icon, ingredient.quantity, ingredient.quantityType)
            }
        }
        return storedReceipt
    }

    fun readLive(receiptId: Long, createdBy: Long) : Flow<ApiRecipe> {
        return combine(
            receiptDao.getFlow(receiptId, createdBy),
            receiptItemDao.readAllForReceiptJoined(receiptId, createdBy),
            receiptDescriptionDao.readFlow(receiptId, createdBy),
        ) { baseReceipt : DbRecipe?, receiptItems : Map<ReceiptItemMapping, DbItem>, receiptDescriptions : List<ReceiptDescriptionMapping>  ->
            // This can in fact happen, if we delete the receipt
            if (baseReceipt == null)
                return@combine ApiRecipe(0, "", 0L, OffsetDateTime.now(), OffsetDateTime.now(), listOf(), listOf())
            val convertedItems = receiptItems.map { mapping ->
                val receiptMapping = mapping.key
                val item = mapping.value
                ApiIngredient(receiptMapping.itemId, item!!.name, item.icon, receiptMapping.quantity, receiptMapping.quantityType)
            }
            val orderedDescriptions = receiptDescriptions.sortedBy { x -> x.descriptionOrder }
            val convertedDescription = orderedDescriptions.map { x ->
                ApiDescription(x.descriptionOrder, x.description)
            }
            ApiRecipe(
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

    fun readAllLive() : LiveData<List<DbRecipe>> {
        return receiptDao.getAllLive()
    }

    suspend fun insertDescription(receiptId: Long, createdBy: Long, order: Int, description: String) {
        withContext(Dispatchers.IO) {
            val mapping = ReceiptDescriptionMapping(
                id = 0L,
                receiptId = receiptId,
                createdBy = createdBy,
                description = description,
                descriptionOrder = order
            )
            receiptDescriptionDao.insert(mapping)
        }
    }

    suspend fun updateDescription(receiptId: Long, createdBy: Long, order: Int, description: String) {
        withContext(Dispatchers.IO) {
            val descriptionMapping = receiptDescriptionDao.read(receiptId, createdBy, order) ?: throw IllegalArgumentException("the requested description does not exist")
            descriptionMapping.description = description
            receiptDescriptionDao.update(descriptionMapping)
        }
    }

    suspend fun update(receipt: ApiRecipe) {
        Log.d("ReceiptLocalDataSource", "Updating: $receipt")
        withContext(Dispatchers.IO) {
            val dbReceipt = receipt.toDbReceipt()
            dbReceipt.lastUpdated = OffsetDateTime.now()
            receiptDao.update(dbReceipt)
            receiptItemDao.deleteAllForReceipt(receipt.onlineId, receipt.createdBy)
            receipt.ingredients.forEach { ingredient ->
                // Check if we might need to create the item first
                val itemExists = itemDao.getItem(ingredient.id)
                if (ingredient.id == 0L || itemExists == null) {
                    val item = DbItem(
                        id = 0L,
                        name = ingredient.name,
                        icon = "",
                    )
                    val itemId = itemRepository.create(item)
                    ingredient.id = itemId
                }
                val convertedIngredient = ReceiptItemMapping(
                    id = ingredient.id,
                    receiptId = receipt.onlineId,
                    createdBy = receipt.createdBy,
                    itemId = ingredient.id,
                    quantity = ingredient.quantity,
                    quantityType = ingredient.quantityType
                )
                receiptItemDao.insert(convertedIngredient)
            }
            val orderedDescriptions = receipt.description.sortedBy { x -> x.order }
            receiptDescriptionDao.deleteAllForReceipt(receipt.onlineId, receipt.createdBy)
            orderedDescriptions.forEachIndexed { index, description ->
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

    suspend fun deleteDescription(receiptId: Long, createdBy: Long, order: Int) {
        withContext(Dispatchers.IO) {
            receiptDescriptionDao.delete(receiptId, createdBy, order)
        }
    }

    suspend fun delete(receiptId: Long, createdBy: Long) {
        withContext(Dispatchers.IO) {
            receiptDao.delete(receiptId, createdBy)
            receiptDescriptionDao.deleteAllForReceipt(receiptId, createdBy)
            receiptItemDao.deleteAllForReceipt(receiptId, createdBy)
        }
    }

}