package com.cloudsheeptech.shoppinglist.data.recipe

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import com.cloudsheeptech.shoppinglist.exception.UserNotAuthenticatedException
import io.ktor.client.network.sockets.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepository
    @Inject
    constructor(
        private val localDataSource: RecipeLocalDataSource,
        private val remoteDataSource: RecipeRemoteDataSource,
        private val binaryFileHandler: BinaryFileHandler,
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
        ): ApiRecipe? = remoteDataSource.read(receiptId, createdBy)

        suspend fun readAllOwnRecipesOnline() {
            val allRelevantRecipeIds = localDataSource.readAllRecipeIds()
            allRelevantRecipeIds.forEach { recipe ->
                val recipeId = recipe.recipeId
                val createdBy = recipe.createdBy
                val remoteRecipe = remoteDataSource.readFull(recipeId, createdBy)
                if (remoteRecipe.first == null) {
                    Log.e(
                        "RecipeRepository",
                        "Recipe $recipeId from $createdBy could not be read from remote",
                    )
                    return@forEach
                }
                Log.d(
                    "RecipeRepository",
                    "Successfully read recipe $recipeId from $createdBy from online endpoint",
                )
                val updatedVersion = localDataSource.update(remoteRecipe.first!!)
                if (updatedVersion != -1L) {
                    Log.d(
                        "RecipeRepository",
                        "Successfully updated recipe $recipeId from $createdBy in offline storage",
                    )
                }
                val updatedImageLocation = mutableListOf<String>()
                remoteRecipe.second.forEachIndexed { index, image ->
                    val imageStoreFile =
                        binaryFileHandler.storeImageToFile(
                            image,
                            "${recipeId}_${createdBy}_$index.png",
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
                    val imageFilePaths = mutableListOf<String>()
                    images.forEachIndexed { index, image ->
                        val fileLocation =
                            binaryFileHandler.storeImageToFile(
                                image,
                                "${recipe.onlineId}_${recipe.createdBy.onlineId}_$index.png",
                            )
                        imageFilePaths.add(fileLocation.toString())
                    }
                    if (localDataSource.exists(recipe.onlineId, recipe.createdBy.onlineId)) {
                        localDataSource.update(recipe)
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
                                "${recipe.onlineId}_${recipe.createdBy.onlineId}_$index.png",
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
            remoteDataSource.delete(receiptId, createdBy)
            localDataSource.delete(receiptId, createdBy)
        }
    }
