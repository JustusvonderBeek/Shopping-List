package com.cloudsheeptech.shoppinglist.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.cloudsheeptech.shoppinglist.data.ShoppingList

@Dao
interface ShoppingListDao {

    @Insert
    fun insertList(list : ShoppingList) : Long

    @Update
    fun updateList(list : ShoppingList)

    @Query("DELETE FROM list_table WHERE ID = :key")
    fun deleteList(key : Long)

    @Query("DELETE FROM list_table")
    fun reset()

    @Query("SELECT ID FROM list_table ORDER BY ID ASC LIMIT 1")
    fun getLatestListIdLive() : LiveData<Long?>

    @Query("SELECT ID FROM list_table ORDER BY ID ASC LIMIT 1")
    fun getLatestListId() : Long?

    @Query("SELECT * FROM list_table ORDER BY ID ASC")
    fun getShoppingListsLive() : LiveData<List<ShoppingList>>

    @Query("SELECT * FROM list_table ORDER BY ID ASC")
    fun getShoppingLists() : List<ShoppingList>

    @Query("SELECT * FROM list_table WHERE ID = :key")
    fun getShoppingListLive(key : Long) : LiveData<ShoppingList>

    @Query("SELECT * FROM list_table WHERE ID = :key")
    fun getShoppingList(key : Long) : ShoppingList?

}