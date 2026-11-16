package com.growse.android.io.github.hidroh.materialistic.search

import android.app.SearchManager
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.growse.android.io.github.hidroh.materialistic.R
import com.growse.android.io.github.hidroh.materialistic.data.AlgoliaClient
import com.growse.android.io.github.hidroh.materialistic.screens.StoryListScreen
import com.growse.android.io.github.hidroh.materialistic.testing.ActionBarMenu
import com.growse.android.io.github.hidroh.materialistic.testing.FakeAlgolia
import com.growse.android.io.github.hidroh.materialistic.testing.FakeBackend
import com.growse.android.io.github.hidroh.materialistic.testing.FakeBackendRule
import com.growse.android.io.github.hidroh.materialistic.testing.FakeHackerNews
import com.growse.android.io.github.hidroh.materialistic.testing.PreferencesRule
import com.growse.android.io.github.hidroh.materialistic.testing.ScenarioRule
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class SearchTest : TestCase() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val backend = FakeBackendRule()

  @get:Rule(order = 2) val scenarios = ScenarioRule()

  @get:Rule(order = 3) val preferences = PreferencesRule(R.string.pref_search_sort)

  /** The sort is read into a static at startup, so reset it alongside the preference. */
  @Before
  @After
  fun sortByRecent() {
    AlgoliaClient.sSortByTime = true
  }

  @Test
  fun matchingStoriesAreListed() = run {
    step("Serve two stories matching \"$QUERY\" and none for anything else") {
      FakeAlgolia.search(FakeAlgolia.BY_DATE) { url ->
        if (url.queryParameter("query") == QUERY) listOf(6001, 6002) else emptyList()
      }
      FakeHackerNews.story(6001, "First match")
      FakeHackerNews.story(6002, "Second match")
    }
    step("Search for \"$QUERY\"") { search(QUERY) }
    step("Verify the query is shown and both matches are listed") {
      StoryListScreen {
        toolbar.hasSubtitle(QUERY)
        storyTitle("First match").isVisible()
        storyTitle("Second match").isVisible()
      }
    }
  }

  @Test
  fun sortingByPopularitySearchesByRelevance() = run {
    step("Serve a different story for each sort") {
      FakeAlgolia.search(FakeAlgolia.BY_DATE, 6001)
      FakeAlgolia.search(FakeAlgolia.BY_RELEVANCE, 6002)
      FakeHackerNews.story(6001, "Most recent match")
      FakeHackerNews.story(6002, "Most popular match")
    }
    step("Search, which sorts by most recent first") {
      search(QUERY)
      StoryListScreen { storyTitle("Most recent match").isVisible() }
    }
    step("Sort by popularity") {
      val context = device.targetContext
      ActionBarMenu.choose(
          R.id.menu_sort,
          context.getString(R.string.pref_search_sort_title),
          context.getString(R.string.search_sort_popularity),
      )
    }
    step("Verify the relevance results replace the recent ones") {
      StoryListScreen {
        storyTitle("Most popular match").isVisible()
        storyTitle("Most recent match").doesNotExist()
      }
    }
    step("Verify the relevance search was for the same query") {
      val relevance = FakeBackend.requests.last { it.encodedPath.endsWith("/search") }
      assertEquals(QUERY, relevance.queryParameter("query"))
    }
  }

  /** Delivers [query] the way the toolbar's search view does. */
  private fun search(query: String) =
      scenarios.launch<SearchActivity>(
          Intent(ApplicationProvider.getApplicationContext(), SearchActivity::class.java)
              .setAction(Intent.ACTION_SEARCH)
              .putExtra(SearchManager.QUERY, query),
      )

  private companion object {
    const val QUERY = "kotlin"
  }
}
