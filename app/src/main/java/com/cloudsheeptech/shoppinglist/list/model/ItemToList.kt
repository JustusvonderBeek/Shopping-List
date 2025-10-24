package com.cloudsheeptech.shoppinglist.list.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "item_to_list_mapping",
    primaryKeys = ["itemId", "listId", "createdBy"],
    indices = [Index(value = ["itemId", "listId", "createdBy"])],
    foreignKeys = [
        ForeignKey(
            entity = DbItem::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.Companion.CASCADE,
        ),
        ForeignKey(
            entity = DbShoppingList::class,
            parentColumns = ["listId", "createdBy"],
            childColumns = ["listId", "createdBy"],
            onDelete = ForeignKey.Companion.CASCADE,
        ),
    ],
)
data class ItemToList(
    var itemId: Long,
    var listId: Long,
    var createdBy: Long,
    var quantity: Long,
    var quantityType: QuantityType,
    var checked: Boolean,
    var addedBy: Long,
)
