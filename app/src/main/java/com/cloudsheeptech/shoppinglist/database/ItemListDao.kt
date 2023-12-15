package com.cloudsheeptech.shoppinglist.database

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.cloudsheeptech.shoppinglist.data.Item
import com.cloudsheeptech.shoppinglist.data.ItemList
import com.cloudsheeptech.shoppinglist.datastructures.ItemListWithName

@Dao
interface ItemListDao {

    @Insert
    fun insertItem(item : Item)

    @Update
    fun updateItem(item : Item)

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

    @Query("SELECT COUNT(ID) FROM items")
    fun getCurrentId() : Long

}