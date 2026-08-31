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
package com.growse.android.io.github.hidroh.materialistic

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class LauncherActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Reconciles the home-screen icon with the toolbar color preference, in case it changed
    // (or the app was just installed) since the icon was last synced. Off the main thread since
    // it touches disk via PackageManager; doesn't need to complete before the redirect below.
    val appContext = applicationContext
    Thread { Preferences.Theme.syncLauncherIcon(appContext) }.start()
    val launchScreens: Map<String, Class<out Activity>> =
        mapOf(
            getString(R.string.pref_launch_screen_value_top) to ListActivity::class.java,
            getString(R.string.pref_launch_screen_value_best) to BestActivity::class.java,
            getString(R.string.pref_launch_screen_value_hot) to PopularActivity::class.java,
            getString(R.string.pref_launch_screen_value_new) to NewActivity::class.java,
            getString(R.string.pref_launch_screen_value_ask) to AskActivity::class.java,
            getString(R.string.pref_launch_screen_value_show) to ShowActivity::class.java,
            getString(R.string.pref_launch_screen_value_jobs) to JobsActivity::class.java,
            getString(R.string.pref_launch_screen_value_saved) to FavoriteActivity::class.java,
        )
    val launchScreen = Preferences.getLaunchScreen(this)
    startActivity(Intent(this, launchScreens[launchScreen] ?: ListActivity::class.java))
    finish()
  }
}
