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
package com.growse.android.io.github.hidroh.materialistic.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.growse.android.io.github.hidroh.materialistic.AppUtils.getThemedResId
import com.growse.android.io.github.hidroh.materialistic.R

class TintableTextView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int = 0,
) : AppCompatTextView(context, attrs, defStyleAttr) {
  private var mTextColor: Int

  init {
    mTextColor = getTextColor(context, attrs)
    val ta =
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.TintableTextView,
            0,
            0,
        )
    setCompoundDrawablesWithIntrinsicBounds(
        ta.getDrawable(R.styleable.TintableTextView_iconStart),
        ta.getDrawable(R.styleable.TintableTextView_iconTop),
        ta.getDrawable(R.styleable.TintableTextView_iconEnd),
        ta.getDrawable(R.styleable.TintableTextView_iconBottom),
    )
    ta.recycle()
  }

  override fun setCompoundDrawablesWithIntrinsicBounds(
      left: Drawable?,
      top: Drawable?,
      right: Drawable?,
      bottom: Drawable?,
  ) {
    super.setCompoundDrawablesWithIntrinsicBounds(
        tint(left),
        tint(top),
        tint(right),
        tint(bottom),
    )
  }

  override fun setTextColor(color: Int) {
    mTextColor = color
    super.setTextColor(color)
    val drawables = compoundDrawables
    setCompoundDrawablesWithIntrinsicBounds(
        drawables[0],
        drawables[1],
        drawables[2],
        drawables[3],
    )
  }

  private fun getTextColor(context: Context, attrs: AttributeSet?): Int {
    val defaultTextColor =
        ContextCompat.getColor(
            getContext(),
            getThemedResId(getContext(), android.R.attr.textColorTertiary),
        )
    val ta =
        context.obtainStyledAttributes(
            attrs,
            intArrayOf(android.R.attr.textAppearance, android.R.attr.textColor),
        )
    val ap = ta.getResourceId(0, 0)
    val textColor: Int
    if (ap == 0) {
      textColor = ta.getColor(1, defaultTextColor)
    } else {
      val tap = context.obtainStyledAttributes(ap, intArrayOf(android.R.attr.textColor))
      textColor = tap.getColor(0, defaultTextColor)
      tap.recycle()
    }
    ta.recycle()
    return textColor
  }

  private fun tint(drawable: Drawable?): Drawable? {
    var drawable = drawable
    if (drawable == null) {
      return null
    }
    drawable = DrawableCompat.wrap(drawable)
    DrawableCompat.setTint(drawable, mTextColor)
    return drawable
  }
}
