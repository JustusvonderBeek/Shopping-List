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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.lang.Exception
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

    val jsonSerializer = Json {
        ignoreUnknownKeys = false
        encodeDefaults = true
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

    fun pushUsername() {
        hideKeyboard()
        if (inputText.value == null)
            return
        if (inputText.value!!.isEmpty())
            return
        val password = generatePassword()
        val user = User(0, inputText.value!!, password)
        val serialized = jsonSerializer.encodeToString(user)
        Log.d("StartViewModel", "Including user in format: $serialized in request")
        recapScope.launch {
            Networking.POST("auth/create", serialized) { response ->
                Log.i("StartViewModel", "Handling response")
                if (response.status != HttpStatusCode.Created) {
                    Log.w("StartViewModel", "Failed to create user at server")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "Failed to store user at server", Toast.LENGTH_SHORT).show()
                    }
                    return@POST
                }
                storeUser(response, password)
            }
        }
    }

    private suspend fun storeUser(response: HttpResponse, password : String) {
        val result = withContext(Dispatchers.IO) {
            try {
                // Store username and ID to disk
                val body = response.bodyAsText(Charsets.UTF_8)
                val decodedUser = Json.decodeFromString<User>(body)
                if (decodedUser.ID == 0L || decodedUser.Password != "accepted") {
                    Log.w("StartViewModel", "Given user is not correctly initialized!")
                    return@withContext false
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
                Toast.makeText(getApplication(), "$ex", Toast.LENGTH_SHORT).show()
            }
            false
        }
        if (!result) {
            Log.d("StartViewModel", "Stored user")
            return
        }
        withContext(Dispatchers.Main) {
            navigateToApp()
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