package com.cloudsheeptech.shoppinglist

import com.cloudsheeptech.shoppinglist.data.items.ApiItem
import com.cloudsheeptech.shoppinglist.data.list.ShoppingList
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListMergeHelper
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListOperation
import com.cloudsheeptech.shoppinglist.data.onlineUser.ListCreator
import org.junit.Assert
import org.junit.Test
import java.time.OffsetDateTime

class ShoppingListMergeHelperTest {
    private fun createItem(
        num: Int,
        creatorId: Long = 234L,
    ): ApiItem {
        val item = ApiItem("item $num", "icon $num", num.toLong(), num % 2 == 0, creatorId)
        return item
    }

    private fun createDefaultList(numItems: Int): ShoppingList {
        val creatorId = 234L
        val list =
            ShoppingList(
                1L,
                "list 1",
                ListCreator(creatorId, "username"),
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                mutableListOf(),
                1L,
            )
        val items = mutableListOf<ApiItem>()
        for (i in 0 until numItems) {
            val item = createItem(i, creatorId)
            items.add(item)
        }
        list.items = items
        return list
    }

    @Test
    fun testAddItem() {
        val oldList = createDefaultList(3)
        val newList = createDefaultList(3)

        val newItem = createItem(4)
        newList.items.add(newItem)

        val operationsPerformed = ShoppingListMergeHelper.getListDelta(oldList, newList)
        Assert.assertTrue(operationsPerformed.contains(ShoppingListOperation.ADD_ITEM))
        Assert.assertNotNull(operationsPerformed.get(ShoppingListOperation.ADD_ITEM))
        Assert.assertEquals(1, operationsPerformed.get(ShoppingListOperation.ADD_ITEM)!!.size)
        Assert.assertEquals(
            listOf(newItem),
            operationsPerformed.get(ShoppingListOperation.ADD_ITEM),
        )
        Assert.assertFalse(operationsPerformed.contains(ShoppingListOperation.REMOVE_ITEM))
        Assert.assertFalse(operationsPerformed.contains(ShoppingListOperation.CHANGE_QUANTITY))
        Assert.assertFalse(operationsPerformed.contains(ShoppingListOperation.RENAME_LIST))
        Assert.assertFalse(operationsPerformed.contains(ShoppingListOperation.CHANGE_CREATOR))
    }

    @Test
    fun testRemoveItem() {
        val oldList = createDefaultList(3)
        val newList = createDefaultList(3)

        val removedItem = newList.items.removeAt(1)

        val operationsPerformed = ShoppingListMergeHelper.getListDelta(oldList, newList)
        Assert.assertTrue(operationsPerformed.contains(ShoppingListOperation.REMOVE_ITEM))
        Assert.assertNotNull(operationsPerformed.get(ShoppingListOperation.REMOVE_ITEM))
        Assert.assertEquals(1, operationsPerformed.get(ShoppingListOperation.REMOVE_ITEM)!!.size)
        Assert.assertEquals(
            listOf(removedItem),
            operationsPerformed.get(ShoppingListOperation.REMOVE_ITEM),
        )
        Assert.assertFalse(operationsPerformed.contains(ShoppingListOperation.ADD_ITEM))
        Assert.assertFalse(operationsPerformed.contains(ShoppingListOperation.CHANGE_QUANTITY))
        Assert.assertFalse(operationsPerformed.contains(ShoppingListOperation.RENAME_LIST))
        Assert.assertFalse(operationsPerformed.contains(ShoppingListOperation.CHANGE_CREATOR))
    }

    @Test
    fun testRenameList() {
        val oldList = createDefaultList(3)
        val newList = createDefaultList(3)

        newList.title = "super duper new title"

        val operationsPerformed = ShoppingListMergeHelper.getListDelta(oldList, newList)
        Assert.assertTrue(operationsPerformed.contains(ShoppingListOperation.RENAME_LIST))
        Assert.assertFalse(operationsPerformed.contains(ShoppingListOperation.ADD_ITEM))
        Assert.assertFalse(operationsPerformed.contains(ShoppingListOperation.CHANGE_QUANTITY))
        Assert.assertFalse(operationsPerformed.contains(ShoppingListOperation.REMOVE_ITEM))
        Assert.assertFalse(operationsPerformed.contains(ShoppingListOperation.CHANGE_CREATOR))
    }

    @Test
    fun testDifferentLists() {
        val oldList = createDefaultList(3)
        val newList = createDefaultList(3)

        newList.listId = 4321L

        var exceptionThrown = false
        try {
            val operationsPerformed = ShoppingListMergeHelper.getListDelta(oldList, newList)
        } catch (ex: IllegalArgumentException) {
            exceptionThrown = true
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test
    fun testMultipleChanges() {
        val oldList = createDefaultList(3)
        val newList = createDefaultList(3)

        val removedItems = mutableListOf<ApiItem>()
        removedItems.add(newList.items.removeAt(1))
        removedItems.add(newList.items.removeAt(1))

        val changedQuantityItem = newList.items.get(0)
        newList.items.get(0).quantity = 12
        changedQuantityItem.quantity = 12
        val changedQuantityItems = listOf(changedQuantityItem)

        val newItem = createItem(4)
        newList.items.add(newItem)
        val newItem2 = createItem(123)
        newList.items.add(newItem2)
        val newItem3 = createItem(321)
        newList.items.add(newItem3)
        val addedItems = listOf(newItem, newItem2, newItem3)

        newList.title = "super duper new title"

        newList.createdBy.onlineId = 665544L

        val operationsPerformed = ShoppingListMergeHelper.getListDelta(oldList, newList)
        Assert.assertTrue(operationsPerformed.contains(ShoppingListOperation.RENAME_LIST))
        Assert.assertTrue(operationsPerformed.contains(ShoppingListOperation.CHANGE_CREATOR))
        Assert.assertTrue(operationsPerformed.contains(ShoppingListOperation.ADD_ITEM))
        Assert.assertTrue(operationsPerformed.contains(ShoppingListOperation.REMOVE_ITEM))
        Assert.assertTrue(operationsPerformed.contains(ShoppingListOperation.CHANGE_QUANTITY))

        Assert.assertEquals(0, operationsPerformed.get(ShoppingListOperation.RENAME_LIST)!!.size)
        Assert.assertEquals(
            0,
            operationsPerformed.get(ShoppingListOperation.CHANGE_CREATOR)!!.size,
        )
        Assert.assertEquals(
            addedItems.size,
            operationsPerformed.get(ShoppingListOperation.ADD_ITEM)!!.size,
        )
        Assert.assertEquals(
            removedItems.size,
            operationsPerformed.get(ShoppingListOperation.REMOVE_ITEM)!!.size,
        )
        Assert.assertEquals(
            changedQuantityItems.size,
            operationsPerformed.get(ShoppingListOperation.CHANGE_QUANTITY)!!.size,
        )
        Assert.assertTrue(
            operationsPerformed.get(ShoppingListOperation.ADD_ITEM)!!.containsAll(addedItems),
        )
        Assert.assertTrue(
            operationsPerformed.get(ShoppingListOperation.REMOVE_ITEM)!!.containsAll(removedItems),
        )
        Assert.assertTrue(
            operationsPerformed
                .get(ShoppingListOperation.CHANGE_QUANTITY)!!
                .containsAll(changedQuantityItems),
        )
    }
}
