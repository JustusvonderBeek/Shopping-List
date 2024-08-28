package com.cloudsheeptech.shoppinglist.data.itemToReceiptMapping

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "item_to_receipt_mapping")
data class DbItemToReceiptMapping(
    @PrimaryKey(autoGenerate = true)
    var id: Long,
    var receiptId: Long,
    var itemId: Long,
    var quantity: Int,
    var quantityType: String,
)
