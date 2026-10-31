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

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.growse.android.io.github.hidroh.materialistic.KeyDelegate.RecyclerViewHelper
import com.growse.android.io.github.hidroh.materialistic.accounts.UserServices
import com.growse.android.io.github.hidroh.materialistic.data.Item
import com.growse.android.io.github.hidroh.materialistic.data.ItemManager
import com.growse.android.io.github.hidroh.materialistic.data.ItemManagerQualifiers
import com.growse.android.io.github.hidroh.materialistic.data.ResponseListener
import com.growse.android.io.github.hidroh.materialistic.data.SyncScheduler
import com.growse.android.io.github.hidroh.materialistic.data.WebItem
import com.growse.android.io.github.hidroh.materialistic.widget.CommentItemDecoration
import com.growse.android.io.github.hidroh.materialistic.widget.ItemRecyclerViewAdapter
import com.growse.android.io.github.hidroh.materialistic.widget.MultiPageItemRecyclerViewAdapter
import com.growse.android.io.github.hidroh.materialistic.widget.PopupMenu
import com.growse.android.io.github.hidroh.materialistic.widget.SinglePageItemRecyclerViewAdapter
import com.growse.android.io.github.hidroh.materialistic.widget.SnappyLinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class ItemFragment : LazyLoadFragment(), Scrollable, Navigable {
  @Inject @Named(ItemManagerQualifiers.HN) lateinit var itemManager: ItemManager
  @Inject lateinit var userServices: UserServices
  @Inject lateinit var popupMenu: PopupMenu
  @Inject lateinit var alertDialogBuilder: AlertDialogBuilder<*>
  @Inject lateinit var resourcesProvider: ResourcesProvider
  @Inject lateinit var syncScheduler: SyncScheduler

  // views are kept across configuration changes when the instance is retained
  private lateinit var fragmentView: View
  private lateinit var recyclerView: RecyclerView
  private lateinit var emptyView: View
  private lateinit var swipeRefreshLayout: SwipeRefreshLayout
  private lateinit var itemDecoration: CommentItemDecoration
  private lateinit var scrollableHelper: RecyclerViewHelper

  private var item: Item? = null
  private var itemId: String = ""
  /** Set for the duration of a user-initiated refresh, so a saved story can be re-synced. */
  private var refreshing = false
  private var adapterItems: SinglePageItemRecyclerViewAdapter.SavedState? = null
  /** Null until the item's comments have been bound, or when it has none. */
  private var adapter: ItemRecyclerViewAdapter<*>? = null
  @ItemManager.CacheMode private var cacheMode = ItemManager.MODE_DEFAULT
  private val preferenceObservable = Preferences.Observable()

  override fun onAttach(context: Context) {
    super.onAttach(context)
    preferenceObservable.subscribe(
        context,
        { key, contextChanged -> onPreferenceChanged(key, contextChanged) },
        R.string.pref_comment_display,
        R.string.pref_max_lines,
        R.string.pref_username,
        R.string.pref_line_height,
        R.string.pref_color_code,
        R.string.pref_thread_indicator,
        R.string.pref_font,
        R.string.pref_text_size,
        R.string.pref_smooth_scroll,
        R.string.pref_color_code_opacity,
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState != null) {
      cacheMode = savedInstanceState.getInt(STATE_CACHE_MODE, ItemManager.MODE_DEFAULT)
      item = BundleCompat.getParcelable(savedInstanceState, STATE_ITEM, Item::class.java)
      itemId = savedInstanceState.getString(STATE_ITEM_ID)!!
      adapterItems =
          BundleCompat.getParcelable(
              savedInstanceState,
              STATE_ADAPTER_ITEMS,
              SinglePageItemRecyclerViewAdapter.SavedState::class.java,
          )
    } else {
      val args = requireArguments()
      cacheMode = args.getInt(EXTRA_CACHE_MODE, ItemManager.MODE_DEFAULT)
      val webItem = BundleCompat.getParcelable(args, EXTRA_ITEM, WebItem::class.java)
      item = webItem as? Item
      itemId = webItem!!.id
    }
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View {
    if (isNewInstance) {
      fragmentView = inflater.inflate(R.layout.fragment_item, container, false)
      emptyView = fragmentView.findViewById(R.id.empty)
      recyclerView = fragmentView.findViewById(R.id.recycler_view)
      recyclerView.layoutManager = SnappyLinearLayoutManager(requireActivity(), true)
      itemDecoration = CommentItemDecoration(requireActivity())
      recyclerView.addItemDecoration(itemDecoration)
      swipeRefreshLayout = fragmentView.findViewById(R.id.swipe_layout)
      swipeRefreshLayout.setColorSchemeResources(R.color.white)
      swipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.redA200)
      swipeRefreshLayout.setOnRefreshListener {
        if (itemId.isEmpty()) {
          return@setOnRefreshListener
        }
        cacheMode = ItemManager.MODE_NETWORK
        refreshing = true
        adapter?.cacheMode = cacheMode
        loadKidData()
      }
    }
    return fragmentView
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    if (isNewInstance) {
      scrollableHelper = RecyclerViewHelper(recyclerView, RecyclerViewHelper.SCROLL_ITEM)
      scrollableHelper.smoothScrollEnabled(Preferences.smoothScrollEnabled(requireActivity()))
    }
  }

  override fun createOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.menu_item_view, menu)
  }

  override fun optionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == R.id.menu_comments) {
      showPreferences()
      return true
    }
    return super.optionsItemSelected(item)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putParcelable(STATE_ITEM, item)
    outState.putString(STATE_ITEM_ID, itemId)
    outState.putParcelable(STATE_ADAPTER_ITEMS, adapterItems)
    outState.putInt(STATE_CACHE_MODE, cacheMode)
  }

  override fun onDestroy() {
    super.onDestroy()
    adapter?.detach(requireActivity(), recyclerView)
  }

  override fun onDetach() {
    super.onDetach()
    preferenceObservable.unsubscribe(requireActivity())
  }

  override fun scrollToTop() {
    scrollableHelper.scrollToTop()
  }

  override fun scrollToNext(): Boolean = scrollableHelper.scrollToNext()

  override fun scrollToPrevious(): Boolean = scrollableHelper.scrollToPrevious()

  override fun onNavigate(direction: Int) {
    val adapter = adapter ?: return // no kids
    adapter.getNextPosition(scrollableHelper.currentPosition, direction) { position ->
      adapter.lockBinding(scrollableHelper.scrollToPosition(position))
    }
  }

  override fun load() {
    if (item != null) {
      bindKidData()
    } else if (itemId.isNotEmpty()) {
      loadKidData()
    }
  }

  private fun loadKidData() {
    itemManager.getItem(itemId, cacheMode, ItemResponseListener(this))
  }

  private fun onItemLoaded(item: Item?) {
    swipeRefreshLayout.isRefreshing = false
    val refreshed = refreshing
    refreshing = false
    if (item == null) {
      return
    }
    adapterItems = null
    this.item = item
    (activity as? ItemChangedListener)?.onItemChanged(item)
    bindKidData()
    if (refreshed && item.isFavorite && isAdded) {
      // the story came back from the network, so we are online and its kid list is
      // current - bring the offline copy of the thread up to date with it
      syncScheduler.scheduleRefresh(requireActivity(), item.id)
    }
  }

  private fun bindKidData() {
    val item = item
    if (item == null || item.kidCount == 0) {
      emptyView.isVisible = true
      return
    }
    emptyView.isVisible = false
    val displayOption = Preferences.getCommentDisplayOption(requireActivity())
    val adapter =
        if (Preferences.isSinglePage(requireActivity(), displayOption)) {
          val autoExpand = Preferences.isAutoExpand(requireActivity(), displayOption)
          // if collapsed or no saved state then start a fresh (adapter items all collapsed)
          val adapterItems =
              adapterItems?.takeIf { autoExpand }
                  ?: SinglePageItemRecyclerViewAdapter.SavedState(ArrayList(item.kidItems.asList()))
          this.adapterItems = adapterItems
          SinglePageItemRecyclerViewAdapter(
              itemManager,
              userServices,
              popupMenu,
              alertDialogBuilder,
              resourcesProvider,
              adapterItems,
              autoExpand,
          )
        } else {
          MultiPageItemRecyclerViewAdapter(
              itemManager,
              userServices,
              popupMenu,
              alertDialogBuilder,
              item.kidItems,
          )
        }
    this.adapter = adapter
    adapter.cacheMode = cacheMode
    adapter.initDisplayOptions(requireActivity())
    adapter.attach(requireActivity(), recyclerView)
    recyclerView.adapter = adapter
  }

  @SuppressLint("NotifyDataSetChanged")
  private fun onPreferenceChanged(key: Int, contextChanged: Boolean) {
    if (contextChanged || key == R.string.pref_comment_display) {
      load()
      return
    }
    val adapter = adapter ?: return
    val context = requireActivity()
    scrollableHelper.smoothScrollEnabled(Preferences.smoothScrollEnabled(context))
    itemDecoration.setColorCodeEnabled(Preferences.colorCodeEnabled(context))
    itemDecoration.setThreadIndicatorEnabled(Preferences.threadIndicatorEnabled(context))
    adapter.initDisplayOptions(context)
    adapter.notifyDataSetChanged()
  }

  private fun showPreferences() {
    PopupSettingsFragment()
        .apply {
          arguments =
              bundleOf(
                  PopupSettingsFragment.EXTRA_TITLE to R.string.font_options,
                  PopupSettingsFragment.EXTRA_SUMMARY to R.string.pull_up_hint,
                  PopupSettingsFragment.EXTRA_XML_PREFERENCES to
                      intArrayOf(R.xml.preferences_font, R.xml.preferences_comments),
              )
        }
        .show(parentFragmentManager, PopupSettingsFragment::class.java.name)
  }

  private class ItemResponseListener(itemFragment: ItemFragment) : ResponseListener<Item?> {
    private val fragment = WeakReference(itemFragment)

    override fun onResponse(response: Item?) {
      fragment.get()?.takeIf { it.isAttached }?.onItemLoaded(response)
    }

    override fun onError(errorMessage: String?) {
      fragment.get()?.takeIf { it.isAttached }?.onItemLoaded(null)
    }
  }

  internal interface ItemChangedListener {
    fun onItemChanged(item: Item)
  }

  companion object {
    @JvmField val EXTRA_ITEM: String = ItemFragment::class.java.name + ".EXTRA_ITEM"
    @JvmField val EXTRA_CACHE_MODE: String = ItemFragment::class.java.name + ".EXTRA_CACHE_MODE"
    private const val STATE_ITEM = "state:item"
    private const val STATE_ITEM_ID = "state:itemId"
    private const val STATE_ADAPTER_ITEMS = "state:adapterItems"
    private const val STATE_CACHE_MODE = "state:cacheMode"
  }
}
