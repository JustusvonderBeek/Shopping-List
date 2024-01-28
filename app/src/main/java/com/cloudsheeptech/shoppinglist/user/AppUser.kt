package com.cloudsheeptech.shoppinglist.user

import android.content.Context
import android.util.Log
import com.cloudsheeptech.shoppinglist.data.User
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.database.UserDao
import com.cloudsheeptech.shoppinglist.network.Networking
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object AppUser {

    private var database : ShoppingListDatabase? = null
    private var userDao : UserDao? = null

    private val job = Job()
    private val localCoroutine = CoroutineScope(Dispatchers.Main + job)

    var ID : Long = 0L
    var Username : String = ""
    var Password : String = ""

    fun Initialized() : Boolean {
        return Username != "" && Password != ""
    }

    fun getUser() : User {
        return User(ID, Username, Password)
    }

    private suspend fun loadUserDatabase() {
        var user = User()
        withContext(Dispatchers.IO) {
            val dbUser = userDao!!.getUser()
            if (dbUser == null) {
                Log.w("AppUser", "User not found")
                return@withContext
            }
            user = dbUser
        }
        withContext(Dispatchers.Main) {
            ID = user.ID
            Username = user.Username
            Password = user.Password
        }
    }

    fun loadUser(appContext: Context) {
        database = ShoppingListDatabase.getInstance(appContext)
        userDao = database!!.userDao()
        localCoroutine.launch {
            loadUserDatabase()
        }
    }

    private suspend fun storeUserDatabase(user: User) {
        withContext(Dispatchers.IO) {
            userDao?.resetUser()
            userDao?.insertUser(user)
        }
    }

    fun storeUser() {
        if (!Initialized())
            return
        Log.d("AppUser", "Storing user")
        localCoroutine.launch {
            val user = getUser()
            storeUserDatabase(user)
        }
    }

    // TODO: Move this method here?
    private suspend fun pushUserOnline() {
        withContext(Dispatchers.IO) {
            val user = getUser()
            val serialized = Json.encodeToString(user)
            Networking.POST("auth/create", serialized) { resp ->
                if (resp.status != HttpStatusCode.Created) {
                    Log.w("AppUser", "Failed to create user online")
                }
                Log.d("AppUser", "User online created")
                val body = resp.bodyAsText(Charsets.UTF_8)
                val parsedUser = Json.decodeFromString<User>(body)
                ID = parsedUser.ID
            }
        }
    }

    fun PostUserOnline() {
        localCoroutine.launch {
            pushUserOnline()
        }
    }

}