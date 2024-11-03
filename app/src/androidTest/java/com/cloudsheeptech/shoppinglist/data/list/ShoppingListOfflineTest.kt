package com.cloudsheeptech.shoppinglist.data.list

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.itemToListMapping.ItemToListLocalDataSource
import com.cloudsheeptech.shoppinglist.data.itemToListMapping.ItemToListRepository
import com.cloudsheeptech.shoppinglist.data.items.ApiItem
import com.cloudsheeptech.shoppinglist.data.items.ItemLocalDataSource
import com.cloudsheeptech.shoppinglist.data.items.ItemRepository
import com.cloudsheeptech.shoppinglist.data.onlineUser.ListCreator
import com.cloudsheeptech.shoppinglist.data.user.AppUserLocalDataSource
import com.cloudsheeptech.shoppinglist.data.user.AppUserRemoteDataSource
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import com.cloudsheeptech.shoppinglist.data.user.UserCreationDataProvider
import com.cloudsheeptech.shoppinglist.network.Networking
import com.cloudsheeptech.shoppinglist.network.TokenProvider
import com.cloudsheeptech.shoppinglist.testUtil.TestUtil
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import java.time.OffsetDateTime
import kotlin.time.Duration

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.DEFAULT)
class ShoppingListOfflineTest {
    private suspend fun createLocalDataSourceAndUserHandling(
        userId: Long,
    ): Triple<ShoppingListLocalDataSource, AppUserLocalDataSource, AppUserRepository> {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)
        database.clearAllTables()
        val localUserDs = AppUserLocalDataSource(database)
        // Cheat the system and the later creation that the user is in fact registered online
        localUserDs.create("local user")
        localUserDs.setOnlineId(userId)
        localUserDs.store()
        val payloadProvider = UserCreationDataProvider(localUserDs)
        val tokenProvider = TokenProvider(payloadProvider)
        val networking = Networking(tokenProvider)
        val remoteUserDs = AppUserRemoteDataSource(networking)
        val userRepository = AppUserRepository(localUserDs, remoteUserDs)
        val localItemDs = ItemLocalDataSource(database)
        val itemRepo = ItemRepository(localItemDs)
        val localItemToListDs = ItemToListLocalDataSource(database)
        val itemToListRepository = ItemToListRepository(localItemToListDs)
        val localDataSource =
            ShoppingListLocalDataSource(database, userRepository, itemRepo, itemToListRepository)
        return Triple(localDataSource, localUserDs, userRepository)
    }

    private suspend fun createLocalDataSourceAndLocalUserRepo(userId: Long): Pair<ShoppingListLocalDataSource, AppUserLocalDataSource> {
        val (slds, userDs, _) = createLocalDataSourceAndUserHandling(userId)
        return Pair(slds, userDs)
    }

    private suspend fun createLocalSLDataSource(): ShoppingListLocalDataSource {
        val (slds, _, _) = createLocalDataSourceAndUserHandling(1234L)
        return slds
    }

    @After
    fun clearDatabase() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)
        database.clearAllTables()
    }

    private fun createApiShoppingList(
        title: String = "list title",
        creatorId: Long = 1234L,
    ): ApiShoppingList {
        val newList =
            ApiShoppingList(
                0L,
                title,
                ListCreator(creatorId, "creator"),
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                mutableListOf(),
            )
        return newList
    }

    private fun createApiItem(
        num: Int,
        addedBy: Long = 1234L,
    ): ApiItem {
        val item =
            ApiItem(
                "item $num",
                "empty icon",
                quantity = num.toLong(),
                checked = num % 2 == 0,
                addedBy,
            )
        return item
    }

    @Test
    fun testCreateList() =
        runTest(timeout = Duration.parse("1m")) {
            TestUtil.initialize()
            val username = "test user"
            TestUtil.initializeUser(username)
            val appUserRepository = TestUtil.shoppingListApplication.appUserRepository
            val testUser = appUserRepository.read()
            Assert.assertNotNull(testUser)

            val localShoppingListDataSource =
                TestUtil.shoppingListApplication.shoppingListLocalDataSource

            val newList =
                createApiShoppingList(creatorId = testUser!!.OnlineID)
            val insertedId = localShoppingListDataSource.create(newList)
            Assert.assertEquals(1L, insertedId)

            val secondList = createApiShoppingList("second title", creatorId = testUser.OnlineID)
            val secondInsertedId = localShoppingListDataSource.create(secondList)
            Assert.assertEquals(2L, secondInsertedId)

            secondList.listId = secondInsertedId
            var exception = false
            try {
                localShoppingListDataSource.create(secondList)
            } catch (ex: IllegalArgumentException) {
                exception = true
            }
            assert(exception)

            // Remote list with all values set according
            val remoteCreatorId = 1440L
            val listWithItems = createApiShoppingList("list with items", remoteCreatorId)
            listWithItems.listId = 1L
            for (num in 1..3) {
                val item =
                    createApiItem(num, addedBy = remoteCreatorId)
                listWithItems.items.add(item)
            }
            val thirdInsertedId = localShoppingListDataSource.create(listWithItems)
            // remote user and therefore keep the list id
            // Also check if the lists with equal ids can be stored side by side
            Assert.assertEquals(1L, thirdInsertedId)
            // Check if the items are correctly inserted
            val itemRepository = TestUtil.shoppingListApplication.itemRepository
            val itemToListRepository = TestUtil.shoppingListApplication.itemToListRepository
            val mappings =
                itemToListRepository.read(thirdInsertedId, listWithItems.createdBy.onlineId)
            Assert.assertEquals(listWithItems.items.size, mappings.size)
            listWithItems.items.forEach { item ->
                val items = itemRepository.readByName(item.name)
                Assert.assertEquals(1, items.size)
                Assert.assertEquals(item.name, items[0].name)
                Assert.assertEquals(item.icon, items[0].icon)
                val mapping = mappings.first { mapping -> mapping.ItemID == items[0].id }
                Assert.assertEquals(item.quantity, mapping.Quantity)
                Assert.assertEquals(item.checked, mapping.Checked)
                Assert.assertEquals(item.addedBy, mapping.AddedBy)
            }
        }

    @Test
    fun testGetList() =
        runTest {
            val localDataSource = createLocalSLDataSource()
            val listWithItems =
                ApiShoppingList(
                    0L,
                    "list with items",
                    ListCreator(1234L, "local creator"),
                    OffsetDateTime.now(),
                    OffsetDateTime.now(),
                    mutableListOf(),
                )
            for (num in 1..3) {
                val item =
                    createApiItem(num)
                listWithItems.items.add(item)
            }
//        Log.d("ShoppingListOfflineTest", "List: $listWithItems")
            val insertedId = localDataSource.create(listWithItems)
            Assert.assertEquals(1L, insertedId)
            listWithItems.listId = insertedId

            // DEBUG
//        val application = ApplicationProvider.getApplicationContext<Application>()
//        val database = ShoppingListDatabase.getInstance(application)
//        val itemToList = database.mappingDao()
//        val mappingsDirect = itemToList.getMappingsForList(insertedId, 1234L)
//        Log.d("ShoppingListOfflineTest", "$mappingsDirect")

            val retrievedList =
                localDataSource.read(listWithItems.listId, listWithItems.createdBy.onlineId)
//        Log.d("ShoppingListOfflineTest", "RetrievedList: $retrievedList")
            Assert.assertNotNull(retrievedList)
            Assert.assertEquals(3, retrievedList!!.items.size)
            Assert.assertEquals(listWithItems, retrievedList)

            val failedIncorrectId = localDataSource.read(222L, 1234L)
            Assert.assertNull(failedIncorrectId)
            val failedIncorrectCreated = localDataSource.read(1L, 4444L)
            Assert.assertNull(failedIncorrectCreated)
        }

    @Test
    fun testUpdateList() =
        runTest {
            val localDataSource = createLocalSLDataSource()
            val listWithItems =
                ApiShoppingList(
                    0L,
                    "list with items",
                    ListCreator(1234L, "local creator"),
                    OffsetDateTime.now(),
                    OffsetDateTime.now(),
                    mutableListOf(),
                )
            for (num in 1..3) {
                val item =
                    createApiItem(num)
                listWithItems.items.add(item)
            }
            val insertedId = localDataSource.create(listWithItems)
            Assert.assertEquals(1L, insertedId)
            listWithItems.listId = insertedId
            val retrievedList =
                localDataSource.read(listWithItems.listId, listWithItems.createdBy.onlineId)
            Assert.assertNotNull(retrievedList)
            Assert.assertEquals(3, retrievedList!!.items.size)
            Assert.assertEquals(listWithItems, retrievedList)

            // Update the base
            listWithItems.title = "new title"
            localDataSource.update(listWithItems)
            val updatedRetrievedList =
                localDataSource.read(listWithItems.listId, listWithItems.createdBy.onlineId)
            Assert.assertNotNull(updatedRetrievedList)
            Assert.assertEquals(listWithItems.title, updatedRetrievedList!!.title)

            // Remove the last item
            listWithItems.items.removeAt(2)
            listWithItems.lastUpdated = OffsetDateTime.now()
            localDataSource.update(listWithItems)
            val updatedItemsRetrievedList =
                localDataSource.read(listWithItems.listId, listWithItems.createdBy.onlineId)
            Assert.assertNotNull(updatedItemsRetrievedList)
            Assert.assertEquals(2, updatedItemsRetrievedList!!.items.size)
            Assert.assertEquals(listWithItems, updatedItemsRetrievedList)
        }

    @Test
    fun testSetIdToNewUserId() =
        runTest {
            val (localDataSource, localUserDataStore) = createLocalDataSourceAndLocalUserRepo(0L)
            val listWithItems =
                ApiShoppingList(
                    0L,
                    "list with items",
                    ListCreator(0L, "local creator"),
                    OffsetDateTime.now(),
                    OffsetDateTime.now(),
                    mutableListOf(),
                )
            val items = mutableListOf<ApiItem>()
            for (num in 1..3) {
                val item =
                    ApiItem(
                        "item $num",
                        "empty icon",
                        quantity = num.toLong(),
                        checked = num % 2 == 0,
                        0L,
                    )
                items.add(item)
            }
            listWithItems.items.addAll(items)
            val insertedId = localDataSource.create(listWithItems)
            Assert.assertEquals(1L, insertedId)
            listWithItems.listId = insertedId
            val retrievedList =
                localDataSource.read(listWithItems.listId, listWithItems.createdBy.onlineId)
            Assert.assertNotNull(retrievedList)
            Assert.assertEquals(3, retrievedList!!.items.size)
            Assert.assertEquals(listWithItems, retrievedList)

            localUserDataStore.setOnlineId(1234L)
            localUserDataStore.store()

            // From the creation of the local data source, the user ID in the repo should be 1234L
            // and the following should in fact work
            localDataSource.updateCreatedById(0L)
            val updatedList = localDataSource.read(insertedId, 1234L)
            Assert.assertNotNull(updatedList)
            Assert.assertEquals(1L, updatedList!!.listId)
            Assert.assertEquals(3, updatedList.items.size)
            Assert.assertNotEquals(items, updatedList.items)
            items.forEach { x -> x.addedBy = 1234L }
            Assert.assertEquals(items, updatedList.items)
        }

    @Test
    fun testResetId() =
        runTest {
            val (localDataSource, localUserDataStore, userRepo) =
                createLocalDataSourceAndUserHandling(
                    1234L,
                )
            val listWithItems =
                ApiShoppingList(
                    0L,
                    "list with items",
                    ListCreator(1234L, "local creator"),
                    OffsetDateTime.now(),
                    OffsetDateTime.now(),
                    mutableListOf(),
                )
            val items = mutableListOf<ApiItem>()
            for (num in 1..3) {
                val item =
                    createApiItem(num)
                items.add(item)
            }
            listWithItems.items.addAll(items)
            val insertedId = localDataSource.create(listWithItems)
            Assert.assertEquals(1L, insertedId)
            listWithItems.listId = insertedId
            val retrievedList =
                localDataSource.read(listWithItems.listId, listWithItems.createdBy.onlineId)
            Assert.assertNotNull(retrievedList)
            Assert.assertEquals(3, retrievedList!!.items.size)
            Assert.assertEquals(listWithItems, retrievedList)

            // Reset the id and check if we can retrieve the list with Id set to 0
            localDataSource.resetCreatedBy()
            val updatedList = localDataSource.read(insertedId, 0L)
            Assert.assertNotNull(updatedList)
            Assert.assertEquals(insertedId, updatedList!!.listId)
            Assert.assertEquals(3, updatedList.items.size)
            items.forEach { x -> x.addedBy = 0L }
            Assert.assertEquals(items, updatedList.items)
        }

    @Test
    fun testDeleteList() =
        runTest {
            val localDataSource = createLocalSLDataSource()
            val listWithItems =
                ApiShoppingList(
                    0L,
                    "list with items",
                    ListCreator(1234L, "local creator"),
                    OffsetDateTime.now(),
                    OffsetDateTime.now(),
                    mutableListOf(),
                )
            for (num in 1..3) {
                val item =
                    createApiItem(num)
                listWithItems.items.add(item)
            }
            val insertedId = localDataSource.create(listWithItems)
            Assert.assertEquals(1L, insertedId)
            listWithItems.listId = insertedId
            val retrievedList =
                localDataSource.read(listWithItems.listId, listWithItems.createdBy.onlineId)
            Assert.assertNotNull(retrievedList)
            Assert.assertEquals(3, retrievedList!!.items.size)
            Assert.assertEquals(listWithItems, retrievedList)

            localDataSource.delete(listWithItems.listId, listWithItems.createdBy.onlineId)
            val nullList = localDataSource.readAll()
            assert(nullList.isEmpty())

            val application = ApplicationProvider.getApplicationContext<Application>()
            val database = ShoppingListDatabase.getInstance(application)
            val itemToList = database.mappingDao()
            val mappings =
                itemToList.getMappingsForList(
                    listWithItems.listId,
                    listWithItems.createdBy.onlineId,
                )
            assert(mappings.isEmpty())
        }
}
