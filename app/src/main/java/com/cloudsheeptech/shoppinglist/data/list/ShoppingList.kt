package com.cloudsheeptech.shoppinglist.data.list

import com.cloudsheeptech.shoppinglist.data.OffsetDateTimeUtil
import com.cloudsheeptech.shoppinglist.data.core.EntityIdentifier
import com.cloudsheeptech.shoppinglist.data.items.AppItem
import com.cloudsheeptech.shoppinglist.data.items.DbItem
import com.cloudsheeptech.shoppinglist.data.items.ItemToList
import com.cloudsheeptech.shoppinglist.data.onlineUser.ListCreator
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

@Serializable
data class ShoppingList(
    var listId: Long,
    var createdBy: ListCreator,
    var title: String,
    @Contextual
    var synchronized: OffsetDateTime,
    var items: MutableList<AppItem> = mutableListOf(),
) : EntityIdentifier<ShoppingListPK> {
    override fun getId(): ShoppingListPK = ShoppingListPK(listId, createdBy.onlineId)

    fun toEntities(): Triple<DbShoppingList, List<DbItem>, List<ItemToList>> {
        val dbList =
            DbShoppingList(
                listId = this.listId,
                createdBy = this.createdBy.onlineId,
                title = this.title,
                synchronized = this.synchronized,
            )
        val dbItemsAndItemToListMapping =
            this.items
                .map { item -> item.toEntities(Pair(this.listId, this.createdBy.onlineId)) }
                .unzip()

        return Triple(dbList, dbItemsAndItemToListMapping.first, dbItemsAndItemToListMapping.second)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShoppingList

        if (listId != other.listId) return false
        if (title != other.title) return false
        if (createdBy != other.createdBy) return false
        val thisLastSync = synchronized.truncatedTo(ChronoUnit.SECONDS)
        val otherLastSync = other.synchronized.truncatedTo(ChronoUnit.SECONDS)

        if (!OffsetDateTimeUtil.areDateTimesEqual(thisLastSync, otherLastSync)) return false
        if (items != other.items) return false

        return true
    }

    override fun hashCode(): Int {
        var result = listId.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + createdBy.hashCode()
        result = 31 * result + synchronized.hashCode()
        result = 31 * result + items.hashCode()
        return result
    }

    override fun toString(): String = "$title ($listId,${createdBy.onlineId})"
}
