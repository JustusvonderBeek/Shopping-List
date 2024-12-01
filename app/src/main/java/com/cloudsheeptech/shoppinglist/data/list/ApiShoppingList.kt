package com.cloudsheeptech.shoppinglist.data.list

import com.cloudsheeptech.shoppinglist.data.OffsetDateTimeUtil
import com.cloudsheeptech.shoppinglist.data.items.ApiItem
import com.cloudsheeptech.shoppinglist.data.onlineUser.ListCreator
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import java.time.OffsetDateTime

@Serializable
data class ApiShoppingList
    @OptIn(ExperimentalSerializationApi::class)
    constructor(
        @JsonNames("listId")
        var listId: Long,
        @JsonNames("title")
        var title: String,
        @JsonNames("createdBy")
        var createdBy: ListCreator,
        @JsonNames("createdAt")
        @Contextual
        var createdAt: OffsetDateTime,
        @JsonNames("lastUpdated")
        @Contextual
        var lastUpdated: OffsetDateTime,
        @JsonNames("items")
        var items: MutableList<ApiItem>,
        @JsonNames("version")
        var version: Long,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ApiShoppingList

            if (listId != other.listId) return false
            if (title != other.title) return false
            if (createdBy != other.createdBy) return false
            if (!OffsetDateTimeUtil.areDateTimesEqual(createdAt, other.createdAt)) return false
            if (!OffsetDateTimeUtil.areDateTimesEqual(createdAt, other.createdAt)) return false
            if (items != other.items) return false
            if (version != other.version) return false

            return true
        }

        override fun hashCode(): Int {
            var result = listId.hashCode()
            result = 31 * result + title.hashCode()
            result = 31 * result + createdBy.hashCode()
            result = 31 * result + createdAt.hashCode()
            result = 31 * result + lastUpdated.hashCode()
            result = 31 * result + items.hashCode()
            result = 31 * result + version.hashCode()
            return result
        }
    }
