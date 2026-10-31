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
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import com.growse.android.io.github.hidroh.materialistic.accounts.UserServices
import com.growse.android.io.github.hidroh.materialistic.data.AlgoliaClient
import com.growse.android.io.github.hidroh.materialistic.data.AlgoliaPopularClient
import com.growse.android.io.github.hidroh.materialistic.data.FavoriteManager
import com.growse.android.io.github.hidroh.materialistic.data.Item
import com.growse.android.io.github.hidroh.materialistic.data.ItemManager
import com.growse.android.io.github.hidroh.materialistic.data.ItemManagerQualifiers
import com.growse.android.io.github.hidroh.materialistic.data.MaterialisticDatabase
import com.growse.android.io.github.hidroh.materialistic.data.SessionManager
import com.growse.android.io.github.hidroh.materialistic.di.DataModule
import com.growse.android.io.github.hidroh.materialistic.widget.PopupMenu
import com.growse.android.io.github.hidroh.materialistic.widget.StoryRecyclerViewAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Named
import rx.Scheduler

@AndroidEntryPoint
class ListFragment : BaseListFragment() {
  @Inject @Named(ItemManagerQualifiers.HN) lateinit var hnItemManager: ItemManager
  @Inject @Named(ItemManagerQualifiers.ALGOLIA) lateinit var algoliaItemManager: ItemManager
  @Inject @Named(ItemManagerQualifiers.POPULAR) lateinit var popularItemManager: ItemManager
  @Inject @Named(DataModule.IO_THREAD) lateinit var ioThreadScheduler: Scheduler
  @Inject lateinit var sessionManager: SessionManager
  @Inject lateinit var popupMenu: PopupMenu
  @Inject lateinit var alertDialogBuilder: AlertDialogBuilder<*>
  @Inject lateinit var userServices: UserServices
  @Inject lateinit var favoriteManager: FavoriteManager

  override val adapter: StoryRecyclerViewAdapter by lazy {
    StoryRecyclerViewAdapter(
        requireActivity(),
        hnItemManager,
        sessionManager,
        popupMenu,
        alertDialogBuilder,
        userServices,
        favoriteManager,
    )
  }

  private val preferenceObservable = Preferences.Observable()
  private val favoriteObserver =
      Observer<Uri?> { uri ->
        if (uri == null) {
          return@Observer
        }
        val messageResId =
            when {
              FavoriteManager.isAdded(uri) -> R.string.toast_saved
              FavoriteManager.isRemoved(uri) -> R.string.toast_removed
              else -> return@Observer
            }
        Snackbar.make(recyclerView, messageResId, Snackbar.LENGTH_SHORT)
            .setAction(R.string.undo) { adapter.toggleSave(uri.lastPathSegment) }
            .show()
      }
  private lateinit var swipeRefreshLayout: SwipeRefreshLayout
  private lateinit var errorView: View
  private lateinit var emptyView: View
  private lateinit var storyListViewModel: StoryListViewModel
  private var refreshCallback: RefreshCallback? = null
  private var filter: String = ""
  private var cacheMode = ItemManager.MODE_DEFAULT

  interface RefreshCallback {
    fun onRefreshed()
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    refreshCallback = context as? RefreshCallback
    preferenceObservable.subscribe(
        context,
        { _, contextChanged -> onPreferenceChanged(contextChanged) },
        R.string.pref_highlight_updated,
        R.string.pref_username,
        R.string.pref_auto_viewed,
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState != null) {
      filter = savedInstanceState.getString(STATE_FILTER) ?: ""
      cacheMode = savedInstanceState.getInt(STATE_CACHE_MODE)
    } else {
      filter = requireArguments().getString(EXTRA_FILTER) ?: ""
    }
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View {
    val view = inflater.inflate(R.layout.fragment_list, container, false)
    errorView = view.findViewById(R.id.empty)
    emptyView = view.findViewById(R.id.empty_search)
    recyclerView = view.findViewById(R.id.recycler_view)
    swipeRefreshLayout = view.findViewById(R.id.swipe_layout)
    swipeRefreshLayout.setColorSchemeResources(R.color.white)
    swipeRefreshLayout.setProgressBackgroundColorSchemeResource(
        AppUtils.getThemedResId(requireActivity(), R.attr.colorAccent)
    )
    if (savedInstanceState == null) {
      swipeRefreshLayout.isRefreshing = true
    }
    swipeRefreshLayout.setOnRefreshListener {
      cacheMode = ItemManager.MODE_NETWORK
      adapter.setCacheMode(cacheMode)
      refresh()
    }
    return view
  }

  @Deprecated("Deprecated in Java")
  override fun onActivityCreated(savedInstanceState: Bundle?) {
    @Suppress("DEPRECATION") super.onActivityCreated(savedInstanceState)
    MaterialisticDatabase.getInstance(requireContext())
        .liveData
        .observe(viewLifecycleOwner, favoriteObserver)
    val itemManager =
        when (requireArguments().getString(EXTRA_ITEM_MANAGER)) {
          AlgoliaClient::class.java.name -> algoliaItemManager
          AlgoliaPopularClient::class.java.name -> popularItemManager
          else -> hnItemManager
        }
    adapter.setHotThresHold(
        when {
          itemManager === popularItemManager -> AppUtils.HOT_THRESHOLD_HIGH
          itemManager !== hnItemManager -> AppUtils.HOT_THRESHOLD_NORMAL
          filter == ItemManager.BEST_FETCH_MODE -> AppUtils.HOT_THRESHOLD_HIGH
          filter == ItemManager.NEW_FETCH_MODE -> AppUtils.HOT_THRESHOLD_LOW
          else -> AppUtils.HOT_THRESHOLD_NORMAL
        }
    )
    adapter.initDisplayOptions(recyclerView)
    adapter.setCacheMode(cacheMode)
    adapter.setUpdateListener(
        object : StoryRecyclerViewAdapter.UpdateListener {
          override fun onUpdated(
              showAll: Boolean,
              itemCount: Int,
              actionClickListener: View.OnClickListener?,
          ) {
            if (showAll) {
              Snackbar.make(
                      recyclerView,
                      resources.getQuantityString(
                          R.plurals.new_stories_count,
                          itemCount,
                          itemCount,
                      ),
                      Snackbar.LENGTH_LONG,
                  )
                  .setAction(R.string.show_me, actionClickListener)
                  .show()
            } else {
              Snackbar.make(
                      recyclerView,
                      resources.getQuantityString(
                          R.plurals.showing_new_stories,
                          itemCount,
                          itemCount,
                      ),
                      Snackbar.LENGTH_INDEFINITE,
                  )
                  .setAction(R.string.show_all, actionClickListener)
                  .show()
            }
          }
        }
    )
    storyListViewModel = ViewModelProvider(this)[StoryListViewModel::class.java]
    storyListViewModel.inject(itemManager, ioThreadScheduler)
    storyListViewModel.getStories(filter, cacheMode)?.observe(viewLifecycleOwner) { itemLists ->
      if (itemLists == null) {
        return@observe
      }
      itemLists.first?.let { onItemsLoaded(it) }
      itemLists.second?.let { onItemsLoaded(it) }
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(STATE_FILTER, filter)
    outState.putInt(STATE_CACHE_MODE, cacheMode)
  }

  override fun onDetach() {
    preferenceObservable.unsubscribe(requireActivity())
    refreshCallback = null
    super.onDetach()
  }

  fun filter(filter: String) {
    this.filter = filter
    adapter.setHighlightUpdated(false)
    swipeRefreshLayout.isRefreshing = true
    refresh()
  }

  private fun onPreferenceChanged(contextChanged: Boolean) {
    if (!contextChanged) {
      adapter.initDisplayOptions(recyclerView)
    }
  }

  private fun refresh() {
    adapter.setShowAll(true)
    storyListViewModel.refreshStories(filter, cacheMode)
  }

  /** Shows [items], or an error state when they are null (the fetch failed). */
  private fun onItemsLoaded(items: Array<Item?>?) {
    if (!isAttached) {
      return
    }
    swipeRefreshLayout.isRefreshing = false
    if (items == null) {
      if (adapter.items.size() == 0) {
        // TODO make refreshing indicator visible in error view
        emptyView.isVisible = false
        recyclerView.visibility = View.INVISIBLE
        errorView.isVisible = true
      } else {
        Toast.makeText(activity, R.string.connection_error, Toast.LENGTH_SHORT).show()
      }
      return
    }
    adapter.setItems(items.filterNotNull())
    emptyView.isVisible = items.isEmpty()
    recyclerView.visibility = if (items.isEmpty()) View.INVISIBLE else View.VISIBLE
    errorView.isVisible = false
    refreshCallback?.onRefreshed()
  }

  companion object {
    val EXTRA_ITEM_MANAGER: String = ListFragment::class.java.name + ".EXTRA_ITEM_MANAGER"
    val EXTRA_FILTER: String = ListFragment::class.java.name + ".EXTRA_FILTER"
    private const val STATE_FILTER = "state:filter"
    private const val STATE_CACHE_MODE = "state:cacheMode"
  }
}
