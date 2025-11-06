package com.cloudsheeptech.shoppinglist.list.repo

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.cloudsheeptech.shoppinglist.list.dao.ShoppingListDao
import com.cloudsheeptech.shoppinglist.list.model.ItemToggleStatus
import com.cloudsheeptech.shoppinglist.list.model.ShoppingList
import com.cloudsheeptech.shoppinglist.list.model.ShoppingListOperation
import com.cloudsheeptech.shoppinglist.list.model.ShoppingListPK
import com.cloudsheeptech.shoppinglist.sharing.repo.OnlineUserRepository
import com.cloudsheeptech.shoppinglist.user.repo.AppUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Handles the storage and retrieval of the list that is used by the application into
 * the local database in a way that makes storage possible.
 */
@Singleton
class ShoppingListLocalDataSource
    @Inject
    constructor(
        private val shoppingListDao: ShoppingListDao,
        private val userRepository: AppUserRepository,
        private val onlineUserRepository: OnlineUserRepository,
    ) {
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
                if (shoppingListDao.listExists(
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

                val listId = shoppingListDao.insertList(copiedList)
                if (listId != copiedList.listId) {
                    // Something in my programming went wrong
                    throw Error("list $newList got new id during insertion")
                }
                val insertedList =
                    shoppingListDao.getList(listId, newList.createdBy.onlineId)
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
                latestId = shoppingListDao.getLatestListId(createdBy).plus(1L)
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
                        shoppingListDao.addItemAndItemMapping(
                            itemToAdd,
                            operation.listPk.listId,
                            operation.listPk.createdBy,
                        )
                        listPk = operation.listPk
                        Log.i("ShoppingListLocalDataSource", "Added item ${operation.item} to list ${operation.listPk}")
                    }

                    is ShoppingListOperation.AddItemByName -> {
                        val itemNameToAdd = operation.itemName
                        val user =
                            userRepository.read()
                                ?: throw IllegalStateException("user null after login")
                        shoppingListDao.addItemById(
                            itemNameToAdd,
                            user.OnlineID,
                            operation.listPk.listId,
                            operation.listPk.createdBy,
                        )
                        listPk = operation.listPk
                        Log.i("ShoppingListLocalDataSource", "Added item ${operation.itemName} to list ${operation.listPk}")
                    }

                    is ShoppingListOperation.ChangeQuantityOfItem -> {
                        if (operation.quantity <= 0) {
                            Log.w("ShoppingListLocalDataSource", "${operation.quantity} <= 0: change quantity is skipped")
                            return@withContext null
                        }
                        val listItems =
                            shoppingListDao.getItems(
                                operation.listPk.listId,
                                operation.listPk.createdBy,
                            )
                        val itemToUpdate = listItems.find { item -> item.name.equals(operation.itemName, ignoreCase = true) }
                        if (itemToUpdate == null) {
//                            throw IllegalArgumentException("item ${operation.itemName} is not contained in list ${operation.listPk}")
                            return@withContext null
                        }
                        itemToUpdate.quantity = operation.quantity
                        if (operation.quantityType != null) {
                            itemToUpdate.quantityType = operation.quantityType
                        }
                        itemToUpdate.opCount = itemToUpdate.opCount.plus(1)
                        shoppingListDao.updateItem(
                            itemToUpdate,
                            operation.listPk.listId,
                            operation.listPk.createdBy,
                        )
                        Log.i(
                            "ShoppingListLocalDataSource",
                            "Updated quantity of ${operation.itemName} to ${operation.quantity} of type ${operation.quantityType ?: "not given"}",
                        )
                        listPk = operation.listPk
                    }

                    is ShoppingListOperation.SetItemCheckedStatus -> {
                        val existingItems =
                            shoppingListDao.getItems(
                                operation.listPK.listId,
                                operation.listPK.createdBy,
                            )
                        val relevantItems =
                            existingItems.filter { item ->
                                item.name.equals(operation.itemName, ignoreCase = true)
                            }
                        if (relevantItems.isEmpty()) {
                            Log.w("ShoppingListLocalDataSource", "Item ${operation.itemName} to toggle not found, skipping update")
                            listPk = operation.listPK
                            return@withContext null
                        } else if (relevantItems.size > 1) {
                            throw IllegalStateException(
                                "More than one item with name ${operation.itemName} found! THIS SHOULD NEVER BE POSSIBLE",
                            )
                        }
                        val relevantItem = relevantItems[0]
                        var newStatus: Boolean
                        if (operation.status == ItemToggleStatus.FALSE) {
                            newStatus = false
                        } else if (operation.status == ItemToggleStatus.TRUE) {
                            newStatus = true
                        } else {
                            newStatus = !relevantItem.checked
                        }
                        relevantItem.checked = newStatus
                        relevantItem.opCount = relevantItem.opCount.plus(1)
                        shoppingListDao.updateItem(
                            relevantItem,
                            operation.listPK.listId,
                            operation.listPK.createdBy,
                        )
                        Log.i(
                            "ShoppingListLocalDataSource",
                            "Update checked of ${operation.itemName} in list ${operation.listPK} to ${relevantItem.checked}",
                        )
                        listPk = operation.listPK
                    }

                    is ShoppingListOperation.Create -> {
                        val newList =
                            ShoppingList(
                                listId = 0L,
                                createdBy = operation.creator,
                                title = operation.title,
                                synchronized = OffsetDateTime.now(),
                                items = operation.items.toMutableList(),
                            )
                        val newListId = shoppingListDao.insertList(newList)
                        listPk = ShoppingListPK(newListId, operation.creator.onlineId)
                        Log.i("ShoppingListLocalDataSource", "Created new list $newList")
                    }

                    is ShoppingListOperation.RemoveItemByName -> {
                        val listItems =
                            shoppingListDao.getItems(
                                operation.listPk.listId,
                                operation.listPk.createdBy,
                            )
                        val itemToRemove =
                            listItems.find { item ->
                                item.name.lowercase().equals(operation.itemName.lowercase())
                            }
                        if (itemToRemove != null) {
                            shoppingListDao.removeItem(
                                itemToRemove,
                                operation.listPk.listId,
                                operation.listPk.createdBy,
                            )
                            Log.i("ShoppingListLocalDataSource", "Removed item ${operation.itemName} from list ${operation.listPk}")
                        } else {
                            Log.i(
                                "ShoppingListLocalDataSource",
                                "Item ${operation.itemName} not removed because not found in list ${operation.listPk}",
                            )
                        }
                        listPk = operation.listPk
                    }

                    is ShoppingListOperation.RenameList -> {
                        shoppingListDao.updateListTitle(
                            operation.newName,
                            operation.listPk.listId,
                            operation.listPk.createdBy,
                        )
                        Log.i("ShoppingListLocalDataSource", "Renamed list ${operation.listPk} to ${operation.newName}")
                        listPk = operation.listPk
                    }

                    is ShoppingListOperation.Delete -> {
                        shoppingListDao.deleteList(operation.listPk.listId, operation.listPk.createdBy)
                        Log.i("ShoppingListLocalDataSource", "Delete list ${operation.listPk}")
                        return@withContext null
                    }
                }
                return@withContext shoppingListDao.getList(listPk.listId, listPk.createdBy)
            }
        }

        suspend fun updateCreatedByForOwnLists(
            previousOnlineId: Long,
            newOnlineId: Long,
        ) {
            withContext(Dispatchers.IO) {
                shoppingListDao.updatedCreatedBy(newOnlineId, previousOnlineId)
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
                val currentList = shoppingListDao.getList(listId, createdBy)
                return@withContext currentList
            }
        }

        /**
         * Reads all list information from the database, including own
         * an foreign lists.
         * @return a list of all found lists
         */
        suspend fun readAll(): List<ShoppingList> {
            return withContext(Dispatchers.IO) {
                val allLists = shoppingListDao.getAllLists()
                return@withContext allLists
            }
        }

        fun readAllLive(): LiveData<List<ShoppingList>> {
            val liveLists = shoppingListDao.getAllListsLive()
            return liveLists
                .combine(userRepository.readLiveFlow()) { lists, user ->
                    lists.map { list ->
                        val correspondingCreatorName =
                            if (list.createdBy.onlineId != user.OnlineID) {
                                onlineUserRepository.read(list.createdBy.onlineId)?.username ?: "user not found"
                            } else {
                                user.Username
                            }
                        list.createdBy.username = correspondingCreatorName
                        list
                    }
                }.asLiveData(Dispatchers.IO)
        }

        fun readAllListItemsLive(
            listId: Long,
            createdBy: Long,
        ) = shoppingListDao.getItemsLive(listId, createdBy)

        /**
         * Function making the insertion and update process more easy.
         * @return true if the list exists, otherwise false
         */
        suspend fun exists(
            listId: Long,
            createdBy: Long,
        ): Boolean =
            withContext(Dispatchers.IO) {
                shoppingListDao.listExists(listId, createdBy)
            }

        /**
         * Currently not implemented
         * @throws NotImplementedError
         */
        fun readLive(
            listId: Long,
            createdBy: Long,
        ): LiveData<ShoppingList> = shoppingListDao.getListLive(listId, createdBy).asLiveData(EmptyCoroutineContext, 5000L)

        suspend fun deleteAll() {
            withContext(Dispatchers.IO) {
                shoppingListDao.dropTable()
            }
        }
    }
