package com.cloudsheeptech.shoppinglist.data.items

import android.util.Log
import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepository
    @Inject
    constructor(
        private val localDataSource: ItemLocalDataSource,
    ) {
        /**
         * Searches for the item stored under this id
         * @return the item if found or null
         */
        suspend fun read(itemId: Long): DbItem? {
            val itemList = readByIds(listOf(itemId))
            if (itemList.isEmpty()) {
                return null
            }
            return itemList[0]
        }

        /**
         * This function tries to retrieve an item with the given
         * name. If none is found a new item is created
         * @return An item with similar name
         */
        suspend fun readOrCreate(itemName: String): DbItem {
            val localItem =
                withContext(Dispatchers.IO) {
                    var localItem = localDataSource.readByExactName(itemName)
                    if (localItem == null) {
                        Log.d("ItemRepository", "Item with name $itemName not found. Creating new item")
                        localItem = DbItem(0L, itemName, "")
                        localItem.id = localDataSource.create(localItem)
                    }
                    return@withContext localItem
                }
            return localItem
        }

        /**
         * Returns all items queried for that can be found.
         * @return a list of all items that can be found or an empty list
         */
        suspend fun readByIds(itemIds: List<Long>): List<DbItem> {
            if (itemIds.isEmpty()) {
                return emptyList()
            }
            val itemsFoundForIds = localDataSource.readByIds(itemIds)
            return itemsFoundForIds
        }

        /**
         * Searches for all items that contain the queried name.
         * Requires at least one character to start the query
         * @return a list of all matching items (containing the search pattern)
         */
        suspend fun readByName(itemName: String): List<DbItem> {
            if (itemName.isEmpty()) {
                return emptyList()
            }
            val itemsFoundForName = localDataSource.readByName(itemName)
            return itemsFoundForName
        }

        fun readForListLive(
            listId: Long,
            createdBy: Long,
        ): LiveData<List<AppItem>> = localDataSource.readForListLive(listId, createdBy)

        fun readForList(
            listId: Long,
            createdBy: Long,
        ): List<AppItem> = localDataSource.readForList(listId, createdBy)

        /**
         * Stores the new item but only
         * item does not exist
         * @throws IllegalStateException if the item did exist
         * @return the id of the new item
         */
        suspend fun create(newItem: DbItem): Long {
            val createdItem = localDataSource.create(newItem)
            return createdItem
        }

        /**
         * Updates an item if it already exists.
         * @throws IllegalStateException if the item did not exist
         * @return the id of the update item
         */
        suspend fun update(item: DbItem): Long = localDataSource.update(item)

        /**
         * Tries to delete the item if it exists.
         * If the item does not exist, nothing happens
         */
        suspend fun delete(item: DbItem) {
            delete(item.id)
        }

        suspend fun delete(itemId: Long) {
            localDataSource.delete(itemId)
        }
    }
