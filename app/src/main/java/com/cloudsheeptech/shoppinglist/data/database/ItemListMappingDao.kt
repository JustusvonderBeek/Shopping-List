package com.cloudsheeptech.shoppinglist.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.cloudsheeptech.shoppinglist.data.ListMapping

@Dao
interface ItemListMappingDao {

    @Insert
    fun insertMapping(listMapping: ListMapping)

    @Update
    fun updateMapping(listMapping: ListMapping)

    @Query("DELETE FROM item_to_list_mapping WHERE ID = :key")
    fun deleteMapping(key : Long)

    @Query("DELETE FROM item_to_list_mapping WHERE ItemID = :itemId AND ListID = :listId")
    fun deleteMappingItemListId(itemId : Long, listId : Long)

    @Query("DELETE FROM item_to_list_mapping")
    fun clearAll()

    @Query("SELECT * FROM item_to_list_mapping WHERE ID = :mappingId")
    fun getMapping(mappingId : Long) : ListMapping?

    @Query("SELECT * FROM item_to_list_mapping WHERE ListID = :listId")
    fun getMappingsForList(listId : Long) : List<ListMapping>

    @Query("SELECT * FROM item_to_list_mapping WHERE ListID = :listId")
    fun getMappingsForListLive(listId : Long) : LiveData<List<ListMapping>>

    @Query("SELECT * FROM item_to_list_mapping WHERE ItemID = :itemId")
    fun getMappingsForItem(itemId : Long) : List<ListMapping>

    @Query("SELECT * FROM item_to_list_mapping WHERE ItemID = :itemId")
    fun getMappingsForItemLive(itemId : Long) : LiveData<List<ListMapping>>

    @Query("SELECT * FROM item_to_list_mapping WHERE ItemID = :itemId AND ListID = :listId")
    fun getMappingForItemAndList(itemId : Long, listId : Long) : List<ListMapping>
}