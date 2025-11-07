package com.cloudsheeptech.shoppinglist.network.token

import kotlinx.serialization.Serializable

@Serializable
data class NetworkToken(
    var token: String,
)
