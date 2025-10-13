package com.cloudsheeptech.shoppinglist.data.list

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.cloudsheeptech.shoppinglist.data.items.AppItem
import com.cloudsheeptech.shoppinglist.data.items.DbItem
import com.cloudsheeptech.shoppinglist.data.items.ItemToList
import com.cloudsheeptech.shoppinglist.data.onlineUser.ListCreator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * This class should abstract for the multiple local repositories handling only DB data
 * I don't see any benefit in an additional wrapper; instead keeping all in a single class
 * and only exporting the final class for handling should make things easier
 */
@Dao
interface ShoppingListDao {
    /**
     * @return The listId of the newly created list
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    @Transaction
    fun insertList(list: ShoppingList): Long {
        val (dbList, dbItems, dbItemToList) = list.toEntities()
        val listId = insertDbList(dbList)
        for ((index, item) in dbItems.withIndex()) {
            val itemId = insertItem(item)
            dbItemToList[index].listId = listId
            dbItemToList[index].itemId = itemId
        }
        for (itemToList in dbItemToList) {
            insertItemMapping(itemToList)
        }
        return listId
    }

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertDbList(dbList: DbShoppingList): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertItem(item: DbItem): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertItemMapping(itemMapping: ItemToList): Long

    /**
     * @return The list including items in the App format or null if not found
     */
    @Transaction
    suspend fun getList(
        listId: Long,
        createdBy: Long,
    ): ShoppingList? {
        val baseList = getBaseList(listId, createdBy) ?: return null
        val items = getItems(listId, createdBy)
        val list =
            ShoppingList(
                listId = listId,
                createdBy = ListCreator(createdBy, "todo"),
                title = baseList.title,
                synchronized = baseList.synchronized,
                items = items.toMutableList(),
            )
        return list
    }

    fun getItems(
        listId: Long,
        createdBy: Long,
    ): List<AppItem> {
        val mappings = getItemMappings(listId, createdBy)
        val baseItems = getBaseItems(mappings.map { itemToList -> itemToList.itemId })
        val items =
            baseItems.mapIndexed { index, item ->
                val mapping = mappings[index]
                AppItem.fromBaseAndMapping(item, mapping)
            }
        return items
    }

    @Query("SELECT * FROM list_table WHERE listId = :listId AND createdBy = :createdBy")
    fun getBaseList(
        listId: Long,
        createdBy: Long,
    ): DbShoppingList?

    @Query("SELECT * FROM item_to_list_mapping WHERE listId = :listId AND createdBy = :createdBy")
    fun getItemMappings(
        listId: Long,
        createdBy: Long,
    ): List<ItemToList>

    @Query("SELECT * FROM items WHERE id in (:id)")
    fun getBaseItems(id: List<Long>): List<DbItem>

    @Transaction
    suspend fun addItem(
        item: AppItem,
        listId: Long,
        createdBy: Long,
    ) {
        val (baseItem, itemMapping) = item.toEntities(Pair(listId, createdBy))
        if (item.id == null || item.id!! <= 0) {
            val itemId = insertItem(baseItem)
            itemMapping.itemId = itemId
        }
        insertItemMapping(itemMapping)
    }

    suspend fun deleteItem(
        item: AppItem,
        listId: Long,
        createdBy: Long,
    ) {
        val (baseItem, itemMapping) = item.toEntities(Pair(listId, createdBy))
        if (item.id == null || item.id!! <= 0L) {
            return
        }
        deleteMapping(itemMapping)
    }

    @Delete
    fun deleteMapping(itemMapping: ItemToList)

    // Because of the dedicated update functions and context of this app, we dont need a full
    // update method anymore. Instead each operation is applied in sequence allow for nicer
    // animations in the UI

    suspend fun updateItems(
        items: List<AppItem>,
        listId: Long,
        createdBy: Long,
    ) {
        for (item in items) {
            updateItem(item, listId, createdBy)
        }
    }

    @Transaction
    suspend fun updateItem(
        item: AppItem,
        listId: Long,
        createdBy: Long,
    ) {
        val (baseItem, itemMapping) = item.toEntities(Pair(listId, createdBy))
        if (item.id == null || item.id!! <= 0L) {
            addItem(item, listId, createdBy)
            return
        }
        updateItemMapping(itemMapping)
    }

    @Update
    fun updateItemMapping(itemMapping: ItemToList)

    @Query("DELETE FROM list_table WHERE listId = :key AND createdBy = :createdBy")
    fun deleteList(
        key: Long,
        createdBy: Long,
    )

    @Query("DELETE FROM list_table")
    fun dropTable()

    // LiveData Functions

    @Query("SELECT * FROM items WHERE id IN (:itemIds)")
    fun getBaseItemsLive(itemIds: List<Long>): Flow<List<DbItem>>

    @Query("SELECT * FROM item_to_list_mapping WHERE listId = :listId AND createdBy = :createdBy")
    fun getItemToListLive(
        listId: Long,
        createdBy: Long,
    ): Flow<List<ItemToList>>

    @Query("SELECT * FROM list_table WHERE listId = :listId AND createdBy = :createdBy")
    fun getBaseListLive(
        listId: Long,
        createdBy: Long,
    ): Flow<DbShoppingList>

    fun getItemsLive(
        listId: Long,
        createdBy: Long,
    ): Flow<List<AppItem>> {
        val itemMappings = getItemToListLive(listId, createdBy)
        return itemMappings.flatMapLatest { mappings ->
            val baseItems = getBaseItemsLive(mappings.map { mapping -> mapping.itemId })
            baseItems.map { items ->
                mappings.mapIndexed { index, mapping ->
                    AppItem.fromBaseAndMapping(items[index], mapping)
                }
            }
        }
    }

    fun getListLive(
        listId: Long,
        createdBy: Long,
    ): Flow<ShoppingList> {
        val baseList = getBaseListLive(listId, createdBy)
        val listItems = getItemsLive(listId, createdBy)
        return combine(baseList, listItems) { list, items ->
            ShoppingList(
                listId = list.listId,
                createdBy = ListCreator(createdBy, ""),
                title = list.title,
                synchronized = list.synchronized,
                items = items.toMutableList(),
            )
        }
    }

    @Query("SELECT COUNT(*) FROM list_table")
    fun getLatestListIdLive(): LiveData<Long>

    @Query("SELECT MAX(listId) FROM list_table")
    fun getLatestListId(): Long

    @Query("SELECT MAX(listId) FROM list_table WHERE listId = 0 OR createdBy = :key")
    fun getLatestOwnListId(key: Long): Long

    @Query("SELECT MAX(listId) FROM list_table WHERE createdBy = :key")
    fun getLatestListId(key: Long): Long

    @Query("SELECT * FROM list_table ORDER BY listId ASC")
    fun getShoppingListsLive(): LiveData<List<DbShoppingList>>

    @Query("SELECT * FROM list_table ORDER BY listId ASC")
    fun getShoppingLists(): List<DbShoppingList>

    @Query("SELECT * FROM list_table WHERE listId = :key AND createdBy = :keyCreatedBy")
    fun getShoppingListLive(
        key: Long,
        keyCreatedBy: Long,
    ): LiveData<DbShoppingList>

    @Query("SELECT * FROM list_table WHERE listId = :key AND createdBy = :keyCreatedBy")
    fun getShoppingList(
        key: Long,
        keyCreatedBy: Long,
    ): DbShoppingList?

    @Query("SELECT * FROM list_table WHERE createdBy = :createdBy")
    fun getOwnShoppingLists(createdBy: Long): List<DbShoppingList>

    @Query("SELECT EXISTS (SELECT * FROM list_table WHERE listId = :key AND createdBy = :keyCreatedBy)")
    fun exists(
        key: Long,
        keyCreatedBy: Long,
    ): Boolean

    @Query("UPDATE list_table SET createdBy = 0 WHERE createdBy = :createdBy")
    fun resetOwnListsCreatorId(createdBy: Long)

    @Query("UPDATE list_table SET createdBy = :createdBy WHERE createdBy = 0")
    fun setListWithOfflineCreatorToOnlineId(createdBy: Long)
}
