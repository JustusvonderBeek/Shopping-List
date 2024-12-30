package com.cloudsheeptech.shoppinglist.data.recipe

import com.cloudsheeptech.shoppinglist.data.onlineUser.ListCreator
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import java.time.OffsetDateTime

@Serializable
data class ApiRecipe @OptIn(ExperimentalSerializationApi::class) constructor(
    @JsonNames("recipeId")
    var onlineId: Long,
    @JsonNames("name")
    var name: String,
    @JsonNames("createdBy")
    var createdBy: ListCreator,
    @JsonNames("createdAt")
    @Contextual
    var createdAt: OffsetDateTime,
    @JsonNames("lastUpdate")
    @Contextual
    var lastUpdated: OffsetDateTime,
    @JsonNames("version")
    var version: Int,
    @JsonNames("defaultPortion")
    var defaultPortion: Int,
    @JsonNames("ingredients")
    var ingredients: List<ApiIngredient>,
    @JsonNames("description")
    var description: List<ApiDescription>,
)
