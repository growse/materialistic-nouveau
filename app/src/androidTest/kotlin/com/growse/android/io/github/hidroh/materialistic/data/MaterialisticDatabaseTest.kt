package com.growse.android.io.github.hidroh.materialistic.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.growse.android.io.github.hidroh.materialistic.data.MaterialisticDatabase.CachedItem
import com.growse.android.io.github.hidroh.materialistic.data.MaterialisticDatabase.DbConstants
import com.growse.android.io.github.hidroh.materialistic.data.MaterialisticDatabase.ReadStory
import com.growse.android.io.github.hidroh.materialistic.data.MaterialisticDatabase.Readable
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Opens a database as an existing install has it, so Room runs the real migrations and checks the
 * entities against the resulting tables, which fails on any column name or nullability drift.
 */
@RunWith(AndroidJUnit4::class)
class MaterialisticDatabaseTest {
  private val context = ApplicationProvider.getApplicationContext<Context>()

  @Before
  @After
  fun deleteDatabase() {
    context.deleteDatabase(DB_NAME)
  }

  @Test
  fun migratesFromVersion4AndKeepsSavedStories() {
    createVersion4Database()

    withDatabase { database ->
      val saved = database.savedStoriesDao.selectByItemId("1")
      assertNotNull(saved)
      assertEquals("https://a.b/", saved!!.url)
      assertEquals("Title", saved.title)
      assertEquals("123", saved.time)
    }
  }

  @Test
  fun cachedItemsUseTheMigratedColumnNames() {
    createVersion4Database()

    withDatabase { database ->
      database.itemCacheDao.insert(
          CachedItem("42").apply {
            title = "Cached"
            isDead = true
            isDeleted = true
            fetchedAt = 1000L
          }
      )

      val restored = database.itemCacheDao.selectByItemId("42")!!
      assertEquals("Cached", restored.title)
      assertTrue(restored.isDead)
      assertTrue(restored.isDeleted)
      assertEquals(1000L, restored.fetchedAt)
      database.openHelper.readableDatabase
          .query("SELECT dead, deleted FROM item_cache WHERE itemid = '42'")
          .use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
            assertEquals(1, cursor.getInt(1))
          }
    }
  }

  @Test
  fun readAndReadableStoriesRoundTrip() {
    createVersion4Database()

    withDatabase { database ->
      database.readStoriesDao.insert(ReadStory("7"))
      database.readableDao.insert(Readable("7", "<p>content</p>"))

      assertEquals("7", database.readStoriesDao.selectByItemId("7")?.itemId)
      assertEquals("<p>content</p>", database.readableDao.selectByItemId("7")?.content)
    }
  }

  private fun createVersion4Database() {
    context.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null).use { db ->
      db.execSQL(DbConstants.SQL_CREATE_SAVED_TABLE)
      db.execSQL(DbConstants.SQL_CREATE_READ_TABLE)
      db.execSQL(DbConstants.SQL_CREATE_READABLE_TABLE)
      db.execSQL(
          "INSERT INTO saved (itemid, url, title, time) VALUES ('1', 'https://a.b/', 'Title', '123')"
      )
      db.version = 4
    }
  }

  private fun withDatabase(block: (MaterialisticDatabase) -> Unit) {
    val database =
        MaterialisticDatabase.setupBuilder(
                Room.databaseBuilder(context, MaterialisticDatabase::class.java, DB_NAME)
            )
            .allowMainThreadQueries()
            .build()
    try {
      block(database)
    } finally {
      database.close()
    }
  }

  private companion object {
    const val DB_NAME = "materialistic-migration-test.db"
  }
}
