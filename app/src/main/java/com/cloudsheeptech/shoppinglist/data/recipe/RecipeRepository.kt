package com.cloudsheeptech.shoppinglist.data.recipe

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import com.cloudsheeptech.shoppinglist.exception.UserNotAuthenticatedException
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
        val binaryImages = binaryFileHandler.readImagesFromFiles(images)
        val success = remoteDataSource.create(recipe, binaryImages)
        if (!success) {
            Log.e("RecipeRepository", "Creating recipe online failed")
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

    // TODO: Fix the different list type
    fun readAllLive(): LiveData<List<DbRecipe>> = localDataSource.readAllLive()

    suspend fun readOnline(
        receiptId: Long,
        createdBy: Long,
    ): ApiRecipe? = remoteDataSource.read(receiptId, createdBy)

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
            var success = false
            val binaryImages = binaryFileHandler.readImagesFromFiles(recipeImages)
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
        }
    }

    suspend fun resetCreatedBy() {
        val user =
            userRepository.read() ?: throw IllegalStateException("user null after login screen")
        localDataSource.resetCreatedBy(user.OnlineID)
    }

    suspend fun insertDescription(
        receiptId: Long,
        createdBy: Long,
        order: Int,
        description: String,
    ) {
        localDataSource.insertDescription(receiptId, createdBy, order, description)
        val localRecipe = localDataSource.read(receiptId, createdBy)
        if (localRecipe.first == null) {
            Log.e("RecipeRepository", "Recipe not found")
            return
        }
        remoteDataSource.update(localRecipe.first!!, emptyList())
    }

    suspend fun updateDescription(
        receiptId: Long,
        createdBy: Long,
        order: Int,
        description: String,
    ) {
        localDataSource.updateDescription(receiptId, createdBy, order, description)
        val localReceipt = localDataSource.read(receiptId, createdBy)
        remoteDataSource.update(localReceipt.first!!, emptyList())
    }

    suspend fun deleteDescription(
        receiptId: Long,
        createdBy: Long,
        order: Int,
    ) {
        localDataSource.deleteDescription(receiptId, createdBy, order)
        val localReceipt = localDataSource.read(receiptId, createdBy)
        remoteDataSource.update(localReceipt.first!!, emptyList())
    }

    suspend fun delete(
        receiptId: Long,
        createdBy: Long,
    ) {
        remoteDataSource.delete(receiptId, createdBy)
        localDataSource.delete(receiptId, createdBy)
    }
}
