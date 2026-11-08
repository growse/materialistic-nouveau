/*
 * Copyright (c) 2015 Ha Duy Trung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.growse.android.io.github.hidroh.materialistic

import android.content.Intent
import android.os.Bundle
import android.provider.SearchRecentSuggestions
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ScrollView
import androidx.annotation.StringRes
import androidx.annotation.XmlRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.growse.android.io.github.hidroh.materialistic.data.SearchRecentSuggestionsProvider
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : DrawerActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_settings)
    setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
    supportActionBar?.displayOptions =
        ActionBar.DISPLAY_SHOW_HOME or ActionBar.DISPLAY_HOME_AS_UP or ActionBar.DISPLAY_SHOW_TITLE
    val scrollView = findViewById<ScrollView>(R.id.content)
    val initialPaddingBottom = scrollView.paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(scrollView) { v, windowInsets ->
      val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
      v.setPadding(
          v.paddingLeft,
          v.paddingTop,
          v.paddingRight,
          initialPaddingBottom + systemBars.bottom,
      )
      windowInsets
    }
    bindPreferenceScreen(R.id.drawer_display, R.string.display, R.xml.preferences_display)
    bindPreferenceScreen(R.id.drawer_offline, R.string.offline, R.xml.preferences_offline)
    bindPreferenceScreen(R.id.menu_list, R.string.list_display_options, R.xml.preferences_list)
    bindPreferenceScreen(R.id.menu_comments, R.string.comments, R.xml.preferences_comments)
    bindPreferenceScreen(R.id.menu_readability, R.string.readability, R.xml.preferences_readability)
    findViewById<View>(R.id.drawer_about).setOnClickListener {
      startActivity(Intent(this, AboutActivity::class.java))
    }
    findViewById<View>(R.id.drawer_release).setOnClickListener {
      startActivity(Intent(this, ReleaseNotesActivity::class.java))
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_settings, menu)
    return super.onCreateOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_clear_recent ->
          confirm(R.string.clear_search_history_confirm) {
            SearchRecentSuggestions(
                    this,
                    SearchRecentSuggestionsProvider.PROVIDER_AUTHORITY,
                    SearchRecentSuggestionsProvider.MODE,
                )
                .clearHistory()
          }
      R.id.menu_reset ->
          confirm(R.string.reset_settings_confirm) {
            Preferences.reset(this)
            recreate()
          }
      R.id.menu_clear_drafts -> Preferences.clearDrafts(this)
      else -> return super.onOptionsItemSelected(item)
    }
    return true
  }

  /** Opens the preference screen [preferences] titled [title] when [viewId] is clicked. */
  private fun bindPreferenceScreen(viewId: Int, @StringRes title: Int, @XmlRes preferences: Int) {
    findViewById<View>(viewId).setOnClickListener {
      startActivity(
          Intent(this, PreferencesActivity::class.java)
              .putExtra(PreferencesActivity.EXTRA_TITLE, title)
              .putExtra(PreferencesActivity.EXTRA_PREFERENCES, preferences),
      )
    }
  }

  private fun confirm(@StringRes message: Int, onConfirmed: () -> Unit) {
    alertDialogBuilder
        .init(this)
        .setMessage(message)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(android.R.string.ok) { _, _ -> onConfirmed() }
        .show()
  }
}
