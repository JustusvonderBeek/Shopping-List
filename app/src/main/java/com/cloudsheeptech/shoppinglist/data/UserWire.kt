package com.cloudsheeptech.shoppinglist.data

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class UserWire(
    var ID : Long,
    var Username : String,
    var Password : String,
    @Contextual
    var Created : OffsetDateTime,
    @Contextual
    var LastLogin : OffsetDateTime,
)
