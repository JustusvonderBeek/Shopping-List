package com.cloudsheeptech.shoppinglist.recipe.model

// Used to select custom columns from Room DB
// No @Entity Annotation needed
data class RecipeIdAndCreatedBy(
    val recipeId: Long,
    val createdBy: Long,
)
