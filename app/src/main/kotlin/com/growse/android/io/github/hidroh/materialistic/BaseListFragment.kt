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
package com.growse.android.io.github.hidroh.materialistic

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.growse.android.io.github.hidroh.materialistic.KeyDelegate.RecyclerViewHelper
import com.growse.android.io.github.hidroh.materialistic.widget.ListRecyclerViewAdapter
import com.growse.android.io.github.hidroh.materialistic.widget.SnappyLinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
abstract class BaseListFragment : BaseFragment(), Scrollable {
  @Inject lateinit var customTabsDelegate: CustomTabsDelegate

  /** Must be assigned by subclasses in `onCreateView()`. */
  protected lateinit var recyclerView: RecyclerView
  private lateinit var scrollableHelper: RecyclerViewHelper
  private val preferenceObservable = Preferences.Observable()

  protected abstract val adapter: ListRecyclerViewAdapter<*, *>

  override fun onAttach(context: Context) {
    super.onAttach(context)
    preferenceObservable.subscribe(
        context,
        { key, contextChanged -> onPreferenceChanged(key, contextChanged) },
        R.string.pref_font,
        R.string.pref_text_size,
        R.string.pref_list_item_view,
    )
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    recyclerView.layoutManager = SnappyLinearLayoutManager(requireActivity(), false)
    val initialPaddingBottom = recyclerView.paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(recyclerView) { v, windowInsets ->
      val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
      v.setPadding(
          v.paddingLeft,
          v.paddingTop,
          v.paddingRight,
          initialPaddingBottom + systemBars.bottom,
      )
      windowInsets
    }
    ViewCompat.requestApplyInsets(recyclerView)
    val verticalMargin = resources.getDimensionPixelSize(R.dimen.cardview_vertical_margin)
    val horizontalMargin = resources.getDimensionPixelSize(R.dimen.cardview_horizontal_margin)
    val divider = resources.getDimensionPixelSize(R.dimen.divider)
    recyclerView.addItemDecoration(
        object : RecyclerView.ItemDecoration() {
          override fun getItemOffsets(
              outRect: Rect,
              view: View,
              parent: RecyclerView,
              state: RecyclerView.State,
          ) {
            if (adapter.isCardViewEnabled) {
              outRect.set(horizontalMargin, verticalMargin, horizontalMargin, 0)
            } else {
              outRect.set(0, 0, 0, divider)
            }
          }
        }
    )
  }

  @Deprecated("Deprecated in Java")
  override fun onActivityCreated(savedInstanceState: Bundle?) {
    @Suppress("DEPRECATION") super.onActivityCreated(savedInstanceState)
    if (savedInstanceState != null) {
      adapter.restoreState(savedInstanceState.getBundle(STATE_ADAPTER))
    }
    adapter.isCardViewEnabled = Preferences.isListItemCardView(requireActivity())
    adapter.setCustomTabsDelegate(customTabsDelegate)
    recyclerView.adapter = adapter
    scrollableHelper = RecyclerViewHelper(recyclerView, RecyclerViewHelper.SCROLL_PAGE)
  }

  override fun createOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.menu_list, menu)
  }

  override fun optionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == R.id.menu_list) {
      showPreferences()
      return true
    }
    return super.optionsItemSelected(item)
  }

  private fun showPreferences() {
    PopupSettingsFragment()
        .apply {
          arguments =
              bundleOf(
                  PopupSettingsFragment.EXTRA_TITLE to R.string.list_display_options,
                  PopupSettingsFragment.EXTRA_SUMMARY to R.string.pull_up_hint,
                  PopupSettingsFragment.EXTRA_XML_PREFERENCES to
                      intArrayOf(R.xml.preferences_font, R.xml.preferences_list),
              )
        }
        .show(parentFragmentManager, PopupSettingsFragment::class.java.name)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putBundle(STATE_ADAPTER, adapter.saveState())
  }

  override fun onDetach() {
    super.onDetach()
    preferenceObservable.unsubscribe(requireActivity())
    recyclerView.adapter = null // force adapter detach
  }

  override fun scrollToTop() {
    scrollableHelper.scrollToTop()
  }

  override fun scrollToNext(): Boolean = scrollableHelper.scrollToNext()

  override fun scrollToPrevious(): Boolean = scrollableHelper.scrollToPrevious()

  private fun onPreferenceChanged(key: Int, contextChanged: Boolean) {
    if (contextChanged) {
      recyclerView.adapter = adapter
    } else if (key == R.string.pref_list_item_view) {
      adapter.isCardViewEnabled = Preferences.isListItemCardView(requireActivity())
    }
  }

  companion object {
    private const val STATE_ADAPTER = "state:adapter"
  }
}
