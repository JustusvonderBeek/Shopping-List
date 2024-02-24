package com.cloudsheeptech.shoppinglist.user

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.cloudsheeptech.shoppinglist.data.DatabaseUser
import com.cloudsheeptech.shoppinglist.data.Serializer.OffsetDateTimeSerializer
import com.cloudsheeptech.shoppinglist.data.User
import com.cloudsheeptech.shoppinglist.data.UserWire
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.database.UserDao
import com.cloudsheeptech.shoppinglist.data.handling.ShoppingListHandler
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

// TODO: Fix storing the user in the database and retrieving values, updating the values
// TODO: Make this class responsible for all the heavy lifting regarding user,
// like sending the user to the server, loading and storing of the data persistently
object AppUser {

    private var database : ShoppingListDatabase? = null
    private var userDao : UserDao? = null
    private var listHandler : ShoppingListHandler? = null

//    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "userdata")

    private val job = Job()
    private val localCoroutine = CoroutineScope(Dispatchers.Main + job)

    private const val letterBytes = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!?$%&(){}[]"
    private const val defaultPasswordLength = 32

    private var pushing : Boolean = false

    var UserId : Long = 0L
    var Username : String = ""
    var Password : String = ""

    val json = Json {
        serializersModule = SerializersModule {
            contextual(OffsetDateTime::class, OffsetDateTimeSerializer())
        }
    }

    private fun setDatabase(appContext: Context) {
        if (database == null)
            database = ShoppingListDatabase.getInstance(appContext)
        if (userDao == null)
            userDao = database!!.userDao()
        if (listHandler == null)
            listHandler = ShoppingListHandler(database!!)
    }

    fun Initialized() : Boolean {
        return Username != "" && Password != ""
    }

    private fun generatePassword() : String {
        val password = StringBuilder()
        for (i in 0..defaultPasswordLength) {
            val randIndex = Random.nextInt(letterBytes.length)
            val char = letterBytes[randIndex]
            password.append(char)
        }
        return password.toString()
    }

    fun new(username : String) {
        // UserId must be assigned by the server
        UserId = 0L
        Username = username
        Password = generatePassword()
    }

    private suspend fun loadUserFromDataStore() {
        var user = DatabaseUser(1, 0,"", "")
        withContext(Dispatchers.IO) {
            val dbUser = userDao!!.getUser()
            if (dbUser == null) {
                Log.e("AppUser", "User not found! This should only happen when no user was created!")
                return@withContext
            }
            user = dbUser
        }
        withContext(Dispatchers.Main) {
            UserId = user.UserId
            Username = user.Username
            Password = user.Password
        }
    }

    fun loadUser(appContext: Context) {
        setDatabase(appContext)
        localCoroutine.launch {
            loadUserFromDataStore()
            if (UserId == 0L && Username.isNotEmpty() && Password.isNotEmpty()) {
                Log.d("AppUser", "User ID not yet available")
                pushUserOnline(appContext)
                storeUserDatabase(DatabaseUser(getUser()))
            }
            Log.d("AppUser", "User ${getUser()} loaded")
        }
    }

    private suspend fun storeUserDatabase(user: DatabaseUser) {
        withContext(Dispatchers.IO) {
            // Singleton database, so this MUST always overwrite the existing user
            userDao?.insertUser(user)
        }
    }

    fun storeUser(appContext: Context) {
        setDatabase(appContext)
        if (!Initialized())
            return
        Log.d("AppUser", "Storing user ${getUser()}")
        localCoroutine.launch {
            val user = DatabaseUser(getUser())
            storeUserDatabase(user)
        }
    }

    private suspend fun makeToast(context: Context?, message : String) {
        if (context == null)
            return
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

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
                val parsedUser = json.decodeFromString<UserWire>(body)
//                Log.d("AppUser", "Received user: $parsedUser")
                UserId = parsedUser.ID
            }
        }
        pushing = false
    }

    private suspend fun deleteUserOnline(context: Context?) {
        withContext(Dispatchers.IO) {
            Networking.DELETE("v1/users/${AppUser.UserId}", "") {
                if (it.status != HttpStatusCode.OK) {
                    makeToast(context, "Failed to delete user online")
                    return@DELETE
                }
                makeToast(context, "Deleted user online")
            }
        }
    }

    fun PostUserOnline(context: Context?) {
        if (UserId != 0L || Username == "")
            return
        localCoroutine.launch {
            pushUserOnline(context)
            storeUserDatabase(DatabaseUser(getUser()))
        }
    }

    suspend fun PostUserOnlineAsync(context: Context?) {
        if (UserId != 0L || Username == "")
            return
        pushUserOnline(context)
        storeUserDatabase(DatabaseUser(getUser()))
    }

    private suspend fun deleteUserFromDatabase() {
        withContext(Dispatchers.IO) {
            userDao?.resetUser()
        }
    }

    fun DeleteUser(context: Context?) {
        if (UserId == 0L || Username == "")
            return
        localCoroutine.launch {
            deleteUserOnline(context)
            Networking.resetToken()
            listHandler?.resetCreatedByForOwnLists(UserId)
            deleteUserFromDatabase()
        }
    }

    fun getUser() : User {
        return User(UserId, Username, Password)
    }

    fun getUserLive() : LiveData<User>? {
        return userDao?.getUserLive()?.map { x -> User(x.UserId, x.Username, x.Password) }
    }

    fun isPushingUser() : Boolean {
        return pushing
    }
}