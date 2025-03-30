package com.cloudsheeptech.shoppinglist.data.recipe

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.items.DbItem
import com.cloudsheeptech.shoppinglist.data.items.ItemRepository
import com.cloudsheeptech.shoppinglist.data.onlineUser.ListCreator
import com.cloudsheeptech.shoppinglist.data.receiptItemAndDescriptionMapping.ReceiptDescriptionMapping
import com.cloudsheeptech.shoppinglist.data.receiptItemAndDescriptionMapping.ReceiptItemMapping
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import kotlinx.serialization.InternalSerializationApi
import java.time.OffsetDateTime
import javax.inject.Inject

class RecipeLocalDataSource
@Inject
constructor(
    private val database: ShoppingListDatabase,
    private val userRepository: AppUserRepository,
    private val itemRepository: ItemRepository,
) {
    private val receiptDao = database.receiptDao()
    private val itemDao = database.itemDao()
    private val receiptItemDao = database.receiptItemDao()
    private val receiptDescriptionDao = database.receiptDescriptionDao()
    private val recipeImageDao = database.recipeImageDao()

    @OptIn(InternalSerializationApi::class)
    private fun DbRecipe.toApiReceipt(): ApiRecipe {
        val apiRecipe =
            ApiRecipe(
                onlineId = this.id,
                name = this.name,
                createdBy = ListCreator(this.createdBy, this.createdByName),
                createdAt = this.createdAt,
                lastUpdated = this.lastUpdated,
                version = this.version,
                defaultPortion = this.defaultPortion,
                ingredients = emptyList(),
                description = emptyList(),
            )
        return apiRecipe
    }

    @OptIn(InternalSerializationApi::class)
    private fun ApiRecipe.toDbReceipt(): DbRecipe {
        val dbRecipe =
            DbRecipe(
                id = this.onlineId,
                name = this.name,
                createdBy = this.createdBy.onlineId,
                createdByName = this.createdBy.username,
                createdAt = this.createdAt,
                lastUpdated = this.lastUpdated,
                version = this.version,
                defaultPortion = this.defaultPortion,
            )
        return dbRecipe
    }

    @OptIn(InternalSerializationApi::class)
    suspend fun create(
        name: String,
        images: List<Uri>,
        defaultPortion: Int,
    ): ApiRecipe {
        val user = userRepository.read() ?: throw IllegalStateException("user null after login")
        val dbRecipe =
            DbRecipe(
                id = 0L,
                name = name,
                createdBy = user.OnlineID,
                createdByName = user.Username,
                createdAt = OffsetDateTime.now(),
                lastUpdated = OffsetDateTime.now(),
                version = 1,
                defaultPortion = defaultPortion,
            )
        withContext(Dispatchers.IO) {
            val receiptId = receiptDao.insert(dbRecipe)
            dbRecipe.id = receiptId
            val recipeImage = RecipeImage(
                recipeId = receiptId,
                createdBy = user.OnlineID,
                imageId = 0,
                fileLocation = "",
            )
            images.forEachIndexed { index, uri ->
                recipeImage.imageId = index
                recipeImage.fileLocation = uri.toString()
                recipeImageDao.insert(recipeImage)
            }
        }
        return dbRecipe.toApiReceipt()
    }

    @OptIn(InternalSerializationApi::class)
    suspend fun read(
        recipeId: Long,
        createdBy: Long,
    ): Pair<ApiRecipe?, List<RecipeImage>> {
        var storedRecipe: ApiRecipe? = null
        var recipeImages: List<RecipeImage> = emptyList()
        withContext(Dispatchers.IO) {
            val dbReceipt = receiptDao.get(recipeId, createdBy) ?: return@withContext
            storedRecipe = dbReceipt.toApiReceipt()
            val storedDescriptions = receiptDescriptionDao.read(recipeId, createdBy)
            storedRecipe!!.description =
                storedDescriptions.map { x -> ApiDescription(x.descriptionOrder, x.description) }
            val storedIngredients = receiptItemDao.readAllForReceipt(recipeId, createdBy)
            storedRecipe!!.ingredients =
                storedIngredients.map { ingredient ->
                    val storedItem = itemDao.getItem(ingredient.itemId)
                    ApiIngredient(
                        ingredient.itemId,
                        storedItem!!.name,
                        storedItem.icon,
                        ingredient.quantity,
                        ingredient.quantityType
                    )
                }
            recipeImages = recipeImageDao.read(recipeId, createdBy)
        }
        return Pair(storedRecipe, recipeImages)
    }

    @OptIn(InternalSerializationApi::class)
    fun readLive(
        receiptId: Long,
        createdBy: Long,
    ): Flow<ApiRecipe> {
        return combine(
            receiptDao.getFlow(receiptId, createdBy),
            receiptItemDao.readAllForReceiptJoined(receiptId, createdBy),
            receiptDescriptionDao.readFlow(receiptId, createdBy),
        ) {
                baseReceipt: DbRecipe?,
                receiptItems: Map<ReceiptItemMapping, DbItem>,
                receiptDescriptions: List<ReceiptDescriptionMapping>,
            ->
            // This can in fact happen, if we delete the receipt
            if (baseReceipt == null) {
                return@combine ApiRecipe(
                    0,
                    "",
                    ListCreator(0L, ""),
                    OffsetDateTime.now(),
                    OffsetDateTime.now(),
                    1,
                    2,
                    listOf(),
                    listOf(),
                )
            }
            val convertedItems =
                receiptItems.map { mapping ->
                    val receiptMapping = mapping.key
                    val item = mapping.value
                    ApiIngredient(
                        receiptMapping.itemId,
                        item.name,
                        item.icon,
                        receiptMapping.quantity,
                        receiptMapping.quantityType
                    )
                }
            val orderedDescriptions = receiptDescriptions.sortedBy { x -> x.descriptionOrder }
            val convertedDescription =
                orderedDescriptions.map { x ->
                    ApiDescription(x.descriptionOrder, x.description)
                }
            ApiRecipe(
                onlineId = baseReceipt.id,
                name = baseReceipt.name,
                createdBy = ListCreator(baseReceipt.createdBy, baseReceipt.createdByName),
                createdAt = baseReceipt.createdAt,
                lastUpdated = baseReceipt.lastUpdated,
                version = baseReceipt.version,
                defaultPortion = baseReceipt.defaultPortion,
                ingredients = convertedItems,
                description = convertedDescription,
            )
        }
    }

    fun readAllLive(): LiveData<List<DbRecipe>> = receiptDao.getAllLive()

    suspend fun insertDescription(
        receiptId: Long,
        createdBy: Long,
        order: Int,
        description: String,
    ) {
        withContext(Dispatchers.IO) {
            val mapping =
                ReceiptDescriptionMapping(
                    recipeId = receiptId,
                    createdBy = createdBy,
                    description = description,
                    descriptionOrder = order,
                )
            receiptDescriptionDao.insert(mapping)
        }
    }

    suspend fun resetCreatedBy(createdBy: Long) {
        withContext(Dispatchers.IO) {
            receiptDao.resetCreatedBy(createdBy)
        }
    }

    suspend fun updateDescription(
        receiptId: Long,
        createdBy: Long,
        order: Int,
        description: String,
    ) {
        withContext(Dispatchers.IO) {
            val descriptionMapping =
                receiptDescriptionDao.read(receiptId, createdBy, order)
                    ?: throw IllegalArgumentException("the requested description does not exist")
            descriptionMapping.description = description
            receiptDescriptionDao.update(descriptionMapping)
        }
    }

    /**
     * returns the version of the updated receipt
     * @throws IllegalArgumentException if the receipt does not exist
     */
    @OptIn(InternalSerializationApi::class)
    suspend fun update(receipt: ApiRecipe): Long {
        if (receipt.onlineId == 0L) {
            throw IllegalArgumentException("receipt does not exist in the database")
        }
        var updatedVersion = -1L
        Log.d("ReceiptLocalDataSource", "Updating: $receipt")
        withContext(Dispatchers.IO) {
            val dbReceipt = receipt.toDbReceipt()
            dbReceipt.lastUpdated = OffsetDateTime.now()
            val recipeExists = receiptDao.get(receipt.onlineId, receipt.createdBy.onlineId)
            if (recipeExists != null && receipt.version <= recipeExists.version) {
                Log.i(
                    "RecipeLocalDataSource",
                    "Updating is skipped because the last local recipe is newer than the incoming update"
                )
                return@withContext
            }
            receiptDao.update(dbReceipt)
            receiptItemDao.deleteAllForReceipt(receipt.onlineId, receipt.createdBy.onlineId)
            receipt.ingredients.forEach { ingredient ->
                // Check if we might need to create the item first
                var itemExists = itemDao.getItem(ingredient.id)
                if (ingredient.id == 0L || itemExists == null) {
                    itemExists = itemDao.getItemFromName(ingredient.name)
                }
                if (itemExists == null) {
                    val item =
                        DbItem(
                            id = 0L,
                            name = ingredient.name,
                            icon = "",
                        )
                    val itemId = itemRepository.create(item)
                    ingredient.id = itemId
                } else {
                    ingredient.id = itemExists.id
                }
                val convertedIngredient =
                    ReceiptItemMapping(
                        id = ingredient.id,
                        recipeId = receipt.onlineId,
                        createdBy = receipt.createdBy.onlineId,
                        itemId = ingredient.id,
                        quantity = ingredient.quantity,
                        quantityType = ingredient.quantityType,
                    )
                receiptItemDao.insert(convertedIngredient)
            }
            val orderedDescriptions = receipt.description.sortedBy { x -> x.order }
            receiptDescriptionDao.deleteAllForReceipt(receipt.onlineId, receipt.createdBy.onlineId)
            orderedDescriptions.forEachIndexed { index, description ->
                val convertedDesc =
                    ReceiptDescriptionMapping(
                        recipeId = receipt.onlineId,
                        createdBy = receipt.createdBy.onlineId,
                        description = description.step,
                        descriptionOrder = index,
                    )
                receiptDescriptionDao.insert(convertedDesc)
            }
            updatedVersion = dbReceipt.version.plus(1L)
        }
        return updatedVersion
    }

    suspend fun deleteDescription(
        receiptId: Long,
        createdBy: Long,
        order: Int,
    ) {
        withContext(Dispatchers.IO) {
            receiptDescriptionDao.delete(receiptId, createdBy, order)
        }
    }

    suspend fun delete(
        receiptId: Long,
        createdBy: Long,
    ) {
        withContext(Dispatchers.IO) {
            receiptDao.delete(receiptId, createdBy)
            receiptDescriptionDao.deleteAllForReceipt(receiptId, createdBy)
            receiptItemDao.deleteAllForReceipt(receiptId, createdBy)
        }
    }
}
