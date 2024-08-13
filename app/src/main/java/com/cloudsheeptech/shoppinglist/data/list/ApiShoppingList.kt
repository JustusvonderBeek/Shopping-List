package com.cloudsheeptech.shoppinglist.data.list

import com.cloudsheeptech.shoppinglist.data.items.ItemWire
import com.cloudsheeptech.shoppinglist.data.ListCreator
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import java.time.OffsetDateTime

@Serializable
data class ApiShoppingList @OptIn(ExperimentalSerializationApi::class) constructor(
    @JsonNames("listId")
    var listId : Long,
    @JsonNames("title")
    var title : String,
    @JsonNames("createdBy")
    var createdBy : ListCreator,
    @JsonNames("createdAt")
    @Contextual
    var createdAt : OffsetDateTime,
    @JsonNames("lastUpdated")
    @Contextual
    var lastUpdated : OffsetDateTime,
    @JsonNames("items")
    var items : MutableList<ItemWire>
)