package com.cloudsheeptech.shoppinglist.data.recipe

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ApiIngredient(
    var id: Long,
    var name: String,
    val icon: String,
    var quantity: Int,
    val quantityType: String,
)
