package com.cloudsheeptech.shoppinglist.data.receiptItemAndDescriptionMapping

import androidx.room.Entity

@Entity(
    tableName = "recipe_to_description",
    primaryKeys = ["recipeId", "createdBy", "descriptionOrder"]
)
data class ReceiptDescriptionMapping(
    var recipeId: Long,
    var createdBy: Long,
    var description: String,
    var descriptionOrder: Int,
)
