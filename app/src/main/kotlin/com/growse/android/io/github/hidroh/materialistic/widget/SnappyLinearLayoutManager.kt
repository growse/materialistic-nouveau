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
import android.graphics.Point
import android.graphics.PointF
import android.view.WindowManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller

/**
 * Light extension to [LinearLayoutManager] that overrides smooth scroller to always snap to start,
 * and lays out [preloadScreens] screens of rows ahead of the scroll direction
 */
class SnappyLinearLayoutManager(context: Context, preloadScreens: Int) :
    LinearLayoutManager(context) {
  private val mExtraSpace: Int = preloadScreens * getDisplayHeight(context)

  override fun smoothScrollToPosition(
      recyclerView: RecyclerView,
      state: RecyclerView.State?,
      position: Int,
  ) {
    val smoothScroller: SmoothScroller =
        object : LinearSmoothScroller(recyclerView.context) {
          override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
            return this@SnappyLinearLayoutManager.computeScrollVectorForPosition(targetPosition)
          }

          override fun getVerticalSnapPreference(): Int {
            return SNAP_TO_START // override base class behavior
          }
        }
    smoothScroller.targetPosition = position
    startSmoothScroll(smoothScroller)
  }

  @Deprecated("Deprecated in Java")
  override fun getExtraLayoutSpace(state: RecyclerView.State?): Int {
    return if (mExtraSpace == 0) super.getExtraLayoutSpace(state) else mExtraSpace
  }

  companion object {
    private fun getDisplayHeight(context: Context): Int {
      val display =
          (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
      val point = Point()
      display.getSize(point)
      return point.y
    }
  }
}
