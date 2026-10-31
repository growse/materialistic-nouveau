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
package com.growse.android.io.github.hidroh.materialistic.preference

import android.content.Context
import android.content.res.TypedArray
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.collection.ArrayMap
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.color.DynamicColors
import com.growse.android.io.github.hidroh.materialistic.Preferences.Theme.disableAutoDayNight
import com.growse.android.io.github.hidroh.materialistic.R

class ThemePreference(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    Preference(context, attrs, defStyleAttr) {
  private var mSelectedTheme: String? = null

  @Suppress("unused") constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

  init {
    layoutResource = R.layout.preference_theme
  }

  override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
    return LIGHT
  }

  @Deprecated("Deprecated in Java")
  override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
    super.onSetInitialValue(restorePersistedValue, defaultValue)
    mSelectedTheme =
        if (restorePersistedValue) getPersistedString(null) else defaultValue as String?
    if (TextUtils.isEmpty(mSelectedTheme)) {
      mSelectedTheme = LIGHT
    }
    setSummary(VALUES.getValue(mSelectedTheme).summary)
  }

  override fun onBindViewHolder(holder: PreferenceViewHolder) {
    super.onBindViewHolder(holder)
    holder.itemView.isClickable = false
    for (i in 0..<BUTTONS.size) {
      val buttonId: Int = BUTTONS.keyAt(i)!!
      val value = BUTTONS.valueAt(i)
      val button = holder.findViewById(buttonId)
      if (SYSTEM == value && !DynamicColors.isDynamicColorAvailable()) {
        // Wallpaper-derived color isn't available on this device/OS version: don't offer
        // a swatch that would resolve to a copy of the plain Light theme.
        button.visibility = View.GONE
        continue
      }
      button.isClickable = true
      button.setOnClickListener {
        mSelectedTheme = value
        if (shouldDisableDependents()) {
          disableAutoDayNight(context)
        }
        setSummary(VALUES.getValue(value).summary)
        persistString(value)
      }
    }
  }

  override fun shouldDisableDependents(): Boolean {
    // assume only auto day-night is dependent
    return VALUES[mSelectedTheme] !is DayNightSpec
  }

  open class ThemeSpec
  internal constructor(
      @field:StringRes @param:StringRes val summary: Int,
      @field:StyleRes @param:StyleRes val theme: Int,
      @field:StyleRes @param:StyleRes val themeOverrides: Int,
  ) {
    var isTranslucent: ThemeSpec? = null

    open fun getTranslucent(): ThemeSpec {
      return this
    }
  }

  internal class DarkSpec
  @JvmOverloads
  constructor(
      @StringRes summary: Int,
      @StyleRes themeOverrides: Int = -1,
  ) : ThemeSpec(summary, R.style.AppTheme_Dark, themeOverrides) {
    override fun getTranslucent(): ThemeSpec {
      if (isTranslucent == null) {
        isTranslucent = ThemeSpec(summary, R.style.AppTheme_Dark_Translucent, themeOverrides)
      }
      return isTranslucent!!
    }
  }

  class DayNightSpec
  @JvmOverloads
  internal constructor(
      @StringRes summary: Int,
      @StyleRes themeOverrides: Int = -1,
  ) : ThemeSpec(summary, R.style.AppTheme_DayNight, themeOverrides) {
    override fun getTranslucent(): ThemeSpec {
      if (isTranslucent == null) {
        isTranslucent = ThemeSpec(summary, R.style.AppTheme_Translucent, themeOverrides)
      }
      return isTranslucent!!
    }
  }

  companion object {
    private const val LIGHT = "light"
    const val SYSTEM: String = "system"
    private const val DARK = "dark"
    private const val BLACK = "black"
    private const val SEPIA = "sepia"
    private const val GREEN = "green"
    private const val SOLARIZED = "solarized"
    private const val SOLARIZED_DARK = "solarized_dark"
    private val BUTTONS = ArrayMap<Int, String>()
    private val VALUES = ArrayMap<String, ThemeSpec>()

    init {
      BUTTONS[R.id.theme_light] = LIGHT
      BUTTONS[R.id.theme_system] = SYSTEM
      BUTTONS[R.id.theme_dark] = DARK
      BUTTONS[R.id.theme_black] = BLACK
      BUTTONS[R.id.theme_sepia] = SEPIA
      BUTTONS[R.id.theme_green] = GREEN
      BUTTONS[R.id.theme_solarized] = SOLARIZED
      BUTTONS[R.id.theme_solarized_dark] = SOLARIZED_DARK

      VALUES[LIGHT] = DayNightSpec(R.string.theme_light)
      VALUES[SYSTEM] = DayNightSpec(R.string.theme_system, R.style.System)
      VALUES[DARK] = DarkSpec(R.string.theme_dark)
      VALUES[BLACK] = DarkSpec(R.string.theme_black, R.style.Black)
      VALUES[SEPIA] = DayNightSpec(R.string.theme_sepia, R.style.Sepia)
      VALUES[GREEN] = DayNightSpec(R.string.theme_green, R.style.Green)
      VALUES[SOLARIZED] = DayNightSpec(R.string.theme_solarized, R.style.Solarized)
      VALUES[SOLARIZED_DARK] =
          DarkSpec(
              R.string.theme_solarized_dark,
              R.style.Solarized_Dark,
          )
    }

    fun getTheme(value: String, isTranslucent: Boolean): ThemeSpec {
      val themeSpec = VALUES.getValue(if (VALUES.containsKey(value)) value else LIGHT)
      return if (isTranslucent) themeSpec.getTranslucent() else themeSpec
    }
  }
}
