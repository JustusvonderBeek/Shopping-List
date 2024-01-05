package com.cloudsheeptech.shoppinglist.create.user

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cloudsheeptech.shoppinglist.data.User
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

    private val letterBytes = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!?$%&(){}[]"
    private val defaultPasswordLength = 32

    val inputText = MutableLiveData<String>()

    private val _navigateToApp = MutableLiveData<Boolean>(false)
    val navigateToApp : LiveData<Boolean> get() = _navigateToApp

    private val _hideKeyboard = MutableLiveData<Boolean>(false)
    val hideKeyboard : LiveData<Boolean> get() = _hideKeyboard

    private val jsonSerializer = Json {
        ignoreUnknownKeys = false
        encodeDefaults = true
    }

    init {
        val exists = checkUserExists()
        if (exists)
            navigateToApp()
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

    private suspend fun loadUser() : User? {
        var user: User? = null
        withContext(Dispatchers.IO) {
            try {
                val file = File(getApplication<Application>().filesDir, "user.json")
                if (file.exists())
                    return@withContext
                val reader = file.reader(Charsets.UTF_8)
                val content = reader.readText()
                user = jsonSerializer.decodeFromString<User>(content)
            } catch (ex : Exception) {
                Log.i("StartViewModel", "No user found. Creating one")
            }
        }
        return user
    }

    private fun checkUserExists() : Boolean {
        var exists = false
        recapScope.launch {
            val user = loadUser()
            if (user != null)
                exists = true
        }
        return exists
    }

    private suspend fun storeUser(response: String, password : String) {
        withContext(Dispatchers.IO) {
            try {
                // Store username and ID to disk
                val decodedUser = Json.decodeFromString<User>(response)
                if (decodedUser.ID == 0L || decodedUser.Password != "accepted") {
                    Log.w("StartViewModel", "User not correctly initialized. Storing for offline use")
//                    return@withContext false
                }
                decodedUser.Password = password
                val encodedUser = jsonSerializer.encodeToString(decodedUser)
                val file = File(getApplication<Application>().filesDir, "user.json")
                val writer = file.writer(Charsets.UTF_8)
                writer.write(encodedUser)
                writer.close()
                Log.i("StartViewModel", "Stored user $encodedUser to disk")
                return@withContext true
            } catch (ex : Exception) {
                Log.w("StartViewModel", "Failed to save username to disk: $ex")
                // We don't want to proceed in case the user cannot be stored!
                Toast.makeText(getApplication(), "Failed to store user!", Toast.LENGTH_SHORT).show()
                throw ex
            }
        }
        withContext(Dispatchers.Main) {
            navigateToApp()
        }
    }

    private suspend fun pushUsernameToServer(user : User) {
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
                    storeUser(serializedUser, user.Password)
                    return@POST
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Created new user online", Toast.LENGTH_SHORT).show()
                }
                storeUser(response.bodyAsText(Charsets.UTF_8), user.Password)
            }
        }
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
            pushUsernameToServer(user)
        }
    }

    private fun hideKeyboard() {
        _hideKeyboard.value = true
    }

    fun keyboardHidden() {
        _hideKeyboard.value = false
    }

    fun navigateToApp() {
        _navigateToApp.value = true
    }

    fun onAppNavigated() {
        _navigateToApp.value = false
    }
}