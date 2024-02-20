package com.cloudsheeptech.shoppinglist.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "user")
data class DatabaseUser(
    @PrimaryKey(autoGenerate = false)
    val ID : Long = 1L,
    var UserId : Long = 0L,
    var Username : String,
    var Password : String
) {
    constructor(user: User) : this(1L, user.ID, user.Username, user.Password)
}
