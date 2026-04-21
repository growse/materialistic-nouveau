package com.growse.android.io.github.hidroh.materialistic

import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.growse.android.io.github.hidroh.materialistic.screens.ReleaseNotesScreen
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import org.junit.Rule
import org.junit.Test

class ReleaseNotesActivityTest : TestCase() {

  @get:Rule val activityRule = ActivityScenarioRule(ReleaseNotesActivity::class.java)

  @Test
  fun webViewIsDisplayed() = run {
    step("Verify the release notes WebView is visible") {
      ReleaseNotesScreen { webView.isVisible() }
    }
  }

  @Test
  fun actionButtonsAreDisplayed() = run {
    step("Verify the 'Got it' button is visible") { ReleaseNotesScreen { okButton.isVisible() } }
    step("Verify the 'Love it' rate button is visible") {
      ReleaseNotesScreen { rateButton.isVisible() }
    }
  }

  @Test
  fun okButtonDismissesActivity() = run {
    step("Click the 'Got it' button") { ReleaseNotesScreen { okButton.click() } }
    step("Verify the activity is destroyed") {
      assert(activityRule.scenario.state == Lifecycle.State.DESTROYED) {
        "Activity should be destroyed after OK click"
      }
    }
  }
}
