package com.cloudsheeptech.shoppinglist.data.recipe

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class ApiRecipe(
    var onlineId: Long,
    var name: String,
    var createdBy: Long,
    @Contextual
    var createdAt: OffsetDateTime,
    @Contextual
    var lastUpdated: OffsetDateTime,
    var ingredients: List<ApiIngredient>,
    var description: List<ApiDescription>,
)
