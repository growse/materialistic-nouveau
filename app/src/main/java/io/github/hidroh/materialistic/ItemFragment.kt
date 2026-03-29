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

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup

import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.Arrays

import javax.inject.Inject
import javax.inject.Named

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.github.hidroh.materialistic.annotation.Synthetic
import io.github.hidroh.materialistic.data.Item
import io.github.hidroh.materialistic.data.ItemManager
import io.github.hidroh.materialistic.data.ResponseListener
import io.github.hidroh.materialistic.data.WebItem
import io.github.hidroh.materialistic.widget.CommentItemDecoration
import io.github.hidroh.materialistic.widget.ItemRecyclerViewAdapter
import io.github.hidroh.materialistic.widget.MultiPageItemRecyclerViewAdapter
import io.github.hidroh.materialistic.widget.SinglePageItemRecyclerViewAdapter
import io.github.hidroh.materialistic.widget.SnappyLinearLayoutManager

open class ItemFragment : LazyLoadFragment(), Scrollable, Navigable {

    const val EXTRA_ITEM: String = ItemFragment.class.getName() + ".EXTRA_ITEM"
    const val EXTRA_CACHE_MODE: String = ItemFragment.class.getName() + ".EXTRA_CACHE_MODE"
    private const val STATE_ITEM: String = "state:item"
    private const val STATE_ITEM_ID: String = "state:itemId"
    private const val STATE_ADAPTER_ITEMS: String = "state:adapterItems"
    private const val STATE_CACHE_MODE: String = "state:cacheMode"
    private var mRecyclerView: RecyclerView? = null
    private var mEmptyView: View? = null
    private var mItem: Item? = null
    private var mItemId: String? = null
    @Inject @Named(ActivityModule.HN) var mItemManager: ItemManager? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapterItems: SinglePageItemRecyclerViewAdapter.SavedState? = null
    private var mAdapter: ItemRecyclerViewAdapter? = null
    private var mScrollableHelper: KeyDelegate.RecyclerViewHelper? = null
    private @ItemManager.CacheMode int mCacheMode = ItemManager.MODE_DEFAULT
    private val mPreferenceObservable: Preferences.Observable = new Preferences.Observable()
    private var mItemDecoration: CommentItemDecoration? = null
    private var mFragmentView: View? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mPreferenceObservable.subscribe(context, this::onPreferenceChanged,
                R.string.pref_comment_display,
                R.string.pref_max_lines,
                R.string.pref_username,
                R.string.pref_line_height,
                R.string.pref_color_code,
                R.string.pref_thread_indicator,
                R.string.pref_font,
                R.string.pref_text_size,
                R.string.pref_smooth_scroll,
                R.string.pref_color_code_opacity)
    }

    override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        if (savedInstanceState != null) {
            mCacheMode = savedInstanceState.getInt(STATE_CACHE_MODE, ItemManager.MODE_DEFAULT)
            mItem = savedInstanceState.getParcelable(STATE_ITEM)
            mItemId = savedInstanceState.getString(STATE_ITEM_ID)
            mAdapterItems = savedInstanceState.getParcelable(STATE_ADAPTER_ITEMS)
        } else {
            mCacheMode = getArguments().getInt(EXTRA_CACHE_MODE, ItemManager.MODE_DEFAULT)
            WebItem item = getArguments().getParcelable(EXTRA_ITEM)
            if (item is Item) {
                mItem = (Item) item
            }
            mItemId = item != null ? item.getId() : null
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View {
        if (isNewInstance()) {
            mFragmentView = inflater.inflate(R.layout.fragment_item, container, false)
            mEmptyView = mFragmentView.findViewById(R.id.empty)
            mRecyclerView = (RecyclerView) mFragmentView.findViewById(R.id.recycler_view)
            mRecyclerView.setLayoutManager(SnappyLinearLayoutManager(getActivity(), true))
            mItemDecoration = CommentItemDecoration(getActivity())
            mRecyclerView.addItemDecoration(mItemDecoration)
            mSwipeRefreshLayout = (SwipeRefreshLayout) mFragmentView.findViewById(R.id.swipe_layout)
            mSwipeRefreshLayout.setColorSchemeResources(R.color.white)
            mSwipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.redA200)
            mSwipeRefreshLayout.setOnRefreshListener(() -> {
                if (TextUtils.isEmpty(mItemId)) {
                    return
                }
                mCacheMode = ItemManager.MODE_NETWORK
                if (mAdapter != null) {
                    mAdapter.setCacheMode(mCacheMode)
                }
                loadKidData()
            })
        }
        return mFragmentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle) {
        super.onViewCreated(view, savedInstanceState)
        if (isNewInstance()) {
            mScrollableHelper = KeyDelegate.RecyclerViewHelper(mRecyclerView,
                    KeyDelegate.RecyclerViewHelper.SCROLL_ITEM)
            mScrollableHelper.smoothScrollEnabled(Preferences.smoothScrollEnabled(getActivity()))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getItemId() == R.id.menu_comments) {
            showPreferences()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(STATE_ITEM, mItem)
        outState.putString(STATE_ITEM_ID, mItemId)
        outState.putParcelable(STATE_ADAPTER_ITEMS, mAdapterItems)
        outState.putInt(STATE_CACHE_MODE, mCacheMode)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mAdapter != null) {
            mAdapter.detach(getActivity(), mRecyclerView)
        }
    }

    override fun onDetach() {
        super.onDetach()
        mPreferenceObservable.unsubscribe(getActivity())
    }

    override fun scrollToTop() {
        mScrollableHelper.scrollToTop()
    }

    override fun scrollToNext(): Boolean {
        return mScrollableHelper.scrollToNext()
    }

    override fun scrollToPrevious(): Boolean {
        return mScrollableHelper.scrollToPrevious()
    }

    override fun onNavigate(direction: Int) {
        if (mAdapter == null) { // no kids
            return
        }
        mAdapter.getNextPosition(mScrollableHelper.getCurrentPosition(),
                direction,
                position -> mAdapter.lockBinding(mScrollableHelper.scrollToPosition(position)))
    }

    protected override fun load() {
        if (mItem != null) {
            bindKidData()
        } else if (!TextUtils.isEmpty(mItemId)) {
            loadKidData()
        }
    }

    protected override fun createOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_item_view, menu)
    }

    private fun loadKidData() {
        mItemManager.getItem(mItemId, mCacheMode, ItemResponseListener(this))
    }

    fun onItemLoaded(item: Item) {
        mSwipeRefreshLayout.setRefreshing(false)
        if (item != null) {
            mAdapterItems = null
            mItem = item
            notifyItemLoaded(item)
            bindKidData()
        }
    }

    private fun bindKidData() {
        if (mItem == null || mItem.getKidCount() == 0) {
            mEmptyView.setVisibility(View.VISIBLE)
            return
        }

        mEmptyView.setVisibility(View.GONE)
        String displayOption = Preferences.getCommentDisplayOption(getActivity())
        if (Preferences.isSinglePage(getActivity(), displayOption)) {
            boolean autoExpand = Preferences.isAutoExpand(getActivity(), displayOption)
            // if collapsed or no saved state then start a fresh (adapter items all collapsed)
            if (!autoExpand || mAdapterItems == null) {
                mAdapterItems = SinglePageItemRecyclerViewAdapter.SavedState(
                        ArrayList<>(Arrays.asList(mItem.getKidItems())))
            }
            mAdapter = SinglePageItemRecyclerViewAdapter(mItemManager, mAdapterItems, autoExpand)
        } else {
            mAdapter = MultiPageItemRecyclerViewAdapter(mItemManager, mItem.getKidItems())
        }
        mAdapter.setCacheMode(mCacheMode)
        mAdapter.initDisplayOptions(getActivity())
        mAdapter.attach(getActivity(), mRecyclerView)
        mRecyclerView.setAdapter(mAdapter)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun onPreferenceChanged(key: Int, contextChanged: Boolean) {
        if (contextChanged || key == R.string.pref_comment_display) {
            load()
        } else if (mAdapter != null) {
            mScrollableHelper.smoothScrollEnabled(Preferences.smoothScrollEnabled(getActivity()))
            mItemDecoration.setColorCodeEnabled(Preferences.colorCodeEnabled(getActivity()))
            mItemDecoration.setThreadIndicatorEnabled(Preferences.threadIndicatorEnabled(getActivity()))
            mAdapter.initDisplayOptions(getActivity())
            mAdapter.notifyDataSetChanged()
        }
    }

    private fun showPreferences() {
        Bundle args = Bundle()
        args.putInt(PopupSettingsFragment.EXTRA_TITLE, R.string.font_options)
        args.putInt(PopupSettingsFragment.EXTRA_SUMMARY, R.string.pull_up_hint)
        args.putIntArray(PopupSettingsFragment.EXTRA_XML_PREFERENCES, new int[]{
                R.xml.preferences_font,
                R.xml.preferences_comments})
        ((DialogFragment) Fragment.instantiate(getActivity(),
                PopupSettingsFragment::class.java.getName(), args))
                .show(getFragmentManager(), PopupSettingsFragment::class.java.getName())
    }

    private fun notifyItemLoaded(item: Item) {
        if (getActivity() is ItemChangedListener) {
            ((ItemChangedListener) getActivity()).onItemChanged(item)
        }
    }

    open class ItemResponseListener : ResponseListener<Item> {
        private var mItemFragment: WeakReference<ItemFragment>? = null

        @Synthetic
        constructor(itemFragment: ItemFragment) {
            mItemFragment = WeakReference<>(itemFragment)
        }

        override fun onResponse(response: Item) {
            if (mItemFragment.get() != null && mItemFragment.get().isAttached()) {
                mItemFragment.get().onItemLoaded(response)
            }
        }

        override fun onError(errorMessage: String) {
            if (mItemFragment.get() != null && mItemFragment.get().isAttached()) {
                mItemFragment.get().onItemLoaded(null)
            }
        }
    }

    interface ItemChangedListener {
        fun onItemChanged(item: Item)
    }
}
