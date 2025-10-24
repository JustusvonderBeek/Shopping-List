package com.cloudsheeptech.shoppinglist.data.recipe

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import com.cloudsheeptech.shoppinglist.list.model.UserNotAuthenticatedException
import io.ktor.client.network.sockets.SocketTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepository
    @Inject
    constructor(
        private val localDataSource: RecipeLocalDataSource,
        private val remoteDataSource: RecipeRemoteDataSource,
        private val binaryFileHandler: BinaryFileHandler,
        private val compressionHandler: CompressionHandler,
        private val userRepository: AppUserRepository,
    ) {
        private fun updateRecipeCreatedBy(recipe: ApiRecipe) {
            if (recipe.createdBy.onlineId != 0L) {
                return
            }
            val user =
                userRepository.read() ?: throw IllegalStateException("user null after login screen")
            recipe.createdBy.onlineId = user.OnlineID
            recipe.createdBy.username = user.Username
        }

        suspend fun create(
            name: String,
            defaultPortion: Int,
            ingredients: List<ApiIngredient>,
            descriptions: List<ApiDescription>,
            images: List<String>,
        ): ApiRecipe {
            val recipe = localDataSource.create(name, ingredients, descriptions, images, defaultPortion)
            val updatedImages =
                binaryFileHandler.persistTemporaryImagesInLocalStorage(
                    recipe.onlineId,
                    recipe.createdBy.onlineId,
                    images,
                    true,
                )
            localDataSource.updateImages(recipe.onlineId, recipe.createdBy.onlineId, updatedImages)
            val binaryImages = binaryFileHandler.readImagesFromFiles(updatedImages)
            val success = remoteDataSource.create(recipe, binaryImages)
            if (!success) {
                Log.e("RecipeRepository", "Creating recipe online failed")
            } else {
                Log.i("RecipeRepository", "The recipe ${recipe.name} was successfully created online")
            }
            return recipe
        }

        suspend fun read(
            recipeId: Long,
            createdBy: Long,
        ): Pair<ApiRecipe, List<ByteArray>>? {
            val recipeAndImages = localDataSource.read(recipeId, createdBy)
            val recipe = recipeAndImages.first ?: return null
            val imagesUris = recipeAndImages.second
            val binaryImages =
                binaryFileHandler.readImagesFromFiles(imagesUris.map { image -> image.fileLocation })
            return Pair(recipe, binaryImages)
        }

        fun readLive(
            receiptId: Long,
            createdBy: Long,
        ): LiveData<ApiRecipe> {
            val localReceipt = localDataSource.readLive(receiptId, createdBy)
            return localReceipt.asLiveData()
        }

        fun readAllLive(): LiveData<List<Pair<DbRecipe, RecipeImage?>>> = localDataSource.readAllLive()

        fun readAllLiveWithImages(): LiveData<List<Pair<DbRecipe, List<RecipeImage>>>> = localDataSource.readAllFlow()

        fun readAllImageLocationsLive(
            recipeId: Long,
            createdBy: Long,
        ): LiveData<List<RecipeImage>> = localDataSource.readAllImageLocationsLive(recipeId, createdBy)

        suspend fun readAllImageLocations(
            recipeId: Long,
            createdBy: Long,
        ): List<RecipeImage> = localDataSource.readAllImageLocations(recipeId, createdBy)

        suspend fun readOnline(
            receiptId: Long,
            createdBy: Long,
        ): ApiRecipe? {
            val onlineRecipe: ApiRecipe? =
                withContext(Dispatchers.IO) {
                    val remoteRecipe = remoteDataSource.readFull(receiptId, createdBy)
                    if (remoteRecipe.first == null) {
                        return@withContext null
                    }
                    updateRecipe(remoteRecipe.first, remoteRecipe.second)
                    return@withContext remoteRecipe.first
                }
            return onlineRecipe
        }

        suspend fun readAllOwnRecipesOnline() {
            val allRelevantRecipeIds = localDataSource.readAllRecipeIds()
            allRelevantRecipeIds.forEach { recipe ->
                val recipeId = recipe.recipeId
                val createdBy = recipe.createdBy
                val remoteRecipe = remoteDataSource.readFull(recipeId, createdBy)
                updateRecipe(remoteRecipe.first, remoteRecipe.second)
            }
        }

        private suspend fun updateRecipe(
            recipe: ApiRecipe?,
            images: List<ByteArray>,
        ) {
            withContext(Dispatchers.IO) {
                if (recipe == null) {
                    Log.e(
                        "RecipeRepository",
                        "Recipe could not be updated because it was null",
                    )
                    return@withContext
                }
                val recipeId = recipe.onlineId
                val createdBy = recipe.createdBy.onlineId
                Log.d(
                    "RecipeRepository",
                    "Successfully read recipe $recipeId from $createdBy from online endpoint",
                )
                val updatedVersion = localDataSource.update(recipe)
                if (updatedVersion != -1L) {
                    Log.i(
                        "RecipeRepository",
                        "Successfully updated recipe $recipeId from $createdBy in offline storage",
                    )
                } else {
                    Log.w("RecipeRepository", "Update of recipe $recipeId from $createdBy aborted")
                    return@withContext
                }
                val updatedImageLocation = mutableListOf<String>()
                images.forEachIndexed { index, image ->
                    val imageStoreFile =
                        binaryFileHandler.storeImageToFile(
                            image,
                            "${recipeId}_${createdBy}_$index.img",
                        )
                    updatedImageLocation.add(imageStoreFile.toString())
                }
                localDataSource.updateImages(recipeId, createdBy, updatedImageLocation)
            }
        }

        suspend fun readAllOwnAndSharedRecipesOnline() {
            val currentUser = userRepository.read()
            if (currentUser == null) {
                Log.e("RecipeRepository", "User null after login screen")
                return
            }
            val recipesAndImages = remoteDataSource.readAllRecipesFull()
            recipesAndImages.forEach { recipeAndImages ->
                val recipe = recipeAndImages.first
                val images = recipeAndImages.second
                if (recipe.createdBy.onlineId != currentUser.OnlineID) {
                    // Recipe from remote, created by someone else
                    val imageFilePaths = mutableListOf<String>()
                    images.forEachIndexed { index, image ->
                        val fileLocation =
                            binaryFileHandler.storeImageToFile(
                                image,
                                "${recipe.onlineId}_${recipe.createdBy.onlineId}_$index.img",
                            )
                        imageFilePaths.add(fileLocation.toString())
                    }
                    if (localDataSource.exists(recipe.onlineId, recipe.createdBy.onlineId)) {
                        localDataSource.update(recipe)
                        localDataSource.updateImages(
                            recipe.onlineId,
                            recipe.createdBy.onlineId,
                            imageFilePaths,
                        )
                    } else {
                        localDataSource.create(
                            recipe.onlineId,
                            recipe.name,
                            recipe.createdBy,
                            recipe.ingredients,
                            recipe.description,
                            imageFilePaths,
                            recipe.defaultPortion,
                        )
                    }
                } else {
                    // Own recipe
                    val updatedVersion = localDataSource.update(recipe)
                    if (updatedVersion != -1L) {
                        Log.d(
                            "RecipeRepository",
                            "Successfully updated recipe ${recipe.onlineId} from ${recipe.createdBy.username} in offline storage",
                        )
                    }
                    val updatedImageLocation = mutableListOf<String>()
                    images.forEachIndexed { index, image ->
                        val imageStoreFile =
                            binaryFileHandler.storeImageToFile(
                                image,
                                "${recipe.onlineId}_${recipe.createdBy.onlineId}_$index.img",
                            )
                        updatedImageLocation.add(imageStoreFile.toString())
                    }
                    localDataSource.updateImages(
                        recipe.onlineId,
                        recipe.createdBy.onlineId,
                        updatedImageLocation,
                    )
                }
            }
        }

        suspend fun update(
            recipe: ApiRecipe,
            recipeImages: List<String>,
        ) {
            try {
                recipe.version++
                val updatedVersion = localDataSource.update(recipe)
                if (updatedVersion < 0L) {
                    return
                }
                val imageLocations =
                    binaryFileHandler.persistTemporaryImagesInLocalStorage(
                        recipe.onlineId,
                        recipe.createdBy.onlineId,
                        recipeImages,
                    )
                localDataSource.updateImages(recipe.onlineId, recipe.createdBy.onlineId, imageLocations)
                var success = false
                val binaryImages = binaryFileHandler.readImagesFromFiles(imageLocations)
                try {
                    success = remoteDataSource.update(recipe, binaryImages)
                } catch (ex: UserNotAuthenticatedException) {
                    Log.w("RecipeRepository", "User might not be authenticated: $ex")
                }
                if (!success) {
                    updateRecipeCreatedBy(recipe)
                    success = remoteDataSource.update(recipe, binaryImages)
                }
                if (!success) {
                    success = remoteDataSource.create(recipe, binaryImages)
                }
                if (success) {
                    Log.i("RecipeRepository", "The recipe ${recipe.onlineId} was updated online")
                } else {
                    Log.i("RecipeRepository", "Updating the recipe ${recipe.onlineId} online failed")
                }
            } catch (ex: UserNotAuthenticatedException) {
                Log.e(
                    "RecipeRepository",
                    "Something went wrong during communication with the server: $ex",
                )
            } catch (ex: IllegalArgumentException) {
                Log.e("RecipeRepository", "Something went wrong with the given recipe to update: $ex")
            } catch (ex: SocketTimeoutException) {
                Log.e("RecipeRepository", "Timeout while communicating with the server: $ex")
            }
        }

        suspend fun resetCreatedBy() {
            val user =
                userRepository.read() ?: throw IllegalStateException("user null after login screen")
            localDataSource.resetCreatedBy(user.OnlineID)
        }

        suspend fun delete(
            receiptId: Long,
            createdBy: Long,
        ) {
            try {
                val success = remoteDataSource.delete(receiptId, createdBy)
                if (!success) {
                    Log.e("RecipeRepository", "Deleting recipe online failed")
                }
                localDataSource.delete(receiptId, createdBy)
            } catch (ex: Exception) {
                Log.e("RecipeRepository", "Unknown error while deleting recipe: $ex")
            }
        }
    }
