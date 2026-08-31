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
}
