package com.cloudsheeptech.shoppinglist.data.receipt

import androidx.lifecycle.MutableLiveData
import kotlinx.serialization.Serializable

@Serializable
data class ApiDescription (
    var order: Int,
    var step: String,
)