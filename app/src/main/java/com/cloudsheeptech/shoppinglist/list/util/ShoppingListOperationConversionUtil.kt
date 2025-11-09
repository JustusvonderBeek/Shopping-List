package com.cloudsheeptech.shoppinglist.list.util

import com.cloudsheeptech.shoppinglist.list.model.PendingListOperation
import com.cloudsheeptech.shoppinglist.list.model.ShoppingListApiOperation
import com.cloudsheeptech.shoppinglist.list.model.ShoppingListOperationConstants
import kotlinx.serialization.json.Json

class ShoppingListOperationConversionUtil {
    companion object {
        fun shoppingListApiOperationToPendingListOperation(apiOperation: ShoppingListApiOperation): PendingListOperation {
            val serializedOp: String = Json.encodeToString(apiOperation)
            return PendingListOperation(
                id = 0L, // Auto generated
                opType = apiOperation.op,
                serializedOp = serializedOp,
            )
        }

        @Throws(IllegalArgumentException::class)
        fun pendingListOperationToShoppingListApiOperation(pendingListOperation: PendingListOperation): ShoppingListApiOperation =
            when (pendingListOperation.opType) {
                ShoppingListOperationConstants.CREATE.op -> {
                    val deserializedOp = Json.decodeFromString<ShoppingListApiOperation.Create>(pendingListOperation.serializedOp)
                    deserializedOp
                }
                ShoppingListOperationConstants.DELETE_LIST.op -> {
                    val deserializedOp = Json.decodeFromString<ShoppingListApiOperation.DeleteList>(pendingListOperation.serializedOp)
                    deserializedOp
                }
                ShoppingListOperationConstants.RENAME_LIST.op -> {
                    val deserializedOp = Json.decodeFromString<ShoppingListApiOperation.RenameList>(pendingListOperation.serializedOp)
                    deserializedOp
                }
                ShoppingListOperationConstants.ADD_ITEM.op -> {
                    val deserializedOp = Json.decodeFromString<ShoppingListApiOperation.AddItem>(pendingListOperation.serializedOp)
                    deserializedOp
                }
                ShoppingListOperationConstants.REMOVE_ITEM.op -> {
                    val deserializedOp = Json.decodeFromString<ShoppingListApiOperation.RemoveItem>(pendingListOperation.serializedOp)
                    deserializedOp
                }
                ShoppingListOperationConstants.CHANGE_QUANTITY_ITEM.op -> {
                    val deserializedOp =
                        Json.decodeFromString<ShoppingListApiOperation.ChangeQuantityItem>(
                            pendingListOperation.serializedOp,
                        )
                    deserializedOp
                }
                ShoppingListOperationConstants.TOGGLE_ITEM.op -> {
                    val deserializedOp = Json.decodeFromString<ShoppingListApiOperation.ToggleItem>(pendingListOperation.serializedOp)
                    deserializedOp
                }
                else -> {
                    throw IllegalArgumentException("given operation ${pendingListOperation.opType} cannot be handled")
                }
            }
    }
}
