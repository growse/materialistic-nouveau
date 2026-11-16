package com.growse.android.io.github.hidroh.materialistic

import android.content.Intent
import androidx.test.espresso.Espresso.closeSoftKeyboard
import com.growse.android.io.github.hidroh.materialistic.screens.LoginScreen
import com.growse.android.io.github.hidroh.materialistic.screens.StoryListScreen
import com.growse.android.io.github.hidroh.materialistic.screens.SubmitScreen
import com.growse.android.io.github.hidroh.materialistic.testing.AccountsRule
import com.growse.android.io.github.hidroh.materialistic.testing.FakeBackend
import com.growse.android.io.github.hidroh.materialistic.testing.FakeBackendRule
import com.growse.android.io.github.hidroh.materialistic.testing.FakeHackerNews
import com.growse.android.io.github.hidroh.materialistic.testing.FakeHackerNews.Feed
import com.growse.android.io.github.hidroh.materialistic.testing.FakeHackerNewsSite
import com.growse.android.io.github.hidroh.materialistic.testing.ScenarioRule
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Submitting a link or a question to HN. */
@HiltAndroidTest
class SubmitTest : TestCase() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val backend = FakeBackendRule()

  @get:Rule(order = 2) val accounts = AccountsRule()

  @get:Rule(order = 3) val scenarios = ScenarioRule()

  private val context
    get() = device.targetContext

  @Test
  fun emptyFieldsAreFlaggedAndNothingIsSent() = run {
    step("Open submit and send with nothing entered") {
      scenarios.launch(SubmitActivity::class.java)
      SubmitScreen { send.click() }
    }
    step("Verify both fields are flagged and nothing was sent") {
      SubmitScreen {
        titleLayout.hasError(context.getString(R.string.title_required))
        contentLayout.hasError(context.getString(R.string.url_text_required))
      }
      assertTrue(FakeBackend.posts.isEmpty())
    }
  }

  @Test
  fun aLinkIsSubmittedWithItsFormTokenThenNewStoriesOpen() = run {
    step("Sign in, and HN accepts the submission") {
      accounts.signIn()
      FakeHackerNewsSite.submitPageIssues(fnid = "token123")
      FakeHackerNewsSite.submissionAccepted()
      FakeHackerNews.feed(Feed.New)
    }
    scenarios.launch(SubmitActivity::class.java)
    fillIn("A great article", "https://example.com/great")
    step("Send, confirming it's a link") {
      SubmitScreen {
        send.click()
        confirmation.message.hasText(context.getString(R.string.confirm_submit_url))
        confirmation.positiveButton.click()
      }
    }
    step("Verify new stories open") {
      StoryListScreen { toolbar.hasTitle(context.getString(R.string.title_activity_new)) }
    }
    step("Verify the link was submitted with the token from the submit page") {
      val form = FakeBackend.postsTo("/r").single().form
      assertEquals("token123", form["fnid"])
      assertEquals("A great article", form["title"])
      assertEquals("https://example.com/great", form["url"])
      assertNull(form["text"])
    }
  }

  @Test
  fun textIsSubmittedAsAQuestion() = run {
    step("Sign in, and HN accepts the submission") {
      accounts.signIn()
      FakeHackerNewsSite.submitPageIssues(fnid = "token123")
      FakeHackerNewsSite.submissionAccepted()
      FakeHackerNews.feed(Feed.New)
    }
    scenarios.launch(SubmitActivity::class.java)
    fillIn("Ask HN: Anything?", "What do you think?")
    step("Send, confirming it's a question") {
      SubmitScreen {
        send.click()
        confirmation.message.hasText(context.getString(R.string.confirm_submit_question))
        confirmation.positiveButton.click()
      }
    }
    step("Verify it was submitted as text") {
      flakySafely {
        val form = FakeBackend.postsTo("/r").single().form
        assertEquals("What do you think?", form["text"])
        assertNull(form["url"])
      }
    }
  }

  @Test
  fun submittingSignedOutAsksToLogIn() = run {
    scenarios.launch(SubmitActivity::class.java)
    fillIn("A great article", "https://example.com/great")
    step("Send without being signed in") {
      SubmitScreen {
        send.click()
        confirmation.positiveButton.click()
      }
    }
    step("Verify login is offered and nothing was submitted") {
      LoginScreen { loginButton.isVisible() }
      assertTrue(FakeBackend.postsTo("/r").isEmpty())
    }
  }

  @Test
  fun sharedTextIsSplitIntoTitleAndLink() = run {
    step("Share \"title - link\" text to the app") {
      scenarios.launch<SubmitActivity>(
          Intent(context, SubmitActivity::class.java)
              .setAction(Intent.ACTION_SEND)
              .setType("text/plain")
              .putExtra(Intent.EXTRA_TEXT, "A great article - https://example.com/great"),
      )
    }
    step("Verify the title and link are filled in separately") {
      SubmitScreen {
        title.hasText("A great article")
        content.hasText("https://example.com/great")
      }
    }
  }

  private fun TestContext<Unit>.fillIn(title: String, content: String) =
      step("Enter the title and content") {
        SubmitScreen {
          this.title.typeText(title)
          this.content.typeText(content)
        }
        closeSoftKeyboard()
      }
}
