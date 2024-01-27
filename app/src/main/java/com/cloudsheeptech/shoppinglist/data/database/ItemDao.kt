package com.cloudsheeptech.shoppinglist.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.cloudsheeptech.shoppinglist.data.Item

@Dao
interface ItemDao {

    @Insert
    fun insertItem(item : Item) : Long

    @Update
    fun updateItem(item : Item) : Long

    @Query("DELETE FROM items WHERE ID = :key")
    fun deleteItem(key : Long)

    @Query("DELETE FROM items")
    fun deleteAll()

    @Query("SELECT * FROM items WHERE ID = :key")
    fun getItemLive(key : Long) : LiveData<Item?>

    @Query("SELECT * FROM items WHERE ID = :key")
    fun getItem(key : Long) : Item?

    @Query("SELECT * FROM items WHERE ID IN (:keys)")
    fun getItems(keys : List<Long>) : List<Item>

    @Query("SELECT * FROM items WHERE ID IN (:keys)")
    fun getItemsLive(keys : List<Long>) : LiveData<List<Item>>

    @Query("SELECT * FROM items WHERE name = :name")
    fun getItemFromName(name : String) : Item?

    @Query("SELECT * FROM items WHERE INSTR(Name, :name) > 0")
    fun getItemsFromName(name : String) : List<Item>

    @Query("SELECT * FROM items")
    fun getAllItems() : List<Item>

    @Query("SELECT COUNT(ID) FROM items")
    fun getCurrentId() : Long

}