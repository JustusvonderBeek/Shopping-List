package com.cloudsheeptech.shoppinglist.list.repo

import com.cloudsheeptech.shoppinglist.list.model.AppItem
import com.cloudsheeptech.shoppinglist.list.model.ItemToggleStatus
import com.cloudsheeptech.shoppinglist.list.model.QuantityType
import com.cloudsheeptech.shoppinglist.list.model.ShoppingListOperation
import com.cloudsheeptech.shoppinglist.list.model.ShoppingListPK
import com.cloudsheeptech.shoppinglist.testUtil.TestUtil
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.DisableOnDebug
import org.junit.rules.TestRule
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.mockito.junit.MockitoJUnitRunner
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

@RunWith(MockitoJUnitRunner::class)
@FixMethodOrder(MethodSorters.DEFAULT)
class ShoppingListRepositoryTest {
    @JvmField
    @Rule
    val testRule: TestRule = DisableOnDebug(Timeout.seconds(500))

    private suspend fun createList() {
        val shoppingListApplication = TestUtil.shoppingListApplication
        val appUserRepository = shoppingListApplication.appUserRepository
        appUserRepository.create("test user")
        val testUser = appUserRepository.read()
        Assert.assertNotNull(testUser) // Even though this is not what we want to test, we need a valid online id in order to proceed

        val shoppingListRepository = shoppingListApplication.shoppingListRepository
        val newShoppingList = shoppingListRepository.create("new list")
        Assert.assertNotNull(newShoppingList)

        val emptyReadList =
            shoppingListRepository.read(
                ShoppingListPK(newShoppingList.listId, newShoppingList.createdBy.onlineId),
            )
        Assert.assertNotNull(emptyReadList)
        Assert.assertEquals(newShoppingList, emptyReadList)

        // Testing the same with items
        val newListWithItems = shoppingListRepository.create("new list with items")
        val operations = mutableListOf<ShoppingListOperation>()
        for (i in 1..3) {
            val item =
                AppItem(
                    name = "item $i",
                    icon = "icon $i",
                    quantity = i.toLong(),
                    quantityType = QuantityType.PIECES,
                    checked = false,
                    addedBy = testUser!!.OnlineID,
                    opCount = 0,
                )
            val addItem =
                ShoppingListOperation.AddItem(
                    ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId),
                    item,
                )
            operations.add(addItem)
            newListWithItems.items.add(item)
        }
        shoppingListRepository.update(operations)

        // Offline should always be correct, but online as well?
        val storedListWithItems =
            shoppingListRepository.read(
                ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId),
            )
        Assert.assertNotNull(storedListWithItems)
        Assert.assertEquals(newListWithItems, storedListWithItems)
    }

    @Test
    fun testCreateListOfflineOnly() =
        runTest(EmptyCoroutineContext, Duration.parse("3m")) {
            TestUtil.initialize(clearDatabase = true, true)
            createList()
        }

    @Test
    fun testCreateList() =
        runTest(EmptyCoroutineContext, Duration.parse("3m")) {
            TestUtil.initialize(clearDatabase = true, mockRemoteToDoNothing = false)
            createList()
        }

    private suspend fun getList() {
        val shoppingListApplication = TestUtil.shoppingListApplication
        val appUserRepository = shoppingListApplication.appUserRepository
        appUserRepository.create("test user")
        val testUser = appUserRepository.read()
        Assert.assertNotNull(testUser) // Even though this is not what we want to test, we need a valid online id in order to proceed

        // Testing the same with items
        val shoppingListRepository = shoppingListApplication.shoppingListRepository
        val newListWithItems = shoppingListRepository.create("new list with items")
        val operations = mutableListOf<ShoppingListOperation>()
        for (i in 1..3) {
            val item =
                AppItem(
                    name = "item $i",
                    icon = "icon $i",
                    quantity = i.toLong(),
                    quantityType = QuantityType.PIECES,
                    checked = false,
                    addedBy = testUser!!.OnlineID,
                    opCount = 0,
                )
            val addItem =
                ShoppingListOperation.AddItem(
                    ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId),
                    item,
                )
            operations.add(addItem)
            newListWithItems.items.add(item)
        }
        shoppingListRepository.update(operations)
        // Should have a list with 3 items now

        // Offline should always be correct, but online as well?
        val storedListWithItems =
            shoppingListRepository.read(
                ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId),
            )
        Assert.assertNotNull(storedListWithItems)
        Assert.assertEquals(newListWithItems, storedListWithItems)
    }

    @Test
    fun testGetListOfflineOnly() =
        runTest(EmptyCoroutineContext, Duration.parse("3m")) {
            TestUtil.initialize(clearDatabase = true, mockRemoteToDoNothing = true)
            getList()
        }

    @Test
    fun testGetOnlineList() =
        runTest {
            throw NotImplementedError("this test is not implemented yet")
            //            val (listRepo, userRepo) = createAppRepo()
            // We need to implement sharing for this first
        }

    private suspend fun removeItemOperation() {
        val shoppingListApplication = TestUtil.shoppingListApplication
        val appUserRepository = shoppingListApplication.appUserRepository
        appUserRepository.create("test user")
        val testUser = appUserRepository.read()
        Assert.assertNotNull(testUser) // Even though this is not what we want to test, we need a valid online id in order to proceed

        // Testing the same with items
        val shoppingListRepository = shoppingListApplication.shoppingListRepository
        val newListWithItems = shoppingListRepository.create("new list with items")
        val operations = mutableListOf<ShoppingListOperation>()
        for (i in 1..3) {
            val item =
                AppItem(
                    name = "item $i",
                    icon = "icon $i",
                    quantity = i.toLong(),
                    quantityType = QuantityType.PIECES,
                    checked = false,
                    addedBy = testUser!!.OnlineID,
                    opCount = 0,
                )
            val addItem =
                ShoppingListOperation.AddItem(
                    ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId),
                    item,
                )
            operations.add(addItem)
            newListWithItems.items.add(item)
        }
        shoppingListRepository.update(operations)

        var storedListWithItems =
            shoppingListRepository.read(ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId))
        Assert.assertNotNull(storedListWithItems)
        Assert.assertEquals(newListWithItems, storedListWithItems)

        Assert.assertTrue(newListWithItems.items.isNotEmpty())
        var rmvOp =
            ShoppingListOperation.RemoveItemByName(
                ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId),
                newListWithItems.items[0].name,
            )
        shoppingListRepository.update(listOf(rmvOp))

        newListWithItems.items.removeAt(0)
        storedListWithItems =
            shoppingListRepository.read(ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId))
        Assert.assertNotNull(storedListWithItems)
        Assert.assertEquals(newListWithItems, storedListWithItems)

        rmvOp =
            ShoppingListOperation.RemoveItemByName(
                ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId),
                "item with name that cannot be found",
            )
        shoppingListRepository.update(listOf(rmvOp))
        storedListWithItems =
            shoppingListRepository.read(ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId))
        Assert.assertNotNull(storedListWithItems)
        Assert.assertEquals(newListWithItems, storedListWithItems)
    }

    @Test
    fun testRemoveItemListOperationOfflineOnly() =
        runTest(EmptyCoroutineContext, Duration.parse("3m")) {
            TestUtil.initialize(clearDatabase = true, mockRemoteToDoNothing = true)
            removeItemOperation()
        }

    @Test
    fun testRemoveItemListOperation() =
        runTest(EmptyCoroutineContext, Duration.parse("3m")) {
            TestUtil.initialize(clearDatabase = true, mockRemoteToDoNothing = false)
            removeItemOperation()
        }

    private suspend fun changeQtyItemOperation() {
        val shoppingListApplication = TestUtil.shoppingListApplication
        val appUserRepository = shoppingListApplication.appUserRepository
        appUserRepository.create("test user")
        val testUser = appUserRepository.read()
        Assert.assertNotNull(testUser) // Even though this is not what we want to test, we need a valid online id in order to proceed

        // Testing the same with items
        val shoppingListRepository = shoppingListApplication.shoppingListRepository
        val newListWithItems = shoppingListRepository.create("new list with items")
        val operations = mutableListOf<ShoppingListOperation>()
        for (i in 1..3) {
            val item =
                AppItem(
                    name = "item $i",
                    icon = "icon $i",
                    quantity = i.toLong(),
                    quantityType = QuantityType.PIECES,
                    checked = false,
                    addedBy = testUser!!.OnlineID,
                    opCount = 0,
                )
            val addItem =
                ShoppingListOperation.AddItem(
                    ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId),
                    item,
                )
            operations.add(addItem)
            newListWithItems.items.add(item)
        }
        shoppingListRepository.update(operations)

        var storedListWithItems =
            shoppingListRepository.read(ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId))
        Assert.assertNotNull(storedListWithItems)
        Assert.assertEquals(newListWithItems, storedListWithItems)

        val newQuantity = 33L
        var qtyOp =
            ShoppingListOperation.ChangeQuantityOfItem(
                ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId),
                newListWithItems.items[0].name,
                newQuantity,
                null,
            )
        shoppingListRepository.update(listOf(qtyOp))

        newListWithItems.items[0].quantity = newQuantity
        newListWithItems.items[0].opCount = newListWithItems.items[0].opCount.plus(1)

        storedListWithItems =
            shoppingListRepository.read(ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId))
        Assert.assertNotNull(storedListWithItems)
        Assert.assertEquals(newListWithItems, storedListWithItems)

        qtyOp =
            ShoppingListOperation.ChangeQuantityOfItem(
                ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId),
                "definitely not existing item",
                newQuantity,
                null,
            )
        shoppingListRepository.update(listOf(qtyOp))
        storedListWithItems =
            shoppingListRepository.read(ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId))
        Assert.assertNotNull(storedListWithItems)
        Assert.assertEquals(newListWithItems, storedListWithItems)

        val negativeQuantity = -1L
        qtyOp =
            ShoppingListOperation.ChangeQuantityOfItem(
                ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId),
                newListWithItems.items[0].name,
                negativeQuantity,
                null,
            )
        shoppingListRepository.update(listOf(qtyOp))

        storedListWithItems =
            shoppingListRepository.read(ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId))
        Assert.assertNotNull(storedListWithItems)
        Assert.assertEquals(newListWithItems, storedListWithItems)

        val kgQuantity = 123L
        qtyOp =
            ShoppingListOperation.ChangeQuantityOfItem(
                ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId),
                newListWithItems.items[1].name,
                kgQuantity,
                QuantityType.KILO,
            )
        shoppingListRepository.update(listOf(qtyOp))

        newListWithItems.items[1].quantity = kgQuantity
        newListWithItems.items[1].quantityType = QuantityType.KILO
        newListWithItems.items[1].opCount = newListWithItems.items[1].opCount.plus(1)
        storedListWithItems =
            shoppingListRepository.read(ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId))
        Assert.assertNotNull(storedListWithItems)
        Assert.assertEquals(newListWithItems, storedListWithItems)
    }

    @Test
    fun testChangeQtyItemListOperationOfflineOnly() =
        runTest(EmptyCoroutineContext, Duration.parse("3m")) {
            TestUtil.initialize(clearDatabase = true, mockRemoteToDoNothing = true)
            changeQtyItemOperation()
        }

    @Test
    fun testChangeQtyItemListOperation() =
        runTest(EmptyCoroutineContext, Duration.parse("3m")) {
            TestUtil.initialize(clearDatabase = true, mockRemoteToDoNothing = false)
            changeQtyItemOperation()
        }

    private suspend fun toggleItemOperation() {
        val shoppingListApplication = TestUtil.shoppingListApplication
        val appUserRepository = shoppingListApplication.appUserRepository
        appUserRepository.create("test user")
        val testUser = appUserRepository.read()
        Assert.assertNotNull(testUser) // Even though this is not what we want to test, we need a valid online id in order to proceed

        // Testing the same with items
        val shoppingListRepository = shoppingListApplication.shoppingListRepository
        val newListWithItems = shoppingListRepository.create("new list with items")
        val operations = mutableListOf<ShoppingListOperation>()
        for (i in 1..3) {
            val item =
                AppItem(
                    name = "item $i",
                    icon = "icon $i",
                    quantity = i.toLong(),
                    quantityType = QuantityType.PIECES,
                    checked = false,
                    addedBy = testUser!!.OnlineID,
                    opCount = 0,
                )
            val addItem =
                ShoppingListOperation.AddItem(
                    ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId),
                    item,
                )
            operations.add(addItem)
            newListWithItems.items.add(item)
        }
        shoppingListRepository.update(operations)

        var storedListWithItems =
            shoppingListRepository.read(ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId))
        Assert.assertNotNull(storedListWithItems)
        Assert.assertEquals(newListWithItems, storedListWithItems)

        var qtyOp =
            ShoppingListOperation.SetItemCheckedStatus(
                ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId),
                newListWithItems.items[0].name,
                ItemToggleStatus.TRUE,
            )
        shoppingListRepository.update(listOf(qtyOp))

        newListWithItems.items[0].checked = true
        newListWithItems.items[0].opCount = newListWithItems.items[0].opCount.plus(1)
        storedListWithItems =
            shoppingListRepository.read(ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId))
        Assert.assertNotNull(storedListWithItems)
        Assert.assertEquals(newListWithItems, storedListWithItems)

        qtyOp =
            ShoppingListOperation.SetItemCheckedStatus(
                ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId),
                newListWithItems.items[0].name,
                ItemToggleStatus.TOGGLE,
            )
        shoppingListRepository.update(listOf(qtyOp))

        newListWithItems.items[0].checked = !newListWithItems.items[0].checked
        newListWithItems.items[0].opCount = newListWithItems.items[0].opCount.plus(1)
        storedListWithItems =
            shoppingListRepository.read(ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId))
        Assert.assertNotNull(storedListWithItems)
        Assert.assertEquals(newListWithItems, storedListWithItems)

        qtyOp =
            ShoppingListOperation.SetItemCheckedStatus(
                ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId),
                "item definitely not found",
                ItemToggleStatus.TOGGLE,
            )
        shoppingListRepository.update(listOf(qtyOp))
        storedListWithItems =
            shoppingListRepository.read(ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId))
        Assert.assertNotNull(storedListWithItems)
        Assert.assertEquals(newListWithItems, storedListWithItems)
    }

    @Test
    fun testToggleItemListOperationOfflineOnly() =
        runTest(EmptyCoroutineContext, Duration.parse("3m")) {
            TestUtil.initialize(clearDatabase = true, mockRemoteToDoNothing = true)
            toggleItemOperation()
        }

    @Test
    fun testToggleItemListOperation() =
        runTest(EmptyCoroutineContext, Duration.parse("3m")) {
            TestUtil.initialize(clearDatabase = true, mockRemoteToDoNothing = false)
            toggleItemOperation()
        }

    private suspend fun renameListOperation() {
        val shoppingListApplication = TestUtil.shoppingListApplication
        val appUserRepository = shoppingListApplication.appUserRepository
        appUserRepository.create("test user")
        val testUser = appUserRepository.read()
        Assert.assertNotNull(testUser) // Even though this is not what we want to test, we need a valid online id in order to proceed

        // Testing the same with items
        val shoppingListRepository = shoppingListApplication.shoppingListRepository
        val newListWithItems = shoppingListRepository.create("new list with items")
        val operations = mutableListOf<ShoppingListOperation>()
        for (i in 1..3) {
            val item =
                AppItem(
                    name = "item $i",
                    icon = "icon $i",
                    quantity = i.toLong(),
                    quantityType = QuantityType.PIECES,
                    checked = false,
                    addedBy = testUser!!.OnlineID,
                    opCount = 0,
                )
            val addItem =
                ShoppingListOperation.AddItem(
                    ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId),
                    item,
                )
            operations.add(addItem)
            newListWithItems.items.add(item)
        }
        shoppingListRepository.update(operations)

        var storedListWithItems =
            shoppingListRepository.read(ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId))
        Assert.assertNotNull(storedListWithItems)
        Assert.assertEquals(newListWithItems, storedListWithItems)

        val newName = "new list name with long name"
        val renameOp =
            ShoppingListOperation.RenameList(
                ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId),
                newName,
            )
        shoppingListRepository.update(listOf(renameOp))

        newListWithItems.title = newName
        storedListWithItems =
            shoppingListRepository.read(ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId))
        Assert.assertNotNull(storedListWithItems)
        Assert.assertEquals(newListWithItems, storedListWithItems)
    }

    @Test
    fun testRenameListOperationOfflineOnly() =
        runTest(EmptyCoroutineContext, Duration.parse("3m")) {
            TestUtil.initialize(clearDatabase = true, mockRemoteToDoNothing = true)
            renameListOperation()
        }

    private suspend fun deleteListOperation() {
        val shoppingListApplication = TestUtil.shoppingListApplication
        val appUserRepository = shoppingListApplication.appUserRepository
        appUserRepository.create("test user")
        val testUser = appUserRepository.read()
        Assert.assertNotNull(testUser) // Even though this is not what we want to test, we need a valid online id in order to proceed

        // Testing the same with items
        val shoppingListRepository = shoppingListApplication.shoppingListRepository
        val newListWithItems = shoppingListRepository.create("new list with items")
        val operations = mutableListOf<ShoppingListOperation>()
        for (i in 1..3) {
            val item =
                AppItem(
                    name = "item $i",
                    icon = "icon $i",
                    quantity = i.toLong(),
                    quantityType = QuantityType.PIECES,
                    checked = false,
                    addedBy = testUser!!.OnlineID,
                    opCount = 0,
                )
            val addItem =
                ShoppingListOperation.AddItem(
                    ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId),
                    item,
                )
            operations.add(addItem)
            newListWithItems.items.add(item)
        }
        shoppingListRepository.update(operations)

        var storedListWithItems =
            shoppingListRepository.read(ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId))
        Assert.assertNotNull(storedListWithItems)
        Assert.assertEquals(newListWithItems, storedListWithItems)

        val rmvOp =
            ShoppingListOperation.Delete(
                ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId),
            )
        shoppingListRepository.update(listOf(rmvOp))

        val deletedListWithItems =
            shoppingListRepository.read(ShoppingListPK(storedListWithItems!!.listId, storedListWithItems.createdBy.onlineId))
        Assert.assertNull(deletedListWithItems)
    }

    @Test
    fun testDeleteListOfflineOnly() =
        runTest(EmptyCoroutineContext, Duration.parse("3m")) {
            TestUtil.initialize(clearDatabase = true, mockRemoteToDoNothing = true)
            deleteListOperation()
        }
}
