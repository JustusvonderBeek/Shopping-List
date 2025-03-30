package com.cloudsheeptech.shoppinglist.data.recipe

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import com.cloudsheeptech.shoppinglist.exception.UserNotAuthenticatedException
import kotlinx.serialization.InternalSerializationApi
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
    @OptIn(InternalSerializationApi::class)
    private fun updateRecipeCreatedBy(recipe: ApiRecipe) {
        if (recipe.createdBy.onlineId != 0L) {
            return
        }
        val user =
            userRepository.read() ?: throw IllegalStateException("user null after login screen")
        recipe.createdBy.onlineId = user.OnlineID
        recipe.createdBy.username = user.Username
    }

    @OptIn(InternalSerializationApi::class)
    suspend fun create(
        name: String,
        defaultPortion: Int,
        images: List<Uri>,
    ): ApiRecipe {
        val recipe = localDataSource.create(name, images, defaultPortion)
        remoteDataSource.create(recipe, images)
        return recipe
    }

    @OptIn(InternalSerializationApi::class)
    suspend fun read(
        receiptId: Long,
        createdBy: Long,
    ): ApiRecipe? {
        val receipt = localDataSource.read(receiptId, createdBy) ?: return null
        return receipt
    }

    @OptIn(InternalSerializationApi::class)
    fun readLive(
        receiptId: Long,
        createdBy: Long,
    ): LiveData<ApiRecipe> {
        val localReceipt = localDataSource.readLive(receiptId, createdBy)
        return localReceipt.asLiveData()
    }

    // TODO: Fix the different list type
    fun readAllLive(): LiveData<List<DbRecipe>> = localDataSource.readAllLive()

    @OptIn(InternalSerializationApi::class)
    suspend fun readOnline(
        receiptId: Long,
        createdBy: Long,
    ): ApiRecipe? = remoteDataSource.read(receiptId, createdBy)

    @OptIn(InternalSerializationApi::class)
    suspend fun update(recipe: ApiRecipe) {
        try {
            recipe.version++
            val updatedVersion = localDataSource.update(recipe)
            if (updatedVersion < 0L) {
                return
            }
            var success = false
            try {
                success = remoteDataSource.update(recipe)
            } catch (ex: UserNotAuthenticatedException) {
                Log.w("RecipeRepository", "User might not be authenticated: $ex")
            }
            if (!success) {
                updateRecipeCreatedBy(recipe)
                success = remoteDataSource.update(recipe)
            }
            if (!success) {
                success = remoteDataSource.create(recipe)
            }
            if (success) {
                Log.i("RecipeRepository", "The recipe ${recipe.onlineId} was updated online")
            } else {
                Log.i("RecipeRepository", "Updating the recipe ${recipe.onlineId} online failed")
            }
        } catch (ex: UserNotAuthenticatedException) {
            Log.e(
                "RecipeRepository",
                "Something went wrong during communication with the server: $ex"
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

    @OptIn(InternalSerializationApi::class)
    suspend fun insertDescription(
        receiptId: Long,
        createdBy: Long,
        order: Int,
        description: String,
    ) {
        localDataSource.insertDescription(receiptId, createdBy, order, description)
        val localReceipt = localDataSource.read(receiptId, createdBy) ?: return
        remoteDataSource.update(localReceipt)
    }

    @OptIn(InternalSerializationApi::class)
    suspend fun updateDescription(
        receiptId: Long,
        createdBy: Long,
        order: Int,
        description: String,
    ) {
        localDataSource.updateDescription(receiptId, createdBy, order, description)
        val localReceipt = localDataSource.read(receiptId, createdBy) ?: return
        remoteDataSource.update(localReceipt)
    }

    @OptIn(InternalSerializationApi::class)
    suspend fun deleteDescription(
        receiptId: Long,
        createdBy: Long,
        order: Int,
    ) {
        localDataSource.deleteDescription(receiptId, createdBy, order)
        val localReceipt = localDataSource.read(receiptId, createdBy) ?: return
        remoteDataSource.update(localReceipt)
    }

    suspend fun delete(
        receiptId: Long,
        createdBy: Long,
    ) {
        remoteDataSource.delete(receiptId, createdBy)
        localDataSource.delete(receiptId, createdBy)
    }
}
