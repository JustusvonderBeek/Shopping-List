package com.cloudsheeptech.shoppinglist.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cloudsheeptech.shoppinglist.data.ListMapping

@Dao
interface ItemListMappingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMapping(listMapping: ListMapping)

    @Update
    fun updateMapping(listMapping: ListMapping)

    @Query("DELETE FROM item_to_list_mapping WHERE ID = :key")
    fun deleteMapping(key : Long)

    @Query("DELETE FROM item_to_list_mapping WHERE ItemID = :itemId AND ListID = :listId")
    fun deleteMappingItemListId(itemId : Long, listId : Long)

    @Query("DELETE FROM item_to_list_mapping WHERE ListID = :listId AND CreatedBy = :createdBy")
    fun deleteMappingsForListId(listId : Long, createdBy: Long)

    @Query("DELETE FROM item_to_list_mapping WHERE ListID = :listId AND Checked = 1")
    fun deleteCheckedMappingsForListId(listId : Long)

    @Query("DELETE FROM item_to_list_mapping")
    fun clearAll()

    @Query("SELECT * FROM item_to_list_mapping WHERE ID = :mappingId")
    fun getMapping(mappingId : Long) : ListMapping?

    @Query("SELECT * FROM item_to_list_mapping WHERE ListID = :listId AND CreatedBy = :createdBy")
    fun getMappingsForList(listId : Long, createdBy: Long) : List<ListMapping>

    @Query("SELECT * FROM item_to_list_mapping WHERE ListID = :listId AND CreatedBy = :createdBy")
    fun getMappingsForListLive(listId : Long, createdBy : Long) : LiveData<List<ListMapping>>

    @Query("SELECT * FROM item_to_list_mapping WHERE ItemID = :itemId")
    fun getMappingsForItem(itemId : Long) : List<ListMapping>

    @Query("SELECT * FROM item_to_list_mapping WHERE ItemID = :itemId")
    fun getMappingsForItemLive(itemId : Long) : LiveData<List<ListMapping>>

    @Query("SELECT * FROM item_to_list_mapping WHERE ItemID = :itemId AND ListID = :listId AND CreatedBy = :createdBy")
    fun getMappingForItemAndList(itemId : Long, listId : Long, createdBy: Long) : List<ListMapping>

    @Query("SELECT CASE " +
            "WHEN (SELECT COUNT(*) FROM item_to_list_mapping WHERE ListID = :listId AND CreatedBy = :createdBy AND Checked = 1) > 0 " +
            "THEN (SELECT COUNT(distinct Checked) FROM item_to_list_mapping WHERE ListID = :listId AND CreatedBy = :createdBy) " +
            "ELSE 0 " +
            "END AS count_distinct_values")
    fun getIsListFinishedLive(listId : Long, createdBy: Long) : LiveData<Int>
}