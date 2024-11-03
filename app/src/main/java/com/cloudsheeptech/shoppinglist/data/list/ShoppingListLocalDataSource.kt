package com.cloudsheeptech.shoppinglist.data.list

import android.util.Log
import androidx.lifecycle.LiveData
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.itemToListMapping.ItemToListRepository
import com.cloudsheeptech.shoppinglist.data.itemToListMapping.ListMapping
import com.cloudsheeptech.shoppinglist.data.items.ApiItem
import com.cloudsheeptech.shoppinglist.data.items.AppItem
import com.cloudsheeptech.shoppinglist.data.items.DbItem
import com.cloudsheeptech.shoppinglist.data.items.ItemRepository
import com.cloudsheeptech.shoppinglist.data.onlineUser.ListCreator
import com.cloudsheeptech.shoppinglist.data.recipe.ApiIngredient
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Handles the storage and retrieval of the list that is used by the application into
 * the local database in a way that makes storage possible.
 */
@Singleton
class ShoppingListLocalDataSource
    @Inject
    constructor(
        private val database: ShoppingListDatabase,
        private val userRepository: AppUserRepository,
        private val itemRepository: ItemRepository,
        private val itemToListRepository: ItemToListRepository,
    ) {
        private val listDao = database.shoppingListDao()

        private fun AppItem.toApiItem(): ApiItem =
            ApiItem(
                name = this.name,
                icon = this.icon,
                quantity = this.quantity,
                checked = this.checked,
                addedBy = this.addedBy,
            )

        private fun ApiItem.toDbItem(): DbItem {
            val dbItem =
                DbItem(
                    id = 0L, // Auto generated
                    name = this.name,
                    icon = this.icon,
                )
            return dbItem
        }

        private fun DbItem.toAppItem(onlineId: Long): AppItem =
            AppItem(
                id = this.id,
                name = this.name,
                icon = this.icon,
                quantity = 1L,
                checked = false,
                addedBy = onlineId,
            )

        private fun ApiItem.toListMapping(
            itemId: Long,
            listId: Long,
            createdBy: Long,
        ): ListMapping {
            val listMapping =
                ListMapping(
                    ID = 0L, // Auto generated
                    ItemID = itemId,
                    ListID = listId,
                    CreatedBy = createdBy,
                    Quantity = this.quantity,
                    Checked = this.checked,
                    AddedBy = this.addedBy,
                )
            return listMapping
        }

        private fun ApiShoppingList.toDbList(): Pair<DbShoppingList, List<DbItem>> {
            val dbList =
                DbShoppingList(
                    listId = this.listId,
                    title = this.title,
                    createdBy = this.createdBy.onlineId,
                    createdByName = this.createdBy.username,
                    lastUpdated = this.lastUpdated,
                )
            val dbItems = this.items.map { item -> item.toDbItem() }
            return Pair(dbList, dbItems)
        }

        // Ignore for now
        private fun DbShoppingList.toApiList(): ApiShoppingList {
            val creator =
                userRepository.read() ?: throw IllegalStateException("user null after creation screen")
            val apiList =
                ApiShoppingList(
                    listId = this.listId,
                    title = this.title,
                    createdBy = ListCreator(creator.OnlineID, creator.Username),
                    createdAt = this.lastUpdated,
                    lastUpdated = this.lastUpdated,
                    items = mutableListOf(),
                )
            return apiList
        }

        private fun ListMapping.toApiItem(): ApiItem {
            val apiItem =
                ApiItem(
                    name = "",
                    icon = "",
                    quantity = this.Quantity,
                    checked = this.Checked,
                    addedBy = this.AddedBy,
                )
            return apiItem
        }

        private suspend fun getUniqueShoppingListID(createdBy: Long): Long {
            // Starting the local listIds with 1
            var latestId = 1L
            withContext(Dispatchers.IO) {
                latestId = listDao.getLatestListId(createdBy).plus(1L)
            }
            Log.d("DatabaseListHandler", "Generated new list Id: $latestId")
            return latestId
        }

        private suspend fun insertItems(
            items: List<DbItem>,
            insertedList: ApiShoppingList,
        ) {
            items.forEachIndexed { index, item ->
                var listMapping: ListMapping
                try {
                    val insertedItemId = itemRepository.create(item)
                    val apiItem = insertedList.items[index]
                    listMapping =
                        apiItem.toListMapping(
                            insertedItemId,
                            insertedList.listId,
                            insertedList.createdBy.onlineId,
                        )
                } catch (ex: IllegalStateException) {
                    val updatedItemId = itemRepository.update(item)
                    val apiItem = insertedList.items[index]
                    listMapping =
                        apiItem.toListMapping(
                            updatedItemId,
                            insertedList.listId,
                            insertedList.createdBy.onlineId,
                        )
                }
                try {
                    itemToListRepository.create(listMapping)
                } catch (ex: IllegalStateException) {
                    itemToListRepository.update(listMapping)
                } catch (ex: IllegalArgumentException) {
                    itemToListRepository.update(listMapping)
                }
            }
        }

        /**
         * This function creates a new shopping list in the local database
         * @return The id of the newly created list
         * @exception IllegalArgumentException in case the list already exists
         */
        @Throws(IllegalArgumentException::class)
        suspend fun create(list: ApiShoppingList): Long {
            var insertedListId = list.listId
            withContext(Dispatchers.IO) {
                // Differentiate between new and existing list
                if (listDao.exists(list.listId, list.createdBy.onlineId)) {
                    throw IllegalArgumentException("list '${list.title}' already exists")
                }
                // Update the id to the latest available ID
                val copiedList = list.copy()
                val user =
                    userRepository.read()
                        ?: throw IllegalStateException("user not set after login screen")
                // In case the list comes from online, we don't want to change the id
                if (copiedList.listId == 0L && list.createdBy.onlineId == user.OnlineID) {
                    copiedList.listId = getUniqueShoppingListID(list.createdBy.onlineId)
                    insertedListId = copiedList.listId
                }
                // We split the list from one single object into 2 parts: basic list and items
                val (dbList, items) = copiedList.toDbList()
                val totalTableRows = listDao.insertList(dbList)
                Log.d("ShoppingListHandler", "Table contains $totalTableRows list(s) after insertion")
                // Insert the items in case we received a remote list which is already populated
                insertItems(items, copiedList)
                Log.d(
                    "ShoppingListHandler",
                    "Inserted list $insertedListId from ${list.createdBy.onlineId} with ${items.size} items into database",
                )
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
        suspend fun read(
            listId: Long,
            createdBy: Long,
        ): ApiShoppingList? {
            var offlineList: ApiShoppingList? = null
            withContext(Dispatchers.IO) {
                val shoppingListBase = listDao.getShoppingList(listId, createdBy) ?: return@withContext
                offlineList = shoppingListBase.toApiList()
                // Combine the mapping and item information to craft the item list
                val mappings = itemToListRepository.read(listId, createdBy)
                if (mappings.isEmpty()) {
                    Log.d(
                        "ShoppingListLocalDataSource",
                        "No items found for list $listId from $createdBy",
                    )
                    return@withContext
                }
                val apiItems =
                    mappings.map { mapping ->
                        val apiItem = mapping.toApiItem()
                        val itemInfo =
                            itemRepository.read(mapping.ItemID)
                                ?: throw IllegalStateException("mapped item not stored in database")
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
        suspend fun readAll(): List<ApiShoppingList> {
            val allLists = mutableListOf<ApiShoppingList>()
            withContext(Dispatchers.IO) {
                val dbLists = listDao.getShoppingLists()
                if (dbLists.isEmpty()) {
                    return@withContext
                }
                val convertedLists = dbLists.map { list -> read(list.listId, list.createdBy) }
                allLists.addAll(convertedLists.filterNotNull())
            }
            return allLists
        }

        fun readAllLive(): LiveData<List<DbShoppingList>> = listDao.getShoppingListsLive()

        /**
         * Function making the insertion and update process more easy.
         * @return true if the list exists, otherwise false
         */
        suspend fun exists(
            listId: Long,
            createdBy: Long,
        ): Boolean {
            var exists: Boolean
            withContext(Dispatchers.IO) {
                exists = listDao.exists(listId, createdBy)
            }
            return exists
        }

        /**
         * Currently not implemented
         * @throws NotImplementedError
         */
        fun readLive(
            listId: Long,
            createdBy: Long,
        ): LiveData<DbShoppingList> {
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
                val existingList =
                    listDao.getShoppingList(updatedList.listId, updatedList.createdBy.onlineId)
                        ?: throw IllegalArgumentException("list does not exist")

                // Fix the createdBy == 0 if the user is already logged in online
                val user = userRepository.read()
                if (existingList.createdBy == 0L && user != null && user.OnlineID != 0L) {
                }

                // Compare last edited value
                // TODO: Make this more elaborate and allow to integrate updates when the
                // remote and locally changed values in the list
                // Ignore everything below seconds
                if (existingList.lastUpdated
                        .truncatedTo(ChronoUnit.SECONDS)
                        .isAfter(updatedList.lastUpdated.truncatedTo(ChronoUnit.SECONDS))
                ) {
                    Log.i(
                        "ShoppingListLocalDataSource",
                        "Updating is skipped because the last local list update is newer than the incoming update: ${existingList.lastUpdated} - (updated) ${updatedList.lastUpdated}",
                    )
                    return@withContext
                }

                // FIXME: Instead of saving the update as truth, compare and make more
                // detailed comparison
                val (dbList, items) = updatedList.toDbList()
                dbList.lastUpdated = OffsetDateTime.now()
                listDao.updateList(dbList)
                itemToListRepository.deleteAllMappingsForList(
                    updatedList.listId,
                    updatedList.createdBy.onlineId,
                )
                items.forEachIndexed { index, item ->
                    // Might be a new item
                    var listMapping: ListMapping
                    try {
                        val insertedItemId = itemRepository.create(item)
                        val apiItem = updatedList.items[index]
                        listMapping =
                            apiItem.toListMapping(
                                insertedItemId,
                                updatedList.listId,
                                updatedList.createdBy.onlineId,
                            )
                    } catch (ex: IllegalStateException) {
                        val updatedItemId = itemRepository.update(item)
                        val apiItem = updatedList.items[index]
                        listMapping =
                            apiItem.toListMapping(
                                updatedItemId,
                                updatedList.listId,
                                updatedList.createdBy.onlineId,
                            )
                    }
                    try {
                        itemToListRepository.create(listMapping)
                    } catch (ex: IllegalStateException) {
                        // THIS SHOULD NEVER HAPPENS, SINCE WE DELETED ALL MAPPINGS FOR THIS LIST
                        itemToListRepository.update(listMapping)
                    }
                }
                Log.d("ShoppingListHandler", "Updated list ${updatedList.listId} in database")
            }
        }

        suspend fun updateCreatedById(currentUserId: Long) {
            val user = userRepository.read() ?: return
            if (user.OnlineID == 0L) {
                Log.d("ShoppingListLocalDataSource", "User not registered online")
                return
            }
            resetAllListCreatedBy(currentUserId, user.OnlineID)
        }

        suspend fun resetCreatedBy() {
            val user = userRepository.read() ?: return
            if (user.OnlineID == 0L) {
                Log.d("ShoppingListLocalDataSource", "User not registered online")
                return
            }
            resetAllListCreatedBy(user.OnlineID, 0L)
        }

        private suspend fun resetAllListCreatedBy(
            currentCreatedById: Long,
            updatedCreatedById: Long,
        ) {
            withContext(Dispatchers.IO) {
                val dbLists = listDao.getOwnShoppingLists(currentCreatedById)
                Log.d(
                    "ShoppingListLocalDataSource",
                    "Updating ${dbLists.size} lists from $currentCreatedById to $updatedCreatedById",
                )
                for (list in dbLists) {
                    list.createdBy = updatedCreatedById
                    listDao.deleteList(list.listId, updatedCreatedById)
                    listDao.insertList(list)
                    val items = itemToListRepository.read(list.listId, currentCreatedById)
                    for (item in items) {
                        item.AddedBy = updatedCreatedById
                        item.CreatedBy = updatedCreatedById
                        itemToListRepository.update(item)
                    }
                }
            }
        }

        suspend fun insertItem(
            listId: Long,
            createdBy: Long,
            item: AppItem,
        ): ApiShoppingList {
            val updatedList: ApiShoppingList
            withContext(Dispatchers.IO) {
                val existingList =
                    read(listId, createdBy) ?: throw IllegalArgumentException("list does not exist")
                existingList.items.add(item.toApiItem())
                existingList.lastUpdated = OffsetDateTime.now()
                update(existingList)
                updatedList = existingList
            }
            return updatedList
        }

        suspend fun insertExistingItem(
            listId: Long,
            createdBy: Long,
            itemId: Long,
        ): ApiShoppingList {
            val updatedList: ApiShoppingList
            withContext(Dispatchers.IO) {
                val existingItem =
                    itemRepository.read(itemId) ?: throw IllegalArgumentException("item does not exits")
                val user =
                    userRepository.read() ?: throw IllegalStateException("user is null after login")
                val existingAppItem = existingItem.toAppItem(user.OnlineID)
                updatedList = insertItem(listId, createdBy, existingAppItem)
            }
            return updatedList
        }

        suspend fun addAll(
            listId: Long,
            createdBy: Long,
            ingredients: List<ApiIngredient>,
        ): ApiShoppingList {
            val updatedList: ApiShoppingList
            withContext(Dispatchers.IO) {
                val existingItems = itemToListRepository.read(listId, createdBy)
                val handledIngredients = mutableListOf<ApiIngredient>()
                existingItems.forEach { mapping ->
                    val additionalMapping = ingredients.find { ingr -> ingr.id == mapping.ItemID }
                    if (additionalMapping != null) {
                        additionalMapping.quantity = max(additionalMapping.quantity, 1)
                        mapping.Quantity = mapping.Quantity.plus(additionalMapping.quantity)
                        itemToListRepository.update(mapping)
                        handledIngredients.add(additionalMapping)
                    }
                }
                val unhandledIngredients = ingredients.minus(handledIngredients.toSet())
                unhandledIngredients.forEach { ingredient ->
                    val newMapping =
                        ListMapping(
                            ID = 0L,
                            ItemID = ingredient.id,
                            ListID = listId,
                            CreatedBy = createdBy,
                            Quantity = max(1L, ingredient.quantity.toLong()),
                            Checked = false,
                            AddedBy = createdBy,
                        )
                    itemToListRepository.create(newMapping)
                }
                updatedList = read(listId, createdBy)
                    ?: throw IllegalStateException("updated list does not exist")
            }
            return updatedList
        }

        suspend fun toggleItem(
            listId: Long,
            createdBy: Long,
            itemId: Long,
        ): ApiShoppingList {
            val updatedList: ApiShoppingList
            withContext(Dispatchers.IO) {
                val mappings = itemToListRepository.read(listId, createdBy)
                if (mappings.isEmpty()) {
                    throw IllegalArgumentException("list does not exist")
                }
                val itemMapping = mappings.find { mapping -> mapping.ItemID == itemId }
                if (itemMapping == null) {
                    throw IllegalArgumentException("mapping does not exist")
                }
                itemMapping.Checked = itemMapping.Checked xor true
                itemToListRepository.update(itemMapping)
                updatedList =
                    read(listId, createdBy) ?: throw IllegalArgumentException("list does not exist")
            }
            return updatedList
        }

        suspend fun updateItemCount(
            listId: Long,
            createdBy: Long,
            itemId: Long,
            quantity: Long,
        ): ApiShoppingList {
            val updatedList: ApiShoppingList
            withContext(Dispatchers.IO) {
                val mappings = itemToListRepository.read(listId, createdBy)
                if (mappings.isEmpty()) {
                    throw IllegalArgumentException("list does not exist")
                }
                val itemMapping = mappings.find { mapping -> mapping.ItemID == itemId }
                if (itemMapping == null) {
                    throw IllegalArgumentException("mapping does not exist")
                }
                itemMapping.Quantity += quantity
                // Because this function is used both for increasing and decreasing the item count
                // check if the new count removes the item from the list
                if (itemMapping.Quantity <= 0L) {
                    itemToListRepository.delete(itemMapping)
                } else {
                    itemToListRepository.update(itemMapping)
                }
                updatedList =
                    read(listId, createdBy) ?: throw IllegalArgumentException("list does not exist")
            }
            return updatedList
        }

        /**
         * Removes the list from the local data storage.
         * Returns immediately if the list cannot be found
         */
        suspend fun delete(
            listId: Long,
            createdBy: Long,
        ) {
            withContext(Dispatchers.IO) {
                listDao.deleteList(listId, createdBy)
                itemToListRepository.deleteAllMappingsForList(listId, createdBy)
            }
        }
    }
