package com.growse.android.io.github.hidroh.materialistic.stories

import android.app.Activity
import com.growse.android.io.github.hidroh.materialistic.NewActivity
import com.growse.android.io.github.hidroh.materialistic.R
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

/** Each story list besides top stories reads its own feed from the HN API. */
@HiltAndroidTest
class StoryFeedsTest : TestCase() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val backend = FakeBackendRule()

  @get:Rule(order = 2) val scenarios = ScenarioRule()

  @Test
  fun newStoriesListsTheNewFeed() =
      checkFeed(NewActivity::class.java, Feed.New, R.string.title_activity_new)

  @Test
  fun bestStoriesListsTheBestFeed() =
      checkFeed(BestActivity::class.java, Feed.Best, R.string.title_activity_best)

  @Test
  fun showHnListsTheShowFeed() =
      checkFeed(ShowActivity::class.java, Feed.Show, R.string.title_activity_show)

  @Test
  fun askHnListsTheAskFeed() =
      checkFeed(AskActivity::class.java, Feed.Ask, R.string.title_activity_ask)

  @Test
  fun jobsListsTheJobsFeed() =
      checkFeed(JobsActivity::class.java, Feed.Jobs, R.string.title_activity_jobs)

  /**
   * Serves a story only on [feed], then checks [activity] is titled [title] and lists it. Top
   * stories are left unserved, so a list reading the wrong feed shows nothing.
   */
  private fun checkFeed(activity: Class<out Activity>, feed: Feed, title: Int) = run {
    val storyTitle = "Only on ${feed.name}"
    step("Serve a story on the ${feed.name} feed") {
      FakeHackerNews.feed(feed, 4001)
      FakeHackerNews.story(4001, storyTitle)
    }
    step("Open ${activity.simpleName}") { scenarios.launch(activity) }
    step("Verify it's titled for its feed and lists the story") {
      StoryListScreen {
        toolbar.hasTitle(device.targetContext.getString(title))
        storyTitle(storyTitle).isVisible()
      }
    }
  }
}
