package com.cloudsheeptech.shoppinglist.data

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class ListShare(
    var ListId : Long,
    var CreatedBy : Long,
    var SharedWith : Long,
    @Contextual
    var Created : OffsetDateTime,
)
