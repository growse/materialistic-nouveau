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
package com.growse.android.io.github.hidroh.materialistic

import android.app.Activity
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import androidx.annotation.IntDef
import androidx.core.widget.NestedScrollView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import kotlin.math.abs

/** Helper that intercepts key events and interprets them into navigation actions */
class KeyDelegate {
  @Retention(AnnotationRetention.SOURCE)
  @IntDef(DIRECTION_NONE, DIRECTION_UP, DIRECTION_DOWN)
  internal annotation class Direction

  private val preferenceListener: OnSharedPreferenceChangeListener
  private var preferenceKey: String? = null
  private var enabled = false
  private var scrollable: Scrollable? = null
  private var appBarLayout: AppBarLayout? = null
  private var appBarEnabled = true
  private var backInterceptor: BackInterceptor? = null

  init {
    preferenceListener =
        OnSharedPreferenceChangeListener { sharedPreferences: SharedPreferences?, key: String? ->
          if (TextUtils.equals(key, preferenceKey)) {
            enabled = sharedPreferences!!.getBoolean(key, false)
          }
        }
  }

  /**
   * Attaches this delegate to given activity lifecycle Should call [.detach] accordingly
   *
   * @param activity active activity to receive key events
   * @see .detach
   */
  fun attach(activity: Activity) {
    preferenceKey = activity.getString(R.string.pref_volume)
    enabled =
        PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(preferenceKey, false)
    PreferenceManager.getDefaultSharedPreferences(activity)
        .registerOnSharedPreferenceChangeListener(preferenceListener)
  }

  /**
   * Detaches this delegate from given activity lifecycle Should already call [.attach]
   *
   * @param activity active activity that has been receiving key events
   * @see .attach
   */
  fun detach(activity: Activity) {
    PreferenceManager.getDefaultSharedPreferences(activity)
        .unregisterOnSharedPreferenceChangeListener(preferenceListener)
    scrollable = null
    appBarLayout = null
  }

  /**
   * Binds navigation objects that would be scrolled by key events
   *
   * @param scrollable vertically scrollable instance
   * @param appBarLayout optional AppBarLayout that expands/collapses while scrolling
   */
  fun setScrollable(scrollable: Scrollable?, appBarLayout: AppBarLayout?) {
    this.scrollable = scrollable
    this.appBarLayout = appBarLayout
  }

  /**
   * Toggle [AppBarLayout] expand/collapse
   *
   * @param enabled true to enable, false otherwise
   */
  fun setAppBarEnabled(enabled: Boolean) {
    appBarEnabled = enabled
  }

  /**
   * Intercepts back pressed
   *
   * @param backInterceptor listener to back pressed event
   */
  fun setBackInterceptor(backInterceptor: BackInterceptor?) {
    this.backInterceptor = backInterceptor
  }

  /**
   * Calls from [Activity.onKeyDown] to delegate
   *
   * @param keyCode event key code
   * @param event key event
   * @return true if is intercepted as navigation, false otherwise
   */
  fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
    if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
      return backInterceptor != null && backInterceptor!!.onBackPressed()
    }
    if (!enabled) {
      return false
    }
    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
      event.startTracking()
      return true
    }
    return false
  }

  /**
   * Calls from [Activity.onKeyUp] to delegate
   *
   * @param keyCode event key code
   * @param event key event
   * @return true if is intercepted as navigation, false otherwise
   */
  fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
    if (!enabled) {
      return false
    }
    val notLongPress = (event.flags and KeyEvent.FLAG_CANCELED_LONG_PRESS) == 0
    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && notLongPress) {
      shortPress(DIRECTION_UP)
      return true
    } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && notLongPress) {
      shortPress(DIRECTION_DOWN)
      return true
    }
    return false
  }

  /**
   * Calls from [Activity.onKeyLongPress] to delegate
   *
   * @param keyCode event key code
   * @param event key event
   * @return true if is intercepted as navigation, false otherwise
   */
  fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
    if (!enabled) {
      return false
    }
    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
      longPress(DIRECTION_UP)
      return true
    } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
      longPress(DIRECTION_DOWN)
      return true
    }
    return false
  }

  private fun shortPress(@Direction direction: Int) {
    if (scrollable == null) {
      return
    }
    when (direction) {
      DIRECTION_UP ->
          if (!scrollable!!.scrollToPrevious() && appBarEnabled && appBarLayout != null) {
            appBarLayout!!.setExpanded(true, true)
          }

      DIRECTION_DOWN ->
          if (
              appBarEnabled &&
                  appBarLayout != null &&
                  appBarLayout!!.height == appBarLayout!!.bottom
          ) {
            appBarLayout!!.setExpanded(false, true)
          } else {
            scrollable!!.scrollToNext()
          }

      DIRECTION_NONE -> {}
      else -> {}
    }
  }

  private fun longPress(@Direction direction: Int) {
    when (direction) {
      DIRECTION_DOWN,
      DIRECTION_NONE -> {}
      DIRECTION_UP -> {
        if (appBarEnabled && appBarLayout != null) {
          appBarLayout!!.setExpanded(true, true)
        }
        if (scrollable != null) {
          scrollable!!.scrollToTop()
        }
      }

      else -> {}
    }
  }

  /** Helper class to navigate vertical RecyclerView */
  internal class RecyclerViewHelper(
      private val recyclerView: RecyclerView,
      @ScrollMode scrollMode: Int,
  ) : Scrollable {
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(SCROLL_ITEM, SCROLL_PAGE)
    internal annotation class ScrollMode

    @ScrollMode private val scrollMode: Int
    private var smoothScroll = true

    init {
      require(recyclerView.layoutManager is LinearLayoutManager) {
        "Only LinearLayoutManager supported"
      }
      this.scrollMode = scrollMode
    }

    override fun scrollToTop() {
      recyclerView.scrollToPosition(0)
    }

    override fun scrollToNext(): Boolean {
      val pos =
          if (scrollMode == SCROLL_ITEM) this.linearLayoutManager!!.findFirstVisibleItemPosition()
          else this.linearLayoutManager!!.findLastCompletelyVisibleItemPosition()
      val next = if (pos != RecyclerView.NO_POSITION) pos + 1 else RecyclerView.NO_POSITION
      if (next != RecyclerView.NO_POSITION && next < recyclerView.adapter!!.itemCount) {
        recyclerView.smoothScrollToPosition(next)
        return true
      } else {
        return false
      }
    }

    override fun scrollToPrevious(): Boolean {
      when (scrollMode) {
        SCROLL_ITEM -> {
          val pos = this.linearLayoutManager!!.findFirstVisibleItemPosition()
          val previous = if (pos != RecyclerView.NO_POSITION) pos - 1 else RecyclerView.NO_POSITION
          if (previous >= 0) {
            recyclerView.smoothScrollToPosition(previous)
            return true
          } else {
            return false
          }
        }

        SCROLL_PAGE ->
            if (this.linearLayoutManager!!.findFirstVisibleItemPosition() <= 0) {
              return false
            } else {
              recyclerView.smoothScrollBy(0, -recyclerView.height)
              return true
            }

        else -> {
          val pos = this.linearLayoutManager!!.findFirstVisibleItemPosition()
          val previous = if (pos != RecyclerView.NO_POSITION) pos - 1 else RecyclerView.NO_POSITION
          if (previous >= 0) {
            recyclerView.smoothScrollToPosition(previous)
            return true
          } else {
            return false
          }
        }
      }
    }

    fun smoothScrollEnabled(enabled: Boolean) {
      smoothScroll = enabled
    }

    val currentPosition: Int
      get() = // TODO handle last page item
          this.linearLayoutManager!!.findFirstVisibleItemPosition()

    fun scrollToPosition(position: Int): IntArray? {
      if (position >= 0 && position < recyclerView.adapter!!.itemCount) {
        if (!smoothScroll) {
          this.linearLayoutManager!!.scrollToPositionWithOffset(position, 0)
          return null
        }
        val first = this.linearLayoutManager!!.findFirstVisibleItemPosition()
        var lock: IntArray? = null
        if (abs(position - first) > 1) { // lock nothing if scroll to adjacent
          if (position < first) { // scroll up, lock lower part
            lock = intArrayOf(position, first - 1)
          } else if (position > first) { // scroll down, lock upper part
            lock = intArrayOf(first, position - 1)
          }
        }
        recyclerView.smoothScrollToPosition(position)
        return lock
      } else {
        return null
      }
    }

    private val linearLayoutManager: LinearLayoutManager?
      get() = recyclerView.layoutManager as LinearLayoutManager?

    companion object {
      const val SCROLL_ITEM: Int = 0
      const val SCROLL_PAGE: Int = 1
    }
  }

  /** Helper class to navigate vertical NestedScrollView */
  internal class NestedScrollViewHelper(private val scrollView: NestedScrollView) : Scrollable {
    override fun scrollToTop() {
      scrollView.smoothScrollTo(0, 0)
    }

    override fun scrollToNext(): Boolean {
      return scrollView.pageScroll(View.FOCUS_DOWN)
    }

    override fun scrollToPrevious(): Boolean {
      return scrollView.pageScroll(View.FOCUS_UP)
    }
  }

  /** Callback interface for back pressed events */
  interface BackInterceptor {
    /**
     * Fired upon back pressed
     *
     * @return true if handled, false otherwise
     */
    fun onBackPressed(): Boolean
  }

  companion object {
    private const val DIRECTION_NONE = 0
    private const val DIRECTION_UP = 1
    private const val DIRECTION_DOWN = 2
  }
}
