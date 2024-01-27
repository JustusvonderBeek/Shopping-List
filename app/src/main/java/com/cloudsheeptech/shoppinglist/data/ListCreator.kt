package com.cloudsheeptech.shoppinglist.data

import kotlinx.serialization.Serializable

@Serializable
data class ListCreator(
    var ID : Long,
    var Name : String,
)
