package com.cloudsheeptech.shoppinglist.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.cloudsheeptech.shoppinglist.data.ShoppingList

@Dao
interface ShoppingListDao {

    @Insert
    fun insertList(list : ShoppingList)

    @Update
    fun updateList(list : ShoppingList)

    @Query("DELETE FROM list_table WHERE ID = :key")
    fun deleteList(key : Long)

    @Query("SELECT ID FROM list_table ORDER BY ID ASC LIMIT 1")
    fun getLatestListId() : LiveData<Long?>

}