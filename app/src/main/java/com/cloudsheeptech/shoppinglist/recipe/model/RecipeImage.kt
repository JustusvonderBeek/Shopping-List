package com.cloudsheeptech.shoppinglist.recipe.model

import androidx.room.Entity

@Entity(tableName = "recipe_images", primaryKeys = ["recipeId", "createdBy", "imageId"])
data class RecipeImage(
    var recipeId: Long,
    var createdBy: Long,
    var imageId: Int,
    var fileLocation: String,
)
