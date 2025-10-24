package com.cloudsheeptech.shoppinglist.sharing.repo

import android.util.Log
import androidx.lifecycle.LiveData
import com.cloudsheeptech.shoppinglist.sharing.model.ShareUserPreview
import javax.inject.Inject

class RecipeShareRepository
    @Inject
    constructor(
        private val localDataSource: RecipeShareLocalDataSource,
        private val remoteDataSource: RecipeShareRemoteDataSource,
    ) {
        suspend fun create(
            recipeId: Long,
            createdBy: Long,
            sharedWith: Long,
        ) {
            try {
                val share = localDataSource.create(recipeId, createdBy, sharedWith)
                if (share == null) {
                    Log.e(
                        "RecipeShareRepository",
                        "Failed to create share, likely because recipe was not created locally and can therefore not be shared",
                    )
                    return
                }
                remoteDataSource.create(share)
            } catch (ex: IllegalArgumentException) {
                Log.e("RecipeShareRepository", "List $recipeId from $createdBy not from current user")
            }
        }

        fun readLive(
            recipeId: Long,
            createdBy: Long,
        ): LiveData<List<ShareUserPreview>> = localDataSource.readLiveWithUser(recipeId, createdBy)

        suspend fun delete(
            recipeId: Long,
            createdBy: Long,
            sharedWith: Long,
        ) {
            try {
                localDataSource.delete(recipeId, createdBy, sharedWith)
                remoteDataSource.delete(recipeId, createdBy, sharedWith)
            } catch (ex: Exception) {
                Log.e("RecipeShareRepository", "Failed to delete share: $ex")
            }
        }

        suspend fun deleteAll(
            recipeId: Long,
            createdBy: Long,
        ) {
            try {
                localDataSource.deleteAll(recipeId, createdBy)
                remoteDataSource.deleteAll(recipeId)
            } catch (ex: Exception) {
                Log.e("RecipeShareRepository", "Failed to delete all shares: $ex")
            }
        }
    }
