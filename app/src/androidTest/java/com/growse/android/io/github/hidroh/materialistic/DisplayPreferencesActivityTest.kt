package com.growse.android.io.github.hidroh.materialistic

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.google.android.material.color.DynamicColors
import com.growse.android.io.github.hidroh.materialistic.screens.DisplayPreferencesScreen
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import org.hamcrest.Matchers.allOf
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain

class DisplayPreferencesActivityTest : TestCase() {

  private val context = ApplicationProvider.getApplicationContext<Context>()

  // Each of these preferences persists its choice to the app's default SharedPreferences, which
  // outlives any single Activity instance. Cleared as an outer rule (rather than @Before) so it
  // runs before ActivityScenarioRule launches the activity, not after - otherwise a prior test's
  // leftover selection would already be bound into the freshly-launched screen.
  private val clearColorPreferencesRule =
      object : ExternalResource() {
        override fun before() {
          PreferenceManager.getDefaultSharedPreferences(context).edit {
            remove(context.getString(R.string.pref_primary_color))
            remove(context.getString(R.string.pref_accent_color))
            remove(context.getString(R.string.pref_theme))
            remove(context.getString(R.string.pref_daynight_auto))
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
  fun themeDefaultsToSystemWithAutoDayNightImplied() = run {
    step("Verify the theme picker defaults to System") {
      DisplayPreferencesScreen { themeTitle.isVisible() }
      onView(
              allOf(
                  withText(R.string.theme_system),
                  hasSibling(withText(R.string.pref_theme_title)),
              )
          )
          .check(matches(isDisplayed()))
    }
    step("Verify day/night follows the OS by default, without needing the separate toggle") {
      // The default state has neither pref_theme nor pref_daynight_auto persisted yet - this is
      // exactly what a fresh install resolves to.
      val mode = Preferences.Theme.getAutoDayNightMode(context)
      assert(mode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
        "Expected MODE_NIGHT_FOLLOW_SYSTEM by default, was $mode"
      }
    }
  }

  @Test
  fun colorPickersAreVisibleWithDefaultSummaries() = run {
    // Preferences.Theme.defaultColorChoice prefers "system" wherever dynamic color is available,
    // falling back to the fixed swatch otherwise - the summaries should track that exactly. Both
    // pickers can legitimately show the same "System" label at once, so the summary is matched
    // by its sibling title rather than by text alone, which would otherwise be ambiguous.
    val defaultPrimaryLabel =
        context.getString(
            if (DynamicColors.isDynamicColorAvailable()) R.string.color_system
            else R.string.color_purple
        )
    val defaultAccentLabel =
        context.getString(
            if (DynamicColors.isDynamicColorAvailable()) R.string.color_system
            else R.string.color_red
        )
    step("Verify the toolbar color picker's default summary") {
      DisplayPreferencesScreen { primaryColorTitle.isVisible() }
      onView(
              allOf(
                  withText(defaultPrimaryLabel),
                  hasSibling(withText(R.string.pref_primary_color_title)),
              )
          )
          .check(matches(isDisplayed()))
    }
    step("Verify the accent color picker's default summary") {
      DisplayPreferencesScreen { accentColorTitle.isVisible() }
      onView(
              allOf(
                  withText(defaultAccentLabel),
                  hasSibling(withText(R.string.pref_accent_color_title)),
              )
          )
          .check(matches(isDisplayed()))
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
    val tealSwatchDescription =
        "${context.getString(R.string.pref_primary_color_title)} ${context.getString(R.string.color_teal)}"
    val systemSwatchDescription =
        "${context.getString(R.string.pref_primary_color_title)} ${context.getString(R.string.color_system)}"
    // "System" is the default (see colorPickersAreVisibleWithDefaultSummaries), so tap away from
    // it first - otherwise tapping an already-selected swatch is a no-op and never persists.
    step("Tap the toolbar color picker's Teal swatch") {
      DisplayPreferencesScreen { swatch(tealSwatchDescription).click() }
    }
    step("Tap the toolbar color picker's System swatch") {
      DisplayPreferencesScreen { swatch(systemSwatchDescription).click() }
    }
    step("Verify the summary reads System again") {
      // Not summaryWithText alone: the accent picker also defaults to "System" and is untouched
      // by this test, so bare text would match two summaries at once.
      onView(
              allOf(
                  withText(context.getString(R.string.color_system)),
                  hasSibling(withText(R.string.pref_primary_color_title)),
              )
          )
          .check(matches(isDisplayed()))
    }
    step("Verify the choice was persisted") {
      val persisted =
          PreferenceManager.getDefaultSharedPreferences(context)
              .getString(context.getString(R.string.pref_primary_color), null)
      assert(persisted == "system") { "Expected pref_primary_color to be persisted as 'system'" }
    }
  }
}
