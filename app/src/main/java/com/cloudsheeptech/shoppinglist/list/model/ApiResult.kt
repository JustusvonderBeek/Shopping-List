package com.cloudsheeptech.shoppinglist.list.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiResult(
    var result: String,
    var errorMessage: String?,
)
