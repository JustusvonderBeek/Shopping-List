package com.cloudsheeptech.shoppinglist.network.token

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@InternalSerializationApi
@Serializable
data class NetworkToken(
    var token: String
)