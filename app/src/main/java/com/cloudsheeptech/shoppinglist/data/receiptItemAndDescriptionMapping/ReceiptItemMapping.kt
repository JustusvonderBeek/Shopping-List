package com.cloudsheeptech.shoppinglist.data.receiptItemAndDescriptionMapping

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipe_to_item")
data class ReceiptItemMapping(
    @PrimaryKey(autoGenerate = true)
    var id: Long,
    var recipeId: Long,
    var createdBy: Long,
    var itemId: Long,
    var quantity: Int,
    var quantityType: String,
)
