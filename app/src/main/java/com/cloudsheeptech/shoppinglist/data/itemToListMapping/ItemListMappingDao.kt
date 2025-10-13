package com.cloudsheeptech.shoppinglist.data.itemToListMapping

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cloudsheeptech.shoppinglist.data.items.ItemToList

@Dao
interface ItemListMappingDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertMapping(itemToList: ItemToList): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateMapping(itemToList: ItemToList)

    @Query("DELETE FROM item_to_list_mapping WHERE id = :key")
    fun deleteMapping(key: Long)

    @Query("DELETE FROM item_to_list_mapping WHERE itemId = :itemId AND listId = :listId AND createdBy = :createdBy")
    fun deleteMappingItemListId(
        itemId: Long,
        listId: Long,
        createdBy: Long,
    )

    @Query("DELETE FROM item_to_list_mapping WHERE listId = :listId AND createdBy = :createdBy")
    fun deleteMappingsForListId(
        listId: Long,
        createdBy: Long,
    )

    @Query("DELETE FROM item_to_list_mapping WHERE listId = :listId AND createdBy = :createdBy AND checked = 1")
    fun deleteCheckedMappingsForListId(
        listId: Long,
        createdBy: Long,
    )

    @Query("DELETE FROM item_to_list_mapping")
    fun deleteAllMappings()

    @Query("SELECT * FROM item_to_list_mapping WHERE id = :mappingId")
    fun getMapping(mappingId: Long): ItemToList?

    @Query("SELECT * FROM item_to_list_mapping WHERE listId = :listId AND createdBy = :createdBy")
    fun getMappingsForList(
        listId: Long,
        createdBy: Long,
    ): List<ItemToList>

    @Query("SELECT * FROM item_to_list_mapping WHERE listId = :listId AND createdBy = :createdBy")
    fun getMappingsForListLive(
        listId: Long,
        createdBy: Long,
    ): LiveData<List<ItemToList>>

    @Query("SELECT * FROM item_to_list_mapping WHERE itemId = :itemId")
    fun getMappingsForItem(itemId: Long): List<ItemToList>

    @Query("SELECT * FROM item_to_list_mapping WHERE itemId = :itemId")
    fun getMappingsForItemLive(itemId: Long): LiveData<List<ItemToList>>

    @Query("SELECT * FROM item_to_list_mapping WHERE itemId = :itemId AND listId = :listId AND createdBy = :createdBy")
    fun getMappingForItemAndList(
        itemId: Long,
        listId: Long,
        createdBy: Long,
    ): List<ItemToList> // FIXME: Why a list?

    @Query(
        "SELECT CASE " +
            "WHEN (SELECT COUNT(*) FROM item_to_list_mapping WHERE listId = :listId AND createdBy = :createdBy AND checked = 1) > 0 " +
            "THEN (SELECT COUNT(distinct checked) FROM item_to_list_mapping WHERE listId = :listId AND createdBy = :createdBy) " +
            "ELSE 0 " +
            "END AS count_distinct_values",
    )
    fun getIsListFinishedLive(
        listId: Long,
        createdBy: Long,
    ): LiveData<Int>

    @Query("UPDATE item_to_list_mapping SET createdBy = 0 WHERE createdBy = :createdBy")
    fun resetCreatorIdForItemsInOwnLists(createdBy: Long)

    @Query("UPDATE item_to_list_mapping SET addedBy = 0 WHERE addedBy = :addedBy")
    fun resetAddedByIdForItemsInOwnLists(addedBy: Long)

    @Query("UPDATE item_to_list_mapping SET createdBy = :createdBy WHERE createdBy = 0")
    fun setCreatorIdForAllItems(createdBy: Long)

    @Query("UPDATE item_to_list_mapping SET addedBy = :addedBy WHERE addedBy = 0")
    fun setAddedByIdForItemsInOwnLists(addedBy: Long)
}
