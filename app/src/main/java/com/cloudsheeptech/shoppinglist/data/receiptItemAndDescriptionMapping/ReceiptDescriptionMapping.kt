package com.cloudsheeptech.shoppinglist.data.receiptItemAndDescriptionMapping

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipt_to_description")
data class ReceiptDescriptionMapping(
    @PrimaryKey(autoGenerate = true)
    var id : Long,
    var receiptId: Long,
    var createdBy: Long,
    var description: String,
    var descriptionOrder: Int,
)
