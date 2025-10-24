package com.cloudsheeptech.shoppinglist.sharing.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cloudsheeptech.shoppinglist.sharing.model.RecipeShare
import com.cloudsheeptech.shoppinglist.sharing.model.ShareUserPreview

@Dao
interface RecipeShareDao {
    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    fun insert(recipeShare: RecipeShare)

    @Delete
    fun delete(recipeShare: RecipeShare)

    @Query("DELETE FROM recipe_share WHERE recipeId = :key AND createdBy = :createdBy")
    fun deleteAll(
        key: Long,
        createdBy: Long,
    )

    @Query("SELECT * FROM recipe_share WHERE recipeId = :key AND createdBy = :createdBy")
    fun getLive(
        key: Long,
        createdBy: Long,
    ): LiveData<List<RecipeShare>>

    @Query(
        "SELECT ou.onlineId as UserId, ou.username as Name, 1 as Shared FROM recipe_share rs JOIN online_user ou ON rs.sharedWithUserId = ou.onlineId WHERE recipeId = :recipeId AND createdBy = :createdBy",
    )
    fun getLiveWithUser(
        recipeId: Long,
        createdBy: Long,
    ): LiveData<List<ShareUserPreview>>

    @Query("SELECT * FROM recipe_share WHERE recipeId = :key AND createdBy = :createdBy")
    fun get(
        key: Long,
        createdBy: Long,
    ): List<RecipeShare>
}
