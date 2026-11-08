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
package com.growse.android.io.github.hidroh.materialistic.appwidget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.Window
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.growse.android.io.github.hidroh.materialistic.R
import com.growse.android.io.github.hidroh.materialistic.ThemedActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WidgetConfigActivity : ThemedActivity() {
  private var mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setResult(RESULT_CANCELED)
    if (
        intent.extras == null ||
            (intent.extras!!
                .getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID,
                )
                .also { mAppWidgetId = it }) == AppWidgetManager.INVALID_APPWIDGET_ID
    ) {
      finish()
      return
    }
    supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
    setContentView(R.layout.activity_widget_config)
    if (savedInstanceState == null) {
      val args = Bundle()
      args.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
      supportFragmentManager
          .beginTransaction()
          .replace(
              R.id.widget_preferences,
              Fragment.instantiate(
                  this,
                  WidgetConfigurationFragment::class.java.name,
                  args,
              ),
              WidgetConfigurationFragment::class.java.name,
          )
          .commit()
    }
    findViewById<View?>(R.id.button_ok).setOnClickListener { v: View? -> configure() }
  }

  override val isDialogTheme: Boolean
    get() = true

  private fun configure() {
    WidgetHelper(this).configure(mAppWidgetId)
    setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId))
    finish()
  }

  class WidgetConfigurationFragment : PreferenceFragmentCompat() {
    private val mListener =
        OnSharedPreferenceChangeListener { sharedPreferences: SharedPreferences?, key: String? ->
          if (TextUtils.equals(key, getString(R.string.pref_widget_query))) {
            setFilterQuery()
          }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      preferenceManager.setSharedPreferencesName(
          WidgetHelper.getConfigName(
              requireArguments().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID),
          ),
      )
      preferenceManager.getSharedPreferences()!!.registerOnSharedPreferenceChangeListener(mListener)
      setFilterQuery()
    }

    override fun onCreatePreferences(bundle: Bundle?, s: String?) {
      addPreferencesFromResource(R.xml.preferences_widget)
    }

    override fun onDestroy() {
      super.onDestroy()
      preferenceManager
          .getSharedPreferences()!!
          .unregisterOnSharedPreferenceChangeListener(mListener)
    }

    private fun setFilterQuery() {
      val key = getString(R.string.pref_widget_query)
      preferenceManager
          .findPreference<Preference?>(key)!!
          .setSummary(preferenceManager.getSharedPreferences()!!.getString(key, null))
    }
  }
}
