package com.cloudsheeptech.shoppinglist.recipe.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiDescription(
    var order: Int,
    var step: String,
)
