package com.cloudsheeptech.shoppinglist.list.model

import kotlinx.serialization.Serializable

@Serializable
data class AppItem(
    var name: String,
    var icon: String,
    var quantity: Long,
    var quantityType: QuantityType,
    var checked: Boolean,
    var addedBy: Long,
    var opCount: Int,
) {
    // TODO: Eventuell verschieben in helper wenn alle anderen auch da
    fun toEntities(identifier: Pair<Long, Long>): Pair<DbItem, ItemToList> {
        val baseItem =
            DbItem(
                name = this.name,
                icon = this.icon,
            )
        val itemMapping =
            ItemToList(
                item = this.name,
                listId = identifier.first,
                createdBy = identifier.second,
                quantity = this.quantity,
                quantityType = this.quantityType,
                checked = this.checked,
                addedBy = this.addedBy,
                opCount = 0,
            )

        return Pair(baseItem, itemMapping)
    }

    companion object {
        fun fromBaseAndMapping(
            baseItem: DbItem,
            mapping: ItemToList,
        ): AppItem =
            AppItem(
                name = baseItem.name,
                icon = baseItem.icon,
                quantity = mapping.quantity,
                quantityType = mapping.quantityType,
                checked = mapping.checked,
                addedBy = mapping.addedBy,
                opCount = mapping.opCount,
            )
    }
}
