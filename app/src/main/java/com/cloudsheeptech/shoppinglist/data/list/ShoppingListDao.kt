package com.cloudsheeptech.shoppinglist.data.list

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ShoppingListDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertList(list: DbShoppingList): Long

    @Update
    fun updateList(list: DbShoppingList)

    @Query("DELETE FROM list_table WHERE listId = :key AND createdBy = :createdBy")
    fun deleteList(
        key: Long,
        createdBy: Long,
    )

    @Query("DELETE FROM list_table")
    fun reset()

    @Query("SELECT COUNT(*) FROM list_table")
    fun getLatestListIdLive(): LiveData<Long>

    @Query("SELECT MAX(listId) FROM list_table")
    fun getLatestListId(): Long

    @Query("SELECT MAX(listId) FROM list_table WHERE listId = 0 OR createdBy = :key")
    fun getLatestOwnListId(key: Long): Long

    @Query("SELECT MAX(listId) FROM list_table WHERE createdBy = :key")
    fun getLatestListId(key: Long): Long

    @Query("SELECT * FROM list_table ORDER BY listId ASC")
    fun getShoppingListsLive(): LiveData<List<DbShoppingList>>

    @Query("SELECT * FROM list_table ORDER BY listId ASC")
    fun getShoppingLists(): List<DbShoppingList>

    @Query("SELECT * FROM list_table WHERE listId = :key AND createdBy = :keyCreatedBy")
    fun getShoppingListLive(
        key: Long,
        keyCreatedBy: Long,
    ): LiveData<DbShoppingList>

    @Query("SELECT * FROM list_table WHERE listId = :key AND createdBy = :keyCreatedBy")
    fun getShoppingList(
        key: Long,
        keyCreatedBy: Long,
    ): DbShoppingList?

    @Query("SELECT * FROM list_table WHERE createdBy = :createdBy")
    fun getOwnShoppingLists(createdBy: Long): List<DbShoppingList>

    @Query("SELECT EXISTS (SELECT * FROM list_table WHERE listId = :key AND createdBy = :keyCreatedBy)")
    fun exists(
        key: Long,
        keyCreatedBy: Long,
    ): Boolean

    @Query("UPDATE list_table SET createdBy = 0 WHERE createdBy = :createdBy")
    fun resetOwnListsCreatorId(createdBy: Long)

    @Query("UPDATE list_table SET createdBy = :createdBy WHERE createdBy = 0")
    fun setListWithOfflineCreatorToOnlineId(createdBy: Long)
}
