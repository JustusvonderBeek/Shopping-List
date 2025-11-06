package com.cloudsheeptech.shoppinglist.list.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "item_to_list_mapping",
    primaryKeys = ["item", "listId", "createdBy"],
    indices = [Index(value = ["item", "listId", "createdBy"])],
    foreignKeys = [
        ForeignKey(
            entity = DbItem::class,
            parentColumns = ["name"],
            childColumns = ["item"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = DbShoppingList::class,
            parentColumns = ["listId", "createdBy"],
            childColumns = ["listId", "createdBy"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ItemToList(
    var item: String,
    var listId: Long,
    var createdBy: Long,
    var quantity: Long = 1,
    var quantityType: QuantityType = QuantityType.PIECES,
    var checked: Boolean = false,
    var addedBy: Long,
    @ColumnInfo(defaultValue = "0")
    var opCount: Int = 0,
)
