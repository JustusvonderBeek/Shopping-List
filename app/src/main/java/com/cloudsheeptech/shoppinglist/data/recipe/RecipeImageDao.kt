package com.cloudsheeptech.shoppinglist.data.recipe

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface RecipeImageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(receipt: RecipeImage): Long

    @Update
    fun update(receipt: RecipeImage)

    @Query("SELECT * FROM recipe_images WHERE recipeId = :recipeId AND createdBy = :createdBy")
    fun read(recipeId: Long, createdBy: Long): List<RecipeImage>

    @Query("SELECT * FROM recipe_images WHERE recipeId = :recipeId AND createdBy = :createdBy")
    fun readLive(recipeId: Long, createdBy: Long): LiveData<List<RecipeImage>>

    @Query("DELETE FROM recipe_images WHERE recipeId = :id AND createdBy = :createdBy")
    fun delete(
        id: Long,
        createdBy: Long,
    )

    @Query("DELETE FROM recipe_images WHERE imageId IN (:imageIds) AND createdBy = :createdBy")
    fun deleteIds(
        imageIds: List<Long>,
        createdBy: Long
    )

}