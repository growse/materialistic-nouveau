package com.growse.android.io.github.hidroh.materialistic

import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.google.android.material.color.DynamicColors
import com.growse.android.io.github.hidroh.materialistic.screens.DisplayPreferencesScreen
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain

class DisplayPreferencesActivityTest : TestCase() {

  private val context = ApplicationProvider.getApplicationContext<Context>()

  // Each ColorPreference persists its choice to the app's default SharedPreferences, which
  // outlives any single Activity instance. Cleared as an outer rule (rather than @Before) so it
  // runs before ActivityScenarioRule launches the activity, not after - otherwise a prior test's
  // leftover selection would already be bound into the freshly-launched screen.
  private val clearColorPreferencesRule =
      object : ExternalResource() {
        override fun before() {
          PreferenceManager.getDefaultSharedPreferences(context).edit {
            remove(context.getString(R.string.pref_primary_color))
            remove(context.getString(R.string.pref_accent_color))
          }
        }
      }

  private val activityScenarioRule =
      ActivityScenarioRule<PreferencesActivity>(
          Intent(context, PreferencesActivity::class.java)
              .putExtra(PreferencesActivity.EXTRA_TITLE, R.string.display)
              .putExtra(PreferencesActivity.EXTRA_PREFERENCES, R.xml.preferences_display)
      )

  @get:Rule
  val rules: RuleChain = RuleChain.outerRule(clearColorPreferencesRule).around(activityScenarioRule)

  @Test
  fun colorPickersAreVisibleWithDefaultSummaries() = run {
    step("Verify the toolbar color picker defaults to Purple") {
      DisplayPreferencesScreen {
        primaryColorTitle.isVisible()
        summaryWithText(context.getString(R.string.color_purple)).isVisible()
      }
    }
    step("Verify the accent color picker defaults to Red") {
      DisplayPreferencesScreen {
        accentColorTitle.isVisible()
        summaryWithText(context.getString(R.string.color_red)).isVisible()
      }
    }
  }

  @Test
  fun tappingATealSwatchUpdatesTheToolbarColorSelection() = run {
    val tealSwatchDescription =
        "${context.getString(R.string.pref_primary_color_title)} ${context.getString(R.string.color_teal)}"
    step("Tap the toolbar color picker's Teal swatch") {
      DisplayPreferencesScreen { swatch(tealSwatchDescription).click() }
    }
    step("Verify the summary now reads Teal") {
      DisplayPreferencesScreen {
        summaryWithText(context.getString(R.string.color_teal)).isVisible()
      }
    }
    step("Verify the choice was persisted") {
      val persisted =
          PreferenceManager.getDefaultSharedPreferences(context)
              .getString(context.getString(R.string.pref_primary_color), null)
      assert(persisted == "teal") { "Expected pref_primary_color to be persisted as 'teal'" }
    }
  }

  @Test
  fun accentColorSelectionIsIndependentOfToolbarColor() = run {
    val tealSwatchDescription =
        "${context.getString(R.string.pref_accent_color_title)} ${context.getString(R.string.color_teal)}"
    step("Tap the accent color picker's Teal swatch") {
      DisplayPreferencesScreen { swatch(tealSwatchDescription).click() }
    }
    // Checked against SharedPreferences rather than the on-screen summary: tapping the lower
    // (accent) row can scroll the toolbar color row out of the RecyclerView's inflated window,
    // making its summary TextView unavailable to Espresso even though nothing changed.
    step("Verify the toolbar color preference itself is unaffected") {
      val persisted =
          PreferenceManager.getDefaultSharedPreferences(context)
              .getString(context.getString(R.string.pref_primary_color), null)
      assert(persisted == null) { "Expected pref_primary_color to remain unset, was '$persisted'" }
    }
  }

  @Test
  fun systemSwatchIsOfferedOnlyWhenDynamicColorIsAvailable() = run {
    // Skips cleanly (not a failure) on a device/emulator without Material You dynamic color,
    // e.g. anything pre-Android 12 - see ColorPreference.buildSwatches.
    assumeTrue(DynamicColors.isDynamicColorAvailable())
    val systemSwatchDescription =
        "${context.getString(R.string.pref_primary_color_title)} ${context.getString(R.string.color_system)}"
    step("Tap the toolbar color picker's System swatch") {
      DisplayPreferencesScreen { swatch(systemSwatchDescription).click() }
    }
    step("Verify the summary now reads System") {
      DisplayPreferencesScreen {
        summaryWithText(context.getString(R.string.color_system)).isVisible()
      }
    }
    step("Verify the choice was persisted") {
      val persisted =
          PreferenceManager.getDefaultSharedPreferences(context)
              .getString(context.getString(R.string.pref_primary_color), null)
      assert(persisted == "system") { "Expected pref_primary_color to be persisted as 'system'" }
    }
  }
}
