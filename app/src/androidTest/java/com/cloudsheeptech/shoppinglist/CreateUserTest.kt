package com.cloudsheeptech.shoppinglist

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreateUserTest {

    private suspend fun pushUsernameToServer() : Boolean {
        return false
    }

    @Test
    fun testPushUsername() = runTest {
        println("Testing if we can create user")
        val success = pushUsernameToServer()
        assert(success)
    }

}