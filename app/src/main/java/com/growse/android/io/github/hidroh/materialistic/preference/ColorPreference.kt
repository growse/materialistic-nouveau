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
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.color.DynamicColors
import com.growse.android.io.github.hidroh.materialistic.R

private data class Swatch(val name: String, val label: CharSequence, @ColorInt val color: Int)

/**
 * A swatch-grid preference for picking one of a fixed set of accent colors (see
 * [R.array.accent_color_names] / [R.array.accent_color_values]), shared by the toolbar and accent
 * color pickers. On a device with Material You dynamic color available, a "System" swatch derived
 * from the wallpaper is appended after the fixed set.
 */
class ColorPreference
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    Preference(context, attrs, defStyleAttr) {

  private val swatches = buildSwatches(context)
  private var selected: String = swatches[0].name

  init {
    layoutResource = R.layout.preference_color
  }

  override fun onGetDefaultValue(a: TypedArray, index: Int): Any =
      a.getString(index) ?: swatches[0].name

  override fun onSetInitialValue(defaultValue: Any?) {
    selected = getPersistedString((defaultValue as? String) ?: swatches[0].name)
    updateSummary()
  }

  override fun onBindViewHolder(holder: PreferenceViewHolder) {
    super.onBindViewHolder(holder)
    holder.itemView.isClickable = false
    val container = holder.findViewById(R.id.color_swatch_container) as LinearLayout
    container.removeAllViews()
    val swatchSize = context.resources.getDimensionPixelSize(R.dimen.color_swatch_size)
    val checkSize = context.resources.getDimensionPixelSize(R.dimen.color_swatch_check_size)
    val margin = context.resources.getDimensionPixelSize(R.dimen.margin)
    for (entry in swatches) {
      val swatch =
          FrameLayout(context).apply {
            layoutParams =
                LinearLayout.LayoutParams(swatchSize, swatchSize).apply {
                  marginStart = margin
                  marginEnd = margin
                }
            background =
                GradientDrawable().apply {
                  shape = GradientDrawable.OVAL
                  setColor(entry.color)
                }
            // Prefixed with the preference's own title so a screen reader (and Espresso, in
            // tests) can tell the toolbar and accent swatches apart.
            contentDescription = "$title ${entry.label}"
            isClickable = true
            foreground =
                context
                    .obtainStyledAttributes(
                        intArrayOf(android.R.attr.selectableItemBackgroundBorderless)
                    )
                    .let {
                      val drawable = it.getDrawable(0)
                      it.recycle()
                      drawable
                    }
            setOnClickListener {
              if (selected != entry.name) {
                selected = entry.name
                persistString(entry.name)
                updateSummary()
                notifyChanged()
              }
            }
          }
      if (entry.name == selected) {
        swatch.addView(
            ImageView(context).apply {
              setImageResource(R.drawable.ic_check_white_24dp)
              layoutParams = FrameLayout.LayoutParams(checkSize, checkSize, Gravity.CENTER)
            }
        )
      }
      container.addView(swatch)
    }
  }

  private fun updateSummary() {
    summary = swatches.firstOrNull { it.name == selected }?.label ?: swatches[0].label
  }

  companion object {
    private fun buildSwatches(context: Context): List<Swatch> {
      val names = context.resources.getStringArray(R.array.accent_color_names)
      val labels = context.resources.getTextArray(R.array.accent_color_labels)
      val colors = context.resources.obtainTypedArray(R.array.accent_color_values)
      val fixed =
          try {
            names.indices.map { i -> Swatch(names[i], labels[i], colors.getColor(i, 0)) }
          } finally {
            colors.recycle()
          }
      if (!DynamicColors.isDynamicColorAvailable()) {
        return fixed
      }
      val systemColor = ContextCompat.getColor(context, android.R.color.system_accent1_600)
      return fixed + Swatch("system", context.getString(R.string.color_system), systemColor)
    }
  }
}
