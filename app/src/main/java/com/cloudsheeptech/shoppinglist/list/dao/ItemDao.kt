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

    @Query("DELETE FROM items WHERE name = :name")
    fun deleteItem(name: String)

    @Query("DELETE FROM items")
    fun deleteAll()

    @Query("SELECT * FROM items WHERE name = :name")
    fun getItemLive(name: String): LiveData<DbItem?>

    @Query("SELECT * FROM items WHERE name = :name")
    fun getItemFlow(name: String): Flow<DbItem>

    @Query("SELECT * FROM items WHERE name = :name")
    fun getItem(name: String): DbItem?

    @Query("SELECT * FROM items WHERE name IN (:names)")
    fun getItems(names: List<String>): List<DbItem>

    @Query("SELECT * FROM items WHERE name IN (:names)")
    fun getItemsLive(names: List<String>): LiveData<List<DbItem>>

    @Query(
        "SELECT i.name, i.icon,m.quantity as quantity, m.quantityType as quantityType, m.checked as checked, m.addedBy as addedBy, m.opCount as opCount FROM items i INNER JOIN item_to_list_mapping m ON i.name = m.item WHERE m.listId = :listId AND m.createdBy = :createdBy",
    )
    fun getItemsWithQuantityInListLive(
        listId: Long,
        createdBy: Long,
    ): LiveData<List<AppItem>>

    @Query(
        "SELECT i.name, i.icon,m.quantity as quantity, m.quantityType as quantityType ,m.checked as checked,m.addedBy as addedBy, m.opCount as opCount FROM items i INNER JOIN item_to_list_mapping m ON i.name = m.item WHERE m.listId = :listId AND m.createdBy = :createdBy",
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

    @Query("SELECT COUNT(name) FROM items")
    fun getCurrentId(): Long
}
