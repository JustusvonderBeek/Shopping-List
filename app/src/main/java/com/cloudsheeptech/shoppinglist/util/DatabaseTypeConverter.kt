package com.cloudsheeptech.shoppinglist.util

import androidx.room.TypeConverter
import com.cloudsheeptech.shoppinglist.list.model.ListCreator
import com.cloudsheeptech.shoppinglist.list.model.PendingListOperation
import com.cloudsheeptech.shoppinglist.list.model.ShoppingListApiOperation
import com.cloudsheeptech.shoppinglist.list.model.ShoppingListOperationConstants
import com.cloudsheeptech.shoppinglist.list.model.ShoppingListPK
import com.cloudsheeptech.shoppinglist.user.model.AppUser
import kotlinx.serialization.json.Json
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class DatabaseTypeConverter {
    @TypeConverter
    fun offsetDateToString(date: OffsetDateTime): String {
        val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        return date.format(formatter)
    }

    @TypeConverter
    fun stringToOffsetDate(date: String): OffsetDateTime {
        val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        return OffsetDateTime.parse(date, formatter)
    }

    @TypeConverter
    fun userToString(user: AppUser): String = Json.encodeToString(user)

    @TypeConverter
    fun stringToUser(string: String): AppUser = Json.decodeFromString<AppUser>(string)

    @TypeConverter
    fun listCreatorToString(listCreator: ListCreator): String = Json.encodeToString(listCreator)

    @TypeConverter
    fun stringToListCreator(string: String): ListCreator = Json.decodeFromString(string)

    @TypeConverter
    fun shoppingListApiOperationToPendingListOperation(apiOperation: ShoppingListApiOperation): PendingListOperation {
        val serializedOp: String = Json.encodeToString(apiOperation)
        return PendingListOperation(
            id = 0L, // Auto generated
            opType = apiOperation.op,
            serializedOp = serializedOp,
        )
    }

    @TypeConverter
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
                val deserializedOp = Json.decodeFromString<ShoppingListApiOperation.ChangeQuantityItem>(pendingListOperation.serializedOp)
                deserializedOp
            }
            ShoppingListOperationConstants.TOGGLE_ITEM.op -> {
                val deserializedOp = Json.decodeFromString<ShoppingListApiOperation.ToggleItem>(pendingListOperation.serializedOp)
                deserializedOp
            }
            else -> {
                ShoppingListApiOperation.Create(
                    ShoppingListPK(0L, 0L),
                    "",
                    ListCreator(0L, ""),
                    ShoppingListOperationConstants.CREATE.op,
                )
            }
        }
}
