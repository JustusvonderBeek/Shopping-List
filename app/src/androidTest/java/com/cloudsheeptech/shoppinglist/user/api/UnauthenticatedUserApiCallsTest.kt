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
class UnauthenticatedUserApiCallsTest {
    @Test
    fun testCreateUserApiCall() =
        runTest(EmptyCoroutineContext, Duration.parse("3m")) {
            TestUtil.initialize(true, null)
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
    fun testLoginUserApiCall() =
        runTest(EmptyCoroutineContext, Duration.parse("3m")) {
            TestUtil.initialize(true, null)
            val userRepo = TestUtil.shoppingListApplication.appUserRepository
            val userApi = TestUtil.shoppingListApplication.userUnauthenticatedApi
            userRepo.create("new user")
            val testUser = userRepo.read()
            Assert.assertNotNull(testUser)

            val onlineUser = userApi.create(testUser!!)
            Assert.assertNotNull(onlineUser)
            Assert.assertEquals("accepted", onlineUser!!.password)
            onlineUser.password = testUser.Password

            val token =
                userApi.login(
                    onlineUser.onlineId,
                    onlineUser,
                )
            Assert.assertNotNull(token)
        }
}
