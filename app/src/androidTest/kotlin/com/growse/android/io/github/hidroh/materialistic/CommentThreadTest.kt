package com.growse.android.io.github.hidroh.materialistic

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.growse.android.io.github.hidroh.materialistic.screens.ItemScreen
import com.growse.android.io.github.hidroh.materialistic.testing.FakeBackendRule
import com.growse.android.io.github.hidroh.materialistic.testing.FakeHackerNews
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class CommentThreadTest : TestCase() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val backend = FakeBackendRule()

  private var scenario: ActivityScenario<*>? = null

  @After
  fun closeActivity() {
    scenario?.close()
  }

  @Test
  fun topLevelCommentsAreShown() = run {
    step("Serve a story with two comments") {
      FakeHackerNews.story(STORY_ID, "Story", kids = listOf(2001, 2002))
      FakeHackerNews.comment(2001, parent = STORY_ID, text = "First comment")
      FakeHackerNews.comment(2002, parent = STORY_ID, text = "Second comment")
    }
    step("Open the story from a link") { openStory() }
    step("Switch to the comments tab") { ItemScreen { tabs.selectTab(0) } }
    step("Verify both comments are shown") {
      ItemScreen {
        comment("First comment").isVisible()
        comment("Second comment").isVisible()
      }
    }
  }

  @Test
  fun repliesAreShown() = run {
    step("Serve a story with a comment and a reply to it") {
      FakeHackerNews.story(STORY_ID, "Story", kids = listOf(2001))
      FakeHackerNews.comment(2001, parent = STORY_ID, text = "Parent comment", kids = listOf(3001))
      FakeHackerNews.comment(3001, parent = 2001, text = "A reply")
    }
    step("Open the story from a link") { openStory() }
    step("Switch to the comments tab") { ItemScreen { tabs.selectTab(0) } }
    step("Verify the comment and its reply are shown") {
      ItemScreen {
        comment("Parent comment").isVisible()
        comment("A reply").isVisible()
      }
    }
  }

  /** Opens [STORY_ID] the way a news.ycombinator.com link does, so it's fetched by id. */
  private fun openStory() {
    scenario =
        ActivityScenario.launch<ItemActivity>(
            Intent(ApplicationProvider.getApplicationContext(), ItemActivity::class.java)
                .setAction(Intent.ACTION_VIEW)
                .setData(Uri.parse("https://news.ycombinator.com/item?id=$STORY_ID")),
        )
  }

  private companion object {
    const val STORY_ID = 1001L
  }
}
