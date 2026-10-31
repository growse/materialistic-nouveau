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
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.growse.android.io.github.hidroh.materialistic.AppUtils.getDimensionInDp
import com.growse.android.io.github.hidroh.materialistic.Preferences.colorCodeEnabled
import com.growse.android.io.github.hidroh.materialistic.Preferences.threadIndicatorEnabled
import com.growse.android.io.github.hidroh.materialistic.R
import kotlin.math.max

class CommentItemDecoration(context: Context) : ItemDecoration() {
  private val mHorizontalMargin: Int
  private val mPaint: Paint = Paint()
  private val mColors: TypedArray
  private val mLevelIndicatorWidth: Int
  private var mColorCodeEnabled: Boolean
  private var mThreadIndicatorEnabled: Boolean

  init {
    mPaint.strokeWidth = context.resources.getDimensionPixelSize(R.dimen.divider).toFloat()
    mHorizontalMargin = context.resources.getDimensionPixelSize(R.dimen.cardview_horizontal_margin)
    mLevelIndicatorWidth = getDimensionInDp(context, R.dimen.level_indicator_width)
    mColors = context.resources.obtainTypedArray(R.array.color_codes)
    mColorCodeEnabled = colorCodeEnabled(context)
    mThreadIndicatorEnabled = threadIndicatorEnabled(context)
  }

  override fun getItemOffsets(
      outRect: Rect,
      view: View,
      parent: RecyclerView,
      state: RecyclerView.State,
  ) {
    outRect.set(mHorizontalMargin, 0, mHorizontalMargin, 0)
  }

  override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
    if (!mThreadIndicatorEnabled) {
      return
    }
    for (i in 0..<parent.childCount) {
      val child = parent.getChildAt(i)
      val level = parent.getChildViewHolder(child).itemViewType
      for (j in 0..<level) {
        val left = mHorizontalMargin + j * mLevelIndicatorWidth + mLevelIndicatorWidth / 2
        if (mColorCodeEnabled) {
          mPaint.color = mColors.getColor(j % mColors.length(), 0)
          mPaint.alpha = 31 // 12% alpha
        } else {
          mPaint.color = Color.GRAY
          mPaint.alpha = max(0, 31 - 4 * j)
        }
        c.drawLine(
            left.toFloat(),
            child.top.toFloat(),
            left.toFloat(),
            child.bottom.toFloat(),
            mPaint,
        )
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
