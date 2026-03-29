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

package io.github.hidroh.materialistic.widget

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.appcompat.widget.AppCompatTextView
import android.util.AttributeSet

import io.github.hidroh.materialistic.AppUtils
import io.github.hidroh.materialistic.R

open class TintableTextView : AppCompatTextView() {

    private var mTextColor: Int = 0

    constructor(context: Context, attrs: AttributeSet) {
        this(context, attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) {
        super(context, attrs, defStyleAttr)
        mTextColor = getTextColor(context, attrs)
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.TintableTextView, 0, 0)
        setCompoundDrawablesWithIntrinsicBounds(
                ta.getDrawable(R.styleable.TintableTextView_iconStart),
                ta.getDrawable(R.styleable.TintableTextView_iconTop),
                ta.getDrawable(R.styleable.TintableTextView_iconEnd),
                ta.getDrawable(R.styleable.TintableTextView_iconBottom))
        ta.recycle()
    }

    override fun setCompoundDrawablesWithIntrinsicBounds(left: Drawable, top: Drawable, right: Drawable, bottom: Drawable) {
        super.setCompoundDrawablesWithIntrinsicBounds(tint(left), tint(top), tint(right), tint(bottom))
    }

    override fun setTextColor(color: Int) {
        mTextColor = color
        super.setTextColor(color)
        Drawable[] drawables = getCompoundDrawables()
        setCompoundDrawablesWithIntrinsicBounds(drawables[0], drawables[1], drawables[2], drawables[3])
    }

    private fun getTextColor(context: Context, attrs: AttributeSet): Int {
        int defaultTextColor = ContextCompat.getColor(getContext(),
                AppUtils.getThemedResId(getContext(), android.R.attr.textColorTertiary))
        TypedArray ta = context.obtainStyledAttributes(attrs,
                new int[]{android.R.attr.textAppearance, android.R.attr.textColor})
        int ap = ta.getResourceId(0, 0)
        int textColor
        if (ap == 0) {
            textColor = ta.getColor(1, defaultTextColor)
        } else {
            TypedArray tap = context.obtainStyledAttributes(ap, new int[]{android.R.attr.textColor})
            textColor = tap.getColor(0, defaultTextColor)
            tap.recycle()
        }
        ta.recycle()
        return textColor
    }

    private fun tint(drawable: Drawable): Drawable {
        if (drawable == null) {
            return null
        }
        drawable = DrawableCompat.wrap(drawable)
        DrawableCompat.setTint(drawable, mTextColor)
        return drawable
    }
}
