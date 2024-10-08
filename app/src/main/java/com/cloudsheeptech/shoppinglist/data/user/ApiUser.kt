package com.cloudsheeptech.shoppinglist.data.user

import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import java.time.OffsetDateTime

/*
* This class is meant to transmit all relevant data for the local and
* any online user in the JSON format.
* In case fields are not set (password, created, login for arbitrary users), they
* should be omitted but the other information still be parsed
*/

@Serializable
data class ApiUser @OptIn(ExperimentalSerializationApi::class) constructor(
    /* MUST BE CONTAINED. The identifier against the server. */
    @JsonNames("onlineId")
    var onlineId : Long = 0L,
    /* MUST BE CONTAINED. The human read-able identifier. */
    @JsonNames("username")
    var username : String,
    /* Authentication method during the login and JWT token generation. */
    @JsonNames("password")
    var password : String?,
    /* Only relevant for the first login. */
    @JsonNames("created")
    @Contextual
    var created : OffsetDateTime?,
    /* Only relevant for other users, in case the app wants to show this
    * information. For the current app, this info can be exchanged but is
    * not used and therefore not displayed. */
    @JsonNames("lastLogin")
    @Contextual
    var lastLogin : OffsetDateTime?,
)
