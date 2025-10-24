package com.cloudsheeptech.shoppinglist

import com.cloudsheeptech.shoppinglist.list.model.DbShoppingList
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import java.time.OffsetDateTime

class ShoppingListCRUDHandlerTest {
    private fun createNewTestList(): DbShoppingList =
        DbShoppingList(
            listId = 0L,
            title = "test",
            createdBy = 12345L,
            createdByName = "test",
            lastSynchronized = OffsetDateTime.now(),
            version = 1L,
        )

    @Test
    fun testCreateNewList() =
        runTest {
            val testSource = TestDataSource<DbShoppingList, Pair<Long, Long>>()
            val handler = ShoppingListCRUDHandler(testSource)

            val newList = createNewTestList()
            handler.create("test", 12345L)

            val comparison = newList.copy(listId = 1L, "test", 12345L)
            Assert.assertEquals(comparison, testSource.handedData)
        }
}
