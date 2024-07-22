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

    private suspend fun pushUsernameToServer() : Boolean {
        return false
    }

    @Test
    fun testPushUsername() = runTest {
        println("Testing if we can create user")
        val success = pushUsernameToServer()
        assert(success)
    }

    private fun storeUserInDatabase(application: Application) {
        AppUserHandler.storeUser(application)
    }

    private fun loadUserFromDatabase(application: Application) {
        AppUserHandler.loadUser(application)
    }

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

        val username = "new user"
        AppUserHandler.new(username)
        val user = AppUserHandler.getUser()
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
        AppUserHandler.storeUser(application)
        // Give enough time to store the user into the database
        Thread.sleep(50)

        val database = ShoppingListDatabase.getInstance(application)
        val userDao = database.userDao()
        val dbUser = userDao.getUser()
        Assert.assertNotNull(dbUser)
        Assert.assertEquals(user, dbUser)

        AppUserHandler.resetPassword()
        val resetPasswordUser = AppUserHandler.getUser()
        Assert.assertNotNull(resetPasswordUser)
        Assert.assertNotEquals(resetPasswordUser?.Password, user.Password)

        AppUserHandler.storeUser(application)
        Thread.sleep(50)
        val dbUser2 = userDao.getUser()
        Assert.assertNotNull(dbUser2)
        Assert.assertEquals(dbUser2?.Password, resetPasswordUser?.Password)

        val newUsername = "new test user"
        AppUserHandler.resetUsername(newUsername)
        val resetUsernameUser = AppUserHandler.getUser()
        Assert.assertNotNull(resetUsernameUser)
        Assert.assertNotEquals(dbUser2?.Username, resetUsernameUser?.Username)

        AppUserHandler.storeUser(application)
        Thread.sleep(50)
        val dbUser3 = userDao.getUser()
        Assert.assertNotNull(dbUser3)
        Assert.assertEquals(dbUser3?.Username, resetUsernameUser?.Username)
    }

    @Test
    fun testStoreIncorrectUserDatabase() = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()

        val database = ShoppingListDatabase.getInstance(application)
        val userDao = database.userDao()
        AppUserHandler.storeUser(application)
        val dbUser = userDao.getUser()
//        Log.d("AppUserOfflineTest", "Retrieved user: $dbUser")
        Assert.assertNull(dbUser)

        val username = "test user"
        AppUserHandler.new(username)
        // This should now only work on a copy not on the original data
        // and therefore storing should be possible afterwards
        AppUserHandler.getUser()!!.Username = ""
        AppUserHandler.storeUser(application)
        // Leave enough time to insert the user
        Thread.sleep(50)
        val dbUser2 = userDao.getUser()
//        Log.d("AppUserOfflineTest", "Retrieved user: $dbUser2")
        Assert.assertNotNull(dbUser2)

        AppUserHandler.getUser()!!.Username = username
        AppUserHandler.getUser()!!.Password = ""
        AppUserHandler.storeUser(application)
        // Leave enough time to insert the user
        Thread.sleep(50)
        val dbUser3 = userDao.getUser()
        Assert.assertNotNull(dbUser3)

        AppUserHandler.getUser()!!.Password = "1234"
        AppUserHandler.storeUser(application)
        // Leave enough time to insert the user
        Thread.sleep(50)
        val dbUser4 = userDao.getUser()
        Assert.assertNotNull(dbUser4)
    }

    @Test
    fun testLoadUser() = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()

        val username = "new user"
        AppUserHandler.new(username)
        AppUserHandler.storeUser(application)
        Thread.sleep(50)

        val database = ShoppingListDatabase.getInstance(application)
        val userDao = database.userDao()
        val dbUser = userDao.getUser()
        Assert.assertNotNull(dbUser)
        // Storing complete, now retrieve

        val newUsername = "test user"
        AppUserHandler.resetUsername(newUsername)
        val updatedUser = AppUserHandler.getUser()
        Assert.assertNotNull(updatedUser)
        Assert.assertEquals(newUsername, updatedUser?.Username)
        AppUserHandler.loadUser(application)
        Thread.sleep(50)
        val loadedUser = AppUserHandler.getUser()
        Assert.assertNotNull(loadedUser)
        Assert.assertEquals(username, loadedUser?.Username)
    }

}