package com.growse.android.io.github.hidroh.materialistic.testing

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import org.junit.rules.ExternalResource

/**
 * Clears the preferences named by [keys] (string resource ids) before and after each test, so a
 * setting one test changes doesn't leak into the next.
 */
class PreferencesRule(private vararg val keys: Int) : ExternalResource() {
  private val context: Context
    get() = ApplicationProvider.getApplicationContext()

  override fun before() = clear()

  override fun after() = clear()

  /** Saves [value] for the preference [key], as the settings screen would. */
  fun set(key: Int, value: String) =
      PreferenceManager.getDefaultSharedPreferences(context).edit(commit = true) {
        putString(context.getString(key), value)
      }

  private fun clear() =
      PreferenceManager.getDefaultSharedPreferences(context).edit(commit = true) {
        keys.forEach { remove(context.getString(it)) }
      }
}
