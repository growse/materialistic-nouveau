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

package io.github.hidroh.materialistic.widget

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.recyclerview.widget.RecyclerView
import android.view.View

import io.github.hidroh.materialistic.AppUtils
import io.github.hidroh.materialistic.Preferences
import io.github.hidroh.materialistic.R

open class CommentItemDecoration : RecyclerView.ItemDecoration() {
    private val mHorizontalMargin: Int = 0
    private var mPaint: Paint? = null
    private var mColors: TypedArray? = null
    private val mLevelIndicatorWidth: Int = 0
    private var mColorCodeEnabled: Boolean = false
    private var mThreadIndicatorEnabled: Boolean = false

    constructor(context: Context) {
        mPaint = Paint()
        mPaint.setStrokeWidth(context.getResources().getDimensionPixelSize(R.dimen.divider))
        mHorizontalMargin = context.getResources()
                .getDimensionPixelSize(R.dimen.cardview_horizontal_margin)
        mLevelIndicatorWidth = AppUtils.getDimensionInDp(context, R.dimen.level_indicator_width)
        mColors = context.getResources().obtainTypedArray(R.array.color_codes)
        mColorCodeEnabled = Preferences.colorCodeEnabled(context)
        mThreadIndicatorEnabled = Preferences.threadIndicatorEnabled(context)
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.set(mHorizontalMargin, 0, mHorizontalMargin, 0)
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (!mThreadIndicatorEnabled) {
            return
        }
        for (int i = 0; i < parent.getChildCount(); i++) {
            val child = parent.getChildAt(i)
            int level = parent.getChildViewHolder(child).getItemViewType()
            for (int j = 0; j < level; j++) {
                int left = mHorizontalMargin + j * mLevelIndicatorWidth + mLevelIndicatorWidth / 2
                if (mColorCodeEnabled) {
                    mPaint.setColor(mColors.getColor(j % mColors.length(), 0))
                    mPaint.setAlpha(31); // 12% alpha
                } else {
                    mPaint.setColor(Color.GRAY)
                    mPaint.setAlpha(Math.max(0, 31 - 4 * j))
                }
                c.drawLine(left, child.getTop(), left, child.getBottom(), mPaint)
            }
        }
    }

    fun setColorCodeEnabled(colorCodeEnabled: Boolean) {
        mColorCodeEnabled = colorCodeEnabled
    }

    fun setThreadIndicatorEnabled(threadIndicatorEnabled: Boolean) {
        mThreadIndicatorEnabled = threadIndicatorEnabled
    }
}
