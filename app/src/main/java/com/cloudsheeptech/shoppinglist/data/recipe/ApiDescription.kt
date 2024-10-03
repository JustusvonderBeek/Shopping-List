package com.cloudsheeptech.shoppinglist.data.recipe

import kotlinx.serialization.Serializable

@Serializable
data class ApiDescription (
    var order: Int,
    var step: String,
)