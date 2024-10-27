package com.cloudsheeptech.shoppinglist.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.user.AppUserLocalDataSource
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppUserRepositoryTest {
    @Test
    fun testCreateUser() =
        runTest {
            val application = ApplicationProvider.getApplicationContext<Application>()
            val database = ShoppingListDatabase.getInstance(application)

            val localSource = AppUserLocalDataSource(database)
            val tokenFile =
                application.applicationContext.dataDir.path
                    .plus("/tokens.txt")
//        val network = Networking(tokenFile)
//        val remoteSource = AppUserRemoteDataSource(network)
//
//        val userRepository = AppUserRepository(localSource, remoteSource)
//        userRepository.create("new user")
//        val user = userRepository.read()
//        Assert.assertNotNull(user)
//        Assert.assertEquals("new user", user!!.Username)
//        Thread.sleep(20)
//        Assert.assertNotEquals(0, user.OnlineID)
//
//        // Check if the user is also correctly stored in the local database
//        val userDao = database.userDao()
//        val dbUser = userDao.getUser()
//        Assert.assertNotNull(dbUser)
//        Assert.assertEquals(user!!.OnlineID, dbUser!!.OnlineID)
        }

    @Test
    fun testUpdateUser() =
        runTest {
            val application = ApplicationProvider.getApplicationContext<Application>()
            val database = ShoppingListDatabase.getInstance(application)

            val localSource = AppUserLocalDataSource(database)
//            val tokenFile =
//                application.applicationContext.dataDir.path
//                    .plus("/tokens.txt")
//            val network = Networking(tokenFile)
//            val remoteSource = AppUserRemoteDataSource(network)
//
//            val userRepository = AppUserRepository(localSource, remoteSource)
//            userRepository.create("new user")
//            val user = userRepository.read()
//            Assert.assertNotNull(user)
//            Assert.assertEquals("new user", user!!.Username)
//
//            user.Username = "updated user"
//            userRepository.update(user)
//            Thread.sleep(20)
//
//            val updatedRepoUser = userRepository.read()
//            Assert.assertNotNull(updatedRepoUser)
//            Assert.assertNotEquals("new user", updatedRepoUser!!.Username)
//            Assert.assertEquals(user.OnlineID, updatedRepoUser.OnlineID)
//
//            val userDao = database.userDao()
//            val dbUser = userDao.getUser()
//            Assert.assertNotNull(dbUser)
//            Assert.assertEquals(updatedRepoUser.Username, dbUser!!.Username)
        }

    @Test
    fun testDeleteUser() =
        runTest {
            val application = ApplicationProvider.getApplicationContext<Application>()
            val database = ShoppingListDatabase.getInstance(application)

            val localSource = AppUserLocalDataSource(database)
//            val tokenFile =
//                application.applicationContext.dataDir.path
//                    .plus("/tokens.txt")
//            val network = Networking(tokenFile)
//            val remoteSource = AppUserRemoteDataSource(network)
//
//            val userRepository = AppUserRepository(localSource, remoteSource)
//            userRepository.create("new user")
//            val user = userRepository.read()
//            Assert.assertNotNull(user)
//            Assert.assertEquals("new user", user!!.Username)
//            Thread.sleep(20)
//            Assert.assertNotEquals(0, user.OnlineID)
//
//            userRepository.delete()
//            Thread.sleep(10)
//            val deletedUser = userRepository.read()
//            Assert.assertNull(deletedUser)
//
//            val userDao = database.userDao()
//            val dbUser = userDao.getUser()
//            Assert.assertNull(dbUser)
        }
}
