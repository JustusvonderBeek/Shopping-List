package com.cloudsheeptech.shoppinglist.user.api

import com.cloudsheeptech.shoppinglist.TestUtil
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.DEFAULT)
class AuthenticatedUserApiCallsTest {
    @Test
    fun testAuthenticationFlowWithPing() =
        runTest(EmptyCoroutineContext, Duration.parse("3m")) {
            TestUtil.initialize(true, false)
            val userRepo = TestUtil.shoppingListApplication.appUserRepository
            val remoteUserDataSource = TestUtil.shoppingListApplication.appUserRemoteDataSource

            userRepo.create("test user for online")
            val userBeforeOnline = userRepo.read()
            Assert.assertNotNull(userBeforeOnline)
            Assert.assertEquals(0L, userBeforeOnline!!.OnlineID)

            remoteUserDataSource.create()

            val currentUser = userRepo.read()
            Assert.assertNotNull(currentUser)
            Assert.assertNotEquals(0L, currentUser!!.OnlineID)
        }

    @Test
    fun testAuthFlowWithCreateList() =
        runTest(EmptyCoroutineContext, Duration.parse("3m")) {
            TestUtil.initialize(true, false)
            val userRepo = TestUtil.shoppingListApplication.appUserRepository
            val shoppingListRepo = TestUtil.shoppingListApplication.shoppingListRepository

            userRepo.create("test user for online")

            var currentUser = userRepo.read()
            Assert.assertNotNull(currentUser)
            Assert.assertEquals(0L, currentUser!!.OnlineID)

            val newList = shoppingListRepo.create("new list")
            Assert.assertNotNull(newList)
            currentUser = userRepo.read()
            Assert.assertNotNull(currentUser)
            Assert.assertNotEquals(0L, currentUser!!.OnlineID)
        }

    @Test
    fun testUpdateUser() =
        runTest(EmptyCoroutineContext, Duration.parse("3m")) {
            TestUtil.initialize(true, false)
            val userRepo = TestUtil.shoppingListApplication.appUserRepository
            val remoteUserDataSource = TestUtil.shoppingListApplication.appUserRemoteDataSource
            userRepo.create("test user for online")

            var currentUser = userRepo.read()
            Assert.assertNotNull(currentUser)
            Assert.assertEquals(0L, currentUser!!.OnlineID)

            remoteUserDataSource.create()

            currentUser = userRepo.read()
            Assert.assertNotNull(currentUser)
            Assert.assertNotEquals(0L, currentUser!!.OnlineID)

            currentUser.Username = "new username"
            userRepo.update(currentUser)

            currentUser = userRepo.read()
            Assert.assertNotNull(currentUser)
            Assert.assertNotEquals(0L, currentUser!!.OnlineID)
            Assert.assertEquals("new username", currentUser.Username)
        }

    @Test
    fun testDeleteUser() =
        runTest(EmptyCoroutineContext, Duration.parse("3m")) {
            TestUtil.initialize(clearDatabase = true, mockRemoteToDoNothing = false)
            val userRepo = TestUtil.shoppingListApplication.appUserRepository
            val remoteUserDataSource = TestUtil.shoppingListApplication.appUserRemoteDataSource

            userRepo.create("test user for online")

            remoteUserDataSource.create()

            // Setup done, user should exist online now
            val currentUser = userRepo.read()
            Assert.assertNotNull(currentUser)
            Assert.assertNotEquals(0L, currentUser!!.OnlineID)

            userRepo.delete()

            val deletedUser = userRepo.read()
            Assert.assertNull(deletedUser)
        }
}
