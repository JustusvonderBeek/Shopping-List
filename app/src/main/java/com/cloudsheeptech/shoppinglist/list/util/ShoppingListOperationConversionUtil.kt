package com.cloudsheeptech.shoppinglist.list.util

import com.cloudsheeptech.shoppinglist.list.model.DbPendingListOperation
import com.cloudsheeptech.shoppinglist.list.model.ListCreator
import com.cloudsheeptech.shoppinglist.list.model.ShoppingListApiOperation
import com.cloudsheeptech.shoppinglist.list.model.ShoppingListOperation
import com.cloudsheeptech.shoppinglist.list.model.ShoppingListOperationConstants
import kotlinx.serialization.json.Json

class ShoppingListOperationConversionUtil {
    companion object {
        val json =
            Json {
                encodeDefaults = false
                ignoreUnknownKeys = true
            }

        fun shoppingListOperationToDatabasePendingListOperation(operation: ShoppingListOperation): DbPendingListOperation {
            val apiOp = shoppingListOperationToApiOperation(operation)
            return shoppingListApiOperationToDatabasePendingListOperation(apiOp)
        }

        fun shoppingListOperationToApiOperation(operation: ShoppingListOperation): ShoppingListApiOperation =
            when (operation) {
                is ShoppingListOperation.AddItem -> TODO()
                is ShoppingListOperation.AddItemByName -> TODO()
                is ShoppingListOperation.ChangeQuantityOfItem -> TODO()
                is ShoppingListOperation.Create -> {
                    ShoppingListApiOperation.Create(
                        operation.title,
                        operation.creator,
                    )
                }
                is ShoppingListOperation.Delete -> TODO()
                is ShoppingListOperation.RemoveItemByName -> TODO()
                is ShoppingListOperation.RenameList -> TODO()
                is ShoppingListOperation.SetItemCheckedStatus -> TODO()
            }

        fun shoppingListApiOperationToDatabasePendingListOperation(apiOperation: ShoppingListApiOperation): DbPendingListOperation {
            val serializedOp: String = json.encodeToString(apiOperation)
            return DbPendingListOperation(
                id = 0L, // Auto generated
                opType = apiOperation.op,
                serializedOp = serializedOp,
            )
        }

        fun shoppingListApiOperationToShoppingListOperation(apiOperation: ShoppingListApiOperation): ShoppingListOperation {
            return when (apiOperation.op) {
                ShoppingListOperationConstants.CREATE.op -> {
                    val castedOp = apiOperation as ShoppingListApiOperation.Create
                    ShoppingListOperation.Create(
                        castedOp.title,
                        castedOp.creator,
                    )
                }
                else -> {
                    return ShoppingListOperation.Create(
                        "",
                        ListCreator(1L, ""),
                    )
                }
            }
        }

        @Throws(IllegalArgumentException::class)
        fun pendingListOperationToShoppingListApiOperation(dbPendingListOperation: DbPendingListOperation): ShoppingListApiOperation =
            when (dbPendingListOperation.opType) {
                ShoppingListOperationConstants.CREATE.op -> {
                    val deserializedOp = json.decodeFromString<ShoppingListApiOperation.Create>(dbPendingListOperation.serializedOp)
                    deserializedOp
                }
                ShoppingListOperationConstants.DELETE_LIST.op -> {
                    val deserializedOp = Json.decodeFromString<ShoppingListApiOperation.DeleteList>(dbPendingListOperation.serializedOp)
                    deserializedOp
                }
                ShoppingListOperationConstants.RENAME_LIST.op -> {
                    val deserializedOp = Json.decodeFromString<ShoppingListApiOperation.RenameList>(dbPendingListOperation.serializedOp)
                    deserializedOp
                }
                ShoppingListOperationConstants.ADD_ITEM.op -> {
                    val deserializedOp = Json.decodeFromString<ShoppingListApiOperation.AddItem>(dbPendingListOperation.serializedOp)
                    deserializedOp
                }
                ShoppingListOperationConstants.REMOVE_ITEM.op -> {
                    val deserializedOp = Json.decodeFromString<ShoppingListApiOperation.RemoveItem>(dbPendingListOperation.serializedOp)
                    deserializedOp
                }
                ShoppingListOperationConstants.CHANGE_QUANTITY_ITEM.op -> {
                    val deserializedOp =
                        Json.decodeFromString<ShoppingListApiOperation.ChangeQuantityItem>(
                            dbPendingListOperation.serializedOp,
                        )
                    deserializedOp
                }
                ShoppingListOperationConstants.TOGGLE_ITEM.op -> {
                    val deserializedOp = Json.decodeFromString<ShoppingListApiOperation.ToggleItem>(dbPendingListOperation.serializedOp)
                    deserializedOp
                }
                else -> {
                    throw IllegalArgumentException("given operation ${dbPendingListOperation.opType} cannot be handled")
                }
            }
    }
}
