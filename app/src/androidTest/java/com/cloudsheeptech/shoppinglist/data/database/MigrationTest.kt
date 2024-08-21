package com.cloudsheeptech.shoppinglist.data.database

import android.provider.BaseColumns
import android.util.Log
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import okio.IOException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.OffsetDateTime

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ShoppingListDatabase::class.java
    )

    @Test
    @Throws(java.io.IOException::class)
    fun migrate18To21() {
        var db = helper.createDatabase(TEST_DB, 18).apply {
            // Insert data into the tables which changed:
            // list_table
            // items
            // user
            execSQL("INSERT INTO list_table (ID, Name, CreatedBy, CreatedByName, LastEdited) VALUES ('1', 'first list', '1', 'username', '1996-12-19T16:39:57-02:00')")
            execSQL("INSERT INTO list_table (ID, Name, CreatedBy, CreatedByName, LastEdited) VALUES ('2', 'second list', '1', 'username', '1996-12-19T16:39:57-02:00')")

            execSQL("INSERT INTO items (ID, Name, Icon) VALUES ('1', 'first item', 'icon image')")
            execSQL("INSERT INTO items (ID, Name, Icon) VALUES ('2', 'second item', 'icon image')")

            execSQL("INSERT INTO user (ID, UserId, Username, Password) VALUES ('1', '87654', 'only user', 'secure password')")

            close()
        }

        db = helper.runMigrationsAndValidate(TEST_DB, 21, true)
        val cursor = db.query("SELECT * FROM list_table")
        val listIds = mutableListOf<Long>()
        with(cursor) {
            while (moveToNext()) {
                val list_id = getLong(getColumnIndexOrThrow("listId"))
                listIds.add(list_id)
                val date = getString(getColumnIndexOrThrow("lastUpdated"))
                OffsetDateTime.parse(date)
            }
        }
        assert(2 == listIds.size)
        assert(1L == listIds[0])
        assert(2L == listIds[1])
        cursor.close()
    }

}