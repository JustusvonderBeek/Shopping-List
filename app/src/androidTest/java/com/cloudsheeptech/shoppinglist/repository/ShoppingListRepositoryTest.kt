package com.cloudsheeptech.shoppinglist.repository

import com.cloudsheeptech.shoppinglist.data.items.ApiItem
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
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.DEFAULT)
class ShoppingListRepositoryTest {
    @JvmField
    @Rule
    val testRule: TestRule = DisableOnDebug(Timeout.seconds(500))

    @Test
    fun testCreateList() =
        runTest(EmptyCoroutineContext, Duration.parse("3m")) {
            TestUtil.initialize(clearDatabase = false)
            val shoppingListApplication = TestUtil.shoppingListApplication
            val appUserRepository = shoppingListApplication.appUserRepository
            appUserRepository.create("test user")
            val testUser = appUserRepository.read()
            Assert.assertNotNull(testUser) // Even though this is not what we want to test, we need a valid online id in order to proceed

            val shoppingListRepository = shoppingListApplication.shoppingListRepository
            val newShoppingList = shoppingListRepository.create("new list")
            Assert.assertNotNull(newShoppingList)

            val emptyReadList = shoppingListRepository.read(newShoppingList.listId, newShoppingList.createdBy.onlineId)
            Assert.assertNotNull(emptyReadList)
            Assert.assertEquals(newShoppingList, emptyReadList)

            // Testing the same with items
            val newListWithItems = shoppingListRepository.create("new list with items")
            for (i in 1..3) {
                val item =
                    ApiItem(
                        name = "item $i",
                        icon = "icon $i",
                        quantity = i.toLong(),
                        checked = false,
                        addedBy = testUser!!.OnlineID,
                    )
                newListWithItems.items.add(item)
            }
            shoppingListRepository.update(newListWithItems)

            // Offline should always be correct, but online as well?
            val storedListWithItems = shoppingListRepository.read(newListWithItems.listId, newListWithItems.createdBy.onlineId)
            Assert.assertNotNull(storedListWithItems)
            Assert.assertEquals(newListWithItems, storedListWithItems)
        }

    @Test
    fun testGetList() =
        runTest {
            TestUtil.initialize(clearDatabase = false)
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
                    ApiItem(
                        name = "item $i",
                        icon = "icon $i",
                        quantity = i.toLong(),
                        checked = false,
                        addedBy = testUser!!.OnlineID,
                    )
                newListWithItems.items.add(item)
            }
            shoppingListRepository.update(newListWithItems)

            // Offline should always be correct, but online as well?
            val storedListWithItems = shoppingListRepository.read(newListWithItems.listId, newListWithItems.createdBy.onlineId)
            Assert.assertNotNull(storedListWithItems)
            Assert.assertEquals(newListWithItems, storedListWithItems)
        }

    @Test
    fun testGetOnlineList() =
        runTest {
            throw NotImplementedError("this test is not implemented yet")
//            val (listRepo, userRepo) = createAppRepo()
            // We need to implement sharing for this first
        }

    @Test
    fun testUpdateList() =
        runTest {
            TestUtil.initialize(clearDatabase = false)
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
                    ApiItem(
                        name = "item $i",
                        icon = "icon $i",
                        quantity = i.toLong(),
                        checked = false,
                        addedBy = testUser!!.OnlineID,
                    )
                newListWithItems.items.add(item)
            }
            shoppingListRepository.update(newListWithItems)

            // Offline should always be correct, but online as well?
            var storedListWithItems = shoppingListRepository.read(newListWithItems.listId, newListWithItems.createdBy.onlineId)
            Assert.assertNotNull(storedListWithItems)
            Assert.assertEquals(newListWithItems, storedListWithItems)

            newListWithItems.items.add(ApiItem("new item", "new icon", 1L, false, testUser!!.OnlineID))
            newListWithItems.title = "new title"
            shoppingListRepository.update(newListWithItems)

            storedListWithItems = shoppingListRepository.read(newListWithItems.listId, newListWithItems.createdBy.onlineId)
            Assert.assertNotNull(storedListWithItems)
            Assert.assertEquals(newListWithItems, storedListWithItems)
        }

    @Test
    fun testDeleteList() =
        runTest {
            TestUtil.initialize(clearDatabase = false)
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
                    ApiItem(
                        name = "item $i",
                        icon = "icon $i",
                        quantity = i.toLong(),
                        checked = false,
                        addedBy = testUser!!.OnlineID,
                    )
                newListWithItems.items.add(item)
            }
            shoppingListRepository.update(newListWithItems)

            // Offline should always be correct, but online as well?
            val storedListWithItems = shoppingListRepository.read(newListWithItems.listId, newListWithItems.createdBy.onlineId)
            Assert.assertNotNull(storedListWithItems)
            Assert.assertEquals(newListWithItems, storedListWithItems)

            shoppingListRepository.delete(storedListWithItems!!.listId, storedListWithItems.createdBy.onlineId)
            val deletedListWithItems = shoppingListRepository.read(storedListWithItems.listId, storedListWithItems.createdBy.onlineId)
            Assert.assertNull(deletedListWithItems)
        }
}
