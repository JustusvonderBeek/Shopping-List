package com.cloudsheeptech.shoppinglist.pricing.model

import com.cloudsheeptech.shoppinglist.list.model.QuantityType
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class ItemPrice(
    var itemId: Long,
    @Contextual
    var price: BigDecimal,
    var quantity: Float,
    var quantityType: QuantityType,
)
