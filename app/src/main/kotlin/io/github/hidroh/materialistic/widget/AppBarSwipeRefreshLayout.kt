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

import android.app.Activity
import android.content.Context
import com.google.android.material.appbar.AppBarLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import android.util.AttributeSet

import io.github.hidroh.materialistic.R

/**
 * Minor extension to {@link SwipeRefreshLayout} that is appbar-aware, only enabling when appbar
 * has been fully expanded.
 * This class assumes activity layout contains an {@link AppBarLayout} with {@link R.id#appbar}
 */
open class AppBarSwipeRefreshLayout : SwipeRefreshLayout(), AppBarLayout.OnOffsetChangedListener {
    private var mAppBar: AppBarLayout? = null

    constructor(context: Context) : super(context) {
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    }

    protected override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (getContext() is Activity) {
            mAppBar = (AppBarLayout) ((Activity) getContext()).findViewById(R.id.appbar)
            if (mAppBar != null) {
                mAppBar.addOnOffsetChangedListener(this)
            }
        }
    }

    protected override fun onDetachedFromWindow() {
        if (mAppBar != null) {
            mAppBar.removeOnOffsetChangedListener(this)
            mAppBar = null
        }
        super.onDetachedFromWindow()
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout, i: Int) {
        this.setEnabled(i == 0)
    }
}
