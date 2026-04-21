package com.growse.android.io.github.hidroh.materialistic

import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.growse.android.io.github.hidroh.materialistic.screens.FeedbackScreen
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import org.junit.Rule
import org.junit.Test

class FeedbackActivityTest : TestCase() {

  @get:Rule val activityRule = ActivityScenarioRule(FeedbackActivity::class.java)

  @Test
  fun formFieldsAreDisplayedAndEmpty() = run {
    step("Verify title and body inputs are visible and empty") {
      FeedbackScreen {
        titleEditText.isVisible()
        titleEditText.hasEmptyText()
        bodyEditText.isVisible()
        bodyEditText.hasEmptyText()
      }
    }
  }

  @Test
  fun sendButtonIsEnabledByDefault() = run {
    step("Verify send button is enabled before any interaction") {
      FeedbackScreen { sendButton.isEnabled() }
    }
  }

  @Test
  fun sendButtonRemainsEnabledWhenFieldsAreEmpty() = run {
    step("Click send with empty fields") { FeedbackScreen { sendButton.click() } }
    step("Verify send button is still enabled (validation failed, no send attempt)") {
      FeedbackScreen { sendButton.isEnabled() }
    }
  }

  @Test
  fun rateButtonIsVisible() = run {
    step("Verify the rate/stars button is visible") { FeedbackScreen { rateButton.isVisible() } }
  }

  @Test
  fun typingInFieldsEnablesSubmission() = run {
    step("Enter a title") { FeedbackScreen { titleEditText.typeText("Test title") } }
    step("Enter a body") { FeedbackScreen { bodyEditText.typeText("Test body text") } }
    step("Send button remains enabled after filling both fields") {
      FeedbackScreen { sendButton.isEnabled() }
    }
  }
}
