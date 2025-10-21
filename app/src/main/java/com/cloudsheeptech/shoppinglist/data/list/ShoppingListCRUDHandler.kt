package com.cloudsheeptech.shoppinglist.data.list

import com.cloudsheeptech.shoppinglist.data.core.AbstractCRUDHandler
import com.cloudsheeptech.shoppinglist.data.core.DataSource
import java.time.OffsetDateTime
import javax.inject.Inject

class ShoppingListCRUDHandler
    @Inject
    constructor(
        override val dataSource: DataSource<DbShoppingList, Pair<Long, Long>>,
    ) : AbstractCRUDHandler<DbShoppingList, Pair<Long, Long>>() {
        suspend fun create(
            listTitle: String,
            createdBy: Long,
        ): DbShoppingList? {
            val newList =
                DbShoppingList(
                    listId = 0L,
                    title = listTitle,
                    createdBy = createdBy,
                    createdByName = "",
                    lastSynchronized = OffsetDateTime.now(),
                    version = 1L,
                )
            return super.create(newList)
        }
    }
