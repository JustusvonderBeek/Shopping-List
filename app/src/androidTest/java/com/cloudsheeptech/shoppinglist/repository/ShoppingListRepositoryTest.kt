package com.cloudsheeptech.shoppinglist.repository

import com.cloudsheeptech.shoppinglist.testUtil.TestUtil
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.DEFAULT)
class ShoppingListRepositoryTest {
    @Test
    fun testCreateList() =
        runTest {
            TestUtil.initialize(clearDatabase = false)
            val shoppingListApplication = TestUtil.shoppingListApplication

            val shoppingListRepository = shoppingListApplication.shoppingListRepository
            val newShoppingList = shoppingListRepository.create("new list")
            Assert.assertNotNull(newShoppingList)

            val emptyReadList = shoppingListRepository.read(newShoppingList.listId, newShoppingList.createdBy.onlineId)
            Assert.assertNotNull(emptyReadList)
            Assert.assertEquals(newShoppingList, emptyReadList)

//            // Testing the same with items
//            val newListWithItems = listRepo.create("new list with items")
//            val user = userRepo.read()
//            Assert.assertNotNull(user) // Even though this is not what we want to test, we need a valid online id in order to proceed
//            for (i in 1..3) {
//                val item = ApiItem(name = "item $i", icon = "icon $i", quantity = 12L, checked = false, addedBy = user!!.OnlineID)
//                newListWithItems.items.add(item)
//            }
//            listRepo.update(newListWithItems)
//
//            // Offline should always be correct, but online as well?
//            val storedListWithItems = listRepo.read(newListWithItems.listId, newListWithItems.createdBy.onlineId)
//            Assert.assertNotNull(storedListWithItems)
//            Assert.assertEquals(newListWithItems, storedListWithItems)
//
//            listRepo.readAllRemote()
            // TODO: How to check that we received the remote data + stored it locally
        }

    @Test
    fun testGetList() =
        runTest {
//            val (listRepo, userRepo) = createAppRepo()
//            val newListWithItems = listRepo.create("new list with items")
//            val user = userRepo.read()
//            Assert.assertNotNull(user) // Even though this is not what we want to test, we need a valid online id in order to proceed
//            for (i in 1..3) {
//                val item = ApiItem(name = "item $i", icon = "icon $i", quantity = 12L, checked = false, addedBy = user!!.OnlineID)
//                newListWithItems.items.add(item)
//            }
//            listRepo.update(newListWithItems)
//
//            val retrievedList = listRepo.read(newListWithItems.listId, newListWithItems.createdBy.onlineId)
//            Assert.assertNotNull(retrievedList)
//            Assert.assertEquals(newListWithItems, retrievedList)
        }

    @Test
    fun testGetOnlineList() =
        runTest {
//            val (listRepo, userRepo) = createAppRepo()
            // We need to implement sharing for this first
        }

    @Test
    fun testUpdateList() =
        runTest {
//            val (listRepo, userRepo) = createAppRepo()
//            val newListWithItems = listRepo.create("new list with items")
//
//            val retrievedListWithoutItems = listRepo.read(newListWithItems.listId, newListWithItems.createdBy.onlineId)
//            Assert.assertNotNull(retrievedListWithoutItems)
//            Assert.assertEquals(newListWithItems.items.size, retrievedListWithoutItems!!.items.size)
//            Assert.assertEquals(newListWithItems, retrievedListWithoutItems)
//
//            val user = userRepo.read()
//            Assert.assertNotNull(user) // Even though this is not what we want to test, we need a valid online id in order to proceed
//            for (i in 1..3) {
//                val item = ApiItem(name = "item $i", icon = "icon $i", quantity = 12L, checked = false, addedBy = user!!.OnlineID)
//                newListWithItems.items.add(item)
//            }
//            listRepo.update(newListWithItems)
//
//            val retrievedListWithItems = listRepo.read(newListWithItems.listId, newListWithItems.createdBy.onlineId)
//            Assert.assertNotNull(retrievedListWithItems)
//            Assert.assertEquals(newListWithItems, retrievedListWithItems)
        }

    @Test
    fun testDeleteList() =
        runTest {
//            val (listRepo, userRepo) = createAppRepo()
//            val newListWithItems = listRepo.create("new list with items")
//
//            val retrievedListWithoutItems = listRepo.read(newListWithItems.listId, newListWithItems.createdBy.onlineId)
//            Assert.assertNotNull(retrievedListWithoutItems)
//            Assert.assertEquals(newListWithItems.items.size, retrievedListWithoutItems!!.items.size)
//            Assert.assertEquals(newListWithItems, retrievedListWithoutItems)
//
//            val user = userRepo.read()
//            Assert.assertNotNull(user) // Even though this is not what we want to test, we need a valid online id in order to proceed
//            for (i in 1..3) {
//                val item = ApiItem(name = "item $i", icon = "icon $i", quantity = 12L, checked = false, addedBy = user!!.OnlineID)
//                newListWithItems.items.add(item)
//            }
//            listRepo.update(newListWithItems)
//            val retrievedListWithItems = listRepo.read(newListWithItems.listId, newListWithItems.createdBy.onlineId)
//            Assert.assertNotNull(retrievedListWithItems)
//            Assert.assertEquals(newListWithItems, retrievedListWithItems)
//
//            listRepo.delete(newListWithItems.listId, newListWithItems.createdBy.onlineId)
//            val deletedList = listRepo.read(newListWithItems.listId, newListWithItems.createdBy.onlineId)
//            Assert.assertNull(deletedList)
//            listRepo.readAllRemote()
//            val deletedList2 = listRepo.read(newListWithItems.listId, newListWithItems.createdBy.onlineId)
//            Assert.assertNull(deletedList2)
        }
}
