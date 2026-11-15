package com.growse.android.io.github.hidroh.materialistic.about

import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import com.growse.android.io.github.hidroh.materialistic.screens.ReleaseNotesScreen
import com.growse.android.io.github.hidroh.materialistic.settings.Preferences
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ReleaseNotesActivityTest : TestCase() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val activityRule = ActivityScenarioRule(ReleaseNotesActivity::class.java)

  @Test
  fun webViewIsDisplayed() = run {
    step("Verify the release notes WebView is visible") {
      ReleaseNotesScreen { webView.isVisible() }
    }
  }

  @Test
  fun openingTheScreenMarksReleaseNotesAsSeen() = run {
    step("Verify the release-notes-seen preference is set once the screen is shown") {
      assert(
          Preferences.isReleaseNotesSeen(
              InstrumentationRegistry.getInstrumentation().targetContext,
          ),
      ) {
        "Release notes should be marked as seen after ReleaseNotesActivity is shown"
      }
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
      // finish() only requests teardown, which runs after the exit animation
      flakySafely(allowedExceptions = setOf(AssertionError::class.java)) {
        assertEquals(
            "Activity should be destroyed after OK click",
            Lifecycle.State.DESTROYED,
            activityRule.scenario.state,
        )
      }
    }
  }
}
