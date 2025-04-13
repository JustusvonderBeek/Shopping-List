package com.cloudsheeptech.shoppinglist.data.recipe

// Used to select custom columns from Room DB
// No @Entity Annotation needed
data class RecipeIdAndCreatedBy(
    val recipeId: Long,
    val createdBy: Long,
)
