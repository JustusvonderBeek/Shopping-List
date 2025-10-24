package com.cloudsheeptech.shoppinglist.list.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.cloudsheeptech.shoppinglist.list.model.AppItem
import com.cloudsheeptech.shoppinglist.list.model.DbItem
import com.cloudsheeptech.shoppinglist.list.model.DbShoppingList
import com.cloudsheeptech.shoppinglist.list.model.ItemToList
import com.cloudsheeptech.shoppinglist.list.model.ListCreator
import com.cloudsheeptech.shoppinglist.list.model.QuantityType
import com.cloudsheeptech.shoppinglist.list.model.ShoppingList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
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
    @Insert(onConflict = OnConflictStrategy.Companion.ABORT)
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

    @Insert(onConflict = OnConflictStrategy.Companion.ABORT)
    fun insertDbList(dbList: DbShoppingList): Long

    @Insert(onConflict = OnConflictStrategy.Companion.ABORT)
    fun insertItem(item: DbItem): Long

    @Insert(onConflict = OnConflictStrategy.Companion.ABORT)
    fun insertItemMapping(itemMapping: ItemToList): Long

    /**
     * @return The list including items in the App format or null if not found
     */
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
                synchronized = baseList.lastSynchronized,
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
                AppItem.Companion.fromBaseAndMapping(item, mapping)
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

    suspend fun getAllLists(): List<ShoppingList> {
        val baseLists = getBaseLists()
        val allLists = mutableListOf<ShoppingList>()
        for (list in baseLists) {
            val listItems = getItems(list.listId, list.createdBy)
            allLists.add(
                ShoppingList(
                    listId = list.listId,
                    createdBy = ListCreator(list.createdBy, "TODO"),
                    title = list.title,
                    synchronized = list.lastSynchronized,
                    items = listItems.toMutableList(),
                ),
            )
        }
        return allLists
    }

    @Query("SELECT * FROM list_table")
    fun getBaseLists(): List<DbShoppingList>

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

    suspend fun addItemById(
        itemId: Long,
        userId: Long,
        listId: Long,
        createdBy: Long,
    ) {
        val item = getBaseItems(listOf(itemId))
        if (item.size != 1) {
            throw IllegalArgumentException("Item with id $itemId not found")
        }
        val itemMapping =
            ItemToList(
                itemId = itemId,
                listId = listId,
                createdBy = createdBy,
                quantity = 1L,
                quantityType = QuantityType.PIECES,
                checked = false,
                addedBy = userId,
            )
        insertItemMapping(itemMapping)
    }

    @Transaction
    suspend fun removeItem(
        item: AppItem,
        listId: Long,
        createdBy: Long,
    ) {
        val (baseItem, itemMapping) = item.toEntities(Pair(listId, createdBy))
        if (item.id == null || item.id!! <= 0L) {
            return
        }
        removeMapping(itemMapping)
    }

    @Delete
    fun removeMapping(itemMapping: ItemToList)

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

    @Query("UPDATE list_table SET title = :title WHERE listId = :listId AND createdBy = :createdBy")
    fun updateListTitle(
        title: String,
        listId: Long,
        createdBy: Long,
    )

    @Query("DELETE FROM list_table WHERE listId = :listId AND createdBy = :createdBy")
    fun deleteList(
        listId: Long,
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
                    AppItem.Companion.fromBaseAndMapping(items[index], mapping)
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
                synchronized = list.lastSynchronized,
                items = items.toMutableList(),
            )
        }
    }

    fun getAllListsLive(): Flow<List<ShoppingList>> {
        val allBaseLists = getAllBaseListsLive()
        return allBaseLists.flatMapLatest { lists ->
            return@flatMapLatest flow {
                lists.map { list ->
                    val items = getItems(list.listId, list.createdBy)
                    ShoppingList(
                        listId = list.listId,
                        createdBy = ListCreator(list.createdBy, ""),
                        title = list.title,
                        synchronized = list.lastSynchronized,
                        items = items.toMutableList(),
                    )
                }
            }
        }
    }

    @Query("SELECT * FROM list_table")
    fun getAllBaseListsLive(): Flow<List<DbShoppingList>>

    @Query("SELECT 1 FROM list_table WHERE listId = :listId AND createdBy = :createdBy LIMIT 1")
    fun listExists(
        listId: Long,
        createdBy: Long,
    ): Boolean

    @Query("SELECT MAX(listId) FROM list_table WHERE createdBy = :createdBy")
    fun getLatestListId(createdBy: Long): Long

    @Transaction
    suspend fun updatedCreatedBy(
        updatedOnlineId: Long,
        previousOnlineId: Long = 0L,
    ) {
        updatedCreatedByForBaseList(previousOnlineId, updatedOnlineId)
        updatedAddedByForItems(previousOnlineId, updatedOnlineId)
    }

    @Query("UPDATE list_table SET createdBy = :updatedOnlineId WHERE createdBy = :previousOnlineId")
    fun updatedCreatedByForBaseList(
        previousOnlineId: Long,
        updatedOnlineId: Long,
    )

    @Query("UPDATE item_to_list_mapping SET addedBy = :updatedOnlineId WHERE addedBy = :previousOnlineId")
    fun updatedAddedByForItems(
        previousOnlineId: Long,
        updatedOnlineId: Long,
    )
}
