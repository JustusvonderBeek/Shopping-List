package com.cloudsheeptech.shoppinglist.data.list

import com.cloudsheeptech.shoppinglist.data.items.ApiItem
import com.cloudsheeptech.shoppinglist.data.ListCreator
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import java.time.Duration
import java.time.OffsetDateTime
import kotlin.math.abs

@Serializable
data class ApiShoppingList @OptIn(ExperimentalSerializationApi::class) constructor(
    @JsonNames("listId")
    var listId : Long,
    @JsonNames("title")
    var title : String,
    @JsonNames("createdBy")
    var createdBy : ListCreator,
    @JsonNames("createdAt")
    @Contextual
    var createdAt : OffsetDateTime,
    @JsonNames("lastUpdated")
    @Contextual
    var lastUpdated : OffsetDateTime,
    @JsonNames("items")
    var items : MutableList<ApiItem>
) {

    override fun equals(other: Any?): Boolean {
        val timeThreshhold = Duration.ofSeconds(3)
        if (other is ApiShoppingList) {
            return other.listId == this.listId &&
                    other.title == this.title &&
                    other.createdBy.onlineId == this.createdBy.onlineId &&
                    Duration.ofNanos(abs(other.lastUpdated.toEpochSecond() - this.lastUpdated.toEpochSecond())) < timeThreshhold &&
                    other.items.size == this.items.size &&
                    // Make more efficient for longer lists... O(n*m)
                    this.items.all { item -> null != other.items.firstOrNull { oItem -> item.name == oItem.name } }
        }
        return false
    }

    override fun hashCode(): Int {
        var result = listId.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + createdBy.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + lastUpdated.hashCode()
        result = 31 * result + items.hashCode()
        return result
    }
}