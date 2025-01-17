package com.cloudsheeptech.shoppinglist.data.sharing.recipe

import androidx.room.Entity
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "recipe_share", primaryKeys = ["recipeId", "createdBy", "sharedWithUserId"])
data class RecipeShare(
    var recipeId: Long,
    var createdBy: Long,
    var sharedWithUserId: Long
)
