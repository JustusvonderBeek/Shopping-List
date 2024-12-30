package com.cloudsheeptech.shoppinglist.data.recipe

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(receipt: DbRecipe): Long

    @Update
    fun update(receipt: DbRecipe)

    @Query("DELETE FROM recipes WHERE id = :key AND createdBy = :createdBy")
    fun delete(
        key: Long,
        createdBy: Long,
    )

    @Query("DELETE FROM recipes")
    fun reset()

    @Query("SELECT * FROM recipes WHERE id = :key AND createdBy = :createdBy")
    fun get(
        key: Long,
        createdBy: Long,
    ): DbRecipe?

    @Query("SELECT * FROM recipes WHERE id = :key AND createdBy = :createdBy")
    fun getLive(
        key: Long,
        createdBy: Long,
    ): LiveData<DbRecipe>

    @Query("SELECT * FROM recipes WHERE id = :key AND createdBy = :createdBy")
    fun getFlow(
        key: Long,
        createdBy: Long,
    ): Flow<DbRecipe>

    @Query("SELECT * FROM recipes")
    fun getAllLive(): LiveData<List<DbRecipe>>

    @Query("UPDATE recipes SET createdBy = 0 WHERE createdBy = :createdBy")
    fun resetCreatedBy(createdBy: Long)

    @Query("UPDATE recipes SET createdBy = :createdBy WHERE createdBy = 0")
    fun updateCreatedBy(createdBy: Long)
}
