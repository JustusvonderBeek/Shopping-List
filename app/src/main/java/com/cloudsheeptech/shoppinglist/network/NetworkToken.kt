package com.cloudsheeptech.shoppinglist.network

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class NetworkToken @OptIn(ExperimentalSerializationApi::class) constructor(
    @JsonNames("token")
    var token: String
)
