package com.growse.android.io.github.hidroh.materialistic.settings

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import androidx.test.espresso.ViewAssertion
import com.growse.android.io.github.hidroh.materialistic.ListActivity
import com.growse.android.io.github.hidroh.materialistic.R
import com.growse.android.io.github.hidroh.materialistic.screens.PreferencesScreen
import com.growse.android.io.github.hidroh.materialistic.testing.FakeBackendRule
import com.growse.android.io.github.hidroh.materialistic.testing.FakeHackerNews
import com.growse.android.io.github.hidroh.materialistic.testing.PreferencesRule
import com.growse.android.io.github.hidroh.materialistic.testing.ScenarioRule
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.views.KView
import junit.framework.AssertionFailedError
import org.junit.Rule
import org.junit.Test

/** Choosing a theme in the display settings recolors the app. */
@HiltAndroidTest
class ThemeTest : TestCase() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val backend = FakeBackendRule()

  @get:Rule(order = 2) val scenarios = ScenarioRule()

  @get:Rule(order = 3)
  val preferences = PreferencesRule(R.string.pref_theme, R.string.pref_daynight_auto)

  private val context
    get() = device.targetContext

  @Test
  fun lightIsTheDefaultPurple() = run {
    step("Open the story list without choosing a theme") { openStories() }
    step("Verify the toolbar is the default purple") { assertToolbarColor(R.color.orange300) }
  }

  @Test
  fun choosingClassicBringsBackTheOriginalOrange() = run {
    step("Choose the Classic theme in the display settings") {
      scenarios.launch<PreferencesActivity>(
          Intent(context, PreferencesActivity::class.java)
              .putExtra(PreferencesActivity.EXTRA_TITLE, R.string.display)
              .putExtra(PreferencesActivity.EXTRA_PREFERENCES, R.xml.preferences_display),
      )
      KView { withId(R.id.theme_classic) }.click()
    }
    step("Verify it's the selected theme") {
      PreferencesScreen { row(context.getString(R.string.theme_classic)).isVisible() }
    }
    step("Open the story list") { openStories() }
    step("Verify the toolbar is the classic orange") {
      assertToolbarColor(R.color.classicOrange300)
    }
  }

  private fun openStories() {
    FakeHackerNews.topStories()
    scenarios.launch(ListActivity::class.java)
  }

  private fun TestContext<Unit>.assertToolbarColor(colorRes: Int) {
    val expected = ContextCompat.getColor(context, colorRes)
    KView { withId(R.id.toolbar) }
        .assert {
          ViewAssertion { view, noViewFound ->
            if (noViewFound != null) throw noViewFound
            val actual = (view.background as ColorDrawable).color
            if (actual != expected) {
              throw AssertionFailedError(
                  "toolbar is #%08X, expected #%08X".format(actual, expected),
              )
            }
          }
        }
  }
}
