package com.cloudsheeptech.shoppinglist.data.user

import android.app.Application
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.testUtil.TestUtil
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.time.OffsetDateTime

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.DEFAULT)
class AppUserOfflineTest {
//
//    @Test
//    fun testPushUsername() = runTest {
//        println("Testing if we can create user")
//        val success = pushUsernameToServer()
//        assert(success)
//    }

    @After
    fun clearDatabase() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)
        val userDao = database.userDao()
        // Clear the db after each test so that no artifacts are contained
        userDao.resetAllUsers()
    }

    /*
     * In order to store a user, we successfully need to:
     * Create
     * Validate
     * Save the user in the database
     * Therefore, these operations are already checked by this test
     */
    @Test
    fun testStoreUser() =
        runTest {
            TestUtil.initialize()

            val username = "new user"
            val appUserLocalDataSource = TestUtil.shoppingListApplication.appUserLocalDataSource
            appUserLocalDataSource.create(username)

            val user = appUserLocalDataSource.getUser()
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

            appUserLocalDataSource.store()
            // Give enough time to store the user into the database
            Thread.sleep(20)

            val database = TestUtil.shoppingListApplication.database
            val userDao = database.userDao()
            val dbUser = userDao.getUser()
            Assert.assertNotNull(dbUser)
            Assert.assertEquals(user, dbUser)

            appUserLocalDataSource.resetPassword()
            val resetPasswordUser = appUserLocalDataSource.getUser()
            Assert.assertNotNull(resetPasswordUser)
            Assert.assertNotEquals(resetPasswordUser?.Password, user.Password)

            appUserLocalDataSource.store()
            Thread.sleep(20)

            val dbUser2 = userDao.getUser()
            Assert.assertNotNull(dbUser2)
            Assert.assertEquals(dbUser2?.Password, resetPasswordUser?.Password)

            val newUsername = "new test user"
            appUserLocalDataSource.setUsername(newUsername)
            val resetUsernameUser = appUserLocalDataSource.getUser()
            Assert.assertNotNull(resetUsernameUser)
            Assert.assertNotEquals(dbUser2?.Username, resetUsernameUser?.Username)

            appUserLocalDataSource.store()
            Thread.sleep(20)

            val dbUser3 = userDao.getUser()
            Assert.assertNotNull(dbUser3)
            Assert.assertEquals(dbUser3?.Username, resetUsernameUser?.Username)
        }

    @Test
    fun testRepeatedStoreUserInDatabase() =
        runTest {
            TestUtil.initialize()

            val username = "new user"
            val appUserLocalDataSource = TestUtil.shoppingListApplication.appUserLocalDataSource
            appUserLocalDataSource.create(username)
            val user = appUserLocalDataSource.getUser()

            appUserLocalDataSource.store()
            // Give enough time to store the user into the database
            Thread.sleep(20)

            val database = TestUtil.shoppingListApplication.database
            val userDao = database.userDao()
            val users = userDao.debugGetAllUserEntries()
            Assert.assertEquals(1, users.size)
            val dbUser = userDao.getUser()
            Assert.assertNotNull(dbUser)
            Assert.assertEquals(user, dbUser)

            val newUsername = "new user with new username"
            appUserLocalDataSource.create(newUsername)
            appUserLocalDataSource.store()
            Thread.sleep(20)

            val allUsers = userDao.debugGetAllUserEntries()
            Assert.assertEquals(1, users.size)
            val newDbUser = userDao.getUser()
            val updatedUser = appUserLocalDataSource.getUser()
            Assert.assertEquals(newDbUser, updatedUser)
            Assert.assertNotEquals(dbUser, newDbUser)
        }

    @Test
    fun testStoreIncorrectUserDatabase() =
        runTest {
            TestUtil.initialize()

            val username = "new user"
            val appUserLocalDataSource = TestUtil.shoppingListApplication.appUserLocalDataSource

            appUserLocalDataSource.store()
            Thread.sleep(10)

            val database = TestUtil.shoppingListApplication.database
            val userDao = database.userDao()
            val dbUser = userDao.getUser()
            Assert.assertNull(dbUser)

            appUserLocalDataSource.create(username)
            val appUser = appUserLocalDataSource.getUser()
            Assert.assertNotNull(appUser)
            // This should now only work on a copy not on the original data
            // and therefore storing should be possible afterwards
            appUser!!.Username = ""
            appUserLocalDataSource.store()
            // Leave enough time to insert the user
            Thread.sleep(20)

            val dbUser2 = userDao.getUser()
            Assert.assertNotNull(dbUser2)

            appUser.Username = username
            appUser.Password = ""
            appUserLocalDataSource.store()
            // Leave enough time to insert the user
            Thread.sleep(20)

            val dbUser3 = userDao.getUser()
            Assert.assertNotNull(dbUser3)

            appUser.Password = "1234"
            appUserLocalDataSource.store()
            // Leave enough time to insert the user
            Thread.sleep(20)

            val dbUser4 = userDao.getUser()
            Assert.assertNotNull(dbUser4)
        }

    @Test
    fun testLoadUser() =
        runTest {
            TestUtil.initialize()

            val username = "new user"
            val appUserLocalDataSource = TestUtil.shoppingListApplication.appUserLocalDataSource
            appUserLocalDataSource.create(username)

            appUserLocalDataSource.store()
            // Give enough time to store the user into the database
            Thread.sleep(20)

            val database = TestUtil.shoppingListApplication.database
            val userDao = database.userDao()
            val dbUser = userDao.getUser()
            Assert.assertNotNull(dbUser)
            // Storing complete, now retrieve

            val newUsername = "test user"
            appUserLocalDataSource.setUsername(newUsername)
            val updatedUser = appUserLocalDataSource.getUser()
            Assert.assertNotNull(updatedUser)
            Assert.assertEquals(newUsername, updatedUser?.Username)
            appUserLocalDataSource.read()
            Thread.sleep(20)

            val loadedUser = appUserLocalDataSource.getUser()
            Assert.assertNotNull(loadedUser)
            Assert.assertEquals(username, loadedUser?.Username)
        }

    @Test
    fun testDeleteUser() =
        runTest {
            TestUtil.initialize()

            val username = "new user"
            val appUserLocalDataSource = TestUtil.shoppingListApplication.appUserLocalDataSource
            appUserLocalDataSource.create(username)

            appUserLocalDataSource.store()
            // Give enough time to store the user into the database
            Thread.sleep(20)

            val database = TestUtil.shoppingListApplication.database
            val userDao = database.userDao()
            val dbUser = userDao.getUser()
            Assert.assertNotNull(dbUser)

            appUserLocalDataSource.delete()
            Thread.sleep(20)

            val users = userDao.debugGetAllUserEntries()
            assert(users.isEmpty())
            val handlerUser = appUserLocalDataSource.getUser()
            Assert.assertNull(handlerUser)
        }
}
