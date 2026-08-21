package com.retailpos.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class RetailDatabaseSmokeTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun closeDatabase() {
        RetailDatabase.closeForRestore()
    }

    @Test
    fun opensAtSchemaTwentyFiveAndSQLiteIsHealthy() {
        val database = RetailDatabase.get(context)
        val cursor = database.openHelper.readableDatabase.query("PRAGMA user_version")
        val version = cursor.use {
            assertTrue(it.moveToFirst())
            it.getInt(0)
        }
        assertEquals(25, version)

        val checkCursor = database.openHelper.readableDatabase.query("PRAGMA quick_check")
        val result = checkCursor.use {
            assertTrue(it.moveToFirst())
            it.getString(0)
        }
        assertEquals("ok", result.lowercase())
    }

    @Test
    fun applicationDatabaseFileCanBeCreatedAndClosed() {
        val database = RetailDatabase.get(context)
        assertTrue(database.openHelper.writableDatabase.isOpen)
        val databaseFile = context.getDatabasePath("retailpos.db")
        assertTrue(databaseFile.exists() || File(databaseFile.parentFile, databaseFile.name).exists())
    }
}
