package com.cloudsheeptech.shoppinglist

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cloudsheeptech.shoppinglist.user.AppUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserTest {

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
        AppUser.storeUser(application)
    }

    private fun loadUserFromDatabase(application: Application) {
        AppUser.loadUser(application)
    }

    @Test
    fun testCreatingNewUser() = runTest {
        println("Testing creation of new user")
        val application = ApplicationProvider.getApplicationContext<Application>()

        val newUsername = "new user"
        AppUser.new(newUsername)
        Assert.assertEquals(0L, AppUser.UserId)
        Assert.assertEquals(newUsername, AppUser.Username)
        Assert.assertNotEquals("", AppUser.Password)

        storeUserInDatabase(application)
        Thread.sleep(100)

        val changedUsername = "changed"
        AppUser.UserId = 12L
        AppUser.new(changedUsername)
        Assert.assertEquals(0L, AppUser.UserId)
        Assert.assertEquals(changedUsername, AppUser.Username)
        Assert.assertNotEquals("", AppUser.Password)

        storeUserInDatabase(application)
        Thread.sleep(100)

        AppUser.UserId = 13L
        AppUser.Username = newUsername
        AppUser.Password = "123"

        loadUserFromDatabase(application)
        Thread.sleep(1000)

        Assert.assertEquals(0L, AppUser.UserId)
        Assert.assertEquals(changedUsername, AppUser.Username)
        Assert.assertNotEquals("", AppUser.Password)
    }

}