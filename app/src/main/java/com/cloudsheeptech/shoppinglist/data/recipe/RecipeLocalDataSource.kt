package com.cloudsheeptech.shoppinglist.data.recipe

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
import java.time.OffsetDateTime
import javax.inject.Inject

class RecipeLocalDataSource
@Inject
constructor(
    database: ShoppingListDatabase,
    private val userRepository: AppUserRepository,
    private val itemRepository: ItemRepository,
) {
    private val recipeDao = database.recipeDao()
    private val itemDao = database.itemDao()
    private val recipeItemDao = database.recipeItemDao()
    private val recipeDescriptionDao = database.recipeDescriptionDao()
    private val recipeImageDao = database.recipeImageDao()

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

    // -------------------------------------------------------------

    private suspend fun insertIngredients(
        recipeId: Long,
        createdBy: Long,
        ingredients: List<ApiIngredient>
    ) {
        withContext(Dispatchers.IO) {
            val mapping = ReceiptItemMapping(
                0L,
                recipeId,
                createdBy,
                0L,
                0,
                ""
            )
            ingredients.forEach { ingr ->
                itemRepository.readByName(ingr.name)
                mapping.itemId = ingr.id
                mapping.quantity = ingr.quantity
                mapping.quantityType = ingr.quantityType
                recipeItemDao.insert(mapping)
            }
        }
    }

    private suspend fun insertDescriptions(
        recipeId: Long,
        createdBy: Long,
        descriptions: List<ApiDescription>
    ) {
        withContext(Dispatchers.IO) {
            val descriptionMapping = ReceiptDescriptionMapping(
                recipeId,
                createdBy,
                "",
                0
            )
            descriptions.forEachIndexed { index, desc ->
                descriptionMapping.descriptionOrder = index
                descriptionMapping.description = desc.step
                recipeDescriptionDao.insert(descriptionMapping)
            }
        }
    }

    // -------------------------------------------------------------

    suspend fun create(
        name: String,
        ingredients: List<ApiIngredient>,
        descriptions: List<ApiDescription>,
        images: List<String>,
        defaultPortion: Int,
    ): ApiRecipe {
        val user = userRepository.read() ?: throw IllegalStateException("user null after login")
        val newRecipe =
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
            val recipeId = recipeDao.insert(newRecipe)
            newRecipe.id = recipeId
            val recipeImage =
                RecipeImage(
                    recipeId = recipeId,
                    createdBy = user.OnlineID,
                    imageId = 0,
                    fileLocation = "",
                )
            images.forEachIndexed { index, uri ->
                recipeImage.imageId = index
                recipeImage.fileLocation = uri
                recipeImageDao.insert(recipeImage)
            }
            insertDescriptions(recipeId, user.OnlineID, descriptions)

        }
        return newRecipe.toApiReceipt()
    }

    suspend fun read(
        recipeId: Long,
        createdBy: Long,
    ): Pair<ApiRecipe?, List<RecipeImage>> {
        var storedRecipe: ApiRecipe? = null
        var recipeImages: List<RecipeImage> = emptyList()
        withContext(Dispatchers.IO) {
            val dbReceipt = recipeDao.get(recipeId, createdBy) ?: return@withContext
            storedRecipe = dbReceipt.toApiReceipt()
            val storedDescriptions = recipeDescriptionDao.read(recipeId, createdBy)
            storedRecipe!!.description =
                storedDescriptions.map { x -> ApiDescription(x.descriptionOrder, x.description) }
            val storedIngredients = recipeItemDao.readAllForReceipt(recipeId, createdBy)
            storedRecipe!!.ingredients =
                storedIngredients.map { ingredient ->
                    val storedItem = itemDao.getItem(ingredient.itemId)
                    ApiIngredient(
                        ingredient.itemId,
                        storedItem!!.name,
                        storedItem.icon,
                        ingredient.quantity,
                        ingredient.quantityType,
                    )
                }
            recipeImages = recipeImageDao.read(recipeId, createdBy)
        }
        return Pair(storedRecipe, recipeImages)
    }

    fun readLive(
        receiptId: Long,
        createdBy: Long,
    ): Flow<ApiRecipe> {
        return combine(
            recipeDao.getFlow(receiptId, createdBy),
            recipeItemDao.readAllForReceiptJoined(receiptId, createdBy),
            recipeDescriptionDao.readFlow(receiptId, createdBy),
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
                        receiptMapping.quantityType,
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

    fun readAllLive(): LiveData<List<DbRecipe>> = recipeDao.getAllLive()

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
            recipeDescriptionDao.insert(mapping)
        }
    }

    suspend fun resetCreatedBy(createdBy: Long) {
        withContext(Dispatchers.IO) {
            recipeDao.resetCreatedBy(createdBy)
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
                recipeDescriptionDao.read(receiptId, createdBy, order)
                    ?: throw IllegalArgumentException("the requested description does not exist")
            descriptionMapping.description = description
            recipeDescriptionDao.update(descriptionMapping)
        }
    }

    /**
     * returns the version of the updated receipt
     * @throws IllegalArgumentException if the receipt does not exist
     */
    suspend fun update(receipt: ApiRecipe): Long {
        if (receipt.onlineId == 0L) {
            throw IllegalArgumentException("receipt does not exist in the database")
        }
        var updatedVersion = -1L
        Log.d("ReceiptLocalDataSource", "Updating: $receipt")
        withContext(Dispatchers.IO) {
            val dbReceipt = receipt.toDbReceipt()
            dbReceipt.lastUpdated = OffsetDateTime.now()
            val recipeExists = recipeDao.get(receipt.onlineId, receipt.createdBy.onlineId)
            if (recipeExists != null && receipt.version <= recipeExists.version) {
                Log.i(
                    "RecipeLocalDataSource",
                    "Updating is skipped because the last local recipe is newer than the incoming update",
                )
                return@withContext
            }
            recipeDao.update(dbReceipt)
            recipeItemDao.deleteAllForReceipt(receipt.onlineId, receipt.createdBy.onlineId)
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
                recipeItemDao.insert(convertedIngredient)
            }
            val orderedDescriptions = receipt.description.sortedBy { x -> x.order }
            recipeDescriptionDao.deleteAllForReceipt(receipt.onlineId, receipt.createdBy.onlineId)
            orderedDescriptions.forEachIndexed { index, description ->
                val convertedDesc =
                    ReceiptDescriptionMapping(
                        recipeId = receipt.onlineId,
                        createdBy = receipt.createdBy.onlineId,
                        description = description.step,
                        descriptionOrder = index,
                    )
                recipeDescriptionDao.insert(convertedDesc)
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
            recipeDescriptionDao.delete(receiptId, createdBy, order)
        }
    }

    suspend fun delete(
        receiptId: Long,
        createdBy: Long,
    ) {
        withContext(Dispatchers.IO) {
            recipeDao.delete(receiptId, createdBy)
            recipeDescriptionDao.deleteAllForReceipt(receiptId, createdBy)
            recipeItemDao.deleteAllForReceipt(receiptId, createdBy)
        }
    }
}
