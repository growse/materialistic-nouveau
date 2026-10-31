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
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.growse.android.io.github.hidroh.materialistic.AppUtils.getThemedResId
import com.growse.android.io.github.hidroh.materialistic.R
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

internal abstract class PeekabooTouchHelperCallback(context: Context) :
    ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
  private val mPaint = Paint()
  private val mPadding: Int
  private val defaultTextColor: Int

  init {
    defaultTextColor =
        ContextCompat.getColor(
            context,
            getThemedResId(context, android.R.attr.textColorPrimary),
        )
    mPaint.textSize = context.resources.getDimensionPixelSize(R.dimen.text_size_small).toFloat()
    mPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    mPadding = context.resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin)
  }

  override fun onMove(
      recyclerView: RecyclerView,
      viewHolder: RecyclerView.ViewHolder,
      target: RecyclerView.ViewHolder,
  ): Boolean {
    return false
  }

  override fun onChildDraw(
      c: Canvas,
      recyclerView: RecyclerView,
      viewHolder: RecyclerView.ViewHolder,
      dX: Float,
      dY: Float,
      actionState: Int,
      isCurrentlyActive: Boolean,
  ) {
    drawPeekingText(c, viewHolder, dX)
    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
  }

  private fun drawPeekingText(canvas: Canvas, viewHolder: RecyclerView.ViewHolder, dX: Float) {
    val itemView = viewHolder.itemView
    val swipeRight = dX > 0
    val text = (if (swipeRight) rightText else leftText)!!
    val rect = Rect()
    mPaint.getTextBounds(text, 0, text.length, rect)
    val textWidth = (rect.right - rect.left).toFloat()
    val textHeight = (rect.bottom - rect.top).toFloat()
    val width = itemView.width.toFloat()
    val paddingY = (itemView.height - textHeight) / 2
    mPaint.color = if (swipeRight) rightTextColor else leftTextColor
    mPaint.alpha = min(255, (255 / getSwipeThreshold(viewHolder) * abs(dX) / width).toInt())
    canvas.drawText(
        text.uppercase(Locale.getDefault()),
        if (swipeRight) (itemView.left + mPadding).toFloat()
        else itemView.right - mPadding - textWidth,
        itemView.bottom - paddingY,
        mPaint,
    )
  }

  override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
    return 1f / 3
  }

  override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
    return Float.MAX_VALUE
  }

  protected abstract val leftText: String?

  protected abstract val rightText: String?

  protected open val leftTextColor: Int
    get() = defaultTextColor

  protected open val rightTextColor: Int
    get() = defaultTextColor
}
