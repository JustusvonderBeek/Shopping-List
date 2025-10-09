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
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListConversionHelper.Companion.toApiItem
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListConversionHelper.Companion.toApiList
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListConversionHelper.Companion.toAppItem
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListConversionHelper.Companion.toDbList
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListConversionHelper.Companion.toListMapping
import com.cloudsheeptech.shoppinglist.data.onlineUser.ListCreator
import com.cloudsheeptech.shoppinglist.data.onlineUser.OnlineUserRepository
import com.cloudsheeptech.shoppinglist.data.recipe.ApiIngredient
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
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
        database: ShoppingListDatabase,
        private val userRepository: AppUserRepository,
        private val onlineUserRepository: OnlineUserRepository,
        private val itemRepository: ItemRepository,
        private val itemToListRepository: ItemToListRepository,
    ) {
        private val listDao = database.shoppingListDao()

        private fun increaseListVersion(listToUpdate: ApiShoppingList) {
            listToUpdate.version = listToUpdate.version.plus(1L)
        }

        /**
         * This function creates a new shopping list in the local database
         * @return The id of the newly created list or the version if updated
         * @exception IllegalArgumentException in case the list already exists
         * @exception IllegalStateException in case creating the list in the db failed
         */
        @Throws(IllegalArgumentException::class, IllegalStateException::class)
        suspend fun createOrUpdate(listForCreationOrUpdate: ApiShoppingList): Long {
            var listIdAfterInsertionOrVersion = listForCreationOrUpdate.listId
            withContext(Dispatchers.IO) {
                // Differentiate between new and existing list
                if (listDao.exists(
                        listForCreationOrUpdate.listId,
                        listForCreationOrUpdate.createdBy.onlineId,
                    )
                ) {
                    listIdAfterInsertionOrVersion = update(listForCreationOrUpdate)
                    return@withContext
                }

                // Update the id to the latest available ID if the list was created locally
                val listForCreation = listForCreationOrUpdate.copy()
                listIdAfterInsertionOrVersion = createListIdForNewLocalList(listForCreation)
                if (listIdAfterInsertionOrVersion < 0L) {
                    listIdAfterInsertionOrVersion = listForCreation.listId
                }

                // We split the list from one single object into 2 parts: basic list and items
                val (listForCreationInDatabaseFormat, itemsForCreationInDatabaseFormat) = listForCreation.toDbList()
                val totalTableRows = listDao.insertList(listForCreationInDatabaseFormat)
                if (totalTableRows == -1L) {
                    Log.e(
                        "ShoppingListLocalDataSource",
                        "Failed to insert list '${listForCreation.title}': ${listForCreation.listId}",
                    )
                    throw IllegalStateException("Failed to insert list '${listForCreation.title}': ${listForCreation.listId}")
                }
                Log.i(
                    "ShoppingListHandler",
                    "Stored new list '${listForCreationInDatabaseFormat.title}': ${listForCreationInDatabaseFormat.listId} in database",
                )

                // Insert the items in case we received a remote list which is already populated
                insertItems(itemsForCreationInDatabaseFormat, listForCreation)
                Log.d(
                    "ShoppingListHandler",
                    "Inserted list '${listForCreationOrUpdate.title}': ${listForCreationOrUpdate.listId} from ${listForCreationOrUpdate.createdBy.onlineId} with ${itemsForCreationInDatabaseFormat.size} items successfully into database",
                )
            }
            return listIdAfterInsertionOrVersion
        }

        private suspend fun createListIdForNewLocalList(list: ApiShoppingList): Long {
            if (!isNewList(list) || !isListFromLocalUser(list)) {
                return -1L
            }
            list.listId = getUniqueShoppingListID(list.createdBy.onlineId)
            return list.listId
        }

        private suspend fun getUniqueShoppingListID(createdBy: Long): Long {
            // Starting the local listIds with 1
            var latestId = 1L
            withContext(Dispatchers.IO) {
                latestId = listDao.getLatestListId(createdBy).plus(1L)
            }
            Log.i("ShoppingListLocalDataSource", "Generated new list id: $latestId")
            return latestId
        }

        /**
         * @throws IllegalStateException in case the user is not set
         */
        private fun isListFromLocalUser(list: ApiShoppingList): Boolean {
            val user =
                userRepository.read()
                    ?: throw IllegalStateException("user not set after login screen")
            return list.createdBy.onlineId == user.OnlineID
        }

        private fun isNewList(list: ApiShoppingList): Boolean = list.listId == 0L

        private suspend fun insertItems(
            itemsForInsertion: List<DbItem>,
            insertedItemsInApiFormat: ApiShoppingList,
        ) {
            itemsForInsertion.forEachIndexed { index, item ->
                val apiItem = insertedItemsInApiFormat.items[index]
                val itemIdAfterInsertionOrUpdate =
                    try {
                        itemRepository.create(item)
                    } catch (_: IllegalStateException) {
                        itemRepository.update(item)
                    }

                val listMapping =
                    apiItem.toListMapping(
                        itemIdAfterInsertionOrUpdate,
                        insertedItemsInApiFormat.listId,
                        insertedItemsInApiFormat.createdBy.onlineId,
                    )

                try {
                    itemToListRepository.create(listMapping)
                } catch (_: IllegalStateException) {
                    itemToListRepository.update(listMapping)
                } catch (_: IllegalArgumentException) {
                    itemToListRepository.update(listMapping)
                }
            }
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
                val user =
                    userRepository.read() ?: throw IllegalStateException("user null after login")
                var username = user.Username
                if (user.OnlineID != createdBy) {
                    val onlineUser = onlineUserRepository.read(createdBy)
                    username = onlineUser?.username ?: ""
                }
                offlineList = shoppingListBase.toApiList(ListCreator(createdBy, username))
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
                offlineList.items.addAll(apiItems)
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

        fun readAllListItemsLive(
            listId: Long,
            createdBy: Long,
        ) = itemRepository.readForListLive(listId, createdBy)

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
        private suspend fun update(updatedList: ApiShoppingList): Long {
            if (updatedList.listId == 0L) {
                throw IllegalArgumentException("list does not exist in the database")
            }
            val updatedVersion =
                withContext(Dispatchers.IO) {
                    val existingList =
                        listDao.getShoppingList(updatedList.listId, updatedList.createdBy.onlineId)
                            ?: throw IllegalArgumentException("list does not exist in the database")

                    // Increase version number
                    updatedList.version = updatedList.version.plus(1L)

                    // Updated last edited time
                    updatedList.lastUpdated = OffsetDateTime.now()

                    // Fix the createdBy == 0 if the user is already logged in online
                    val moveListToNewId =
                        updateCreatedByForLocallyCreatedLists(existingList, updatedList)

                    if (updatedList.version <= existingList.version) {
                        // TODO: For this to work, we need delta information what happened since the last updates
                        Log.i(
                            "ShoppingListLocalDataSource",
                            "Updating is skipped because the last local list update is newer than the incoming update: ${existingList.version} - (updated) ${updatedList.version}",
                        )
                        return@withContext -1L
                    }

                    // FIXME: Instead of saving the update as truth, compare and make more
                    // detailed comparison
                    val (updatedListInDbFormat, updatedItemsInDbFormat) = updatedList.toDbList()
                    // Write me a function that compares the existing list with the updatedList and
                    // returns what parts are changed
                    listDao.updateList(updatedListInDbFormat)

                    itemToListRepository.deleteAllMappingsForList(
                        updatedList.listId,
                        updatedList.createdBy.onlineId,
                    )
                    insertItems(updatedItemsInDbFormat, updatedList)
                    Log.d(
                        "ShoppingListHandler",
                        "Updated list ${updatedList.listId} with ${updatedItemsInDbFormat.size} items in database",
                    )

                    if (moveListToNewId) {
                        // Delete list for old userId = 0
                        listDao.deleteList(updatedList.listId, 0L)
                    }

                    return@withContext updatedListInDbFormat.version
                }
            return updatedVersion
        }

        private fun updateCreatedByForLocallyCreatedLists(
            existingList: DbShoppingList,
            updatedList: ApiShoppingList,
        ): Boolean {
            val user =
                userRepository.read() ?: throw IllegalStateException("user null after login")
            var moveListToNewId = false
            if (existingList.createdBy == 0L && user.OnlineID != 0L) {
                updatedList.createdBy.onlineId = user.OnlineID
                Log.i(
                    "ShoppingListLocalDataSource",
                    "Updated createdBy from ${existingList.createdBy} to ${user.OnlineID} for list '${existingList.title}': ${existingList.listId}",
                )
                moveListToNewId = true
            }
        return moveListToNewId
    }

        private fun mergeListBestEffort(
            oldList: ApiShoppingList,
            newList: ApiShoppingList,
        ): ApiShoppingList {
            if (oldList.listId != newList.listId) {
                throw IllegalArgumentException("Cannot merge lists with different ids")
            }
            if (oldList.createdBy.onlineId != newList.createdBy.onlineId) {
                throw IllegalArgumentException("Cannot merge lists with different creator ids")
            }
            val oldItems = oldList.items
            val mergedItems = mutableListOf<ApiItem>()
            val itemContainedInNewList = newList.items.toSet()
            for (item in oldItems) {
                if (!itemContainedInNewList.contains(item)) {
                    mergedItems.add(item)
                }
            }
            val mergedList = newList.copy(items = mergedItems)
            return mergedList
        }

        suspend fun updateCreatedByForList(
            listId: Long,
            createdBy: Long,
        ) {
            withContext(Dispatchers.IO) {
                listDao.setListWithOfflineCreatorToOnlineId(createdBy)
                itemToListRepository.setCreatorIdForAllItems(createdBy)
                itemToListRepository.setAddedByForAllItems(createdBy)
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
            withContext(Dispatchers.IO) {
                listDao.resetOwnListsCreatorId(user.OnlineID)
                itemToListRepository.resetCreatorIdForAllItemsInOwnLists(user.OnlineID)
                itemToListRepository.resetAddedByForOwnLists(user.OnlineID)
            }
//            resetAllListCreatedBy(user.OnlineID, 0L)
        }

        suspend fun resetAddedBy(addedBy: Long) {
            withContext(Dispatchers.IO) {
                itemToListRepository.resetAddedByForOwnLists(addedBy)
            }
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
                increaseListVersion(existingList)
                update(existingList)
                updatedList =
                    read(listId, createdBy) ?: throw IllegalArgumentException("list does not exist")
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

        suspend fun removeItem(
            listId: Long,
            createdBy: Long,
            itemId: Long,
        ): ApiShoppingList {
            val updatedList: ApiShoppingList
            withContext(Dispatchers.IO) {
                itemToListRepository.delete(itemId, listId, createdBy)
                listDao.markUpdated(listId, createdBy)
                val existingList =
                    read(listId, createdBy) ?: throw IllegalArgumentException("list does not exist")
                updatedList = existingList
            }
            return updatedList
        }

        suspend fun remoteCheckedItems(
            listId: Long,
            createdBy: Long,
        ): ApiShoppingList {
            val updatedList: ApiShoppingList
            withContext(Dispatchers.IO) {
                itemToListRepository.deleteAllCheckedMappingsForList(listId, createdBy)
                listDao.markUpdated(listId, createdBy)
                updatedList =
                    read(listId, createdBy) ?: throw IllegalArgumentException("list does not exist")
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
                listDao.markUpdated(listId, createdBy)
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
                listDao.markUpdated(listId, createdBy)
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
                listDao.markUpdated(listId, createdBy)
                updatedList =
                    read(listId, createdBy) ?: throw IllegalArgumentException("list does not exist")
            }
            return updatedList
        }

        suspend fun updateTitle(
            listId: Long,
            createdBy: Long,
            title: String,
        ) {
            withContext(Dispatchers.IO) {
                val existingList =
                    listDao.getShoppingList(listId, createdBy)
                        ?: throw IllegalArgumentException("list does not exist in the database")
                existingList.title = title
                listDao.updateList(existingList)
                listDao.markUpdated(listId, createdBy)
            }
        }

        suspend fun deleteAll() {
            withContext(Dispatchers.IO) {
                listDao.reset()
                itemToListRepository.deleteAllMappings()
            }
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
                listDao.markUpdated(listId, createdBy)
            }
        }
    }
