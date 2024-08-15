package com.cloudsheeptech.shoppinglist.data.list

import android.util.Log
import androidx.lifecycle.LiveData
import com.cloudsheeptech.shoppinglist.data.ListCreator
import com.cloudsheeptech.shoppinglist.data.itemToListMapping.ListMapping
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.itemToListMapping.ItemToListRepository
import com.cloudsheeptech.shoppinglist.data.items.ApiItem
import com.cloudsheeptech.shoppinglist.data.items.DbItem
import com.cloudsheeptech.shoppinglist.data.items.ItemRepository
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import javax.inject.Inject

/**
 * Handles the storage and retrieval of the list that is used by the application into
 * the local database in a way that makes storage possible.
 */
class ShoppingListLocalDataSource @Inject constructor(
    private val database: ShoppingListDatabase,
    private val userRepository: AppUserRepository,
    private val itemRepository: ItemRepository,
    private val itemToListRepository: ItemToListRepository,
) {

    private val listDao = database.shoppingListDao()

    private fun ApiItem.toDbItem() : DbItem {
        val dbItem = DbItem(
            id = 0L, // Auto generated
            name = this.name,
            icon = this.icon,
        )
        return dbItem
    }

    private fun ApiItem.toListMapping(itemId: Long, listId: Long, createdBy: Long) : ListMapping {
        val listMapping = ListMapping(
            ID = 0L, // Auto generated
            ItemID = itemId,
            ListID = listId,
            CreatedBy = createdBy,
            Quantity = this.quantity,
            Checked = this.checked,
            AddedBy = this.addedBy
        )
        return listMapping
    }

    private fun ApiShoppingList.toDbList() : Pair<DbShoppingList, List<DbItem>> {
        val dbList = DbShoppingList(
            listId = this.listId,
            title = this.title,
            createdBy = this.createdBy.onlineId,
            createdByName = this.createdBy.username,
            lastUpdated = this.lastUpdated
        )
        val dbItems = this.items.map { item -> item.toDbItem() }
        return Pair(dbList, dbItems)
    }

    // Ignore for now
    private fun DbShoppingList.toApiList() : ApiShoppingList {
        val creator = userRepository.read() ?: throw IllegalStateException("user null after creation screen")
        val apiList = ApiShoppingList(
            listId = this.listId,
            title = this.title,
            createdBy = ListCreator(creator.OnlineID, creator.Username),
            createdAt = this.lastUpdated,
            lastUpdated = this.lastUpdated,
            items = mutableListOf(),
        )
        return apiList
    }

    private fun ListMapping.toApiItem() : ApiItem {
        val apiItem = ApiItem(
            name = "",
            icon = "",
            quantity = this.Quantity,
            checked = this.Checked,
            addedBy = this.AddedBy,
        )
        return apiItem
    }

    private suspend fun getUniqueShoppingListID(createdBy: Long) : Long {
        // Starting the local listIds with 1
        var latestId = 1L
        withContext(Dispatchers.IO) {
            latestId = listDao.getLatestListId(createdBy).plus(1L)
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
    suspend fun create(list: ApiShoppingList) : Long {
        var insertedListId = list.listId
        withContext(Dispatchers.IO) {
            // Differentiate between new and existing list
            val existingList = listDao.getShoppingList(list.listId, list.createdBy.onlineId)
            if (existingList != null) {
                throw IllegalArgumentException("list already exists")
            }
            // Update the id to the latest available ID
            val copiedList = list.copy()
            copiedList.listId = getUniqueShoppingListID(list.createdBy.onlineId)
            // We split the list from one single object into 2 parts: basic list and items
            val (dbList, items) = copiedList.toDbList()
            insertedListId = listDao.insertList(dbList)
            if (insertedListId != copiedList.listId) {
                Log.e("ShoppingListLocalDataSource", "Database assigned a new ID during insertion which should never happen.")
                return@withContext
            }
            // Insert the items in case we received a remote list which is already populated
            if (items.isNotEmpty()) {
                items.forEachIndexed { index, item ->
                    var listMapping : ListMapping
                    try {
                        val insertedItemId = itemRepository.create(item)
                        val apiItem = copiedList.items[index]
                        listMapping = apiItem.toListMapping(insertedItemId, list.listId, list.createdBy.onlineId)
                    } catch (ex: IllegalStateException) {
                        val updatedItemId = itemRepository.update(item)
                        val apiItem = copiedList.items[index]
                        listMapping = apiItem.toListMapping(updatedItemId, list.listId, list.createdBy.onlineId)
                    }
                    try {
                        val updatedMappingId = itemToListRepository.create(listMapping)
                    } catch (ex: IllegalStateException) {
                        val updatedMappingId = itemToListRepository.update(listMapping)
                    } catch (ex: IllegalArgumentException) {
                        val updatedMappingId = itemToListRepository.update(listMapping)
                    }
                }
            }
            Log.d("ShoppingListHandler", "Inserted list $insertedListId into database")
            return@withContext
        }
        return insertedListId
    }

    /**
     * Reads the list information from the database and returns the list
     * in case information are found
     * @throws IllegalStateException in case the item information for the list is not found
     * @return DbShoppingList in case the list is found or null
     */
    @Throws(IllegalStateException::class)
    suspend fun read(listId: Long, createdBy: Long): ApiShoppingList? {
        var offlineList : ApiShoppingList? = null
        withContext(Dispatchers.IO) {
            val shoppingListBase = listDao.getShoppingList(listId, createdBy) ?: return@withContext
            offlineList = shoppingListBase.toApiList()
            // Combine the mapping and item information to craft the item list
            val mappings = itemToListRepository.read(listId, createdBy)
            if (mappings.isEmpty())
                return@withContext
            val apiItems = mappings.map { mapping ->
                val apiItem = mapping.toApiItem()
                val itemInfo = itemRepository.read(mapping.ItemID) ?: throw IllegalStateException("mapped item not stored in database")
                apiItem.name = itemInfo.name
                apiItem.icon = itemInfo.icon
                apiItem
            }
            offlineList!!.items.addAll(apiItems)
        }
        return offlineList
    }

    /**
     * Reads all list information from the database, including own
     * an foreign lists.
     * @return a list of all found lists
     */
    suspend fun readAll() : List<ApiShoppingList> {
        val allLists = mutableListOf<ApiShoppingList>()
        withContext(Dispatchers.IO) {
            val dbLists = listDao.getShoppingLists()
            if (dbLists.isEmpty())
                return@withContext
            val convertedLists = dbLists.map { list -> read(list.listId, list.createdBy) }
            allLists.addAll(convertedLists.filterNotNull())
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
    suspend fun update(updatedList: ApiShoppingList) {
        if (updatedList.listId == 0L) {
            throw IllegalArgumentException("list does not exist in the database")
        }
        withContext(Dispatchers.IO) {
            val existingList = listDao.getShoppingList(updatedList.listId, updatedList.createdBy.onlineId)
                ?: throw IllegalArgumentException("list does not exist")
            // Compare last edited value
            // TODO: Make this more elaborate and allow to integrate updates when the
            // remote and locally changed values in the list
            if (existingList.lastUpdated.isAfter(updatedList.lastUpdated)) {
                Log.i("ShoppingListLocalDataSource", "Updating is skipped because the last list update is newer than the currently applied update")
                return@withContext
            }

            // FIXME: Instead of saving the update as truth, compare and make more
            // detailed comparison
            val (dbList, items) = updatedList.toDbList()
            dbList.lastUpdated = OffsetDateTime.now()
            listDao.updateList(dbList)
            itemToListRepository.deleteAllMappingsForList(updatedList.listId, updatedList.createdBy.onlineId)
            items.forEachIndexed { index, item ->
                // Might be a new item
                var listMapping : ListMapping
                try {
                    val insertedItemId = itemRepository.create(item)
                    val apiItem = updatedList.items[index]
                    listMapping = apiItem.toListMapping(insertedItemId, updatedList.listId, updatedList.createdBy.onlineId)
                } catch (ex: IllegalStateException) {
                    val updatedItemId = itemRepository.update(item)
                    val apiItem = updatedList.items[index]
                    listMapping = apiItem.toListMapping(updatedItemId, updatedList.listId, updatedList.createdBy.onlineId)
                }
                try {
                    itemToListRepository.create(listMapping)
                } catch (ex : IllegalStateException) {
                    // THIS SHOULD NEVER HAPPENS, SINCE WE DELETED ALL MAPPINGS FOR THIS LIST
                    itemToListRepository.update(listMapping)
                }
            }
            Log.d("ShoppingListHandler", "Updated list ${updatedList.listId} in database")
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