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

package io.github.hidroh.materialistic.appwidget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceFragmentCompat
import android.text.TextUtils
import android.view.Window

import io.github.hidroh.materialistic.InjectableActivity
import io.github.hidroh.materialistic.R

open class WidgetConfigActivity : InjectableActivity() {
    private var mAppWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    protected override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        if (getIntent().getExtras() == null ||
                (mAppWidgetId = getIntent().getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID)) == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_widget_config)
        if (savedInstanceState == null) {
            Bundle args = Bundle()
            args.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.widget_preferences,
                            Fragment.instantiate(this, WidgetConfigurationFragment::class.java.getName(), args),
                            WidgetConfigurationFragment::class.java.getName())
                    .commit()
        }
        //noinspection ConstantConditions
        findViewById(R.id.button_ok).setOnClickListener(v -> configure())
    }

    protected override fun isDialogTheme(): Boolean {
        return true
    }

    private fun configure() {
        WidgetHelper(this).configure(mAppWidgetId)
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId))
        finish()
    }

    open class WidgetConfigurationFragment : PreferenceFragmentCompat() {

        private SharedPreferences.OnSharedPreferenceChangeListener mListener =
                (sharedPreferences, key) -> {
                    if (TextUtils.equals(key, getString(R.string.pref_widget_query))) {
                        setFilterQuery()
                    }
                }

        override fun onCreate(savedInstanceState: Bundle) {
            super.onCreate(savedInstanceState)
            getPreferenceManager().setSharedPreferencesName(WidgetHelper.getConfigName(
                    getArguments().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID)))
            getPreferenceManager().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(mListener)
            setFilterQuery()
        }

        override fun onCreatePreferences(bundle: Bundle, s: String) {
            addPreferencesFromResource(R.xml.preferences_widget)
        }

        override fun onDestroy() {
            super.onDestroy()
            getPreferenceManager().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(mListener)
        }

        private fun setFilterQuery() {
            String key = getString(R.string.pref_widget_query)
            getPreferenceManager().findPreference(key)
                    .setSummary(getPreferenceManager().getSharedPreferences()
                            .getString(key, null))
        }
    }
}
