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

import android.app.ActivityManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import android.text.TextUtils
import android.view.Menu

abstract class ThemedActivity : AppCompatActivity() {
    private val mMenuTintDelegate: MenuTintDelegate = new MenuTintDelegate()
    private val mThemeObservable: Preferences.Observable = new Preferences.Observable()
    private var mResumed: Boolean = true
    private var mPendingThemeChanged: Boolean = false

    protected override fun onCreate(savedInstanceState: Bundle) {
        Preferences.Theme.apply(this, isDialogTheme(), isTranslucent())
        super.onCreate(savedInstanceState)
        setTaskTitle(getTitle())
        mMenuTintDelegate.onActivityCreated(this)
    }

    protected override fun onPostCreate(savedInstanceState: Bundle) {
        super.onPostCreate(savedInstanceState)
        mThemeObservable.subscribe(this, (key, contextChanged) ->  onThemeChanged(key),
                R.string.pref_theme, R.string.pref_daynight_auto)
    }

    @CallSuper
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        mMenuTintDelegate.onOptionsMenuCreated(menu)
        return super.onCreateOptionsMenu(menu)
    }

    protected override fun onResume() {
        super.onResume()
        mResumed = true
        if (mPendingThemeChanged) {
            AppUtils.restart(this, false)
        }
    }

    protected override fun onPause() {
        super.onPause()
        mResumed = false
    }

    protected override fun onDestroy() {
        super.onDestroy()
        mThemeObservable.unsubscribe(this)
    }

    override fun setTitle(title: CharSequence) {
        super.setTitle(title)
        setTaskTitle(title)
    }

    protected fun isDialogTheme(): Boolean {
        return false
    }

    protected fun isTranslucent(): Boolean {
        return false
    }

    private fun onThemeChanged(key: Int) {
        if (key == R.string.pref_daynight_auto) {
            AppCompatDelegate.setDefaultNightMode(Preferences.Theme.getAutoDayNightMode(this))
        }
        if (mResumed) {
            AppUtils.restart(this, true)
        } else {
            mPendingThemeChanged = true
        }
    }

    fun setTaskTitle(title: CharSequence) {
        if (!TextUtils.isEmpty(title)) {
            setTaskDescription(ActivityManager.TaskDescription(title.toString(),
                    BitmapFactory.decodeResource(getResources(), R.drawable.ic_app),
                    ContextCompat.getColor(this, AppUtils.getThemedResId(this, R.attr.colorPrimary))))
        }
    }
}
