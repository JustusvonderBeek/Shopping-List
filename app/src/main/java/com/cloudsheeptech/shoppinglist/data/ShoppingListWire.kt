package com.cloudsheeptech.shoppinglist.data

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class ShoppingListWire(
    var ListId : Long,
    var Name : String,
    var CreatedBy : ListCreator,
    @Contextual
    var Created : OffsetDateTime,
    @Contextual
    var LastEdited : OffsetDateTime,
    var Items : MutableList<ItemWire>
) {

}