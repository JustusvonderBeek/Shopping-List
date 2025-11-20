package com.cloudsheeptech.shoppinglist.list.repo

import android.util.Log
import com.cloudsheeptech.shoppinglist.list.dao.PendingListOperationDao
import com.cloudsheeptech.shoppinglist.list.model.ShoppingListApiOperation
import com.cloudsheeptech.shoppinglist.list.model.ShoppingListOperation
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

        suspend fun getAllPendingOperations(): List<ShoppingListApiOperation> =
            withContext(Dispatchers.IO) {
                val allOperations = pendingListDao.getAllOperations()
                if (allOperations.isNullOrEmpty()) {
                    return@withContext emptyList()
                }
                val apiOperations = mutableListOf<ShoppingListApiOperation>()
                for (operation in allOperations) {
                    try {
                        val convertedOp = ShoppingListOperationConversionUtil.pendingListOperationToShoppingListApiOperation(operation)
                        apiOperations.add(convertedOp)
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

        suspend fun deleteAllPendingOperations() {
            withContext(Dispatchers.IO) {
                pendingListDao.deleteAllOperations()
            }
        }
    }
