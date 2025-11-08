package com.cloudsheeptech.shoppinglist.user.api

import com.cloudsheeptech.shoppinglist.TestUtil
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

@RunWith(JUnit4::class)
class UserApiTest {
    @Test
    fun testDeleteUser() =
        runTest(EmptyCoroutineContext, Duration.parse("3m")) {
            TestUtil.initialize(clearDatabase = true, mockRemoteToDoNothing = false)
            val userRepo = TestUtil.shoppingListApplication.appUserRepository
            val userApi = TestUtil.shoppingListApplication.userAuthenticatedApi

            userRepo.create("test user for online")

            userApi.ping()

            // Setup done, user should exist online now
            val currentUser = userRepo.read()
            Assert.assertNotNull(currentUser)

            userRepo.delete()

            val deletedUser = userRepo.read()
            Assert.assertNotNull(deletedUser)
            Assert.assertEquals(0L, deletedUser!!.OnlineID)
        }
}
