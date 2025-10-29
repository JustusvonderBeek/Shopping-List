package com.cloudsheeptech.shoppinglist.data.list

import android.app.Application
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.cloudsheeptech.shoppinglist.data.items.ApiItem
import com.cloudsheeptech.shoppinglist.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.list.model.ListCreator
import com.cloudsheeptech.shoppinglist.list.model.ShoppingList
import com.cloudsheeptech.shoppinglist.list.repo.ItemLocalDataSource
import com.cloudsheeptech.shoppinglist.list.repo.ShoppingListLocalDataSource
import com.cloudsheeptech.shoppinglist.network.Networking
import com.cloudsheeptech.shoppinglist.network.token.ShoppingListAuthenticationTokenProvider
import com.cloudsheeptech.shoppinglist.sharing.repo.OnlineUserLocalDataSource
import com.cloudsheeptech.shoppinglist.sharing.repo.OnlineUserRemoteDataSource
import com.cloudsheeptech.shoppinglist.sharing.repo.OnlineUserRepository
import com.cloudsheeptech.shoppinglist.testUtil.TestUtil
import com.cloudsheeptech.shoppinglist.user.repo.AppUserLocalDataSource
import com.cloudsheeptech.shoppinglist.user.repo.AppUserRemoteDataSource
import com.cloudsheeptech.shoppinglist.user.repo.AppUserRepository
import com.cloudsheeptech.shoppinglist.user.util.UserCreationDataProvider
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
        val tokenProvider = ShoppingListAuthenticationTokenProvider(payloadProvider, "tmp/")
        val networking = Networking(tokenProvider)
        val remoteUserDs = AppUserRemoteDataSource(networking)
        val userRepository = AppUserRepository(localUserDs, remoteUserDs)
        val localItemDs = ItemLocalDataSource(database)
        val itemRepo = ItemRepository(localItemDs)
        val localItemToListDs = ItemToListLocalDataSource(database)
        val itemToListRepository = ItemToListRepository(localItemToListDs)
        val onlineUserLocalDataSource = OnlineUserLocalDataSource(database)
        val onlineUserRemoteDataSource = OnlineUserRemoteDataSource(networking)
        val onlineUserRepository =
            OnlineUserRepository(onlineUserLocalDataSource, onlineUserRemoteDataSource)
        val localDataSource =
            ShoppingListLocalDataSource(
                database,
                userRepository,
                onlineUserRepository,
                itemRepo,
                itemToListRepository,
            )
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
        creatorName: String = "creator",
        version: Long = 1L,
    ): ShoppingList {
        val newList =
            ShoppingList(
                0L,
                title,
                ListCreator(creatorId, creatorName),
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                mutableListOf(),
                version,
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

            // Remote list with all values set according
            val remoteCreatorId = 1440L
            val listWithItems = createApiShoppingList("list with items", remoteCreatorId)
            listWithItems.listId = 1L
            for (num in 1..3) {
                val item =
                    createApiItem(num, addedBy = remoteCreatorId)
                listWithItems.items.add(item)
            }
            val thirdInsertedIdAndVersion =
                localShoppingListDataSource.create(listWithItems)
            // remote user and therefore keep the list id
            // Also check if the lists with equal ids can be stored side by side
            Assert.assertEquals(1L, thirdInsertedIdAndVersion.first)
            Assert.assertEquals(1L, thirdInsertedIdAndVersion.second)
            // Check if the items are correctly inserted
            val itemRepository = TestUtil.shoppingListApplication.itemRepository
            val itemToListRepository = TestUtil.shoppingListApplication.itemToListRepository
            val mappings =
                itemToListRepository.read(
                    thirdInsertedIdAndVersion.first,
                    listWithItems.createdBy.onlineId,
                )
            Assert.assertEquals(listWithItems.items.size, mappings.size)
            listWithItems.items.forEach { item ->
                val items = itemRepository.readByName(item.name)
                Assert.assertEquals(1, items.size)
                Assert.assertEquals(item.name, items[0].name)
                Assert.assertEquals(item.icon, items[0].icon)
                val mapping = mappings.first { mapping -> mapping.itemId == items[0].id }
                Assert.assertEquals(item.quantity, mapping.quantity)
                Assert.assertEquals(item.checked, mapping.checked)
                Assert.assertEquals(item.addedBy, mapping.addedBy)
            }
        }

    @Test
    fun testGetList() =
        runTest {
            TestUtil.initialize()
            val username = "test user"
            TestUtil.initializeUser(username)
            val appUserRepository = TestUtil.shoppingListApplication.appUserRepository
            val testUser = appUserRepository.read()
            Assert.assertNotNull(testUser)

            val localShoppingListDataSource =
                TestUtil.shoppingListApplication.shoppingListLocalDataSource

            // Create local list first
            val listWithItems =
                createApiShoppingList("list with items", testUser!!.OnlineID, username)
            listWithItems.listId = 0L
            for (num in 1..3) {
                val item =
                    createApiItem(num, addedBy = testUser.OnlineID)
                listWithItems.items.add(item)
            }

            val insertedIdAndVersion = localShoppingListDataSource.create(listWithItems)
            Assert.assertEquals(1L, insertedIdAndVersion.first)
            Assert.assertEquals(1L, insertedIdAndVersion.second)
            listWithItems.listId = insertedIdAndVersion.first

            val retrievedList =
                localShoppingListDataSource.read(
                    listWithItems.listId,
                    listWithItems.createdBy.onlineId,
                )
            Assert.assertNotNull(retrievedList)
            Assert.assertEquals(3, retrievedList!!.items.size)
            Assert.assertEquals(listWithItems, retrievedList)
            Log.i("ShoppingListOfflineTest", "Local list successfully retrieved")

            // Create remote list
            val remoteUserId = 44332211L
            TestUtil.shoppingListApplication.onlineUserRepository.create(
                ListCreator(
                    remoteUserId,
                    "creator",
                ),
            )
            val remoteListWithItems =
                createApiShoppingList("list with items", remoteUserId)
            remoteListWithItems.listId = 33L
            for (num in 1..3) {
                val item =
                    createApiItem(num, addedBy = remoteUserId)
                remoteListWithItems.items.add(item)
            }

            val insertedRemoteId = localShoppingListDataSource.create(remoteListWithItems)
            Assert.assertEquals(33L, insertedRemoteId)

            val retrievedRemoteList =
                localShoppingListDataSource.read(
                    remoteListWithItems.listId,
                    remoteListWithItems.createdBy.onlineId,
                )
            Assert.assertNotNull(retrievedRemoteList)
            Assert.assertEquals(3, retrievedRemoteList!!.items.size)
            Assert.assertEquals(remoteListWithItems, retrievedRemoteList)
            Log.i("ShoppingListOfflineTest", "Remote list successfully retrieved")

            val failedIncorrectId = localShoppingListDataSource.read(222L, testUser.OnlineID)
            Assert.assertNull(failedIncorrectId)
            Log.i("ShoppingListOfflineTest", "Wrong list id correctly not found")
            val failedIncorrectCreated = localShoppingListDataSource.read(1L, 4444L)
            Assert.assertNull(failedIncorrectCreated)
            Log.i("ShoppingListOfflineTest", "Wrong created by correctl ynot found")
        }

    @Test
    fun testUpdateList() =
        runTest {
            TestUtil.initialize()
            val username = "test user"
            TestUtil.initializeUser(username)
            val appUserRepository = TestUtil.shoppingListApplication.appUserRepository
            val testUser = appUserRepository.read()
            Assert.assertNotNull(testUser)

            val localShoppingListDataSource =
                TestUtil.shoppingListApplication.shoppingListLocalDataSource

            val listWithItems =
                createApiShoppingList("list with items", testUser!!.OnlineID, username)
            listWithItems.listId = 0L
            for (num in 1..3) {
                val item =
                    createApiItem(num, addedBy = testUser.OnlineID)
                listWithItems.items.add(item)
            }

            val insertedIdAndVersion = localShoppingListDataSource.create(listWithItems)
            Assert.assertEquals(1L, insertedIdAndVersion.first)
            listWithItems.listId = insertedIdAndVersion.first

            // Add a new item
            val newItem = createApiItem(4, addedBy = testUser.OnlineID)
            listWithItems.items.add(newItem)
            localShoppingListDataSource.create(listWithItems)

            val retrievedList =
                localShoppingListDataSource.read(
                    listWithItems.listId,
                    listWithItems.createdBy.onlineId,
                )
            Assert.assertNotNull(retrievedList)
            Assert.assertEquals(4, retrievedList!!.items.size)
            Assert.assertEquals(listWithItems, retrievedList)

            // Update the base
            listWithItems.title = "new title"
            localShoppingListDataSource.create(listWithItems)
            val updatedRetrievedList =
                localShoppingListDataSource.read(
                    listWithItems.listId,
                    listWithItems.createdBy.onlineId,
                )
            Assert.assertNotNull(updatedRetrievedList)
            Assert.assertEquals(listWithItems.title, updatedRetrievedList!!.title)

            // Remove the last item
            listWithItems.items.removeAt(2)
            listWithItems.synchronized = OffsetDateTime.now()
            localShoppingListDataSource.create(listWithItems)
            val updatedItemsRetrievedList =
                localShoppingListDataSource.read(
                    listWithItems.listId,
                    listWithItems.createdBy.onlineId,
                )
            Assert.assertNotNull(updatedItemsRetrievedList)
            Assert.assertEquals(3, updatedItemsRetrievedList!!.items.size)
            Assert.assertEquals(listWithItems, updatedItemsRetrievedList)
        }

    @Test
    fun testUpdateListFromRemote() =
        runTest {
            TestUtil.initialize()
            val username = "test user"
            TestUtil.initializeUser(username)
            val appUserRepository = TestUtil.shoppingListApplication.appUserRepository
            val testUser = appUserRepository.read()
            Assert.assertNotNull(testUser)

            val localShoppingListDataSource =
                TestUtil.shoppingListApplication.shoppingListLocalDataSource

            val remoteUserId = 554421L
            TestUtil.shoppingListApplication.onlineUserRepository.create(
                ListCreator(
                    remoteUserId,
                    "creator",
                ),
            )
            val listWithItems =
                createApiShoppingList("list with items", remoteUserId, version = 3L)
            listWithItems.listId = 22L
            for (num in 1..3) {
                val item =
                    createApiItem(num, addedBy = remoteUserId)
                listWithItems.items.add(item)
            }

            val insertedIdAndVersion = localShoppingListDataSource.create(listWithItems)
            Assert.assertEquals(22L, insertedIdAndVersion.first)
            Assert.assertEquals(3L, insertedIdAndVersion.second)
            Assert.assertEquals("creator", listWithItems.createdBy.username)
            Assert.assertEquals(remoteUserId, listWithItems.createdBy.onlineId)

            // Add a new item from remote user and update list
            val newItem = createApiItem(4, addedBy = remoteUserId)
            listWithItems.items.add(newItem)
            listWithItems.version = listWithItems.version.plus(1L) // REMOTE UPDATE
            localShoppingListDataSource.create(listWithItems)

            val receivedList =
                localShoppingListDataSource.read(
                    listWithItems.listId,
                    listWithItems.createdBy.onlineId,
                )
            Assert.assertNotNull(receivedList)
            Assert.assertEquals(4, receivedList!!.items.size)
            Assert.assertEquals(listWithItems, receivedList)
            Assert.assertEquals(4L, receivedList.version)
            Log.i("ShoppingListOfflineTest", "Updating list from remote works")

            // Update list the list from local user
            listWithItems.title = "new title"
            listWithItems.items.removeAt(0)
            localShoppingListDataSource.create(listWithItems)

            val updatedReceivedList =
                localShoppingListDataSource.read(
                    listWithItems.listId,
                    listWithItems.createdBy.onlineId,
                )
            Assert.assertNotNull(updatedReceivedList)
            Assert.assertEquals(listWithItems.title, updatedReceivedList!!.title)
            Assert.assertEquals(3, updatedReceivedList.items.size)
            Assert.assertEquals(listWithItems, updatedReceivedList)
            Assert.assertEquals(5L, updatedReceivedList.version)
            Log.i("ShoppingListOfflineTest", "Updating list from local works")
        }

    @Test
    fun testUpdateSharedListWithRemoteItems() =
        runTest {
            TestUtil.initialize()
            val username = "test user"
            TestUtil.initializeUser(username)
            val appUserRepository = TestUtil.shoppingListApplication.appUserRepository
            val testUser = appUserRepository.read()
            Assert.assertNotNull(testUser)

            val localShoppingListDataSource =
                TestUtil.shoppingListApplication.shoppingListLocalDataSource

            val listWithItems =
                createApiShoppingList("list with items", testUser!!.OnlineID, username)
            listWithItems.listId = 0L
            for (num in 1..3) {
                val item = createApiItem(num, addedBy = testUser.OnlineID)
                listWithItems.items.add(item)
            }

            val insertedIdAndVersion = localShoppingListDataSource.create(listWithItems)
            Assert.assertEquals(1L, insertedIdAndVersion.first)
            listWithItems.listId = insertedIdAndVersion.first
            Log.i(
                "ShoppingListOfflineTest",
                "Successfully inserted base list with ${listWithItems.items.size} items",
            )

            // Remote user adds another item
            val locallyChangedList = listWithItems.copy()
            val remoteUserId = 123456L
            val newItem = createApiItem(42, addedBy = remoteUserId)
            listWithItems.items.add(newItem)
            var idAndVersion = localShoppingListDataSource.create(listWithItems)
            Assert.assertEquals(2L, idAndVersion.second)
            locallyChangedList.version = idAndVersion.second
            Log.i("ShoppingListOfflineTest", "Remote user successfully updated list with new item")

            val newLocalItem = createApiItem(55, testUser.OnlineID)
            newLocalItem.quantity = 12
            locallyChangedList.items.add(newLocalItem)
            idAndVersion = localShoppingListDataSource.create(locallyChangedList)
            Assert.assertEquals(3, idAndVersion)
            Log.i("ShoppingListOfflineTest", "Local user successfully updated list with new item")

            val retrievedList =
                localShoppingListDataSource.read(
                    listWithItems.listId,
                    listWithItems.createdBy.onlineId,
                )
            Assert.assertNotNull(retrievedList)
            Assert.assertEquals(5, retrievedList!!.items.size)
            Assert.assertEquals(locallyChangedList, retrievedList)
        }

    @Test
    fun testSetIdToNewUserId() =
        runTest {
            val (localDataSource, localUserDataStore) = createLocalDataSourceAndLocalUserRepo(0L)
            val listWithItems =
                ShoppingList(
                    0L,
                    "list with items",
                    ListCreator(0L, "local creator"),
                    OffsetDateTime.now(),
                    OffsetDateTime.now(),
                    mutableListOf(),
                    1L,
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
            val insertedIdAndVersion = localDataSource.create(listWithItems)
            Assert.assertEquals(1L, insertedIdAndVersion.first)
            listWithItems.listId = insertedIdAndVersion.first
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
            val updatedList = localDataSource.read(insertedIdAndVersion.first, 1234L)
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
            val (localDataSource, _, _) =
                createLocalDataSourceAndUserHandling(
                    1234L,
                )
            val listWithItems =
                ShoppingList(
                    0L,
                    "list with items",
                    ListCreator(1234L, "local creator"),
                    OffsetDateTime.now(),
                    OffsetDateTime.now(),
                    mutableListOf(),
                    1L,
                )
            val items = mutableListOf<ApiItem>()
            for (num in 1..3) {
                val item =
                    createApiItem(num)
                items.add(item)
            }
            listWithItems.items.addAll(items)
            val insertedIdAndVersion = localDataSource.create(listWithItems)
            Assert.assertEquals(1L, insertedIdAndVersion.first)
            listWithItems.listId = insertedIdAndVersion.first
            val retrievedList =
                localDataSource.read(listWithItems.listId, listWithItems.createdBy.onlineId)
            Assert.assertNotNull(retrievedList)
            Assert.assertEquals(3, retrievedList!!.items.size)
            Assert.assertEquals(listWithItems, retrievedList)

            // Reset the id and check if we can retrieve the list with Id set to 0
            localDataSource.resetCreatedBy()
            val updatedList = localDataSource.read(insertedIdAndVersion.first, 0L)
            Assert.assertNotNull(updatedList)
            Assert.assertEquals(insertedIdAndVersion, updatedList!!.listId)
            Assert.assertEquals(3, updatedList.items.size)
            items.forEach { x -> x.addedBy = 0L }
            Assert.assertEquals(items, updatedList.items)
        }

    @Test
    fun testDeleteList() =
        runTest {
            val localDataSource = createLocalSLDataSource()
            val listWithItems =
                ShoppingList(
                    0L,
                    "list with items",
                    ListCreator(1234L, "local creator"),
                    OffsetDateTime.now(),
                    OffsetDateTime.now(),
                    mutableListOf(),
                    1L,
                )
            for (num in 1..3) {
                val item =
                    createApiItem(num)
                listWithItems.items.add(item)
            }
            val insertedIdAndVersion = localDataSource.create(listWithItems)
            Assert.assertEquals(1L, insertedIdAndVersion.first)
            listWithItems.listId = insertedIdAndVersion.first
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
