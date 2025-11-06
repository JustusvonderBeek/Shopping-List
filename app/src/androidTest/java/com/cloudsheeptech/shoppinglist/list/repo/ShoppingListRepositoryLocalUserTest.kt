package com.cloudsheeptech.shoppinglist.list.repo

import com.cloudsheeptech.shoppinglist.list.model.AppItem
import com.cloudsheeptech.shoppinglist.list.model.ItemToggleStatus
import com.cloudsheeptech.shoppinglist.list.model.QuantityType
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
class ShoppingListRepositoryLocalUserTest {
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
            shoppingListRepository.insertItem(ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId), item)
            newListWithItems.items.add(item)
        }

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
            shoppingListRepository.insertItem(ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId), item)
            newListWithItems.items.add(item)
        }
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

    private suspend fun removeItemByNameOperation() {
        val shoppingListApplication = TestUtil.shoppingListApplication
        val appUserRepository = shoppingListApplication.appUserRepository
        appUserRepository.create("test user")
        val testUser = appUserRepository.read()
        Assert.assertNotNull(testUser) // Even though this is not what we want to test, we need a valid online id in order to proceed

        val shoppingListRepository = shoppingListApplication.shoppingListRepository
        val newListWithItems = shoppingListRepository.create("new list with items")
        val listPk = ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId)
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
            shoppingListRepository.insertItem(listPk, item)
            newListWithItems.items.add(item)
        }

        var storedListWithItems =
            shoppingListRepository.read(listPk)
        Assert.assertNotNull(storedListWithItems)
        Assert.assertEquals(newListWithItems, storedListWithItems)

        Assert.assertTrue(newListWithItems.items.isNotEmpty())
        shoppingListRepository.removeItemByName(listPk, newListWithItems.items[0].name)

        newListWithItems.items.removeAt(0)
        storedListWithItems =
            shoppingListRepository.read(listPk)
        Assert.assertNotNull(storedListWithItems)
        Assert.assertEquals(newListWithItems, storedListWithItems)

        shoppingListRepository.removeItemByName(listPk, "item with name that cannot be found")

        storedListWithItems =
            shoppingListRepository.read(listPk)
        Assert.assertNotNull(storedListWithItems)
        Assert.assertEquals(newListWithItems, storedListWithItems)
    }

    @Test
    fun testRemoveItemByNameListOperationOfflineOnly() =
        runTest(EmptyCoroutineContext, Duration.parse("3m")) {
            TestUtil.initialize(clearDatabase = true, mockRemoteToDoNothing = true)
            removeItemByNameOperation()
        }

    @Test
    fun testRemoveItemByNameListOperation() =
        runTest(EmptyCoroutineContext, Duration.parse("3m")) {
            TestUtil.initialize(clearDatabase = true, mockRemoteToDoNothing = false)
            removeItemByNameOperation()
        }

    private suspend fun changeQtyItemOperation() {
        val shoppingListApplication = TestUtil.shoppingListApplication
        val appUserRepository = shoppingListApplication.appUserRepository
        appUserRepository.create("test user")
        val testUser = appUserRepository.read()
        Assert.assertNotNull(testUser) // Even though this is not what we want to test, we need a valid online id in order to proceed

        val shoppingListRepository = shoppingListApplication.shoppingListRepository
        val newListWithItems = shoppingListRepository.create("new list with items")
        val listPk = ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId)
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
            shoppingListRepository.insertItem(listPk, item)
            newListWithItems.items.add(item)
        }

        var storedListWithItems =
            shoppingListRepository.read(listPk)
        Assert.assertNotNull(storedListWithItems)
        Assert.assertEquals(newListWithItems, storedListWithItems)

        val newQuantity = 33L
        shoppingListRepository.setItemQuantity(listPk, newListWithItems.items[0].name, newQuantity)

        newListWithItems.items[0].quantity = newQuantity
        newListWithItems.items[0].opCount = newListWithItems.items[0].opCount.plus(1)

        storedListWithItems =
            shoppingListRepository.read(listPk)
        Assert.assertNotNull(storedListWithItems)
        Assert.assertEquals(newListWithItems, storedListWithItems)

        shoppingListRepository.setItemQuantity(listPk, "definitely not existing item", newQuantity)

        storedListWithItems =
            shoppingListRepository.read(listPk)
        Assert.assertNotNull(storedListWithItems)
        Assert.assertEquals(newListWithItems, storedListWithItems)

        val negativeQuantity = -1L
        shoppingListRepository.setItemQuantity(listPk, newListWithItems.items[0].name, negativeQuantity)

        storedListWithItems =
            shoppingListRepository.read(listPk)
        Assert.assertNotNull(storedListWithItems)
        Assert.assertEquals(newListWithItems, storedListWithItems)

        val kgQuantity = 123L
        shoppingListRepository.setItemQuantity(
            listPk,
            newListWithItems.items[1].name,
            kgQuantity,
            QuantityType.KILO,
        )

        newListWithItems.items[1].quantity = kgQuantity
        newListWithItems.items[1].quantityType = QuantityType.KILO
        newListWithItems.items[1].opCount = newListWithItems.items[1].opCount.plus(1)

        storedListWithItems =
            shoppingListRepository.read(listPk)
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

    private suspend fun setItemToggleOperation() {
        val shoppingListApplication = TestUtil.shoppingListApplication
        val appUserRepository = shoppingListApplication.appUserRepository
        appUserRepository.create("test user")
        val testUser = appUserRepository.read()
        Assert.assertNotNull(testUser) // Even though this is not what we want to test, we need a valid online id in order to proceed

        val shoppingListRepository = shoppingListApplication.shoppingListRepository
        val newListWithItems = shoppingListRepository.create("new list with items")
        val listPk = ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId)
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
            shoppingListRepository.insertItem(listPk, item)
            newListWithItems.items.add(item)
        }

        var storedListWithItems =
            shoppingListRepository.read(listPk)
        Assert.assertNotNull(storedListWithItems)
        Assert.assertEquals(newListWithItems, storedListWithItems)

        shoppingListRepository.setItemToggle(
            listPk,
            newListWithItems.items[0].name,
            ItemToggleStatus.TRUE,
        )
        newListWithItems.items[0].checked = true
        newListWithItems.items[0].opCount = newListWithItems.items[0].opCount.plus(1)

        storedListWithItems =
            shoppingListRepository.read(listPk)
        Assert.assertNotNull(storedListWithItems)
        Assert.assertEquals(newListWithItems, storedListWithItems)

        shoppingListRepository.setItemToggle(
            listPk,
            newListWithItems.items[0].name,
            ItemToggleStatus.TOGGLE,
        )
        newListWithItems.items[0].checked = !newListWithItems.items[0].checked
        newListWithItems.items[0].opCount = newListWithItems.items[0].opCount.plus(1)

        storedListWithItems =
            shoppingListRepository.read(listPk)
        Assert.assertNotNull(storedListWithItems)
        Assert.assertEquals(newListWithItems, storedListWithItems)

        shoppingListRepository.setItemToggle(
            listPk,
            "item definitely not found",
            ItemToggleStatus.TOGGLE,
        )

        storedListWithItems =
            shoppingListRepository.read(listPk)
        Assert.assertNotNull(storedListWithItems)
        Assert.assertEquals(newListWithItems, storedListWithItems)
    }

    @Test
    fun testSetItemToggleListOperationOfflineOnly() =
        runTest(EmptyCoroutineContext, Duration.parse("3m")) {
            TestUtil.initialize(clearDatabase = true, mockRemoteToDoNothing = true)
            setItemToggleOperation()
        }

    @Test
    fun testSetItemToggleListOperation() =
        runTest(EmptyCoroutineContext, Duration.parse("3m")) {
            TestUtil.initialize(clearDatabase = true, mockRemoteToDoNothing = false)
            setItemToggleOperation()
        }

    private suspend fun renameListOperation() {
        val shoppingListApplication = TestUtil.shoppingListApplication
        val appUserRepository = shoppingListApplication.appUserRepository
        appUserRepository.create("test user")
        val testUser = appUserRepository.read()
        Assert.assertNotNull(testUser) // Even though this is not what we want to test, we need a valid online id in order to proceed

        val shoppingListRepository = shoppingListApplication.shoppingListRepository
        val newListWithItems = shoppingListRepository.create("new list with items")
        val listPk = ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId)
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
            shoppingListRepository.insertItem(listPk, item)
            newListWithItems.items.add(item)
        }

        var storedListWithItems =
            shoppingListRepository.read(listPk)
        Assert.assertNotNull(storedListWithItems)
        Assert.assertEquals(newListWithItems, storedListWithItems)

        val newName = "new list name with long name"
        shoppingListRepository.renameList(listPk, newName)
        newListWithItems.title = newName

        storedListWithItems =
            shoppingListRepository.read(listPk)
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

        val shoppingListRepository = shoppingListApplication.shoppingListRepository
        val newListWithItems = shoppingListRepository.create("new list with items")
        val listPk = ShoppingListPK(newListWithItems.listId, newListWithItems.createdBy.onlineId)
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
            shoppingListRepository.insertItem(listPk, item)
            newListWithItems.items.add(item)
        }

        var storedListWithItems =
            shoppingListRepository.read(listPk)
        Assert.assertNotNull(storedListWithItems)
        Assert.assertEquals(newListWithItems, storedListWithItems)

        shoppingListRepository.deleteList(listPk)

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
