package com.cloudsheeptech.shoppinglist.data.items

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Insert
    fun insertItem(dbItem : DbItem) : Long

    @Update
    fun updateItem(dbItem : DbItem)

    @Query("DELETE FROM items WHERE id = :key")
    fun deleteItem(key : Long)

    @Query("DELETE FROM items")
    fun deleteAll()

    @Query("SELECT * FROM items WHERE id = :key")
    fun getItemLive(key : Long) : LiveData<DbItem?>

    @Query("SELECT * FROM items WHERE id = :key")
    fun getItemFlow(key: Long) : Flow<DbItem>

    @Query("SELECT * FROM items WHERE id = :key")
    fun getItem(key : Long) : DbItem?

    @Query("SELECT * FROM items WHERE id IN (:keys)")
    fun getItems(keys : List<Long>) : List<DbItem>

    @Query("SELECT * FROM items WHERE id IN (:keys)")
    fun getItemsLive(keys : List<Long>) : LiveData<List<DbItem>>

    @Query("SELECT * FROM items WHERE name = :name")
    fun getItemFromName(name : String) : DbItem?

    @Query("SELECT * FROM items WHERE INSTR(name, :name) > 0")
    fun getItemsFromName(name : String) : List<DbItem>

    @Query("SELECT * FROM items")
    fun getAllItems() : List<DbItem>

    @Query("SELECT COUNT(id) FROM items")
    fun getCurrentId() : Long

}