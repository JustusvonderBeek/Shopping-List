package com.cloudsheeptech.shoppinglist.data.user

import android.content.Context
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.cloudsheeptech.shoppinglist.data.Serializer.OffsetDateTimeSerializer
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.network.Networking
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.time.OffsetDateTime
import kotlin.random.Random

/*
* This singleton represents the app-wide handler for all user operations and
* provides a single access point to offline and online operations like:
* Creating and deleting the app user
* Updating user information
* Storing and retrieving the information from disk
* Pushing the information online
 */

// TODO: Fix storing the user in the database and retrieving values, updating the values
// TODO: Make this class responsible for all the heavy lifting regarding user,
// like sending the user to the server, loading and storing of the data persistently
object AppUserHandler {

    private var database : ShoppingListDatabase? = null
    private var appUserDao : AppUserDao? = null

//    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "userdata")

    private val job = Job()
    private val localCoroutine = CoroutineScope(Dispatchers.Main + job)

//    private const val LETTER_BYTES = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!?$%&(){}[]"
    private const val DEFAULT_PASSWORD_LENGTH = 64
    private val tokenArray = ByteArray(DEFAULT_PASSWORD_LENGTH)
    private var appUser : AppUser? = null

    private var pushing : Boolean = false

    val json = Json {
        serializersModule = SerializersModule {
            contextual(OffsetDateTime::class, OffsetDateTimeSerializer())
        }
        // Because this class is only used to handle the user information and not
        // arbitrary user data, unknown keys represent a protocol violation
        ignoreUnknownKeys = false
        // Again, all information should be contained in the serialized JSON
        // that is sent to the server
        encodeDefaults = true
    }

    private fun setDatabase(appContext: Context) {
        if (database == null)
            database = ShoppingListDatabase.getInstance(appContext)
        if (appUserDao == null)
            appUserDao = database!!.userDao()
    }

    fun isInitialized() : Boolean {
        return appUser != null && appUser?.Username!!.isNotEmpty() && appUser?.Password!!.isNotEmpty()
    }

    fun isAuthenticatedOnline(): Boolean {
        return appUser?.OnlineID != 0L
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

    fun new(username : String) {
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

    fun getUser() : AppUser? {
        // Prevent modifications to the underlying object
        return appUser?.copy()
    }

    private suspend fun loadUserFromDataStore() {
        var loadedUser : AppUser? = null
        withContext(Dispatchers.IO) {
            val storedUser = appUserDao?.getUser()
            if (storedUser == null) {
                Log.e("AppUser", "No user stored in database. Should only happen at the first start")
                return@withContext
            }
            loadedUser = storedUser
        }
        if (loadedUser == null)
            return
        withContext(Dispatchers.Main) {
            appUser = loadedUser
        }
    }

    fun loadUser(appContext: Context) {
        setDatabase(appContext)
        localCoroutine.launch {
            loadUserFromDataStore()
            // The using application needs to take care of pushing the user online
//            if (UserId == 0L && Username.isNotEmpty() && Password.isNotEmpty()) {
//                Log.d("AppUser", "User ID not yet available")
//                pushUserOnline(appContext)
//                storeUserDatabase(DatabaseUser(getUser()))
//            }
            Log.d("AppUser", "User ${getUser()} loaded")
        }
    }

    private suspend fun storeUserInDataStore(user: AppUser) {
        withContext(Dispatchers.IO) {
            // Singleton database, so this MUST always overwrite the existing user
            appUserDao?.insertUser(user)
        }
    }

    fun storeUser(appContext: Context) {
        setDatabase(appContext)
        if (!isInitialized()) {
            Log.w("AppUserHandler", "Cannot store uninitialized user!")
            return
        }
        Log.d("AppUser", "Storing user ${getUser()}")
        localCoroutine.launch {
            // Confirmed before that not null
            val user = getUser()!!
            storeUserInDataStore(user)
        }
    }

    private suspend fun makeToast(context: Context?, message : String) {
        if (context == null)
            return
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    // TODO: I would move these functions out of the current handler and
    // create some other indirection that loads and pushes the user before
    // trying to push any other list, etc.
    private suspend fun pushUserOnline(context: Context?) {
        var userId = 0L
        pushing = true
        withContext(Dispatchers.IO) {
            val user = getUser()
            val serialized = Json.encodeToString(user)
            Networking.POST("auth/create", serialized) { resp ->
                if (resp.status != HttpStatusCode.Created) {
                    Log.w("AppUser", "Failed to create user online")
                    makeToast(context, "Creating user online failed!")
                    return@POST
                }
                Log.d("AppUser", "User online created")
                makeToast(context, "Created user online")
                val body = resp.bodyAsText(Charsets.UTF_8)
                val parsedOnlineUser = json.decodeFromString<OnlineUser>(body)
//                Log.d("AppUser", "Received user: $parsedUser")
//                UserId = parsedOnlineUser.OnlineID
            }
        }
        pushing = false
    }

    private suspend fun deleteUserOnline(context: Context?) {
        withContext(Dispatchers.IO) {
//            Networking.DELETE("v1/users/$UserId", "") {
//                if (it.status != HttpStatusCode.OK) {
//                    makeToast(context, "Failed to delete user online")
//                    return@DELETE
//                }
//                makeToast(context, "Deleted user online")
//            }
        }
    }

    fun PostUserOnline(context: Context?) {
        if (appUser?.OnlineID != 0L || appUser?.Username == "")
            return
//        localCoroutine.launch {
////            pushUserOnline(context)
////            storeUserInDataStore(getUser())
//        }
    }

    suspend fun PostUserOnlineAsync(context: Context?) {
//        if (UserId != 0L || Username == "")
//            return
//        pushUserOnline(context)
//        storeUserInDataStore(DatabaseUser(getUser()))
    }

    private suspend fun deleteUserFromDatabase() {
        withContext(Dispatchers.IO) {
            appUserDao?.resetAllUsers()
        }
    }

    fun DeleteUser(context: Context?) {
//        if (UserId == 0L || Username == "")
//            return
//        localCoroutine.launch {
//            deleteUserOnline(context)
//            Networking.resetToken()
//            listHandler?.resetCreatedByForOwnLists(UserId)
//            deleteUserFromDatabase()
//        }
    }



    fun getUserLive() : LiveData<AppUser>? {
        return appUserDao?.getUserLive()?.map { x -> AppUser(x.ID, x.OnlineID, x.Username, x.Password, x.Created) }
    }

    fun isPushingUser() : Boolean {
        return pushing
    }
}