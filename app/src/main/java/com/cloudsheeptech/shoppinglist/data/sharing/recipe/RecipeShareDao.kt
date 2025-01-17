package com.cloudsheeptech.shoppinglist.data.sharing.recipe

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RecipeShareDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(recipeShare: RecipeShare)

    @Delete
    fun delete(recipeShare: RecipeShare)

    @Query("DELETE FROM recipe_share WHERE recipeId = :key AND createdBy = :createdBy")
    fun deleteAll(key: Long, createdBy: Long)

    @Query("SELECT * FROM recipe_share WHERE recipeId = :key AND createdBy = :createdBy")
    fun getLive(key: Long, createdBy: Long): LiveData<List<RecipeShare>>

    @Query("SELECT * FROM recipe_share WHERE recipeId = :key AND createdBy = :createdBy")
    fun get(key: Long, createdBy: Long): List<RecipeShare>
}