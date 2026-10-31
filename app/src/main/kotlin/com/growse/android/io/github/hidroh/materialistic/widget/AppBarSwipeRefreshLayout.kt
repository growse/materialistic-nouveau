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

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.growse.android.io.github.hidroh.materialistic.R

/**
 * Minor extension to [SwipeRefreshLayout] that is appbar-aware, only enabling when appbar has been
 * fully expanded. This class assumes activity layout contains an [AppBarLayout] with [R.id.appbar]
 */
class AppBarSwipeRefreshLayout : SwipeRefreshLayout, OnOffsetChangedListener {
  private var mAppBar: AppBarLayout? = null

  constructor(context: Context) : super(context)

  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    if (context is Activity) {
      mAppBar = (context as Activity).findViewById<AppBarLayout?>(R.id.appbar)
      if (mAppBar != null) {
        mAppBar!!.addOnOffsetChangedListener(this)
      }
    }
  }

  override fun onDetachedFromWindow() {
    if (mAppBar != null) {
      mAppBar!!.removeOnOffsetChangedListener(this)
      mAppBar = null
    }
    super.onDetachedFromWindow()
  }

  override fun onOffsetChanged(appBarLayout: AppBarLayout?, i: Int) {
    this.setEnabled(i == 0)
  }
}
