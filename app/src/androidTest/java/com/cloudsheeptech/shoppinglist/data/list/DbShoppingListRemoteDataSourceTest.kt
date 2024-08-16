package com.cloudsheeptech.shoppinglist.data.list

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import java.time.OffsetDateTime

@RunWith(AndroidJUnit4::class)
class DbShoppingListRemoteDataSourceTest {

    private fun createDefaultList() : DbShoppingList {
        val list = DbShoppingList(
            listId = 0,
            title = "Default List",
            createdBy = 1234,
            createdByName = "Default User",
            lastUpdated = OffsetDateTime.now(),
        )
        return list
    }

    @Test
    fun testListInsert() = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = ShoppingListDatabase.getInstance(application)
//        val onlineHandler = ShoppingListRemoteDataSource(database)
//
////        val user = AppUser.storeUser(application.applicationContext)
//
//        val list = createDefaultList()
//        val insertId = onlineHandler.storeShoppingList(list)
////        Log.d("DatabaseListHandlerTest", "List: $list")
//        assert(insertId != 0L)

    }

}