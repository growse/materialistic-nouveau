package com.growse.android.io.github.hidroh.materialistic.item

import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.pressBack
import com.growse.android.io.github.hidroh.materialistic.screens.ComposeScreen
import com.growse.android.io.github.hidroh.materialistic.screens.LoginScreen
import com.growse.android.io.github.hidroh.materialistic.settings.Preferences
import com.growse.android.io.github.hidroh.materialistic.testing.AccountsRule
import com.growse.android.io.github.hidroh.materialistic.testing.AccountsRule.Companion.PASSWORD
import com.growse.android.io.github.hidroh.materialistic.testing.AccountsRule.Companion.USERNAME
import com.growse.android.io.github.hidroh.materialistic.testing.FakeBackend
import com.growse.android.io.github.hidroh.materialistic.testing.FakeBackendRule
import com.growse.android.io.github.hidroh.materialistic.testing.FakeHackerNewsSite
import com.growse.android.io.github.hidroh.materialistic.testing.ScenarioRule
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Replying to a story or comment. */
@HiltAndroidTest
class ComposeTest : TestCase() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val backend = FakeBackendRule()

  @get:Rule(order = 2) val accounts = AccountsRule()

  @get:Rule(order = 3) val scenarios = ScenarioRule()

  private val context
    get() = device.targetContext

  @Before
  @After
  fun deleteDraft() {
    Preferences.deleteDraft(context, PARENT_ID)
  }

  @Test
  fun aReplyIsPostedToItsParentAndCloses() = run {
    step("Sign in, and HN accepts the reply") {
      accounts.signIn()
      FakeHackerNewsSite.replySucceeds()
    }
    val scenario = openCompose()
    step("Write and send a reply") {
      ComposeScreen { body.typeText("My reply") }
      closeSoftKeyboard()
      ComposeScreen { send.click() }
    }
    step("Verify it closes once sent") {
      flakySafely { assertEquals(Lifecycle.State.DESTROYED, scenario.state) }
    }
    step("Verify the reply went to its parent, as the signed-in user") {
      val form = FakeBackend.postsTo("/comment").single().form
      assertEquals(PARENT_ID, form["parent"])
      assertEquals("My reply", form["text"])
      assertEquals(USERNAME, form["acct"])
      assertEquals(PASSWORD, form["pw"])
    }
  }

  @Test
  fun anEmptyReplyIsNotSent() = run {
    step("Sign in") { accounts.signIn() }
    openCompose()
    step("Send without writing anything") { ComposeScreen { send.click() } }
    step("Verify nothing was sent") {
      ComposeScreen { body.isVisible() }
      assertTrue(FakeBackend.postsTo("/comment").isEmpty())
    }
  }

  @Test
  fun replyingSignedOutAsksToLogIn() = run {
    openCompose()
    step("Write and send a reply without being signed in") {
      ComposeScreen { body.typeText("My reply") }
      closeSoftKeyboard()
      ComposeScreen { send.click() }
    }
    step("Verify login is offered and nothing was sent") {
      LoginScreen { loginButton.isVisible() }
      assertTrue(FakeBackend.postsTo("/comment").isEmpty())
    }
  }

  @Test
  fun aReplyTurnedAwayAsksToLogInAgain() = run {
    step("Sign in, but HN turns the reply away") {
      accounts.signIn()
      FakeHackerNewsSite.replyRejected()
    }
    openCompose()
    step("Write and send a reply") {
      ComposeScreen { body.typeText("My reply") }
      closeSoftKeyboard()
      ComposeScreen { send.click() }
    }
    step("Verify it asks to log in again") { LoginScreen { loginButton.isVisible() } }
  }

  @Test
  fun quotingInsertsTheParentText() = run {
    step("Sign in") { accounts.signIn() }
    openCompose(parentText = "The point being replied to")
    step("Quote the parent") { ComposeScreen { quote.click() } }
    step("Verify the reply starts with the quoted text") {
      ComposeScreen { body.startsWithText("> The point being replied to") }
    }
  }

  @Test
  fun backingOutOfAnUnsavedReplyOffersToKeepItAsADraft() = run {
    step("Sign in") { accounts.signIn() }
    openCompose()
    step("Write a reply, then back out") {
      ComposeScreen { body.typeText("Unfinished thought") }
      closeSoftKeyboard()
      pressBack()
    }
    step("Keep it as a draft") { ComposeScreen { saveDraftPrompt.positiveButton.click() } }
    step("Verify the draft is kept, and comes back when replying again") {
      assertEquals("Unfinished thought", Preferences.getDraft(context, PARENT_ID))
      openCompose()
      ComposeScreen { body.hasText("Unfinished thought") }
    }
  }

  private fun openCompose(parentText: String? = null) =
      scenarios.launch<ComposeActivity>(
          Intent(context, ComposeActivity::class.java)
              .putExtra(ComposeActivity.EXTRA_PARENT_ID, PARENT_ID)
              .apply {
                if (parentText != null) putExtra(ComposeActivity.EXTRA_PARENT_TEXT, parentText)
              },
      )

  private companion object {
    const val PARENT_ID = "2001"
  }
}
