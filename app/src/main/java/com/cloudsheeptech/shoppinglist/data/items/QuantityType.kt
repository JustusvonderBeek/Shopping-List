package com.cloudsheeptech.shoppinglist.data.items

import kotlinx.serialization.Serializable

@Serializable
enum class QuantityType {
    PIECES,
    KILO,
    LITER,
}
