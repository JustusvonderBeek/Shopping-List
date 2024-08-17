package com.cloudsheeptech.shoppinglist.data.onlineUser

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.DEFAULT)
class OnlineUserOfflineTest {

    private fun createDefaultListCreator() : ListCreator {
        return ListCreator(
            onlineId = 12345L,
            username = "test creator",
        )
    }

    @After
    fun deleteAllEntries() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)
        database.clearAllTables()
    }

    @Test
    fun testCreateUser() = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)

        val offlineDS = OnlineUserLocalDataSource(database)
        val creator = createDefaultListCreator()
        offlineDS.create(creator)

        val onlineUserDao = database.onlineUserDao()
        val allUsers = onlineUserDao.getAllOnlineUsers()
        Assert.assertEquals(1, allUsers.size)
        Assert.assertEquals(creator, allUsers[0])
    }

    @Test
    fun testReadUser() = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)

        val offlineDS = OnlineUserLocalDataSource(database)
        val creator = createDefaultListCreator()
        offlineDS.create(creator)

        val onlineUserDao = database.onlineUserDao()
        val allUsers = onlineUserDao.getAllOnlineUsers()
        Assert.assertEquals(1, allUsers.size)
        Assert.assertEquals(creator, allUsers[0])

        val retrievedUser = offlineDS.read(creator.onlineId)
        Assert.assertNotNull(retrievedUser)

        val nullUser = offlineDS.read(4321L)
        Assert.assertNull(nullUser)
    }

    @Test
    fun testUpdateUser() = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)

        val offlineDS = OnlineUserLocalDataSource(database)
        val creator = createDefaultListCreator()
        offlineDS.create(creator)

        val retrievedUser = offlineDS.read(creator.onlineId)
        Assert.assertNotNull(retrievedUser)

        creator.username = "new user"
        offlineDS.update(creator)
        val updatedUser = offlineDS.read(creator.onlineId)
        Assert.assertNotNull(updatedUser)
        Assert.assertEquals(creator, updatedUser)

        creator.onlineId = 10L
        var exception = false
        try {
            offlineDS.update(creator)
        } catch (ex: IllegalArgumentException) {
            exception = true
        }
        assert(exception)
    }

    @Test
    fun testDeleteUser() = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)

        val offlineDS = OnlineUserLocalDataSource(database)
        val creator = createDefaultListCreator()
        offlineDS.create(creator)

        val retrievedUser = offlineDS.read(creator.onlineId)
        Assert.assertNotNull(retrievedUser)

        offlineDS.delete(creator.onlineId)
        val deletedUser = offlineDS.read(creator.onlineId)
        Assert.assertNull(deletedUser)

    }


}