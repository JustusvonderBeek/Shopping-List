package com.cloudsheeptech.shoppinglist.data

import android.app.Application
import android.util.Log
import com.cloudsheeptech.shoppinglist.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.database.UserDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object AppUser {

    private var database : ShoppingListDatabase? = null
    private var userDao : UserDao? = null

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

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

    fun loadUser(application: Application) {
        database = ShoppingListDatabase.getInstance(application.applicationContext)
        userDao = database!!.userDao()
        scope.launch {
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
        scope.launch {
            val user = getUser()
            storeUserDatabase(user)
        }
    }

}