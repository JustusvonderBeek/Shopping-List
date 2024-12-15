package com.cloudsheeptech.shoppinglist.data.items

import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemLocalDataSource @Inject constructor(val database: ShoppingListDatabase) {

    private val itemDao = database.itemDao()

    suspend fun read(itemId: Long): DbItem? {
        var dbItem: DbItem? = null
        withContext(Dispatchers.IO) {
            val dbItemList = readByIds(listOf(itemId))
            if (dbItemList.isEmpty())
                return@withContext
            dbItem = dbItemList[0]
        }
        return dbItem
    }

    suspend fun readByIds(itemIds: List<Long>): List<DbItem> {
        val finalList = mutableListOf<DbItem>()
        withContext(Dispatchers.IO) {
            val dbItems = itemDao.getItems(itemIds)
            finalList.addAll(dbItems)
        }
        return finalList
    }

    suspend fun readByName(itemName: String): List<DbItem> {
        val finalList = mutableListOf<DbItem>()
        withContext(Dispatchers.IO) {
            val matchingDbItems = itemDao.getItemsFromName(itemName)
            finalList.addAll(matchingDbItems)
        }
        return finalList
    }

    fun readForListLive(listId: Long) = itemDao.getItemsWithQuantityInListLive(listId)

    /**
     * Creating a new item in the database.
     * Creates the item only if no other item with the same characters (case-insensitive)
     * exist
     * @throws IllegalStateException when the item already exists
     * @return the id of the new item
     */
    @Throws(IllegalStateException::class)
    suspend fun create(item: DbItem): Long {
        var dbItemId = 0L
        withContext(Dispatchers.IO) {
            val possibleItems = readByName(item.name)
            if (possibleItems.isNotEmpty()) {
                val matchingItem = possibleItems.firstOrNull { dbItem -> dbItem == item }
                if (matchingItem != null)
                    throw IllegalStateException("item already exists in database")
            }
            dbItemId = itemDao.insertItem(item)
        }
        return dbItemId
    }

    /**
     * Updates the item if it exists
     * @throws IllegalStateException if the item did not exist
     * @return the id of the update item
     */
    @Throws(IllegalStateException::class)
    suspend fun update(item: DbItem): Long {
        var itemId = 0L
        withContext(Dispatchers.IO) {
            val possibleItems = itemDao.getItemsFromName(item.name)
            if (possibleItems.isEmpty())
                throw IllegalStateException("item is not in database")
            // As it doesn't make sense to update the name but only the icon
            // this comparison holds
            val matchingItem = possibleItems.firstOrNull { dbItem -> dbItem == item }
            if (matchingItem == null)
                throw IllegalStateException("item is not in database")
            // Fix an error in case the item did exist, but was received from
            // remote without a matching local id
            // and prevent clustering with too many items
            item.id = matchingItem.id
            itemId = matchingItem.id
            itemDao.updateItem(item)
        }
        return itemId
    }

    suspend fun delete(item: DbItem) {
        delete(item.id)
    }

    suspend fun delete(itemId: Long) {
        withContext(Dispatchers.IO) {
            itemDao.deleteItem(itemId)
        }
    }

}