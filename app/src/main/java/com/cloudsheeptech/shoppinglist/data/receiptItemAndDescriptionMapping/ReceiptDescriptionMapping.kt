package com.cloudsheeptech.shoppinglist.data.receiptItemAndDescriptionMapping

import androidx.room.Entity

@Entity(tableName = "receipt_to_description", primaryKeys = ["recipeId", "createdBy"])
data class ReceiptDescriptionMapping(
    var id: Long,
    var recipeId: Long,
    var createdBy: Long,
    var description: String,
    var descriptionOrder: Int,
)
