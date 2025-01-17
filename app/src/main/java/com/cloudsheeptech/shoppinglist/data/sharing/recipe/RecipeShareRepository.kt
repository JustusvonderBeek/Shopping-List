package com.cloudsheeptech.shoppinglist.data.sharing.recipe

import javax.inject.Inject

class RecipeShareRepository @Inject constructor(
    private val localDataSource: RecipeShareLocalDataSource,
    private val remoteDataSource: RecipeShareRemoteDataSource
) {

    suspend fun create(recipeId: Long, createdBy: Long, sharedWith: Long) {
        val share = localDataSource.create(recipeId, createdBy, sharedWith)
        remoteDataSource.create(share)
    }

    suspend fun delete(recipeId: Long, createdBy: Long, sharedWith: Long) {
        localDataSource.delete(recipeId, createdBy, sharedWith)
        remoteDataSource.delete(recipeId, createdBy, sharedWith)
    }

    suspend fun deleteAll(recipeId: Long, createdBy: Long) {
        localDataSource.deleteAll(recipeId, createdBy)
        remoteDataSource.deleteAll(recipeId, createdBy)
    }

}