package com.cloudsheeptech.shoppinglist.data.user

import android.app.Application
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.time.OffsetDateTime

@RunWith(AndroidJUnit4::class)
class AppUserOfflineTest {
//
//    @Test
//    fun testPushUsername() = runTest {
//        println("Testing if we can create user")
//        val success = pushUsernameToServer()
//        assert(success)
//    }

    /*
    * In order to store a user, we successfully need to:
    * Create
    * Validate
    * Save the user in the database
    * Therefore, these operations are already checked by this test
     */
    @Test
    fun testStoreUser() = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()

        val database = ShoppingListDatabase.getInstance(application)
        val localAppUserHandler = AppUserLocalDataSource(database)

        val username = "new user"
        localAppUserHandler.create(username)
        val user = localAppUserHandler.getUser()
        Assert.assertNotNull(user)
        Assert.assertEquals(1L, user?.ID)
        Assert.assertEquals(0L, user?.OnlineID)
        Assert.assertEquals(username, user?.Username)
        Assert.assertNotEquals("", user?.Password)
        Assert.assertNotNull(user?.Created)
        val now = OffsetDateTime.now()
        Log.d("AppUserOfflineTest", "Now: $now, Created: ${user?.Created}")
        assert(now.isEqual(user?.Created) || now.isAfter(user?.Created))
        assert(64 <= user?.Password!!.length)

//        storeUserInDatabase(application)
        localAppUserHandler.store()
        // Give enough time to store the user into the database
        Thread.sleep(50)

        val userDao = database.userDao()
        val dbUser = userDao.getUser()
        Assert.assertNotNull(dbUser)
        Assert.assertEquals(user, dbUser)

        localAppUserHandler.resetPassword()
        val resetPasswordUser = localAppUserHandler.getUser()
        Assert.assertNotNull(resetPasswordUser)
        Assert.assertNotEquals(resetPasswordUser?.Password, user.Password)

        localAppUserHandler.store()
        Thread.sleep(50)
        val dbUser2 = userDao.getUser()
        Assert.assertNotNull(dbUser2)
        Assert.assertEquals(dbUser2?.Password, resetPasswordUser?.Password)

        val newUsername = "new test user"
        localAppUserHandler.resetUsername(newUsername)
        val resetUsernameUser = localAppUserHandler.getUser()
        Assert.assertNotNull(resetUsernameUser)
        Assert.assertNotEquals(dbUser2?.Username, resetUsernameUser?.Username)

        localAppUserHandler.store()
        Thread.sleep(50)
        val dbUser3 = userDao.getUser()
        Assert.assertNotNull(dbUser3)
        Assert.assertEquals(dbUser3?.Username, resetUsernameUser?.Username)
    }

    @Test
    fun testRepeatedStoreUserInDatabase() = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()

        val database = ShoppingListDatabase.getInstance(application)
        val localAppUserHandler = AppUserLocalDataSource(database)

        val username = "new user"
        localAppUserHandler.create(username)
        val createdUser = localAppUserHandler.getUser()
        localAppUserHandler.store()
        Thread.sleep(50)

        val userDao = database.userDao()
        val users = userDao.debugGetAllUserEntries()
        assert(1 == users.size)
        val dbUser = userDao.getUser()
        Assert.assertNotNull(dbUser)
        Assert.assertEquals(createdUser, dbUser)

        val newUsername = "new user with new username"
        localAppUserHandler.create(newUsername)
        localAppUserHandler.store()
        Thread.sleep(50)

        val allUsers = userDao.debugGetAllUserEntries()
        assert(1 == allUsers.size)
        val newDbUser = userDao.getUser()
        val updatedUser = localAppUserHandler.getUser()
        Assert.assertEquals(newDbUser, updatedUser)
        Assert.assertNotEquals(dbUser, newDbUser)
    }

    @Test
    fun testStoreIncorrectUserDatabase() = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()

        val database = ShoppingListDatabase.getInstance(application)
        val localAppUserHandler = AppUserLocalDataSource(database)

        val userDao = database.userDao()
        localAppUserHandler.store()
        val dbUser = userDao.getUser()
//        Log.d("AppUserOfflineTest", "Retrieved user: $dbUser")
        Assert.assertNull(dbUser)

        val username = "test user"
        localAppUserHandler.create(username)
        // This should now only work on a copy not on the original data
        // and therefore storing should be possible afterwards
        localAppUserHandler.getUser()!!.Username = ""
        localAppUserHandler.store()
        // Leave enough time to insert the user
        Thread.sleep(50)
        val dbUser2 = userDao.getUser()
//        Log.d("AppUserOfflineTest", "Retrieved user: $dbUser2")
        Assert.assertNotNull(dbUser2)

        localAppUserHandler.getUser()!!.Username = username
        localAppUserHandler.getUser()!!.Password = ""
        localAppUserHandler.store()
        // Leave enough time to insert the user
        Thread.sleep(50)
        val dbUser3 = userDao.getUser()
        Assert.assertNotNull(dbUser3)

        localAppUserHandler.getUser()!!.Password = "1234"
        localAppUserHandler.store()
        // Leave enough time to insert the user
        Thread.sleep(50)
        val dbUser4 = userDao.getUser()
        Assert.assertNotNull(dbUser4)
    }

    @Test
    fun testLoadUser() = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()

        val database = ShoppingListDatabase.getInstance(application)
        val localAppUserHandler = AppUserLocalDataSource(database)

        val username = "new user"
        localAppUserHandler.create(username)
        localAppUserHandler.store()
        Thread.sleep(50)

        val userDao = database.userDao()
        val dbUser = userDao.getUser()
        Assert.assertNotNull(dbUser)
        // Storing complete, now retrieve

        val newUsername = "test user"
        localAppUserHandler.resetUsername(newUsername)
        val updatedUser = localAppUserHandler.getUser()
        Assert.assertNotNull(updatedUser)
        Assert.assertEquals(newUsername, updatedUser?.Username)
        localAppUserHandler.read()
        Thread.sleep(50)
        val loadedUser = localAppUserHandler.getUser()
        Assert.assertNotNull(loadedUser)
        Assert.assertEquals(username, loadedUser?.Username)
    }

    @Test
    fun testDeleteUser() = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()

        val database = ShoppingListDatabase.getInstance(application)
        val localAppUserHandler = AppUserLocalDataSource(database)

        val username = "new user"
        localAppUserHandler.create(username)
        localAppUserHandler.store()
        Thread.sleep(50)

        val userDao = database.userDao()
        val dbUser = userDao.getUser()
        Assert.assertNotNull(dbUser)

        localAppUserHandler.delete()
        Thread.sleep(50)

        val users = userDao.debugGetAllUserEntries()
        assert(users.isEmpty())
        val handlerUser = localAppUserHandler.getUser()
        Assert.assertNull(handlerUser)
    }

}