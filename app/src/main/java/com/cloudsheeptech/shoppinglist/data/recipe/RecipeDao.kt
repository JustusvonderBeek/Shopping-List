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
    fun insert(receipt : DbRecipe) : Long

    @Update
    fun update(receipt: DbRecipe)

    @Query("DELETE FROM receipts WHERE id = :key AND createdBy = :createdBy")
    fun delete(key : Long, createdBy : Long)

    @Query("DELETE FROM receipts")
    fun reset()

    @Query("SELECT * FROM receipts WHERE id = :key AND createdBy = :createdBy")
    fun get(key: Long, createdBy: Long) : DbRecipe?

    @Query("SELECT * FROM receipts WHERE id = :key AND createdBy = :createdBy")
    fun getLive(key: Long, createdBy: Long) : LiveData<DbRecipe>

    @Query("SELECT * FROM receipts WHERE id = :key AND createdBy = :createdBy")
    fun getFlow(key: Long, createdBy: Long) : Flow<DbRecipe>

    @Query("SELECT * FROM receipts")
    fun getAllLive() : LiveData<List<DbRecipe>>

}