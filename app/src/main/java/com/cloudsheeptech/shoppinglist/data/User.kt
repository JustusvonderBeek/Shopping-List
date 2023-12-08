package com.cloudsheeptech.shoppinglist.data

import kotlinx.serialization.Serializable

@Serializable
data class User(
    var ID : Long,
    var Name : String,
    var FavouriteRecipe : Long
)
