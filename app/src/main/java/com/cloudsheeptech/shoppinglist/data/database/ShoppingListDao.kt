package com.cloudsheeptech.shoppinglist.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cloudsheeptech.shoppinglist.data.ShoppingList

@Dao
interface ShoppingListDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertList(list : ShoppingList) : Long

    @Update
    fun updateList(list : ShoppingList)

    @Query("DELETE FROM list_table WHERE ID = :key AND CreatedByID = :createdBy")
    fun deleteList(key : Long, createdBy : Long)

    @Query("DELETE FROM list_table")
    fun reset()

    @Query("SELECT COUNT(*) FROM list_table")
    fun getLatestListIdLive() : LiveData<Long>

    @Query("SELECT MAX(ID) FROM list_table")
    fun getLatestListId() : Long

    @Query("SELECT MAX(ID) FROM list_table WHERE ID = 0 OR CreatedByName = :key")
    fun getLatestOwnListId(key : Long) : Long

    @Query("SELECT * FROM list_table ORDER BY ID ASC")
    fun getShoppingListsLive() : LiveData<List<ShoppingList>>

    @Query("SELECT * FROM list_table ORDER BY ID ASC")
    fun getShoppingLists() : List<ShoppingList>

    @Query("SELECT * FROM list_table WHERE ID = :key AND CreatedByID = :keyCreatedBy")
    fun getShoppingListLive(key : Long, keyCreatedBy : Long) : LiveData<ShoppingList>

    @Query("SELECT * FROM list_table WHERE ID = :key AND CreatedByID = :keyCreatedBy")
    fun getShoppingList(key : Long, keyCreatedBy: Long) : ShoppingList?

}