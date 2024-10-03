package com.cloudsheeptech.shoppinglist.data.recipe

import kotlinx.serialization.Serializable

@Serializable
data class ApiIngredient(
    var id: Long,
    var name: String,
    var icon: String,
    var quantity: Int,
    var quantityType: String,
)
