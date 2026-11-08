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

import android.app.ActivityManager.TaskDescription
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Menu
import androidx.activity.OnBackPressedCallback
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat

abstract class ThemedActivity : AppCompatActivity() {
  private val menuTintDelegate = MenuTintDelegate()
  private val themeObservable = Preferences.Observable()
  private val backPressedCallback =
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          // TODO http://b.android.com/176265
          try {
            supportFinishAfterTransition()
          } catch (_: IllegalStateException) {
            finish()
          }
        }
      }
  private var resumed = true
  private var pendingThemeChanged = false

  var isActivityDestroyed: Boolean = false
    private set

  protected open val isDialogTheme: Boolean
    get() = false

  protected open val isTranslucent: Boolean
    get() = false

  override fun onCreate(savedInstanceState: Bundle?) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    Preferences.Theme.apply(this, isDialogTheme, isTranslucent)
    super.onCreate(savedInstanceState)
    onBackPressedDispatcher.addCallback(this, backPressedCallback)
    setTaskTitle(title)
    menuTintDelegate.onActivityCreated(this)
  }

  override fun onPostCreate(savedInstanceState: Bundle?) {
    super.onPostCreate(savedInstanceState)
    themeObservable.subscribe(
        this,
        { key, _ -> onThemeChanged(key) },
        R.string.pref_theme,
        R.string.pref_daynight_auto,
    )
  }

  @CallSuper
  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuTintDelegate.onOptionsMenuCreated(menu)
    return super.onCreateOptionsMenu(menu)
  }

  override fun onResume() {
    super.onResume()
    resumed = true
    if (pendingThemeChanged) {
      recreate()
    }
  }

  override fun onPause() {
    super.onPause()
    resumed = false
  }

  override fun onDestroy() {
    super.onDestroy()
    themeObservable.unsubscribe(this)
    isActivityDestroyed = true
  }

  override fun setTitle(title: CharSequence?) {
    super.setTitle(title)
    setTaskTitle(title)
  }

  /** Sets the title shown for this task in the recents screen. */
  protected fun setTaskTitle(title: CharSequence?) {
    if (title.isNullOrEmpty()) {
      return
    }
    @Suppress("DEPRECATION")
    setTaskDescription(
        TaskDescription(
            title.toString(),
            BitmapFactory.decodeResource(resources, R.drawable.ic_app),
            ContextCompat.getColor(this, AppUtils.getThemedResId(this, R.attr.colorPrimary)),
        ),
    )
  }

  private fun onThemeChanged(key: Int) {
    if (key == R.string.pref_daynight_auto) {
      AppCompatDelegate.setDefaultNightMode(Preferences.Theme.getAutoDayNightMode(this))
    }
    if (resumed) {
      recreate()
    } else {
      pendingThemeChanged = true
    }
  }
}
