package com.cloudsheeptech.shoppinglist.data

import androidx.room.Entity
import kotlinx.serialization.Serializable

@Serializable
@Entity
data class User(
    var ID : Long = 0L,
    var Name : String = "",
    var FavouriteRecipe : Long = -1L
)
