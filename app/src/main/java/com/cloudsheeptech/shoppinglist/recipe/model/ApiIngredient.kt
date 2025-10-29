package com.cloudsheeptech.shoppinglist.recipe.model

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ApiIngredient(
    var id: Long = 0L,
    var name: String,
    val icon: String,
    var quantity: Int,
    val quantityType: String = "#",
)
