package com.cloudsheeptech.shoppinglist.data.itemToListMapping

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ItemListMappingDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertMapping(listMapping: ListMapping): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateMapping(listMapping: ListMapping)

    @Query("DELETE FROM item_to_list_mapping WHERE ID = :key")
    fun deleteMapping(key: Long)

    @Query("DELETE FROM item_to_list_mapping WHERE ItemID = :itemId AND ListID = :listId AND CreatedBy = :createdBy")
    fun deleteMappingItemListId(
        itemId: Long,
        listId: Long,
        createdBy: Long,
    )

    @Query("DELETE FROM item_to_list_mapping WHERE ListID = :listId AND CreatedBy = :createdBy")
    fun deleteMappingsForListId(
        listId: Long,
        createdBy: Long,
    )

    @Query("DELETE FROM item_to_list_mapping WHERE ListID = :listId AND Checked = 1")
    fun deleteCheckedMappingsForListId(listId: Long)

    @Query("DELETE FROM item_to_list_mapping")
    fun clearAll()

    @Query("SELECT * FROM item_to_list_mapping WHERE ID = :mappingId")
    fun getMapping(mappingId: Long): ListMapping?

    @Query("SELECT * FROM item_to_list_mapping WHERE ListID = :listId AND CreatedBy = :createdBy")
    fun getMappingsForList(
        listId: Long,
        createdBy: Long,
    ): List<ListMapping>

    @Query("SELECT * FROM item_to_list_mapping WHERE ListID = :listId AND CreatedBy = :createdBy")
    fun getMappingsForListLive(
        listId: Long,
        createdBy: Long,
    ): LiveData<List<ListMapping>>

    @Query("SELECT * FROM item_to_list_mapping WHERE ItemID = :itemId")
    fun getMappingsForItem(itemId: Long): List<ListMapping>

    @Query("SELECT * FROM item_to_list_mapping WHERE ItemID = :itemId")
    fun getMappingsForItemLive(itemId: Long): LiveData<List<ListMapping>>

    @Query("SELECT * FROM item_to_list_mapping WHERE ItemID = :itemId AND ListID = :listId AND CreatedBy = :createdBy")
    fun getMappingForItemAndList(itemId: Long, listId: Long, createdBy: Long): List<ListMapping> // FIXME: Why a list?

    @Query(
        "SELECT CASE " +
            "WHEN (SELECT COUNT(*) FROM item_to_list_mapping WHERE ListID = :listId AND CreatedBy = :createdBy AND Checked = 1) > 0 " +
            "THEN (SELECT COUNT(distinct Checked) FROM item_to_list_mapping WHERE ListID = :listId AND CreatedBy = :createdBy) " +
            "ELSE 0 " +
            "END AS count_distinct_values",
    )
    fun getIsListFinishedLive(
        listId: Long,
        createdBy: Long,
    ): LiveData<Int>

    @Query("UPDATE item_to_list_mapping SET CreatedBy = 0 WHERE CreatedBy = :createdBy")
    fun resetCreatorIdForItemsInOwnLists(createdBy: Long)

    @Query("UPDATE item_to_list_mapping SET AddedBy = 0 WHERE AddedBy = :addedBy")
    fun resetAddedByIdForItemsInOwnLists(addedBy: Long)

    @Query("UPDATE item_to_list_mapping SET CreatedBy = :createdBy WHERE CreatedBy = 0")
    fun setCreatorIdForAllItems(createdBy: Long)

    @Query("UPDATE item_to_list_mapping SET AddedBy = :addedBy WHERE AddedBy = 0")
    fun setAddedByIdForItemsInOwnLists(addedBy: Long)
}
