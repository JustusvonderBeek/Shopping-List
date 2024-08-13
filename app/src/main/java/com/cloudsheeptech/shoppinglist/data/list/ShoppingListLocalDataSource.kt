package com.cloudsheeptech.shoppinglist.data.list

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cloudsheeptech.shoppinglist.data.DbShoppingList
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

class ShoppingListLocalDataSource(database: ShoppingListDatabase) {

    private val listDao = database.shoppingListDao()

    private suspend fun getUniqueShoppingListID() : Long {
        // Starting the local listIds with 1
        var latestId = 1L
        withContext(Dispatchers.IO) {
            latestId = listDao.getLatestListId().plus(1L)
        }
        Log.d("DatabaseListHandler", "Generated new list Id: $latestId")
        return latestId
    }

    /**
     * This function creates a new shopping list in the local database
     * @return The id of the newly created list
     * @exception IllegalArgumentException in case the list already exists
     */
    @Throws(IllegalArgumentException::class)
    suspend fun create(list: DbShoppingList) : Long {
        var insertedListId = list.listId
        withContext(Dispatchers.IO) {
            // Differentiate between new and existing list
            val existingList = listDao.getShoppingList(list.listId, list.createdBy)
            if (insertedListId != 0L || existingList != null) {
                throw IllegalArgumentException("list already exists")
            }
            // Update the id to the latest available ID
            val copiedList = list.copy()
            copiedList.listId = getUniqueShoppingListID()
            insertedListId = listDao.insertList(copiedList)
            if (insertedListId != copiedList.listId) {
                Log.e("ShoppingListLocalDataSource", "Database assigned a new ID during insertion which should never happen.")
                return@withContext
            }
            Log.d("ShoppingListHandler", "Inserted list $insertedListId into database")
            return@withContext
        }
        return insertedListId
    }

    /**
     * Reads the list information from the database and returns the list
     * in case information are found
     * @return DbShoppingList in case the list is found or null
     */
    suspend fun read(listId: Long, createdBy: Long): DbShoppingList? {
        var offlineList : DbShoppingList? = null
        withContext(Dispatchers.IO) {
            val list = listDao.getShoppingList(listId, createdBy) ?: return@withContext
            offlineList = list
        }
        return offlineList
    }

    /**
     * Reads all list information from the database, including own
     * an foreign lists.
     * @return a list of all found lists
     */
    suspend fun readAll() : List<DbShoppingList> {
        val allLists = mutableListOf<DbShoppingList>()
        withContext(Dispatchers.IO) {
            val dbLists = listDao.getShoppingLists()
            allLists.addAll(dbLists)
        }
        return allLists
    }

    /**
     * Currently not implemented
     * @throws NotImplementedError
     */
    fun readLive(listId: Long, createdBy: Long) : LiveData<DbShoppingList> {
        throw NotImplementedError("readLive not implemented yet")
//        return MutableLiveData<DbShoppingList>()
    }

    /**
     * Similar to create but expecting that the list already exists
     * @throws IllegalArgumentException if the list does not exist
     */
    @Throws(IllegalArgumentException::class)
    suspend fun update(updatedList: DbShoppingList) {
        if (updatedList.listId == 0L) {
            throw IllegalArgumentException("list does not exist in the database")
        }
        withContext(Dispatchers.IO) {
            try {
                val existingList = listDao.getShoppingList(updatedList.listId, updatedList.createdBy)
                    ?: throw IllegalArgumentException("list does not exist")
                // Compare last edited value
                if (existingList.lastUpdated.isAfter(updatedList.lastUpdated)) {
                    Log.i("ShoppingListLocalDataSource", "Updating is skipped because the last list update is newer than the currently applied update")
                    return@withContext
                }
                listDao.updateList(updatedList)
                Log.d("ShoppingListHandler", "Updated list ${updatedList.listId} in database")
            } catch (ex : DateTimeParseException) {
                Log.w("ShoppingListHandler", "Failed to parse time (${updatedList.lastUpdated}): $ex")
            } catch (ex : Exception) {
                // Most likely because the instant failed to parse. Don't updated in this case
                Log.w("ShoppingListHandler", "Failed to update list: $ex")
            }
        }
    }

    /**
     * Removes the list from the local data storage.
     * Returns immediately if the list cannot be found
     */
    suspend fun delete(listId: Long, createdBy: Long) {
        withContext(Dispatchers.IO) {
            listDao.deleteList(listId, createdBy)
        }
    }


}