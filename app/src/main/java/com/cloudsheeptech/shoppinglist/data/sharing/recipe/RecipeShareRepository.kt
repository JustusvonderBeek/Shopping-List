package com.cloudsheeptech.shoppinglist.data.sharing.recipe

import androidx.lifecycle.LiveData
import com.cloudsheeptech.shoppinglist.data.sharing.ShareUserPreview
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
            val share = localDataSource.create(recipeId, createdBy, sharedWith)
            remoteDataSource.create(share)
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
            localDataSource.delete(recipeId, createdBy, sharedWith)
            remoteDataSource.delete(recipeId, createdBy, sharedWith)
        }

        suspend fun deleteAll(
            recipeId: Long,
            createdBy: Long,
        ) {
            localDataSource.deleteAll(recipeId, createdBy)
            remoteDataSource.deleteAll(recipeId)
        }
    }
