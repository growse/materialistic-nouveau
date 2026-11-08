package com.growse.android.io.github.hidroh.materialistic

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.growse.android.io.github.hidroh.materialistic.data.HackerNewsItem
import com.growse.android.io.github.hidroh.materialistic.data.MaterialisticDatabase
import com.growse.android.io.github.hidroh.materialistic.screens.ItemScreen
import com.growse.android.io.github.hidroh.materialistic.screens.SavedStoriesScreen
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import org.junit.After
import org.junit.Before
import org.junit.Test

class SavedStoriesTest : TestCase() {
  private val context = ApplicationProvider.getApplicationContext<Context>()

  @Before
  @After
  fun removeSavedStory() {
    MaterialisticDatabase.getInstance(context).savedStoriesDao.deleteByItemId(STORY_ID.toString())
  }

  @Test
  fun savedStoryIsListedInSavedStories() = run {
    step("Open a story and tap its bookmark") {
      ActivityScenario.launch<ItemActivity>(
              Intent(context, ItemActivity::class.java).putExtra(ItemActivity.EXTRA_ITEM, story()),
          )
          .use { ItemScreen { bookmark.click() } }
    }
    step("Verify the story is listed in saved stories") {
      ActivityScenario.launch(FavoriteActivity::class.java).use {
        SavedStoriesScreen { storyTitle(STORY_TITLE).isVisible() }
      }
    }
  }

  // Gson is how real stories get built, as HackerNewsItem exposes no way to set a title
  private fun story() =
      Gson()
          .fromJson(
              """{"id":$STORY_ID,"type":"story","title":"$STORY_TITLE"}""",
              HackerNewsItem::class.java,
          )

  private companion object {
    const val STORY_ID = 999999999L
    const val STORY_TITLE = "A story worth saving"
  }
}
