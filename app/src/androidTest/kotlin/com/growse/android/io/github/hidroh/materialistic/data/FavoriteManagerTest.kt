package com.growse.android.io.github.hidroh.materialistic.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.growse.android.io.github.hidroh.materialistic.data.MaterialisticDatabase.SavedStory
import com.growse.android.io.github.hidroh.materialistic.data.android.Cache
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

@RunWith(AndroidJUnit4::class)
class FavoriteManagerTest {
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private lateinit var database: MaterialisticDatabase
  private lateinit var manager: FavoriteManager

  @Before
  fun setUp() {
    database =
        MaterialisticDatabase.setupBuilder(
                Room.inMemoryDatabaseBuilder(context, MaterialisticDatabase::class.java),
            )
            .build()
    manager =
        FavoriteManager(
            Cache(
                database,
                database.savedStoriesDao,
                database.readStoriesDao,
                database.readableDao,
                database.itemCacheDao,
                AndroidSchedulers.mainThread(),
            ),
            Schedulers.io(),
            database.savedStoriesDao,
            database.openHelper,
        )
  }

  @After
  fun tearDown() {
    manager.detach()
    database.close()
  }

  @Test
  fun sizeIsZeroBeforeAttaching() {
    database.savedStoriesDao.insert(SavedStory("1", "https://a.b/", "Title", "123"))

    assertEquals(0, manager.size)
  }

  @Test
  fun sizeAndItemsReflectSavedStoriesOnceLoaded() {
    database.savedStoriesDao.insert(
        SavedStory("1", "https://a.b/", "Older", "100"),
        SavedStory("2", "https://c.d/", "Newer", "200"),
    )

    attachAndAwaitLoad()

    assertEquals(2, manager.size)
    assertEquals("2", manager.getItem(0)?.id)
    assertEquals("1", manager.getItem(1)?.id)
  }

  @Test
  fun sizeIsZeroAfterDetaching() {
    database.savedStoriesDao.insert(SavedStory("1", "https://a.b/", "Title", "123"))
    attachAndAwaitLoad()

    manager.detach()

    assertEquals(0, manager.size)
  }

  private fun attachAndAwaitLoad() {
    val loaded = CountDownLatch(1)
    manager.attach(
        object : LocalItemManager.Observer {
          override fun onChanged() {
            loaded.countDown()
          }
        },
        "",
    )
    assertTrue("timed out loading saved stories", loaded.await(5, TimeUnit.SECONDS))
  }
}
