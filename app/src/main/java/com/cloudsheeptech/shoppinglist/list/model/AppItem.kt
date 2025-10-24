package com.cloudsheeptech.shoppinglist.list.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class AppItem(
    // We need this ID locally, because of actions like toggle the checkbox etc.
    @Transient
    var id: Long? = null,
    var name: String,
    var icon: String,
    var quantity: Long,
    var quantityType: QuantityType,
    var checked: Boolean,
    var addedBy: Long,
) {
    // TODO: Eventuell verschieben in helper wenn alle anderen auch da
    fun toEntities(identifier: Pair<Long, Long>): Pair<DbItem, ItemToList> {
        val baseItem =
            DbItem(
                id = this.id ?: 0L,
                name = this.name,
                icon = this.icon,
            )
        val itemMapping =
            ItemToList(
                itemId = this.id ?: 0L,
                listId = identifier.first,
                createdBy = identifier.second,
                quantity = this.quantity,
                quantityType = this.quantityType,
                checked = this.checked,
                addedBy = this.addedBy,
            )

        return Pair(baseItem, itemMapping)
    }

    companion object {
        fun fromBaseAndMapping(
            baseItem: DbItem,
            mapping: ItemToList,
        ): AppItem =
            AppItem(
                id = baseItem.id,
                name = baseItem.name,
                icon = baseItem.icon,
                quantity = mapping.quantity,
                quantityType = mapping.quantityType,
                checked = mapping.checked,
                addedBy = mapping.addedBy,
            )
    }
}
