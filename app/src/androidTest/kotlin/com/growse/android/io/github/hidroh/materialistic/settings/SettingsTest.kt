package com.growse.android.io.github.hidroh.materialistic.settings

import androidx.test.espresso.Espresso.pressBack
import com.growse.android.io.github.hidroh.materialistic.ItemActivity
import com.growse.android.io.github.hidroh.materialistic.R
import com.growse.android.io.github.hidroh.materialistic.screens.ItemScreen
import com.growse.android.io.github.hidroh.materialistic.screens.PreferencesScreen
import com.growse.android.io.github.hidroh.materialistic.screens.SettingsScreen
import com.growse.android.io.github.hidroh.materialistic.testing.FakeBackendRule
import com.growse.android.io.github.hidroh.materialistic.testing.FakeHackerNews
import com.growse.android.io.github.hidroh.materialistic.testing.PreferencesRule
import com.growse.android.io.github.hidroh.materialistic.testing.ScenarioRule
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

/** Settings changed through the UI take effect where they're used. */
@HiltAndroidTest
class SettingsTest : TestCase() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val backend = FakeBackendRule()

  @get:Rule(order = 2) val scenarios = ScenarioRule()

  @get:Rule(order = 3)
  val preferences = PreferencesRule(R.string.pref_story_display, R.string.pref_comment_display)

  private val context
    get() = device.targetContext

  @Test
  fun defaultOpenCommentsOpensStoriesOnTheirComments() = run {
    step("Open the list display settings") {
      scenarios.launch(SettingsActivity::class.java)
      SettingsScreen { listOptions.click() }
    }
    step("Set stories to open on their comments") {
      PreferencesScreen {
        row(context.getString(R.string.pref_story_display_title)).click()
        dropDownOption(context.getString(R.string.story_display_comments)).click()
      }
    }
    serveStoryWithComment()
    step("Open the story from a link") { openStory() }
    step("Verify its comment is shown without switching tabs") {
      ItemScreen { comment(COMMENT).isVisible() }
    }
  }

  @Test
  fun threadSettingChangesHowRepliesAreShown() = run {
    step("Open the comment settings") {
      scenarios.launch(SettingsActivity::class.java)
      SettingsScreen { commentOptions.click() }
    }
    step("Show threads collapsed") {
      PreferencesScreen {
        row(context.getString(R.string.pref_comment_display_title)).click()
        dropDownOption(context.getString(R.string.comment_display_collapsed)).click()
      }
      pressBack()
    }
    serveStoryWithComment(reply = REPLY)
    step("Open the story's comments") {
      openStory()
      ItemScreen { showComments() }
    }
    step("Verify the reply is hidden until the comment is expanded") {
      ItemScreen {
        comment(COMMENT).isVisible()
        comment(REPLY).doesNotExist()
      }
    }
  }

  private fun TestContext<Unit>.serveStoryWithComment(reply: String? = null) =
      step("Serve a story with a comment${if (reply != null) " and a reply" else ""}") {
        FakeHackerNews.story(1001, "Story", kids = listOf(2001))
        FakeHackerNews.comment(
            2001,
            parent = 1001,
            text = COMMENT,
            kids = if (reply != null) listOf(3001) else emptyList(),
        )
        if (reply != null) FakeHackerNews.comment(3001, parent = 2001, text = reply)
      }

  private fun openStory() =
      scenarios.launchLink(ItemActivity::class.java, "https://news.ycombinator.com/item?id=1001")

  private companion object {
    const val COMMENT = "A comment"
    const val REPLY = "A reply"
  }
}
