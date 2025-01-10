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
            icon: String?,
        ): ApiRecipe {
            val receipt = localDataSource.create(name, icon, defaultPortion)
            remoteDataSource.create(receipt)
            return receipt
        }

        suspend fun read(
            receiptId: Long,
            createdBy: Long,
        ): ApiRecipe? {
            val receipt = localDataSource.read(receiptId, createdBy) ?: return null
            return receipt
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

        suspend fun update(recipe: ApiRecipe) {
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
            val localReceipt = localDataSource.read(receiptId, createdBy) ?: return
            remoteDataSource.update(localReceipt)
        }

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
