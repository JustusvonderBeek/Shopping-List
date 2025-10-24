package com.cloudsheeptech.shoppinglist.data.recipe

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import com.cloudsheeptech.shoppinglist.data.receiptItemAndDescriptionMapping.ReceiptDescriptionMapping
import com.cloudsheeptech.shoppinglist.data.receiptItemAndDescriptionMapping.ReceiptItemMapping
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import com.cloudsheeptech.shoppinglist.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.list.model.DbItem
import com.cloudsheeptech.shoppinglist.list.model.ListCreator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import javax.inject.Inject

class RecipeLocalDataSource
    @Inject
    constructor(
        database: ShoppingListDatabase,
        private val userRepository: AppUserRepository,
        private val itemRepository: ItemRepository,
        private val binaryFileHandler: BinaryFileHandler,
    ) {
        private val recipeDao = database.recipeDao()
        private val itemDao = database.itemDao()
        private val recipeItemDao = database.recipeItemDao()
        private val recipeDescriptionDao = database.recipeDescriptionDao()
        private val recipeImageDao = database.recipeImageDao()

        private fun DbRecipe.toApiReceipt(
            ingredients: List<ApiIngredient>,
            description: List<ApiDescription>,
        ): ApiRecipe {
            val apiRecipe =
                ApiRecipe(
                    onlineId = this.id,
                    name = this.name,
                    createdBy = ListCreator(this.createdBy, this.createdByName),
                    createdAt = this.createdAt,
                    lastUpdated = this.lastUpdated,
                    version = this.version,
                    defaultPortion = this.defaultPortion,
                    ingredients = ingredients,
                    description = description,
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
            ingredients: List<ApiIngredient>,
        ): Boolean {
            var success = true
            withContext(Dispatchers.IO) {
                val mapping =
                    ReceiptItemMapping(
                        0L,
                        recipeId,
                        createdBy,
                        0L,
                        0,
                        "",
                    )
                ingredients.forEach { ingr ->
                    var item = itemRepository.readOrCreate(ingr.name)
                    mapping.itemId = item.id
                    mapping.quantity = ingr.quantity
                    mapping.quantityType = ingr.quantityType
                    recipeItemDao.insert(mapping)
                }
            }
            return success
        }

        private suspend fun insertDescriptions(
            recipeId: Long,
            createdBy: Long,
            descriptions: List<ApiDescription>,
        ) {
            withContext(Dispatchers.IO) {
                val descriptionMapping =
                    ReceiptDescriptionMapping(
                        recipeId,
                        createdBy,
                        "",
                        0,
                    )
                descriptions.forEachIndexed { index, desc ->
                    descriptionMapping.descriptionOrder = index
                    descriptionMapping.description = desc.step
                    recipeDescriptionDao.insert(descriptionMapping)
                }
            }
        }

        // -------------------------------------------------------------

        suspend fun exists(
            onlineId: Long,
            createdBy: Long,
        ): Boolean {
            return withContext(Dispatchers.IO) {
                val dbRecipe = recipeDao.get(onlineId, createdBy)
                return@withContext dbRecipe != null
            }
        }

        suspend fun create(
            name: String,
            ingredients: List<ApiIngredient>,
            descriptions: List<ApiDescription>,
            images: List<String>,
            defaultPortion: Int,
        ): ApiRecipe {
            val user = userRepository.read() ?: throw IllegalStateException("user null after login")
            val recipeCreator = ListCreator(user.OnlineID, user.Username)
            return create(
                0L,
                name,
                recipeCreator,
                ingredients,
                descriptions,
                images,
                defaultPortion,
            )
        }

        // If the onlineId is set to 0L, no new ID is allocated
        suspend fun create(
            onlineId: Long = 0L,
            name: String,
            recipeCreator: ListCreator,
            ingredients: List<ApiIngredient>,
            descriptions: List<ApiDescription>,
            images: List<String>,
            defaultPortion: Int,
        ): ApiRecipe {
            val newRecipe =
                ApiRecipe(
                    onlineId = onlineId,
                    name = name,
                    createdBy = recipeCreator,
                    createdAt = OffsetDateTime.now(),
                    lastUpdated = OffsetDateTime.now(),
                    version = 1,
                    defaultPortion = defaultPortion,
                    ingredients = ingredients,
                    description = descriptions,
                )
            withContext(Dispatchers.IO) {
                val user =
                    userRepository.read()
                        ?: throw IllegalStateException("user not set after login screen")
                // TODO: Handle existing recipe in DB
                // In case the recipe comes from online, we don't want to change the id
                if (onlineId == 0L && recipeCreator.onlineId == user.OnlineID) {
                    newRecipe.onlineId = getUniqueRecipeID(recipeCreator.onlineId)
                }
                recipeDao.insert(newRecipe.toDbReceipt())
                updateImages(newRecipe.onlineId, recipeCreator.onlineId, images)
                insertDescriptions(newRecipe.onlineId, recipeCreator.onlineId, descriptions)
                insertIngredients(newRecipe.onlineId, recipeCreator.onlineId, ingredients)
            }
            return newRecipe
        }

        private suspend fun getUniqueRecipeID(createdBy: Long): Long {
            // Starting the local listIds with 1
            var latestId = 1L
            withContext(Dispatchers.IO) {
                latestId = recipeDao.getLatestListId(createdBy).plus(1L)
            }
            Log.d("RecipeLocalDataSource", "Generated new recipe Id: $latestId")
            return latestId
        }

        suspend fun read(
            recipeId: Long,
            createdBy: Long,
        ): Pair<ApiRecipe?, List<RecipeImage>> {
            var storedRecipe: ApiRecipe? = null
            var recipeImages: List<RecipeImage> = emptyList()
            withContext(Dispatchers.IO) {
                val dbReceipt = recipeDao.get(recipeId, createdBy) ?: return@withContext
                storedRecipe = dbReceipt.toApiReceipt(emptyList(), emptyList())
                val storedDescriptions = recipeDescriptionDao.read(recipeId, createdBy)
                storedRecipe.description =
                    storedDescriptions.map { x -> ApiDescription(x.descriptionOrder, x.description) }
                val storedIngredients = recipeItemDao.readAllForReceipt(recipeId, createdBy)
                storedRecipe.ingredients =
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

        fun readAllLive(): LiveData<List<Pair<DbRecipe, RecipeImage?>>> =
            recipeDao.getAllLive().switchMap { recipes ->
                val recipesLive = mutableListOf<Pair<DbRecipe, RecipeImage?>>()
                recipes.map { recipe ->
                    val imagesForRecipe = recipeImageDao.read(recipe.id, recipe.createdBy)
                    recipesLive.add(Pair(recipe, imagesForRecipe[0]))
                }
                liveData {
                    emit(recipesLive)
                }
            }

        fun readAllFlow(): LiveData<List<Pair<DbRecipe, List<RecipeImage>>>> =
            recipeDao
                .getAllFlow()
                .flatMapLatest { recipes ->
                    combine(
                        recipes.map { recipe ->
                            recipeImageDao.readFlow(recipe.id, recipe.createdBy).map { images ->
                                recipe to images
                            }
                        },
                    ) { combinedList -> combinedList.toList() }
                        .onStart { emit(emptyList()) }
                }.asLiveData(Dispatchers.IO)

        fun readAllImageLocationsLive(
            recipeId: Long,
            createdBy: Long,
        ): LiveData<List<RecipeImage>> = recipeImageDao.readLive(recipeId, createdBy)

        suspend fun readAllImageLocations(
            recipeId: Long,
            createdBy: Long,
        ): List<RecipeImage> {
            return withContext(Dispatchers.IO) {
                return@withContext recipeImageDao.read(recipeId, createdBy)
            }
        }

        suspend fun readAllRecipeIds(): List<RecipeIdAndCreatedBy> {
            return withContext(Dispatchers.IO) {
                return@withContext recipeDao.getAllRecipeIds()
            }
        }

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

        suspend fun updateImages(
            recipeId: Long,
            createdBy: Long,
            images: List<String>,
        ) {
            withContext(Dispatchers.IO) {
                // Deleting ALL images for the recipe
                recipeImageDao.delete(recipeId, createdBy)
                val recipeImage =
                    RecipeImage(
                        recipeId = recipeId,
                        createdBy = createdBy,
                        imageId = 0,
                        fileLocation = "",
                    )
                for ((index, image) in images.withIndex()) {
                    recipeImage.imageId = index
                    recipeImage.fileLocation = image
                    recipeImageDao.insert(recipeImage)
                }
            }
        }

        /**
         * returns the version of the updated receipt
         * @throws IllegalArgumentException if the receipt does not exist
         */
        suspend fun update(recipe: ApiRecipe): Long {
            if (recipe.onlineId == 0L) {
                throw IllegalArgumentException("receipt does not exist in the database")
            }
            var updatedVersion = -1L
            Log.d("ReceiptLocalDataSource", "Updating: $recipe")
            withContext(Dispatchers.IO) {
                val dbRecipe = recipe.toDbReceipt()
                dbRecipe.lastUpdated = OffsetDateTime.now()
                val recipeExists = recipeDao.get(recipe.onlineId, recipe.createdBy.onlineId)
                if (recipeExists != null && recipe.version <= recipeExists.version) {
                    Log.i(
                        "RecipeLocalDataSource",
                        "Updating is skipped because the last local recipe is newer than the incoming update",
                    )
                    return@withContext
                }
                recipeDao.update(dbRecipe)
                recipeItemDao.deleteAllForReceipt(recipe.onlineId, recipe.createdBy.onlineId)
                insertIngredients(recipe.onlineId, recipe.createdBy.onlineId, recipe.ingredients)
                recipeDescriptionDao.deleteAllForReceipt(recipe.onlineId, recipe.createdBy.onlineId)
                insertDescriptions(recipe.onlineId, recipe.createdBy.onlineId, recipe.description)
                updatedVersion = dbRecipe.version.plus(1L)
            }
            return updatedVersion
        }

        suspend fun deleteDescription(
            recipeId: Long,
            createdBy: Long,
            order: Int,
        ) {
            withContext(Dispatchers.IO) {
                recipeDescriptionDao.delete(recipeId, createdBy, order)
            }
        }

        suspend fun delete(
            recipeId: Long,
            createdBy: Long,
        ) {
            withContext(Dispatchers.IO) {
                recipeDao.delete(recipeId, createdBy)
                recipeDescriptionDao.deleteAllForReceipt(recipeId, createdBy)
                recipeItemDao.deleteAllForReceipt(recipeId, createdBy)
                val imagesForRecipe =
                    recipeImageDao.read(recipeId, createdBy).map {
                        it.fileLocation
                    }
                binaryFileHandler.deleteImagesForRecipe(imagesForRecipe)
                recipeImageDao.delete(recipeId, createdBy)
            }
        }
    }
