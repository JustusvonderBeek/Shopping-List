package com.cloudsheeptech.shoppinglist.start

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.data.User
import com.cloudsheeptech.shoppinglist.network.Networking
import com.cloudsheeptech.shoppinglist.network.Util
import io.ktor.client.call.body
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

class StartViewModel(application: Application) : AndroidViewModel(application) {

    private val job = Job()
    private val recapScope = CoroutineScope(Dispatchers.IO + job)

    val inputText = MutableLiveData<String>()

    private val _navigateToApp = MutableLiveData<Boolean>(false)
    val navigateToApp : LiveData<Boolean> get() = _navigateToApp

    private val _hideKeyboard = MutableLiveData<Boolean>(false)
    val hideKeyboard : LiveData<Boolean> get() = _hideKeyboard

    val jsonSerializer = Json {
        ignoreUnknownKeys = false
        encodeDefaults = true
    }

    fun pushUsername() {
        hideKeyboard()
        if (inputText.value == null)
            return
        if (inputText.value!!.isEmpty())
            return
        val user = User(0, inputText.value!!, -1)
        val serialized = jsonSerializer.encodeToString(user)
        recapScope.launch {
            Networking.POST("user", serialized) { response ->
                Log.i("StartViewModel", "Handling response")
                if (response.status != HttpStatusCode.Accepted) {
                    Log.w("StartViewModel", "Failed to write username to server")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "Failed to make POST request", Toast.LENGTH_SHORT).show()
                    }
                    return@POST
                }
                storeUser(response)
            }
        }
    }

    private suspend fun storeUser(response: HttpResponse) {
        val result = withContext(Dispatchers.IO) {
            try {
                // Store username and ID to disk
                val body = response.bodyAsText(Charsets.UTF_8)
                val decodedUser = Json.decodeFromString<User>(body)
                if (decodedUser.ID == 0L || decodedUser.FavouriteRecipe != -1L) {
                    Log.w("StartViewModel", "Given user is not correctly initialized!")
                    return@withContext false
                }
                val file = File(getApplication<Application>().filesDir, "username.json")
                val writer = file.writer(Charsets.UTF_8)
                writer.write(body)
                writer.close()
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
        navigateToApp()
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