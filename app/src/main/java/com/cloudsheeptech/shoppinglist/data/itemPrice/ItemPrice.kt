package com.cloudsheeptech.shoppinglist.data.itemPrice

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class ItemPrice(
    var itemId: Long,
    @Contextual
    var price: BigDecimal,
    var quantity: Float,
    var quantityType: Float,
)
