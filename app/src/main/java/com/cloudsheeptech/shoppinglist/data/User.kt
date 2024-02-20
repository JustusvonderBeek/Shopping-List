package com.cloudsheeptech.shoppinglist.data

import kotlinx.serialization.Serializable

@Serializable
data class User(
    var ID : Long,
    var Username : String,
    var Password : String,
) {
    constructor(dbUser : DatabaseUser) : this(dbUser.UserId, dbUser.Username, dbUser.Password)
}
