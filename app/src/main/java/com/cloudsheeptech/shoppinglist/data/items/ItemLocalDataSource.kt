package com.cloudsheeptech.shoppinglist.data.items

import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ItemLocalDataSource(val database: ShoppingListDatabase) {

    private val itemDao = database.itemDao()

    suspend fun read(itemId: Long) : DbItem? {
        var dbItem : DbItem? = null
        withContext(Dispatchers.IO) {
            val dbItemList = readByIds(listOf(itemId))
            if (dbItemList.isEmpty())
                return@withContext
            dbItem = dbItemList[0]
        }
        return dbItem
    }

    suspend fun readByIds(itemIds: List<Long>) : List<DbItem> {
        val finalList = mutableListOf<DbItem>()
        withContext(Dispatchers.IO) {
            val dbItems =  itemDao.getItems(itemIds)
            finalList.addAll(dbItems)
        }
        return finalList
    }

    suspend fun readByName(itemName: String) : List<DbItem> {
        val finalList = mutableListOf<DbItem>()
        withContext(Dispatchers.IO) {
            val matchingDbItems = itemDao.getItemsFromName(itemName)
            finalList.addAll(matchingDbItems)
        }
        return finalList
    }

    suspend fun create(item: DbItem) : DbItem {
        withContext(Dispatchers.IO) {
            val locaItemId = itemDao.insertItem(item)
            item.id = locaItemId
        }
        return item
    }

    suspend fun update(item: DbItem) {
        withContext(Dispatchers.IO) {
            itemDao.updateItem(item)
        }
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