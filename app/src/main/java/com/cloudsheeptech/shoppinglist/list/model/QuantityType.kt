package com.cloudsheeptech.shoppinglist.list.model

import kotlinx.serialization.Serializable

@Serializable
enum class QuantityType {
    PIECES,
    KILO,
    LITER,
}
