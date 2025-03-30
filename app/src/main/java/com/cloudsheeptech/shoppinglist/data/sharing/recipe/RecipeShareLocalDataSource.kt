package com.cloudsheeptech.shoppinglist.data.sharing.recipe

import androidx.lifecycle.LiveData
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.recipe.RecipeRepository
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.InternalSerializationApi
import javax.inject.Inject

class RecipeShareLocalDataSource
    @Inject
    constructor(
        database: ShoppingListDatabase,
        private val userRepo: AppUserRepository,
        private val recipeRepo: RecipeRepository,
    ) {
        private val shareDao = database.recipeShareDao()

        @OptIn(InternalSerializationApi::class)
        suspend fun create(
            recipeId: Long,
            createdBy: Long,
            sharedWith: Long,
        ): RecipeShare {
            var createdSharedWith: RecipeShare
            withContext(Dispatchers.IO) {
                val user = userRepo.read() ?: throw IllegalStateException("user not initialized")
                val recipe =
                    recipeRepo.read(recipeId, createdBy)
                        ?: throw IllegalArgumentException("recipe does not exist")
                // TODO: Check if the user we share with is contained in our local store
                // Consequence might be an error on the server, but not too relevant
                val newShare = RecipeShare(recipeId, createdBy, sharedWith)
                createdSharedWith = newShare
                shareDao.insert(newShare)
            }
            return createdSharedWith
        }

        suspend fun read(
            recipeId: Long,
            createdBy: Long,
        ): List<RecipeShare> {
            val recipeSharedWithUserIds = mutableListOf<RecipeShare>()
            withContext(Dispatchers.IO) {
                val sharedWithIds = shareDao.get(recipeId, createdBy)
                recipeSharedWithUserIds.addAll(sharedWithIds)
            }
            return recipeSharedWithUserIds
        }

        fun readLive(
            recipeId: Long,
            createdBy: Long,
        ): LiveData<List<RecipeShare>> = shareDao.getLive(recipeId, createdBy)

        suspend fun delete(
            recipeId: Long,
            createdBy: Long,
            sharedWith: Long,
        ) {
            withContext(Dispatchers.IO) {
                shareDao.delete(RecipeShare(recipeId, createdBy, sharedWith))
            }
        }

        suspend fun deleteAll(
            recipeId: Long,
            createdBy: Long,
        ) {
            withContext(Dispatchers.IO) {
                shareDao.deleteAll(recipeId, createdBy)
            }
        }
    }
