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
import android.view.MenuItem
import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.PopupMenu as SupportPopupMenu

interface PopupMenu {
  /**
   * Create a new popup menu with an anchor view and alignment gravity. Must be called right after
   * construction.
   *
   * @param context Context the popup menu is running in, through which it can access the current
   *   theme, resources, etc.
   * @param anchor Anchor view for this popup. The popup will appear below the anchor if there is
   *   room, or above it if there is not.
   * @param gravity The [Gravity] value for aligning the popup with its anchor.
   * @return this (fluent API)
   */
  fun create(context: Context, anchor: View, gravity: Int): PopupMenu

  /**
   * Inflate a menu resource into this PopupMenu. This is equivalent to calling
   * popupMenu.getMenuInflater().inflate(menuRes, popupMenu.getMenu()).
   *
   * @param menuRes Menu resource to inflate
   * @return this (fluent API)
   */
  fun inflate(@MenuRes menuRes: Int): PopupMenu

  /**
   * Set menu item visibility
   *
   * @param itemResId item ID
   * @param visible true to be visible, false otherwise
   * @return this (fluent API)
   */
  fun setMenuItemVisible(@IdRes itemResId: Int, visible: Boolean): PopupMenu

  /**
   * Set menu item title
   *
   * @param itemResId item ID
   * @param title item title
   * @return this (fluent API)
   */
  fun setMenuItemTitle(@IdRes itemResId: Int, @StringRes title: Int): PopupMenu

  /**
   * Set a listener that will be notified when the user selects an item from the menu.
   *
   * @param listener Listener to notify
   * @return this (fluent API)
   */
  fun setOnMenuItemClickListener(listener: OnMenuItemClickListener): PopupMenu

  /** Show the menu popup anchored to the view specified during construction. */
  fun show()

  /**
   * Interface responsible for receiving menu item click events if the items themselves do not have
   * individual item click listeners.
   */
  fun interface OnMenuItemClickListener {
    /**
     * This method will be invoked when a menu item is clicked if the item itself did not already
     * handle the event.
     *
     * @param item [MenuItem] that was clicked
     * @return `true` if the event was handled, `false` otherwise.
     */
    fun onMenuItemClick(item: MenuItem): Boolean
  }

  class Impl : PopupMenu {
    private var mSupportPopupMenu: SupportPopupMenu? = null

    override fun create(
        context: Context,
        anchor: View,
        gravity: Int,
    ): PopupMenu {
      mSupportPopupMenu = SupportPopupMenu(context, anchor, gravity)
      return this
    }

    override fun inflate(@MenuRes menuRes: Int): PopupMenu {
      mSupportPopupMenu!!.inflate(menuRes)
      return this
    }

    override fun setMenuItemVisible(
        @IdRes itemResId: Int,
        visible: Boolean,
    ): PopupMenu {
      mSupportPopupMenu!!.menu.findItem(itemResId).isVisible = visible
      return this
    }

    override fun setMenuItemTitle(
        @IdRes itemResId: Int,
        @StringRes title: Int,
    ): PopupMenu {
      mSupportPopupMenu!!.menu.findItem(itemResId).setTitle(title)
      return this
    }

    override fun setOnMenuItemClickListener(listener: OnMenuItemClickListener): PopupMenu {
      mSupportPopupMenu!!.setOnMenuItemClickListener(
          SupportPopupMenu.OnMenuItemClickListener { listener.onMenuItemClick(it) },
      )
      return this
    }

    override fun show() {
      mSupportPopupMenu!!.show()
    }
  }
}
