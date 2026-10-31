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

import android.app.SearchManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.core.content.getSystemService
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.growse.android.io.github.hidroh.materialistic.accounts.UserServices
import com.growse.android.io.github.hidroh.materialistic.data.FavoriteManager
import com.growse.android.io.github.hidroh.materialistic.data.LocalItemManager
import com.growse.android.io.github.hidroh.materialistic.data.SyncScheduler
import com.growse.android.io.github.hidroh.materialistic.widget.FavoriteRecyclerViewAdapter
import com.growse.android.io.github.hidroh.materialistic.widget.PopupMenu
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FavouriteFragment :
    BaseListFragment(), FavoriteRecyclerViewAdapter.ActionModeDelegate, LocalItemManager.Observer {
  @Inject lateinit var favoriteManager: FavoriteManager
  @Inject lateinit var actionViewResolver: ActionViewResolver
  @Inject lateinit var alertDialogBuilder: AlertDialogBuilder<*>
  @Inject lateinit var popupMenu: PopupMenu
  @Inject lateinit var userServices: UserServices
  @Inject lateinit var syncScheduler: SyncScheduler

  override val adapter: FavoriteRecyclerViewAdapter by lazy {
    FavoriteRecyclerViewAdapter(
        requireActivity(),
        popupMenu,
        alertDialogBuilder,
        userServices,
        favoriteManager,
        syncScheduler,
        this,
    )
  }
  private var actionMode: ActionMode? = null
  private var filter = ""
  private var searchViewExpanded = false
  private lateinit var emptySearchView: View
  private lateinit var emptyView: View

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState != null) {
      filter = savedInstanceState.getString(STATE_FILTER) ?: ""
      searchViewExpanded = savedInstanceState.getBoolean(STATE_SEARCH_VIEW_EXPANDED)
    } else {
      filter = requireArguments().getString(EXTRA_FILTER) ?: ""
    }
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View {
    val view = inflater.inflate(R.layout.fragment_favorite, container, false)
    recyclerView = view.findViewById(R.id.recycler_view)
    emptySearchView = view.findViewById(R.id.empty_search)
    emptyView = view.findViewById(R.id.empty)
    emptyView.findViewById<View>(R.id.header_card_view).setOnLongClickListener {
      val bookmark = emptyView.findViewById<View>(R.id.bookmarked)
      bookmark.isInvisible = bookmark.isVisible
      true
    }
    emptyView.isInvisible = true
    return view
  }

  override fun onStart() {
    super.onStart()
    favoriteManager.attach(this, filter)
  }

  override fun createOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.menu_search, menu)
    createSearchView(menu.findItem(R.id.menu_search))
    if (adapter.itemCount > 0) {
      inflater.inflate(R.menu.menu_favorite, menu)
      super.createOptionsMenu(menu, inflater)
    }
  }

  override fun prepareOptionsMenu(menu: Menu) {
    // allow clearing filter if empty, or filter if non-empty
    menu.findItem(R.id.menu_search).isVisible = filter.isNotEmpty() || adapter.itemCount > 0
    if (adapter.itemCount > 0) {
      super.prepareOptionsMenu(menu)
    }
  }

  override fun optionsItemSelected(item: MenuItem): Boolean =
      when (item.itemId) {
        R.id.menu_clear -> {
          clear()
          true
        }
        R.id.menu_export -> {
          favoriteManager.export(requireActivity(), filter)
          true
        }
        else -> super.optionsItemSelected(item)
      }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(STATE_FILTER, filter)
    outState.putBoolean(STATE_SEARCH_VIEW_EXPANDED, searchViewExpanded)
  }

  override fun onStop() {
    super.onStop()
    favoriteManager.detach()
  }

  override fun onDetach() {
    super.onDetach()
    actionMode?.finish()
  }

  /** Filters list data by given [query]. */
  fun filter(query: String) {
    searchViewExpanded = false
    filter = query
    favoriteManager.attach(this, query)
  }

  override fun startActionMode(callback: ActionMode.Callback): Boolean {
    if (searchViewExpanded) {
      return false
    }
    if (actionMode == null) {
      actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(callback)
    }
    return true
  }

  override val isInActionMode: Boolean
    get() = actionMode != null && !searchViewExpanded

  override fun stopActionMode() {
    actionMode = null
  }

  override fun onChanged() {
    adapter.notifyChanged()
    if (!isDetached) {
      toggleEmptyView(adapter.itemCount == 0)
      requireActivity().invalidateOptionsMenu()
    }
  }

  private fun toggleEmptyView(isEmpty: Boolean) {
    val shown =
        when {
          !isEmpty -> null
          filter.isEmpty() -> emptyView
          else -> emptySearchView
        }
    emptyView.isInvisible = shown != emptyView
    emptySearchView.isInvisible = shown != emptySearchView
    shown?.bringToFront()
  }

  private fun createSearchView(menuSearch: MenuItem) {
    val searchView = actionViewResolver.getActionView(menuSearch) as SearchView
    searchView.apply {
      queryHint = getString(R.string.hint_search_saved_stories)
      setSearchableInfo(
          requireContext()
              .getSystemService<SearchManager>()
              ?.getSearchableInfo(requireActivity().componentName)
      )
      isIconified = !searchViewExpanded
      setQuery(filter, false)
      setOnSearchClickListener { v ->
        searchViewExpanded = true
        v.requestFocus()
      }
      setOnCloseListener {
        // trigger a dummy empty search intent, as empty query does not get submitted
        setQuery(FavoriteActivity.EMPTY_QUERY, true)
        false
      }
    }
  }

  private fun clear() {
    alertDialogBuilder
        .init(requireActivity())
        .setMessage(R.string.confirm_clear)
        .setPositiveButton(android.R.string.ok) { _, _ ->
          favoriteManager.clear(requireActivity(), filter)
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
  }

  companion object {
    val EXTRA_FILTER = FavouriteFragment::class.java.name + ".EXTRA_FILTER"
    private const val STATE_FILTER = "state:filter"
    private const val STATE_SEARCH_VIEW_EXPANDED = "state:searchViewExpanded"
  }
}
