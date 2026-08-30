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
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.growse.android.io.github.hidroh.materialistic.R

/**
 * A swatch-grid preference for picking one of a fixed set of accent colors (see
 * [R.array.accent_color_names] / [R.array.accent_color_values]), shared by the toolbar and accent
 * color pickers.
 */
class ColorPreference
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    Preference(context, attrs, defStyleAttr) {

  private val names = context.resources.getStringArray(R.array.accent_color_names)
  private val labels = context.resources.getTextArray(R.array.accent_color_labels)
  private var selected: String = names[0]

  init {
    layoutResource = R.layout.preference_color
  }

  override fun onGetDefaultValue(a: TypedArray, index: Int): Any = a.getString(index) ?: names[0]

  override fun onSetInitialValue(defaultValue: Any?) {
    selected = getPersistedString((defaultValue as? String) ?: names[0])
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
    val colors = context.resources.obtainTypedArray(R.array.accent_color_values)
    try {
      for (i in names.indices) {
        val name = names[i]
        val color = colors.getColor(i, 0)
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
                    setColor(color)
                  }
              contentDescription = labels[i]
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
                if (selected != name) {
                  selected = name
                  persistString(name)
                  updateSummary()
                  notifyChanged()
                }
              }
            }
        if (name == selected) {
          swatch.addView(
              ImageView(context).apply {
                setImageResource(R.drawable.ic_check_white_24dp)
                layoutParams = FrameLayout.LayoutParams(checkSize, checkSize, Gravity.CENTER)
              }
          )
        }
        container.addView(swatch)
      }
    } finally {
      colors.recycle()
    }
  }

  private fun updateSummary() {
    val index = names.indexOf(selected).coerceAtLeast(0)
    summary = labels[index]
  }
}
