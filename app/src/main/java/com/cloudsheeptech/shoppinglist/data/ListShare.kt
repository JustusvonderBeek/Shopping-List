package com.cloudsheeptech.shoppinglist.data

import kotlinx.serialization.Serializable

@Serializable
data class ListShare(
    var ID : Long,
    var ListId : Long,
    var SharedWith : Long
)
