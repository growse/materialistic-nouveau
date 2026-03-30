/*
 * Copyright (c) 2016 Ha Duy Trung
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

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.appcompat.app.ActionBar
import androidx.preference.PreferenceFragmentCompat
import androidx.appcompat.widget.Toolbar
import android.view.MenuItem

open class PreferencesActivity : ThemedActivity() {
    val EXTRA_TITLE: String = PreferencesActivity::class.java.name + ".EXTRA_TITLE"
    val EXTRA_PREFERENCES: String = PreferencesActivity::class.java.name + ".EXTRA_PREFERENCES"

    protected override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)
        setTitle(getIntent().getIntExtra(EXTRA_TITLE, 0))
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar))
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE)
        if (savedInstanceState == null) {
            val args = Bundle()
            args.putInt(EXTRA_PREFERENCES, getIntent().getIntExtra(EXTRA_PREFERENCES, 0))
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.content_frame,
                            Fragment.instantiate(this, SettingsFragment::class.java.getName(), args),
                            SettingsFragment::class.java.getName())
                    .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getItemId() == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    open class SettingsFragment : PreferenceFragmentCompat() {

        override fun onCreate(savedInstanceState: Bundle) {
            super.onCreate(savedInstanceState)
            Preferences.sync(getPreferenceManager())
        }

        override fun onCreatePreferences(bundle: Bundle, s: String) {
            addPreferencesFromResource(getArguments().getInt(EXTRA_PREFERENCES))
        }
    }
}
