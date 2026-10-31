package com.growse.android.io.github.hidroh.materialistic

import android.content.Intent
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import com.growse.android.io.github.hidroh.materialistic.screens.PreferencesScreen
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Covers the settings that use [androidx.preference.DropDownPreference], using the comment "Thread"
 * display setting as the example.
 */
class DropDownPreferenceTest : TestCase() {
  private val context
    get() = device.targetContext

  private val prefs
    get() = PreferenceManager.getDefaultSharedPreferences(context)

  private val key
    get() = context.getString(R.string.pref_comment_display)

  private val title
    get() = context.getString(R.string.pref_comment_display_title)

  private var originalValue: String? = null
  private var scenario: ActivityScenario<PreferencesActivity>? = null

  @Before
  fun saveOriginalValue() {
    originalValue = prefs.getString(key, null)
  }

  @After
  fun restoreOriginalValue() {
    scenario?.close()
    prefs.edit(commit = true) { putString(key, originalValue) }
  }

  @Test
  fun showsDefaultValueAsSummary() = run {
    step("Clear any saved value and open the comment settings") {
      prefs.edit(commit = true) { remove(key) }
      launchCommentPreferences()
    }
    step("The summary shows the default option") {
      PreferencesScreen {
        summaryOf(title).hasText(context.getString(R.string.comment_display_single))
      }
    }
  }

  @Test
  fun showsPreviouslySavedValue() = run {
    step("Save a value the way the old spinner preference did, then open the settings") {
      prefs.edit(commit = true) {
        putString(key, context.getString(R.string.pref_comment_display_value_collapsed))
      }
      launchCommentPreferences()
    }
    step("The summary shows the saved option") {
      PreferencesScreen {
        summaryOf(title).hasText(context.getString(R.string.comment_display_collapsed))
      }
    }
  }

  @Test
  fun selectingAnOptionSavesItAndUpdatesSummary() = run {
    val multiple = context.getString(R.string.comment_display_multiple)
    step("Open the comment settings with the default value") {
      prefs.edit(commit = true) { remove(key) }
      launchCommentPreferences()
    }
    step("Open the dropdown and pick a different option") {
      PreferencesScreen {
        row(title).click()
        dropDownOption(multiple).click()
      }
    }
    step("The summary shows the new option") {
      PreferencesScreen { summaryOf(title).hasText(multiple) }
    }
    step("The new value is saved") {
      assertEquals(
          context.getString(R.string.pref_comment_display_value_multiple),
          prefs.getString(key, null),
      )
    }
  }

  private fun launchCommentPreferences() {
    scenario =
        ActivityScenario.launch(
            Intent(context, PreferencesActivity::class.java)
                .putExtra(PreferencesActivity.EXTRA_TITLE, R.string.comments)
                .putExtra(PreferencesActivity.EXTRA_PREFERENCES, R.xml.preferences_comments)
        )
  }
}
