package com.growse.android.io.github.hidroh.materialistic

import androidx.test.core.app.ActivityScenario
import com.growse.android.io.github.hidroh.materialistic.screens.ItemScreen
import com.growse.android.io.github.hidroh.materialistic.screens.StoryListScreen
import com.growse.android.io.github.hidroh.materialistic.testing.FakeBackendRule
import com.growse.android.io.github.hidroh.materialistic.testing.FakeHackerNews
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class StoryListTest : TestCase() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val backend = FakeBackendRule()

  private var scenario: ActivityScenario<*>? = null

  @After
  fun closeActivity() {
    scenario?.close()
  }

  @Test
  fun topStoriesAreListed() = run {
    step("Serve three top stories") {
      FakeHackerNews.topStories(1001, 1002, 1003)
      FakeHackerNews.story(1001, "First story")
      FakeHackerNews.story(1002, "Second story")
      FakeHackerNews.story(1003, "Third story")
    }
    step("Open the top stories list") {
      scenario = ActivityScenario.launch(ListActivity::class.java)
    }
    step("Verify each story's title is listed") {
      StoryListScreen {
        storyTitle("First story").isVisible()
        storyTitle("Second story").isVisible()
        storyTitle("Third story").isVisible()
      }
    }
  }

  @Test
  fun tappingCommentCountOpensComments() = run {
    step("Serve a top story with one comment") {
      FakeHackerNews.topStories(1001)
      FakeHackerNews.story(1001, "Story with a comment", kids = listOf(2001))
      FakeHackerNews.comment(2001, parent = 1001, text = "The only comment")
    }
    step("Open the top stories list") {
      scenario = ActivityScenario.launch(ListActivity::class.java)
    }
    step("Tap the story's comment count") {
      StoryListScreen { commentsButton("Story with a comment").click() }
    }
    step("Verify its comment is shown") { ItemScreen { comment("The only comment").isVisible() } }
  }

  @Test
  fun failedFetchShowsConnectionError() = run {
    step("Make the top stories request fail") { FakeHackerNews.topStoriesFail() }
    step("Open the top stories list") {
      scenario = ActivityScenario.launch(ListActivity::class.java)
    }
    step("Verify the connection error is shown") { StoryListScreen { connectionError.isVisible() } }
  }
}
