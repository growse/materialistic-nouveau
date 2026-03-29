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

package io.github.hidroh.materialistic

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import android.view.Menu
import android.view.MenuItem

/**
 * Helper to tint menu items for activities and fragments
 */
open class MenuTintDelegate {
    private var mTextColorPrimary: Int = 0

    /**
     * Callback that should be triggered after activity has been created
     * @param context    activity context
     */
    fun onActivityCreated(context: Context) {
        mTextColorPrimary = ContextCompat.getColor(context,
                AppUtils.getThemedResId(context, android.R.attr.textColorPrimary))
    }

    /**
     * Callback that should be triggered after menu has been inflated
     * @param menu    inflated menu
     */
    fun onOptionsMenuCreated(menu: Menu) {
        for (int i = 0; i < menu.size(); i++) {
            Drawable drawable = menu.getItem(i).getIcon()
            if (drawable == null) {
                continue
            }
            drawable = DrawableCompat.wrap(drawable)
            DrawableCompat.setTint(drawable, mTextColorPrimary)
        }
    }

    @SuppressWarnings("unused")
    fun setIcon(item: MenuItem, icon: Int) {
        item.setIcon(icon)
        Drawable drawable = item.getIcon()
        drawable = DrawableCompat.wrap(drawable)
        DrawableCompat.setTint(drawable, mTextColorPrimary)
    }
}
