package com.growse.android.io.github.hidroh.materialistic

import com.growse.android.io.github.hidroh.materialistic.screens.ItemScreen
import com.growse.android.io.github.hidroh.materialistic.screens.LoginScreen
import com.growse.android.io.github.hidroh.materialistic.testing.AccountsRule
import com.growse.android.io.github.hidroh.materialistic.testing.AccountsRule.Companion.PASSWORD
import com.growse.android.io.github.hidroh.materialistic.testing.AccountsRule.Companion.USERNAME
import com.growse.android.io.github.hidroh.materialistic.testing.FakeBackend
import com.growse.android.io.github.hidroh.materialistic.testing.FakeBackendRule
import com.growse.android.io.github.hidroh.materialistic.testing.FakeHackerNews
import com.growse.android.io.github.hidroh.materialistic.testing.FakeHackerNewsSite
import com.growse.android.io.github.hidroh.materialistic.testing.ScenarioRule
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Upvoting a story from its page. */
@HiltAndroidTest
class VotingTest : TestCase() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val backend = FakeBackendRule()

  @get:Rule(order = 2) val accounts = AccountsRule()

  @get:Rule(order = 3) val scenarios = ScenarioRule()

  @Test
  fun votingSendsAnUpvoteForTheStory() = run {
    step("Sign in, and HN accepts the vote") {
      accounts.signIn()
      FakeHackerNewsSite.voteSucceeds()
    }
    openStory()
    step("Vote") { ItemScreen { voteButton.click() } }
    step("Verify an upvote for the story was sent as the signed-in user") {
      flakySafely {
        val form = FakeBackend.postsTo("/vote").single().form
        assertEquals(STORY_ID.toString(), form["id"])
        assertEquals("up", form["how"])
        assertEquals(USERNAME, form["acct"])
        assertEquals(PASSWORD, form["pw"])
      }
    }
  }

  @Test
  fun votingSignedOutAsksToLogIn() = run {
    openStory()
    step("Vote without being signed in") { ItemScreen { voteButton.click() } }
    step("Verify login is offered and no vote was sent") {
      LoginScreen { loginButton.isVisible() }
      assertTrue(FakeBackend.postsTo("/vote").isEmpty())
    }
  }

  private fun openStory() {
    FakeHackerNews.story(STORY_ID, "Story worth a vote")
    scenarios.launchLink(ItemActivity::class.java, "https://news.ycombinator.com/item?id=$STORY_ID")
    ItemScreen { voteButton.isVisible() }
  }

  private companion object {
    const val STORY_ID = 1001L
  }
}
