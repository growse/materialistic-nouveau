package com.growse.android.io.github.hidroh.materialistic.stories

import android.text.format.DateUtils
import com.growse.android.io.github.hidroh.materialistic.R
import com.growse.android.io.github.hidroh.materialistic.screens.StoryListScreen
import com.growse.android.io.github.hidroh.materialistic.testing.ActionBarMenu
import com.growse.android.io.github.hidroh.materialistic.testing.FakeAlgolia
import com.growse.android.io.github.hidroh.materialistic.testing.FakeBackendRule
import com.growse.android.io.github.hidroh.materialistic.testing.FakeHackerNews
import com.growse.android.io.github.hidroh.materialistic.testing.PreferencesRule
import com.growse.android.io.github.hidroh.materialistic.testing.ScenarioRule
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

/** The "Catch Up" list: stories ranked by popularity within a chosen time range. */
@HiltAndroidTest
class PopularTest : TestCase() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val backend = FakeBackendRule()

  @get:Rule(order = 2) val scenarios = ScenarioRule()

  @get:Rule(order = 3) val preferences = PreferencesRule(R.string.pref_popular_range)

  @Test
  fun defaultRangeIsTheLast24Hours() = run {
    serveStoriesByAge()
    step("Open Catch Up") { scenarios.launch(PopularActivity::class.java) }
    step("Verify it's showing the last 24 hours") {
      StoryListScreen {
        toolbar.hasSubtitle(device.targetContext.getString(R.string.popular_range_last_24h))
        storyTitle(TODAY).isVisible()
        storyTitle(THIS_WEEK).doesNotExist()
      }
    }
  }

  @Test
  fun choosingPastWeekShowsTheWeeksStories() = run {
    serveStoriesByAge()
    step("Open Catch Up") {
      scenarios.launch(PopularActivity::class.java)
      StoryListScreen { storyTitle(TODAY).isVisible() }
    }
    step("Choose the past week") {
      val context = device.targetContext
      ActionBarMenu.choose(
          R.id.menu_range,
          context.getString(R.string.range),
          context.getString(R.string.popular_range_past_week),
      )
    }
    step("Verify the week's stories replace the day's") {
      StoryListScreen {
        toolbar.hasSubtitle(device.targetContext.getString(R.string.popular_range_past_week))
        storyTitle(THIS_WEEK).isVisible()
        storyTitle(TODAY).doesNotExist()
      }
    }
  }

  /** Answers with [TODAY] for a range of up to a day, and [THIS_WEEK] for anything longer. */
  private fun serveStoriesByAge() {
    FakeAlgolia.search(FakeAlgolia.BY_RELEVANCE) { url ->
      val maxAge = FakeAlgolia.requestedMaxAgeSeconds(url) ?: return@search emptyList()
      if (maxAge <= DAY_SECONDS + SLACK_SECONDS) listOf(7001L) else listOf(7002L)
    }
    FakeHackerNews.story(7001, TODAY)
    FakeHackerNews.story(7002, THIS_WEEK)
  }

  private companion object {
    const val TODAY = "Popular today"
    const val THIS_WEEK = "Popular this week"
    const val DAY_SECONDS = DateUtils.DAY_IN_MILLIS / 1000

    /** Covers the time between the app computing the range and the fake reading it. */
    const val SLACK_SECONDS = 60
  }
}
