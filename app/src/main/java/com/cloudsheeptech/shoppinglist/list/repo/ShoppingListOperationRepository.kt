package com.cloudsheeptech.shoppinglist.list.repo

import android.util.Log
import com.cloudsheeptech.shoppinglist.list.dao.PendingListOperationDao
import com.cloudsheeptech.shoppinglist.list.model.ListCreator
import com.cloudsheeptech.shoppinglist.list.model.ShoppingListOperation
import com.cloudsheeptech.shoppinglist.list.model.ShoppingListPK
import com.cloudsheeptech.shoppinglist.list.util.ShoppingListOperationConversionUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ShoppingListOperationRepository
    @Inject
    constructor(
        private val pendingListDao: PendingListOperationDao,
    ) {
        suspend fun insert(operation: ShoppingListOperation): Long {
            return withContext(Dispatchers.IO) {
                try {
                    val convertedOp = ShoppingListOperationConversionUtil.shoppingListOperationToDatabasePendingListOperation(operation)
                    val id = pendingListDao.insert(convertedOp)
                    if (id < 0) {
                        Log.e("ShoppingListOperationRepository", "Failed to insert operation $operation")
                    }
                    return@withContext id
                } catch (ex: Exception) {
                    Log.e("ShoppingListOperationRepository", "Failed to insert operation: $ex")
                }
                return@withContext -1L
            }
        }

        suspend fun updateOperation(
            id: Long,
            updatedOperation: ShoppingListOperation,
        ) {
            withContext(Dispatchers.IO) {
                try {
                    val convertedOp =
                        ShoppingListOperationConversionUtil.shoppingListOperationToDatabasePendingListOperation(
                            updatedOperation,
                        )
                    pendingListDao.update(id, convertedOp.serializedOp)
                    Log.i("ShoppingListOperationRepository", "Updated operation $id to ${convertedOp.serializedOp}")
                } catch (ex: Exception) {
                    Log.e("ShoppingListOperationRepository", "Failed to insert operation: $ex")
                }
            }
        }

        suspend fun updateCreatorIdForAllPendingOperations(creatorId: Long) {
            withContext(Dispatchers.IO) {
                try {
                    val allOperations = getAllPendingOperationsWithOperationId()
                    if (allOperations.isEmpty()) {
                        return@withContext
                    }
                    for (idAndOperation in allOperations) {
                        val id = idAndOperation.first
                        var operation = idAndOperation.second
                        val serializedOp =
                            if (operation is ShoppingListOperation.Create) {
                                operation =
                                    ShoppingListOperation.Create(
                                        operation.title,
                                        ListCreator(creatorId, operation.creator.username),
                                    )
                                ShoppingListOperationConversionUtil.shoppingListOperationToDatabasePendingListOperation(operation)
                            } else if (operation is ShoppingListOperation.RenameList) {
                                operation =
                                    ShoppingListOperation.RenameList(
                                        ShoppingListPK(operation.listPk.listId, creatorId),
                                        operation.newName,
                                    )
                                ShoppingListOperationConversionUtil.shoppingListOperationToDatabasePendingListOperation(operation)
                            } else if (operation is ShoppingListOperation.Delete) {
                                operation =
                                    ShoppingListOperation.Delete(
                                        ShoppingListPK(operation.listPk.listId, creatorId),
                                    )
                                ShoppingListOperationConversionUtil.shoppingListOperationToDatabasePendingListOperation(operation)
                            } else if (operation is ShoppingListOperation.AddItem) {
                                operation =
                                    ShoppingListOperation.AddItem(
                                        ShoppingListPK(operation.listPk.listId, creatorId),
                                        operation.item,
                                    )
                                ShoppingListOperationConversionUtil.shoppingListOperationToDatabasePendingListOperation(operation)
                            } else if (operation is ShoppingListOperation.AddItemByName) {
                                operation =
                                    ShoppingListOperation.AddItemByName(
                                        ShoppingListPK(operation.listPk.listId, creatorId),
                                        operation.itemName,
                                    )
                                ShoppingListOperationConversionUtil.shoppingListOperationToDatabasePendingListOperation(operation)
                            } else if (operation is ShoppingListOperation.ChangeQuantityOfItem) {
                                operation =
                                    ShoppingListOperation.ChangeQuantityOfItem(
                                        ShoppingListPK(operation.listPk.listId, creatorId),
                                        operation.itemName,
                                        operation.quantity,
                                        operation.quantityType,
                                    )
                                ShoppingListOperationConversionUtil.shoppingListOperationToDatabasePendingListOperation(operation)
                            } else if (operation is ShoppingListOperation.SetItemCheckedStatus) {
                                operation =
                                    ShoppingListOperation.SetItemCheckedStatus(
                                        ShoppingListPK(operation.listPk.listId, creatorId),
                                        operation.itemName,
                                        operation.status,
                                    )
                                ShoppingListOperationConversionUtil.shoppingListOperationToDatabasePendingListOperation(operation)
                            } else if (operation is ShoppingListOperation.RemoveItemByName) {
                                operation =
                                    ShoppingListOperation.RemoveItemByName(
                                        ShoppingListPK(operation.listPk.listId, creatorId),
                                        operation.itemName,
                                    )
                                ShoppingListOperationConversionUtil.shoppingListOperationToDatabasePendingListOperation(operation)
                            } else {
                                throw IllegalArgumentException("Operation $operation not supported")
                            }
                        pendingListDao.update(id, serializedOp.serializedOp)
                    }
                } catch (ex: Exception) {
                    Log.e("ShoppingListOperationRepository", "Failed to update creatorId to $creatorId: $ex")
                }
            }
        }

        suspend fun getAllPendingOperationsWithOperationId(): List<Pair<Long, ShoppingListOperation>> {
            return withContext(Dispatchers.IO) {
                val allOperations = pendingListDao.getAllOperations()
                if (allOperations.isEmpty()) {
                    return@withContext emptyList()
                }
                val apiOperations = mutableListOf<Pair<Long, ShoppingListOperation>>()
                for (operation in allOperations) {
                    try {
                        val convertedOp = ShoppingListOperationConversionUtil.databaseListOperationToShoppingListOperation(operation)
                        apiOperations.add(Pair(operation.id, convertedOp))
                    } catch (ex: IllegalArgumentException) {
                        Log.e(
                            "ShoppingListOperationRepository",
                            "Failed to convert operation ${operation.opType}: $ex, skipping this operation",
                        )
                        continue
                    }
                }
                return@withContext apiOperations
            }
        }

        suspend fun getAllPendingOperations(): List<ShoppingListOperation> =
            withContext(Dispatchers.IO) {
                val allOperationsWithId = getAllPendingOperationsWithOperationId()
                val idsAndOperations = allOperationsWithId.unzip()
                return@withContext idsAndOperations.second
            }

        suspend fun deleteAllPendingOperations() {
            withContext(Dispatchers.IO) {
                pendingListDao.deleteAllOperations()
            }
        }
    }
