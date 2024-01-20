package com.cloudsheeptech.shoppinglist

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cloudsheeptech.shoppinglist.create.list.CreateShoppinglistViewModel
import com.cloudsheeptech.shoppinglist.create.list.CreateShoppinglistViewModelFactory
import com.cloudsheeptech.shoppinglist.data.AppUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ListOperationTests {

    private suspend fun createList() : Boolean {
        val listTitle = "New List"
        val application = ApplicationProvider.getApplicationContext<Application>()
        AppUser.loadUser(application)
        val factory = CreateShoppinglistViewModelFactory(application)
        val store = ViewModelStore()
        val viewModel = ViewModelProvider(store, factory)[CreateShoppinglistViewModel::class.java]
        // Setting the required properties
        withContext(Dispatchers.Main) {
            viewModel.title.value = listTitle
        }
        viewModel.create()
        Thread.sleep(1000)
        return true
    }

    @Test
    fun testCreateList() = runTest {
        Log.i("ListOperationTest", "Testing creating new list")
        val success = createList()
        assert(success)
    }

    fun testShareList() {

    }

    fun testRemoveList() {

    }

    fun testLoadList() {

    }

    fun testAddItemToList() {
        // Dont require to push the item to the server first
        // Simply add the item via the list (self-explanatory)

    }

}