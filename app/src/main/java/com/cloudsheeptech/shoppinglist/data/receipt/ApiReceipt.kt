package com.cloudsheeptech.shoppinglist.data.receipt

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class ApiReceipt(
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
