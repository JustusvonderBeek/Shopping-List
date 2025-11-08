package com.cloudsheeptech.shoppinglist.network

import com.cloudsheeptech.shoppinglist.testUtil.TestUtil
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

@RunWith(JUnit4::class)
class AuthenticationFlowTest {
    @Test
    fun testCreateUser() =
        runTest(EmptyCoroutineContext, Duration.parse("3m")) {
            TestUtil.initialize(true, false)
            val userRepo = TestUtil.shoppingListApplication.appUserRepository
            val userApi = TestUtil.shoppingListApplication.userUnauthenticatedApi
            userRepo.create("new user")
            val testUser = userRepo.read()
            Assert.assertNotNull(testUser)

            val onlineUser =
                userApi.create(
                    testUser!!,
                )
            Assert.assertNotNull(onlineUser)
            Assert.assertNotEquals(0L, onlineUser!!.onlineId)
        }

    @Test
    fun testAuthFlowWithPing() =
        runTest(EmptyCoroutineContext, Duration.parse("3m")) {
            TestUtil.initialize(true, false)
            val userRepo = TestUtil.shoppingListApplication.appUserRepository
            val userApi = TestUtil.shoppingListApplication.userAuthenticatedApi

            userRepo.create("test user for online")

            userApi.ping()

            val currentUser = userRepo.read()
            Assert.assertNotNull(currentUser)
            Assert.assertNotEquals(0L, currentUser!!.OnlineID)
        }
}
