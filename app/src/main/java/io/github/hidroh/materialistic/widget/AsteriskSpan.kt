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
import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import android.text.style.ReplacementSpan

import io.github.hidroh.materialistic.AppUtils
import io.github.hidroh.materialistic.R

open class AsteriskSpan : ReplacementSpan() {
    private val mBackgroundColor: Int = 0
    private val mTextColor: Int = 0
    private val mPadding: Float = 0

    constructor(context: Context) {
        super()
        mBackgroundColor = ContextCompat.getColor(context,
                AppUtils.getThemedResId(context, R.attr.colorAccent))
        mTextColor = ContextCompat.getColor(context, android.R.color.transparent)
        mPadding = context.getResources().getDimension(R.dimen.padding_asterisk)
    }

    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt): Int {
        return Math.round(mPadding * 4); // padding let + radius * 2 + padding right
    }

    override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
        float radius = mPadding
        float centerX = x + radius + mPadding
        float centerY = top + radius + mPadding
        paint.setColor(mBackgroundColor)
        canvas.drawCircle(centerX, centerY, radius, paint)
        paint.setColor(mTextColor)
        canvas.drawText(text, start, end, x + mPadding * 2, y, paint)
    }
}
