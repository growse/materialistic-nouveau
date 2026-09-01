package com.growse.android.io.github.hidroh.materialistic

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies Preferences.Theme.syncLauncherIcon flips exactly one of .LauncherActivity or its
 * color-swatch activity-aliases (AndroidManifest.xml) to enabled, matching pref_primary_color.
 */
@RunWith(AndroidJUnit4::class)
class LauncherIconSyncTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val packageManager = context.packageManager

  private val allComponents =
      listOf(
              "LauncherActivity",
              "LauncherAliasRed",
              "LauncherAliasIndigo",
              "LauncherAliasBlue",
              "LauncherAliasTeal",
              "LauncherAliasBrown",
          )
          .map { ComponentName(context.packageName, "${context.packageName}.$it") }

  @Before
  @After
  fun clearPrimaryColorPreference() {
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      remove(context.getString(R.string.pref_primary_color))
    }
  }

  private fun enabledComponentName(): String? {
    val enabled = allComponents.filter {
      packageManager.getComponentEnabledSetting(it) !=
          PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }
    assert(enabled.size == 1) { "Expected exactly one enabled launcher component, got $enabled" }
    return enabled.single().className.substringAfterLast('.')
  }

  @Test
  fun defaultsToTheBaseLauncherActivity() {
    Preferences.Theme.syncLauncherIcon(context)
    assert(enabledComponentName() == "LauncherActivity")
  }

  @Test
  fun tealPreferenceEnablesTheTealAlias() {
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putString(context.getString(R.string.pref_primary_color), "teal")
    }
    Preferences.Theme.syncLauncherIcon(context)
    assert(enabledComponentName() == "LauncherAliasTeal")
  }

  @Test
  fun switchingBackToPurpleReEnablesTheBaseActivity() {
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putString(context.getString(R.string.pref_primary_color), "teal")
    }
    Preferences.Theme.syncLauncherIcon(context)
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putString(context.getString(R.string.pref_primary_color), "purple")
    }
    Preferences.Theme.syncLauncherIcon(context)
    assert(enabledComponentName() == "LauncherActivity")
  }

  // Regression test for disabling an activity-alias closing the task it launched (see
  // syncLauncherIcon's kdoc): if the alias that just launched the current task would otherwise
  // be disabled, it must be left alone instead.
  @Test
  fun neverDisablesTheComponentThatLaunchedTheCurrentTask() {
    val redAlias = ComponentName(context.packageName, "${context.packageName}.LauncherAliasRed")
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putString(context.getString(R.string.pref_primary_color), "red")
    }
    Preferences.Theme.syncLauncherIcon(context)
    assert(
        packageManager.getComponentEnabledSetting(redAlias) !=
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    ) {
      "Precondition failed: red alias should be enabled before switching away from it"
    }

    // Switch the preference to Teal, as if from within a task that was launched via the Red
    // alias - simulating the exact scenario where naively disabling Red would close that task.
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putString(context.getString(R.string.pref_primary_color), "teal")
    }
    Preferences.Theme.syncLauncherIcon(context, redAlias)

    assert(
        packageManager.getComponentEnabledSetting(redAlias) !=
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    ) {
      "The component that launched the current task must never be disabled by syncLauncherIcon"
    }
    val tealAlias = ComponentName(context.packageName, "${context.packageName}.LauncherAliasTeal")
    assert(
        packageManager.getComponentEnabledSetting(tealAlias) ==
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    ) {
      "The new target should still be enabled even though the old one was skipped"
    }

    // Clean up the intentionally-left-enabled Red alias so later tests in this class still see
    // exactly one enabled component, as if a later cold start (no protected component) reconciled.
    Preferences.Theme.syncLauncherIcon(context)
  }
}
