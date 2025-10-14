package com.cloudsheeptech.shoppinglist.data.list

import android.util.Log
import androidx.lifecycle.LiveData
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.itemToListMapping.ItemToListRepository
import com.cloudsheeptech.shoppinglist.data.items.AppItem
import com.cloudsheeptech.shoppinglist.data.items.ItemRepository
import com.cloudsheeptech.shoppinglist.data.items.ItemToList
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListConversionHelper.Companion.toApiItem
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListConversionHelper.Companion.toApiList
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListConversionHelper.Companion.toAppItem
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListConversionHelper.Companion.toDbList
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

        /**
         * This function creates a new shopping list in the local database
         * @param newList expecting a new list with listId == 0; a new listId will be generated
         * by this function!
         * @return The newly inserted list with updated listId
         * @exception IllegalStateException in case the list already exists
         * @exception IllegalArgumentException if the listId != 0
         */
        @Throws(IllegalArgumentException::class, IllegalStateException::class)
        suspend fun create(newList: ShoppingList): ShoppingList {
            return withContext(Dispatchers.IO) {
                if (listDao.listExists(
                        newList.listId,
                        newList.createdBy.onlineId,
                    )
                ) {
                    throw IllegalStateException("list $newList exists")
                }

                // Break reference to outside list
                val copiedList = newList.copy()
                // Update the id to the latest available ID if the list was created locally
                val newListId = createListIdForNewLocalList(copiedList)
                if (newListId < 0) {
                    throw IllegalArgumentException("list $copiedList is not a new list from local user")
                }

                val listId = listDao.insertList(copiedList)
                if (listId != copiedList.listId) {
                    // Something in my programming went wrong
                    throw Error("list $newList got new id during insertion")
                }
                val insertedList = listDao.getList(listId, newList.createdBy.onlineId)
                    ?: throw IllegalStateException("failed to insert $newList - cannot find list after insertion")
                return@withContext insertedList
            }
        }

    private suspend fun createListIdForNewLocalList(list: ShoppingList): Long {
        if (!isNewList(list) && !isListFromLocalUser(list)) {
            return 0L
        }
        if (!isNewList(list) && isListFromLocalUser(list)) {
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
        private fun isListFromLocalUser(list: ShoppingList): Boolean {
            val user =
                userRepository.read()
                    ?: throw IllegalStateException("user not set after login screen")
            return list.createdBy.onlineId == user.OnlineID
        }

    private fun isNewList(list: ShoppingList): Boolean = list.listId == 0L


    suspend fun applyUpdates(operations: List<ShoppingListOperation>): ShoppingList? {
        return withContext(Dispatchers.IO) {
            var finalList: ShoppingList? = null
            for (operation in operations) {
                finalList = update(operation)
            }
            return@withContext finalList
        }
    }

    suspend fun update(operation: ShoppingListOperation): ShoppingList? {
        var listPk: ShoppingListPK
        return withContext(Dispatchers.IO) {
            when (operation) {
                is ShoppingListOperation.AddItem -> {
                    val itemToAdd = operation.item
                    listDao.addItem(itemToAdd, operation.listPk.listId, operation.listPk.createdBy)
                    listPk = operation.listPk
                }

                is ShoppingListOperation.ChangeQuantityOfItem -> {
                    val listItems =
                        listDao.getItems(operation.listPk.listId, operation.listPk.createdBy)
                    val itemToUpdate = listItems.find { item -> item.id == operation.itemId }
                    if (itemToUpdate == null) {
                        throw IllegalArgumentException("item ${operation.itemId} is not contained in list ${operation.listPk}")
                    }
                    if (operation.quantity != null) {
                        itemToUpdate.quantity = operation.quantity
                    }
                    if (operation.quantityType != null) {
                        itemToUpdate.quantityType = operation.quantityType
                    }
                    listDao.updateItem(
                        itemToUpdate,
                        operation.listPk.listId,
                        operation.listPk.createdBy
                    )
                    listPk = operation.listPk
                }

                is ShoppingListOperation.Create -> {
                    val newList = ShoppingList(
                        listId = 0L,
                        createdBy = operation.creator,
                        title = operation.title,
                        synchronized = OffsetDateTime.now(),
                        items = operation.items.toMutableList()
                    )
                    val newListId = listDao.insertList(newList)
                    listPk = ShoppingListPK(newListId, operation.creator.onlineId)
                }

                is ShoppingListOperation.RemoveItemById -> {
                    val listItems =
                        listDao.getItems(operation.listPk.listId, operation.listPk.createdBy)
                    val itemToRemove = listItems.find { item -> item.id == operation.itemId }
                    if (itemToRemove != null) {
                        listDao.removeItem(
                            itemToRemove,
                            operation.listPk.listId,
                            operation.listPk.createdBy
                        )
                    }
                    listPk = operation.listPk
                }

                is ShoppingListOperation.RemoveItemByName -> {
                    val listItems =
                        listDao.getItems(operation.listPk.listId, operation.listPk.createdBy)
                    val itemToRemove = listItems.find { item ->
                        item.name.lowercase().equals(operation.itemName.lowercase())
                    }
                    if (itemToRemove != null) {
                        listDao.removeItem(
                            itemToRemove,
                            operation.listPk.listId,
                            operation.listPk.createdBy
                        )
                    }
                    listPk = operation.listPk
                }

                is ShoppingListOperation.RenameList -> {
                    listDao.updateListTitle(
                        operation.newName,
                        operation.listPk.listId,
                        operation.listPk.createdBy
                    )
                    listPk = operation.listPk
                }

                is ShoppingListOperation.Delete -> {
                    listDao.deleteList(operation.listPk.listId, operation.listPk.createdBy)
                    return@withContext null
                }
                }
            return@withContext listDao.getList(listPk.listId, listPk.createdBy)
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
        ): ShoppingList? {
            return withContext(Dispatchers.IO) {
                val currentList = listDao.getList(listId, createdBy)
                return@withContext currentList
            }
        }

        /**
         * Reads all list information from the database, including own
         * an foreign lists.
         * @return a list of all found lists
         */
        suspend fun readAll(): List<ShoppingList> {
            val allLists = mutableListOf<ShoppingList>()
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
         * @param operation The operation that is performed on the list
         * @return The version of the updated list
         * @throws IllegalArgumentException if the list does not exist
         */
        @Throws(IllegalArgumentException::class)
        private suspend fun update(
            listToUpdate: ShoppingList,
            operation: ShoppingListOperation
        ): Long {


            if (updatedList.listId == 0L) {
                throw IllegalArgumentException("list does not exist in the database")
            }
            val {
                updatedVersion =
                    withContext(Dispatchers.IO) {
                        val existingList =
                            listDao.getShoppingList(
                                updatedList.listId,
                                updatedList.createdBy.onlineId
                            )
                                ?: throw IllegalArgumentException("list does not exist in the database")

                        // Increase version number
                        updatedList.version = increaseVersionNumberForLocalChanges(updatedList)

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
                        val existingItems =
                            itemRepository
                                .readForList(updatedList.listId, updatedList.createdBy.onlineId)
                                .map { item -> item.toApiItem() }
                                .toMutableList()
                        val performedOperations =
                            ShoppingListMergeHelper.getListDelta(
                                existingList.toApiList(
                                    ListCreator(
                                        existingList.createdBy,
                                        "",
                                    ),
                                    existingItems,
                                ),
                                updatedList,
                            )
                        listDao.updateList(updatedListInDbFormat)

                        // TODO: Implement more graceful deletion and addition of list changes
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
            }

        }

    private fun performOperation(
        listToUpdate: ShoppingList?,
        operation: ShoppingListOperation
    ): ShoppingList {
        when (operation) {
            is ShoppingListOperation.AddItem -> {
                listToUpdate?.items?.add(operation.item.toApiItem())
            }

            is ShoppingListOperation.ChangeQuantityOfItem -> {
                listToUpdate?.items?.map { item ->
                    if (item.name == "TODO") {
                        item.quantity = operation.quantity
                    }
                }
            }

            is ShoppingListOperation.Create -> {
                val newList = ShoppingList
            }

            is ShoppingListOperation.Delete -> TODO()
            is ShoppingListOperation.RemoveItemById -> TODO()
            is ShoppingListOperation.RemoveItemByName -> TODO()
            is ShoppingListOperation.RenameList -> TODO()
        }
    }

    private fun increaseVersionNumberForLocalChanges(updatedList: ShoppingList): Long {
        val user = userRepository.read() ?: throw IllegalStateException("user null after login")
        if (updatedList.createdBy.onlineId != 0L && updatedList.createdBy.onlineId != user.OnlineID) {
            return updatedList.version
        }
        return updatedList.version.plus(1L)
        }

        private fun updateCreatedByForLocallyCreatedLists(
            existingList: DbShoppingList,
            updatedList: ShoppingList,
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
                        item.addedBy = updatedCreatedById
                        item.createdBy = updatedCreatedById
                        itemToListRepository.update(item)
                    }
                }
            }
        }

        suspend fun insertItem(
            listId: Long,
            createdBy: Long,
            item: AppItem,
        ): ShoppingList {
            val updatedList: ShoppingList
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
        ): ShoppingList {
            val updatedList: ShoppingList
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
        ): ShoppingList {
            val updatedList: ShoppingList
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
        ): ShoppingList {
            val updatedList: ShoppingList
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
        ): ShoppingList {
            val updatedList: ShoppingList
            withContext(Dispatchers.IO) {
                val existingItems = itemToListRepository.read(listId, createdBy)
                val handledIngredients = mutableListOf<ApiIngredient>()
                existingItems.forEach { mapping ->
                    val additionalMapping = ingredients.find { ingr -> ingr.id == mapping.itemId }
                    if (additionalMapping != null) {
                        additionalMapping.quantity = max(additionalMapping.quantity, 1)
                        mapping.quantity = mapping.quantity.plus(additionalMapping.quantity)
                        itemToListRepository.update(mapping)
                        handledIngredients.add(additionalMapping)
                    }
                }
                val unhandledIngredients = ingredients.minus(handledIngredients.toSet())
                unhandledIngredients.forEach { ingredient ->
                    val newMapping =
                        ItemToList(
                            id = 0L,
                            itemId = ingredient.id,
                            listId = listId,
                            createdBy = createdBy,
                            quantity = max(1L, ingredient.quantity.toLong()),
                            checked = false,
                            addedBy = createdBy,
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
        ): ShoppingList {
            val updatedList: ShoppingList
            withContext(Dispatchers.IO) {
                val mappings = itemToListRepository.read(listId, createdBy)
                if (mappings.isEmpty()) {
                    throw IllegalArgumentException("list does not exist")
                }
                val itemMapping = mappings.find { mapping -> mapping.itemId == itemId }
                if (itemMapping == null) {
                    throw IllegalArgumentException("mapping does not exist")
                }
                itemMapping.checked = itemMapping.checked xor true
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
        ): ShoppingList {
            val updatedList: ShoppingList
            withContext(Dispatchers.IO) {
                val mappings = itemToListRepository.read(listId, createdBy)
                if (mappings.isEmpty()) {
                    throw IllegalArgumentException("list does not exist")
                }
                val itemMapping = mappings.find { mapping -> mapping.itemId == itemId }
                if (itemMapping == null) {
                    throw IllegalArgumentException("mapping does not exist")
                }
                itemMapping.quantity += quantity
                // Because this function is used both for increasing and decreasing the item count
                // check if the new count removes the item from the list
                if (itemMapping.quantity <= 0L) {
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
                listDao.dropTable()
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
