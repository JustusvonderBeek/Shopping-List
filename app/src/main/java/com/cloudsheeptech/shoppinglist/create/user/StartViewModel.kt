package com.cloudsheeptech.shoppinglist.create.user

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cloudsheeptech.shoppinglist.data.AppUser
import com.cloudsheeptech.shoppinglist.data.User
import com.cloudsheeptech.shoppinglist.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.network.Networking
import io.ktor.client.statement.HttpResponse
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
import java.io.File
import kotlin.Exception
import kotlin.random.Random

class StartViewModel(application: Application) : AndroidViewModel(application) {

    private val job = Job()
    private val recapScope = CoroutineScope(Dispatchers.IO + job)

    // -----------------------------------------------

    private val database = ShoppingListDatabase.getInstance(application.applicationContext)
    private val userDao = database.userDao()

    // UI State

    private val _hideKeyboard = MutableLiveData<Boolean>(false)
    val hideKeyboard : LiveData<Boolean> get() = _hideKeyboard

    // Data

    private val letterBytes = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!?$%&(){}[]"
    private val defaultPasswordLength = 32
    private val jsonSerializer = Json {
        ignoreUnknownKeys = false
        encodeDefaults = true
    }

    val user = userDao.getUserLive()
    val inputText = MutableLiveData<String>()


    // -----------------------------------------------

    init {
        if (user.value != null) {
            Log.i("StartViewModel", "User already exists. Navigate back")
        }
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

    private suspend fun storeUser(user : User) {
        withContext(Dispatchers.IO) {
            try {
                AppUser.ID = user.ID
                AppUser.Username = user.Username
                AppUser.Password = user.Password
                AppUser.storeUser()
                Log.i("StartViewModel", "Stored user to database")
            } catch (ex : Exception) {
                Log.w("StartViewModel", "Failed to save user: $ex")
                // We don't want to proceed in case the user cannot be stored!
                Toast.makeText(getApplication(), "Failed to store user!", Toast.LENGTH_SHORT).show()
                throw ex
            }
        }
    }

    private suspend fun pushUsernameToServer(user : User) : User {
        withContext(Dispatchers.IO) {
            val serializedUser = jsonSerializer.encodeToString(user)
            // Make sure that one can use the app without internet
            Networking.POST("auth/create", serializedUser) { response ->
//                Log.i("StartViewModel", "Handling response")
                if (response.status != HttpStatusCode.Created) {
                    Log.w("StartViewModel", "Failed to create user at server")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "Failed to store user at server", Toast.LENGTH_SHORT).show()
                    }
                    return@POST
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Created new user online", Toast.LENGTH_SHORT).show()
                }
                val respBody = response.bodyAsText(Charsets.UTF_8)
                val decodedUser = Json.decodeFromString<User>(respBody)
                if (decodedUser.ID == 0L || decodedUser.Password != "accepted") {
                    Log.e("StartViewModel", "User not correctly initialized!")
                    return@POST
                }
                user.ID = decodedUser.ID
            }
        }
        return user
    }

    fun pushUsername() {
        hideKeyboard()
        if (inputText.value == null)
            return
        if (inputText.value!!.isEmpty())
            return
        val password = generatePassword()
        val user = User(0, inputText.value!!, password)
        Log.d("StartViewModel", "Creating user $user")
        recapScope.launch {
            val onlineUser = pushUsernameToServer(user)
            storeUser(onlineUser)
        }
    }

    // -----------------------------------------------

    private fun hideKeyboard() {
        _hideKeyboard.value = true
    }

    fun keyboardHidden() {
        _hideKeyboard.value = false
    }
}