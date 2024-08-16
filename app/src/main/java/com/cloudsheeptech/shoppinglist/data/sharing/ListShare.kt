package com.cloudsheeptech.shoppinglist.data.sharing

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class ListShare(
    var CreatedBy : Long,
    var SharedWith : List<Long>,
    @Contextual
    var Created : OffsetDateTime,
)
