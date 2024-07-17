package com.cloudsheeptech.shoppinglist.handling

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cloudsheeptech.shoppinglist.data.ShoppingList
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.handling.DatabaseListHandler
import com.cloudsheeptech.shoppinglist.data.handling.OnlineListHandler
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import java.time.OffsetDateTime

@RunWith(AndroidJUnit4::class)
class OnlineListHandlerTest {

    private fun createDefaultList() : ShoppingList {
        val list = ShoppingList(
            ID = 0,
            Name = "Default List",
            CreatedBy = 1234,
            CreatedByName = "Default User",
            LastEdited = OffsetDateTime.now(),
        )
        return list
    }

    @Test
    fun testListInsert() = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)
        val onlineHandler = OnlineListHandler()

        val list = createDefaultList()
        val insertId = onlineHandler.storeShoppingList(list)
//        Log.d("DatabaseListHandlerTest", "List: $list")


    }

}