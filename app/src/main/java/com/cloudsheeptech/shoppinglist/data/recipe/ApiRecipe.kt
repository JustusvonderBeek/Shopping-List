package com.cloudsheeptech.shoppinglist.data.recipe

import com.cloudsheeptech.shoppinglist.data.onlineUser.ListCreator
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class ApiRecipe(
    var onlineId: Long,
    var name: String,
    var createdBy: ListCreator,
    @Contextual
    var createdAt: OffsetDateTime,
    @Contextual
    var lastUpdated: OffsetDateTime,
    var ingredients: List<ApiIngredient>,
    var description: List<ApiDescription>,
)
