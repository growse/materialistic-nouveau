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
package com.growse.android.io.github.hidroh.materialistic.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.growse.android.io.github.hidroh.materialistic.AppUtils.getThemedResId
import com.growse.android.io.github.hidroh.materialistic.R

class IconButton
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageButton(context, attrs, defStyleAttr) {
  private val mColorStateList: ColorStateList
  private val mTinted: Boolean

  init {
    setBackgroundResource(getThemedResId(context, R.attr.selectableItemBackgroundBorderless))
    val ta = context.theme.obtainStyledAttributes(attrs, R.styleable.IconButton, 0, 0)
    val colorDisabled =
        ContextCompat.getColor(
            context,
            getThemedResId(context, android.R.attr.textColorSecondary),
        )
    val colorDefault =
        ContextCompat.getColor(
            context,
            getThemedResId(context, android.R.attr.textColorPrimary),
        )
    val colorEnabled = ta.getColor(R.styleable.IconButton_tint, colorDefault)
    mColorStateList = ColorStateList(STATES, intArrayOf(colorEnabled, colorDisabled))
    mTinted = ta.hasValue(R.styleable.IconButton_tint)
    if (suggestedMinimumWidth == 0) {
      minimumWidth = context.resources.getDimensionPixelSize(R.dimen.icon_button_width)
    }
    scaleType = ScaleType.CENTER
    setImageDrawable(drawable)
    ta.recycle()
  }

  override fun setImageResource(resId: Int) {
    setImageDrawable(ContextCompat.getDrawable(context, resId))
  }

  override fun setImageDrawable(drawable: Drawable?) {
    super.setImageDrawable(tint(drawable))
  }

  private fun tint(drawable: Drawable?): Drawable? {
    if (drawable == null) {
      return null
    }
    val tintDrawable = DrawableCompat.wrap(if (mTinted) drawable.mutate() else drawable)
    DrawableCompat.setTintList(tintDrawable, mColorStateList)
    return tintDrawable
  }

  companion object {
    private val STATES =
        arrayOf<IntArray?>(
            intArrayOf(android.R.attr.state_enabled),
            intArrayOf(-android.R.attr.state_enabled),
        )
  }
}
