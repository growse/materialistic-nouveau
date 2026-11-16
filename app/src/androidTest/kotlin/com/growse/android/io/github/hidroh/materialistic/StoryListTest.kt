package com.growse.android.io.github.hidroh.materialistic

import com.growse.android.io.github.hidroh.materialistic.screens.ItemScreen
import com.growse.android.io.github.hidroh.materialistic.screens.StoryListScreen
import com.growse.android.io.github.hidroh.materialistic.testing.FakeBackendRule
import com.growse.android.io.github.hidroh.materialistic.testing.FakeHackerNews
import com.growse.android.io.github.hidroh.materialistic.testing.FakeHackerNews.Feed
import com.growse.android.io.github.hidroh.materialistic.testing.ScenarioRule
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class StoryListTest : TestCase() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val backend = FakeBackendRule()

  @get:Rule(order = 2) val scenarios = ScenarioRule()

  @Test
  fun topStoriesAreListed() = run {
    step("Serve three top stories") {
      FakeHackerNews.topStories(1001, 1002, 1003)
      FakeHackerNews.story(1001, "First story")
      FakeHackerNews.story(1002, "Second story")
      FakeHackerNews.story(1003, "Third story")
    }
    step("Open the top stories list") { scenarios.launch(ListActivity::class.java) }
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
    step("Open the top stories list") { scenarios.launch(ListActivity::class.java) }
    step("Tap the story's comment count") {
      StoryListScreen { openComments("Story with a comment") }
    }
    step("Verify its comment is shown") { ItemScreen { comment("The only comment").isVisible() } }
  }

  @Test
  fun failedFetchShowsConnectionError() = run {
    step("Make the top stories request fail") { FakeHackerNews.topStoriesFail() }
    step("Open the top stories list") { scenarios.launch(ListActivity::class.java) }
    step("Verify the connection error is shown") { StoryListScreen { connectionError.isVisible() } }
  }

  @Test
  fun pullingToRefreshShowsNewStories() = run {
    step("Serve one top story and open the list") {
      FakeHackerNews.topStories(1001)
      FakeHackerNews.story(1001, "Older story")
      scenarios.launch(ListActivity::class.java)
      StoryListScreen { storyTitle("Older story").isVisible() }
    }
    step("A new story reaches the top") {
      FakeHackerNews.topStories(1002, 1001)
      FakeHackerNews.story(1002, "Newer story")
    }
    step("Pull to refresh") { StoryListScreen { refreshLayout.swipeDown() } }
    step("Verify both stories are listed") {
      StoryListScreen {
        storyTitle("Newer story").isVisible()
        storyTitle("Older story").isVisible()
      }
    }
  }

  @Test
  fun failedRefreshKeepsTheStoriesAlreadyShown() = run {
    step("Serve one top story and open the list") {
      FakeHackerNews.topStories(1001)
      FakeHackerNews.story(1001, "Story already shown")
      scenarios.launch(ListActivity::class.java)
      StoryListScreen { storyTitle("Story already shown").isVisible() }
    }
    step("Make the next top stories request fail") { FakeHackerNews.feedFails(Feed.Top) }
    step("Pull to refresh") { StoryListScreen { refreshLayout.swipeDown() } }
    step("Verify the story is still listed, with no connection error in its place") {
      StoryListScreen {
        storyTitle("Story already shown").isVisible()
        connectionError.isNotDisplayed()
      }
    }
  }
}
