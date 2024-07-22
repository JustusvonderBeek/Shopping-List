package com.cloudsheeptech.shoppinglist.data.handling

import android.util.Log
import com.cloudsheeptech.shoppinglist.data.ShoppingList
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

class DatabaseListHandler(database: ShoppingListDatabase) : ListHandler(database) {

    private val listDao = database.shoppingListDao()
    private val itemDao = database.itemDao()
    private val mappingDao = database.mappingDao()
    private val onlineUserDao = database.onlineUserDao()
    private val shareDao = database.sharedDao()

    private suspend fun getUniqueShoppingListID() : Long {
        var latestId = 1L
        withContext(Dispatchers.IO) {
            latestId = (listDao.getLatestListId()?.plus(1L)) ?: 1L
        }
        Log.d("DatabaseListHandler", "Decided for value: $latestId")
        return latestId
    }

    override suspend fun storeShoppingList(list: ShoppingList) : Long {
        var insertedListId = list.ID
        withContext(Dispatchers.IO) {
            // Differentiate between new and existing list
            val existingList = listDao.getShoppingList(list.ID, list.CreatedByID)
            if (insertedListId == 0L || existingList == null) {
                // Update the id to the latest available ID
                val copiedList = list.copy()
                copiedList.ID = getUniqueShoppingListID()
                insertedListId = listDao.insertList(copiedList)
                Log.d("ShoppingListHandler", "Inserted list $insertedListId into database")
                return@withContext
            }
            // In this case, the ID is already existing and set in the variable returned
            try {
                // Compare last edited value
                if (existingList.LastEdited.isAfter(list.LastEdited))
                    return@withContext
                listDao.updateList(list)
                Log.d("ShoppingListHandler", "Updated list ${list.ID} in database")
            } catch (ex : DateTimeParseException) {
                Log.w("ShoppingListHandler", "Failed to parse time (${list.LastEdited}) and (${existingList.LastEdited}): $ex")
            } catch (ex : Exception) {
                // Most likely because the instant failed to parse. Don't updated in this case
                Log.w("ShoppingListHandler", "Failed to update list: $ex")
            }
        }
        return insertedListId
    }

    override suspend fun retrieveShoppingList(listId: Long, createdBy: Long): ShoppingList? {
        var offlineList : ShoppingList? = null
        withContext(Dispatchers.IO) {
            val list = listDao.getShoppingList(listId, createdBy) ?: return@withContext
            offlineList = list
        }
        return offlineList
    }

    override suspend fun updateLastEditedNow(listId: Long, createdBy: Long): ShoppingList? {
        var updatedList: ShoppingList? = null
        withContext(Dispatchers.IO) {
            val existingList = retrieveShoppingList(listId, createdBy) ?: return@withContext
            existingList.LastEdited = OffsetDateTime.now()
            updatedList = existingList
            storeShoppingList(updatedList!!)
        }
        return updatedList
    }

    override suspend fun deleteShoppingList(listId: Long, createdBy: Long) {
        withContext(Dispatchers.IO) {
            listDao.deleteList(listId, createdBy)
        }
    }


}