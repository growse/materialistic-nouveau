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

package io.github.hidroh.materialistic

import android.content.Intent
import android.os.Bundle
import android.provider.SearchRecentSuggestions
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import android.view.Menu
import android.view.MenuItem

import javax.inject.Inject

import io.github.hidroh.materialistic.data.SearchRecentSuggestionsProvider

open class SettingsActivity : DrawerActivity() {
    @Inject var mAlertDialogBuilder: AlertDialogBuilder? = null

    protected override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar))
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE)
        findViewById(R.id.drawer_display).setOnClickListener(v ->
                startActivity(Intent(SettingsActivity.this, PreferencesActivity::class.java)
                        .putExtra(PreferencesActivity.EXTRA_TITLE, R.string.display)
                        .putExtra(PreferencesActivity.EXTRA_PREFERENCES, R.xml.preferences_display)))
        findViewById(R.id.drawer_offline).setOnClickListener(v ->
                startActivity(Intent(SettingsActivity.this, PreferencesActivity::class.java)
                        .putExtra(PreferencesActivity.EXTRA_TITLE, R.string.offline)
                        .putExtra(PreferencesActivity.EXTRA_PREFERENCES, R.xml.preferences_offline)))
        findViewById(R.id.menu_list).setOnClickListener(v ->
                startActivity(Intent(SettingsActivity.this, PreferencesActivity::class.java)
                        .putExtra(PreferencesActivity.EXTRA_TITLE, R.string.list_display_options)
                        .putExtra(PreferencesActivity.EXTRA_PREFERENCES, R.xml.preferences_list)))
        findViewById(R.id.menu_comments).setOnClickListener(v ->
                startActivity(Intent(SettingsActivity.this, PreferencesActivity::class.java)
                        .putExtra(PreferencesActivity.EXTRA_TITLE, R.string.comments)
                        .putExtra(PreferencesActivity.EXTRA_PREFERENCES, R.xml.preferences_comments)))
        findViewById(R.id.menu_readability).setOnClickListener(v ->
                startActivity(Intent(SettingsActivity.this, PreferencesActivity::class.java)
                        .putExtra(PreferencesActivity.EXTRA_TITLE, R.string.readability)
                        .putExtra(PreferencesActivity.EXTRA_PREFERENCES, R.xml.preferences_readability)))
        findViewById(R.id.drawer_about).setOnClickListener(v ->
                startActivity(Intent(SettingsActivity.this, AboutActivity::class.java)))
        findViewById(R.id.drawer_release).setOnClickListener(v ->
                startActivity(Intent(SettingsActivity.this, ReleaseNotesActivity::class.java)))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        getMenuInflater().inflate(R.menu.menu_settings, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getItemId() == R.id.menu_clear_recent) {
            mAlertDialogBuilder
                    .init(this)
                    .setMessage(R.string.clear_search_history_confirm)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, (dialog, which) ->
                            SearchRecentSuggestions(SettingsActivity.this,
                                    SearchRecentSuggestionsProvider.PROVIDER_AUTHORITY,
                                    SearchRecentSuggestionsProvider.MODE)
                                    .clearHistory())
                    .create()
                    .show()
            return true
        }
        if (item.getItemId() == R.id.menu_reset) {
            mAlertDialogBuilder
                    .init(this)
                    .setMessage(R.string.reset_settings_confirm)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        Preferences.reset(SettingsActivity.this)
                        AppUtils.restart(SettingsActivity.this, false)
                    })
                    .create()
                    .show()
        }
        if (item.getItemId() == R.id.menu_clear_drafts) {
            Preferences.clearDrafts(this)
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
