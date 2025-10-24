package com.cloudsheeptech.shoppinglist.list.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.cloudsheeptech.shoppinglist.list.model.AppItem
import com.cloudsheeptech.shoppinglist.list.model.DbItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Insert
    fun insertItem(dbItem: DbItem): Long

    @Update
    fun updateItem(dbItem: DbItem)

    @Query("DELETE FROM items WHERE id = :key")
    fun deleteItem(key: Long)

    @Query("DELETE FROM items")
    fun deleteAll()

    @Query("SELECT * FROM items WHERE id = :key")
    fun getItemLive(key: Long): LiveData<DbItem?>

    @Query("SELECT * FROM items WHERE id = :key")
    fun getItemFlow(key: Long): Flow<DbItem>

    @Query("SELECT * FROM items WHERE id = :key")
    fun getItem(key: Long): DbItem?

    @Query("SELECT * FROM items WHERE id IN (:keys)")
    fun getItems(keys: List<Long>): List<DbItem>

    @Query("SELECT * FROM items WHERE id IN (:keys)")
    fun getItemsLive(keys: List<Long>): LiveData<List<DbItem>>

    @Query(
        "SELECT i.id, i.name, i.icon,m.quantity as quantity, m.quantityType as quantityType ,m.checked as checked,m.addedBy as addedBy FROM items i INNER JOIN item_to_list_mapping m ON i.id = m.itemId WHERE m.listId = :listId AND m.createdBy = :createdBy",
    )
    fun getItemsWithQuantityInListLive(
        listId: Long,
        createdBy: Long,
    ): LiveData<List<AppItem>>

    @Query(
        "SELECT i.id, i.name, i.icon,m.quantity as quantity, m.quantityType as quantityType ,m.checked as checked,m.addedBy as addedBy FROM items i INNER JOIN item_to_list_mapping m ON i.id = m.itemId WHERE m.listId = :listId AND m.createdBy = :createdBy",
    )
    fun getItemsWithQuantityInList(
        listId: Long,
        createdBy: Long,
    ): List<AppItem>

    @Query("SELECT * FROM items WHERE name = :name")
    fun getItemFromName(name: String): DbItem?

    @Query("SELECT * FROM items WHERE name LIKE '%' || :name || '%'")
    fun getItemsFromName(name: String): List<DbItem>

    @Query("SELECT * FROM items WHERE name = :name")
    fun getItemFromNameExactMatch(name: String): DbItem?

    @Query("SELECT * FROM items")
    fun getAllItems(): List<DbItem>

    @Query("SELECT COUNT(id) FROM items")
    fun getCurrentId(): Long
}
