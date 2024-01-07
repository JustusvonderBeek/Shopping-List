package com.cloudsheeptech.shoppinglist.create.list

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.data.AppUser
import com.cloudsheeptech.shoppinglist.data.ShoppingList
import com.cloudsheeptech.shoppinglist.data.ShoppingListWire
import com.cloudsheeptech.shoppinglist.data.User
import com.cloudsheeptech.shoppinglist.database.ShoppingListDatabase
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
import java.time.Instant
import java.time.format.DateTimeFormatter

class CreateShoppinglistViewModel(application: Application) : AndroidViewModel(application) {

    private val job = Job()
    private val createSLCoroutine = CoroutineScope(Dispatchers.Main + job)

    val title = MutableLiveData<String>("")

    private val _navigateBack = MutableLiveData<Boolean>(false)
    val navigateBack : LiveData<Boolean> get() = _navigateBack

    private val _navigateToCreatedList = MutableLiveData<Long>(-1)
    val navigateToCreatedList : LiveData<Long> get() = _navigateToCreatedList

    private val database = ShoppingListDatabase.getInstance(application.applicationContext)
    private val shoppingListDao = database.shoppingListDao()
    private val userDao = database.userDao()

    private val user = userDao.getUserLive()

//    init {
//        if (user.value == null || user.value!!.ID == 0L) {
//            Log.w("CreateShoppinglistViewModel", "User not correctly initialized")
//        }
//    }

    fun create() {
        Log.d("CreateShoppinglistViewModel", "Creating list pressed")
        if (title.value == null || title.value!!.isEmpty()) {
            return
        }
        if (!AppUser.Initialized()) {   // SHOULD never happen
            Log.w("CreateShoppinglistViewModel", "Because the user is not correctly initialized we cannot create this list!")
            return
        }
        // Let the server assign the ID
//        Log.d("CreateShoppinglistViewModel", "Instant Now: ${Instant.now()}")
        val nowFormatted = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        val newShoppingList = ShoppingList(ID=0, Name = title.value!!, CreatedBy = AppUser.getUser(), nowFormatted)
        createSLCoroutine.launch {
            val updatedIdList = storeShoppingListOnline(newShoppingList)
            storeShoppingListDatabase(updatedIdList)
        }
    }

    private suspend fun storeShoppingListDatabase(list : ShoppingList) {
        withContext(Dispatchers.IO) {
            if (list.ID == 0L) {
                Log.w("CreateShoppinglistViewModel", "Cannot create list because the ID == 0")
                return@withContext
            }
            shoppingListDao.insertList(list)
            Log.d("CreateShoppingListViewModel", "Stored list to database")
        }
        withContext(Dispatchers.Main) {
            navigateBack()
        }
    }

    private suspend fun storeShoppingListOnline(list: ShoppingList): ShoppingList {
        val updatedList = withContext(Dispatchers.IO) {
            val wireList = ShoppingListWire(list.ID, list.Name, AppUser.ID, list.LastEdited)
            val serialized = Json.encodeToString(wireList)
            var decodedList: ShoppingListWire? = null
            Networking.POST("v1/list", serialized) { resp ->
                if (resp.status != HttpStatusCode.Created) {
                    Log.w(
                        "CreateShoppinglistViewModel",
                        "Creation of list at server was not successful"
                    )
                    return@POST
                }
                val body = resp.bodyAsText(Charsets.UTF_8)
                Log.d("CreateShoppinglistViewModel", "Got answer: $body")
                decodedList = Json.decodeFromString<ShoppingListWire>(body)
            }
            return@withContext decodedList
        } ?: return list
        return ShoppingList(updatedList.ID, list.Name, list.CreatedBy, list.LastEdited)
    }

    fun navigateBack() {
        _navigateBack.value = true
    }

    fun onBackNavigated() {
        _navigateBack.value = false
    }

    fun navigateCreatedList() {
        _navigateToCreatedList.value = 1
    }

    fun onCreatedListNavigated() {
        _navigateToCreatedList.value = -1
    }
}