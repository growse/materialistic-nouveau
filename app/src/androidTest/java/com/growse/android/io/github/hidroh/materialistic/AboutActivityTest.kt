package com.growse.android.io.github.hidroh.materialistic

import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.growse.android.io.github.hidroh.materialistic.screens.AboutScreen
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import org.junit.Rule
import org.junit.Test

class AboutActivityTest : TestCase() {

  @get:Rule val activityRule = ActivityScenarioRule(AboutActivity::class.java)

  @Test
  fun toolbarIsDisplayed() = run {
    step("Verify toolbar is shown") { AboutScreen { toolbar.isVisible() } }
  }

  @Test
  fun allInfoSectionsAreDisplayed() = run {
    step("Verify application info text is shown") {
      AboutScreen { applicationInfoText.isVisible() }
    }
    step("Verify developer info text is shown") { AboutScreen { developerInfoText.isVisible() } }
    step("Verify libraries text is shown") { AboutScreen { librariesText.isVisible() } }
    step("Verify license text is shown") { AboutScreen { licenseText.isVisible() } }
    step("Verify third-party licenses text is shown") {
      AboutScreen { thirdPartyLicensesText.isVisible() }
    }
    step("Verify privacy policy text is shown") { AboutScreen { privacyPolicyText.isVisible() } }
  }

  @Test
  fun upButtonFinishesActivity() = run {
    step("Verify toolbar with navigation is shown") { AboutScreen { toolbar.isVisible() } }
  }
}
