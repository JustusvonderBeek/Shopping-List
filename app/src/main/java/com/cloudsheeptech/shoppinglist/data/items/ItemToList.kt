package com.cloudsheeptech.shoppinglist.data.items

import androidx.room.Entity
import androidx.room.ForeignKey
import com.cloudsheeptech.shoppinglist.data.list.DbShoppingList

@Entity(
    tableName = "item_to_list_mapping",
    primaryKeys = ["itemId", "listId", "createdBy"],
    foreignKeys = [
        ForeignKey(
            entity = DbItem::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = DbShoppingList::class,
            parentColumns = ["id"],
            childColumns = ["ListId"],
            onDelete = ForeignKey.CASCADE,
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
