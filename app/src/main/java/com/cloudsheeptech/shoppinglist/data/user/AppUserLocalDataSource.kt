package com.cloudsheeptech.shoppinglist.data.user

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import javax.inject.Inject
import kotlin.random.Random

/*
* This singleton represents the app-wide handler for all user operations and
* provides a single access point to offline operations like:
* Creating and deleting the app user
* Updating user information
* Storing and retrieving the information from disk
* Delete the user information
 */

class AppUserLocalDataSource @Inject constructor(database: ShoppingListDatabase) {

    private val appUserDao: AppUserDao = database.userDao()

    //    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "userdata")

    //    private const val LETTER_BYTES = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!?$%&(){}[]"
    private val DEFAULT_PASSWORD_LENGTH = 64
    private val tokenArray = ByteArray(DEFAULT_PASSWORD_LENGTH)
    private var appUser: AppUser? = null

    fun isInitialized(): Boolean {
        return appUser != null && appUser?.Username!!.isNotEmpty() && appUser?.Password!!.isNotEmpty()
    }

    private fun generateNewPassword() {
        Random.nextBytes(tokenArray)
//        val password = StringBuilder()
//        for (i in 0..DEFAULT_PASSWORD_LENGTH) {
//            val randIndex = Random.nextInt(LETTER_BYTES.length)
//            val char = LETTER_BYTES[randIndex]
//            password.append(char)
//        }
//        return password.toString()
    }

    fun create(username: String) {
        // UserId must be assigned by the server
        generateNewPassword()
        val passwordString = Base64.encodeToString(tokenArray, 0)
        this.appUser = AppUser(Username = username, Password = passwordString, Created = OffsetDateTime.now())
    }

    fun resetPassword() {
        if (!isInitialized())
            return
        generateNewPassword()
        val passwordString = Base64.encodeToString(tokenArray, 0)
        this.appUser?.Password = passwordString
    }

    fun resetUsername(username: String) {
        this.appUser?.Username = username
    }

    fun resetOnlineId(onlineId: Long) {
        this.appUser?.OnlineID = onlineId
    }

    fun getUser(): AppUser? {
        // Prevent modifications to the underlying object
        return appUser?.copy()
    }

    fun getUserLive(): LiveData<AppUser> {
        return appUserDao.getUserLive()
    }

    private suspend fun loadUserFromDataStore() : AppUser? {
        var loadedUser: AppUser? = null
        withContext(Dispatchers.IO) {
            val storedUser = appUserDao.getUser()
            if (storedUser == null) {
                Log.e(
                    "AppUser",
                    "No user stored in database. Should only happen at the first start"
                )
                return@withContext
            }
            loadedUser = storedUser
        }
        return loadedUser
    }

    suspend fun read() {
        val loadedUser = loadUserFromDataStore() ?: return
        appUser = loadedUser
        // The using application needs to take care of pushing the user online
        //            if (UserId == 0L && Username.isNotEmpty() && Password.isNotEmpty()) {
        //                Log.d("AppUser", "User ID not yet available")
        //                pushUserOnline(appContext)
        //                storeUserDatabase(DatabaseUser(getUser()))
        //            }
        Log.d("AppUser", "User ${getUser()} loaded from database")
    }

    private suspend fun storeUserInDataStore(user: AppUser) {
        withContext(Dispatchers.IO) {
            // Singleton database, so this MUST always overwrite the existing user
            appUserDao.insertUser(user)
        }
    }

    suspend fun store() {
        if (!isInitialized()) {
            Log.w("AppUserHandler", "Cannot store uninitialized user!")
            return
        }
        Log.d("AppUser", "Storing user ${getUser()}")
        val user = getUser()!!
        storeUserInDataStore(user)
    }

    private suspend fun deleteUserFromDatabase() {
        withContext(Dispatchers.IO) {
            appUserDao.resetAllUsers()
        }
    }

    suspend fun delete() {
        if (!isInitialized())
            return
        deleteUserFromDatabase()
        this.appUser = null
    }
}