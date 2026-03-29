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

package io.github.hidroh.materialistic.preference

import android.content.Context
import android.content.res.TypedArray
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.collection.ArrayMap
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View

import io.github.hidroh.materialistic.Preferences
import io.github.hidroh.materialistic.R
import io.github.hidroh.materialistic.annotation.Synthetic

open class ThemePreference : Preference() {

    private const val LIGHT: String = "light"
    private const val DARK: String = "dark"
    private const val BLACK: String = "black"
    private const val SEPIA: String = "sepia"
    private const val GREEN: String = "green"
    private const val SOLARIZED: String = "solarized"
    private const val SOLARIZED_DARK: String = "solarized_dark"
    private const val BUTTONS: ArrayMap<Int, String> = new ArrayMap<>()
    private const val VALUES: ArrayMap<String, ThemeSpec> = new ArrayMap<>()
    static {
        BUTTONS.put(R.id.theme_light, LIGHT)
        BUTTONS.put(R.id.theme_dark, DARK)
        BUTTONS.put(R.id.theme_black, BLACK)
        BUTTONS.put(R.id.theme_sepia, SEPIA)
        BUTTONS.put(R.id.theme_green, GREEN)
        BUTTONS.put(R.id.theme_solarized, SOLARIZED)
        BUTTONS.put(R.id.theme_solarized_dark, SOLARIZED_DARK)

        VALUES.put(LIGHT, DayNightSpec(R.string.theme_light))
        VALUES.put(DARK, DarkSpec(R.string.theme_dark))
        VALUES.put(BLACK, DarkSpec(R.string.theme_black, R.style.Black))
        VALUES.put(SEPIA, DayNightSpec(R.string.theme_sepia, R.style.Sepia))
        VALUES.put(GREEN, DayNightSpec(R.string.theme_green, R.style.Green))
        VALUES.put(SOLARIZED, DayNightSpec(R.string.theme_solarized, R.style.Solarized))
        VALUES.put(SOLARIZED_DARK, DarkSpec(R.string.theme_solarized_dark,
                R.style.Solarized_Dark))
    }

    private var mSelectedTheme: String? = null

    fun getTheme(value: String, isTranslucent: Boolean): ThemeSpec {
        val themeSpec = VALUES.get(VALUES.containsKey(value) ? value : LIGHT)
        return isTranslucent ? themeSpec.getTranslucent() : themeSpec
    }

    @SuppressWarnings("unused")
    constructor(context: Context, attrs: AttributeSet) {
        this(context, attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setLayoutResource(R.layout.preference_theme)
    }

    protected override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return LIGHT
    }

    protected override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any) {
        super.onSetInitialValue(restorePersistedValue, defaultValue)
        mSelectedTheme = restorePersistedValue ? getPersistedString(null): (String) defaultValue
        if (TextUtils.isEmpty(mSelectedTheme)) {
            mSelectedTheme = LIGHT
        }
        setSummary(VALUES.get(mSelectedTheme).summary)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.itemView.setClickable(false)
        for (int i = 0; i < BUTTONS.size(); i++) {
            final int buttonId = BUTTONS.keyAt(i)
            val value = BUTTONS.valueAt(i)
            val button = holder.findViewById(buttonId)
            button.setClickable(true)
            button.setOnClickListener(v -> {
                mSelectedTheme = value
                if (shouldDisableDependents()) {
                    Preferences.Theme.disableAutoDayNight(getContext())
                }
                setSummary(VALUES.get(value).summary)
                persistString(value)
            })
        }
    }

    override fun shouldDisableDependents(): Boolean {
        // assume only auto day-night is dependent
        return !(VALUES.get(mSelectedTheme) is DayNightSpec)
    }

    open class ThemeSpec {
        final @StringRes int summary
        public final @StyleRes int theme
        public final @StyleRes int themeOverrides
        var translucent: ThemeSpec? = null

        @Synthetic
        constructor(summary: Int, theme: Int, themeOverrides: Int) {
            this.summary = summary
            this.theme = theme
            this.themeOverrides = themeOverrides
        }

        fun getTranslucent(): ThemeSpec {
            return this
        }
    }

    open class DarkSpec : ThemeSpec() {

        constructor(summary: Int) {
            this(summary, -1)
        }

        constructor(summary: Int, themeOverrides: Int) : super(summary, R.style.AppTheme_Dark, themeOverrides) {
        }

        override fun getTranslucent(): ThemeSpec {
            if (translucent == null) {
                translucent = ThemeSpec(summary, R.style.AppTheme_Dark_Translucent, themeOverrides)
            }
            return translucent
        }
    }

    open class DayNightSpec : ThemeSpec() {

        constructor(summary: Int) {
            this(summary, -1)
        }

        constructor(summary: Int, themeOverrides: Int) : super(summary, R.style.AppTheme_DayNight, themeOverrides) {
        }

        override fun getTranslucent(): ThemeSpec {
            if (translucent == null) {
                translucent = ThemeSpec(summary, R.style.AppTheme_Translucent, themeOverrides)
            }
            return translucent
        }
    }
}
