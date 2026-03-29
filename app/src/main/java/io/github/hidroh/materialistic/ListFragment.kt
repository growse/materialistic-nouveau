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

import androidx.lifecycle.Observer
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.annotation.Nullable
import com.google.android.material.snackbar.Snackbar

import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import javax.inject.Inject
import javax.inject.Named

import io.github.hidroh.materialistic.annotation.Synthetic
import io.github.hidroh.materialistic.data.AlgoliaClient
import io.github.hidroh.materialistic.data.AlgoliaPopularClient
import io.github.hidroh.materialistic.data.FavoriteManager
import io.github.hidroh.materialistic.data.Item
import io.github.hidroh.materialistic.data.ItemManager
import io.github.hidroh.materialistic.data.MaterialisticDatabase
import io.github.hidroh.materialistic.widget.StoryRecyclerViewAdapter
import rx.Scheduler

open class ListFragment : BaseListFragment() {

    const val EXTRA_ITEM_MANAGER: String = ListFragment.class.getName() + ".EXTRA_ITEM_MANAGER"
    const val EXTRA_FILTER: String = ListFragment.class.getName() + ".EXTRA_FILTER"
    private const val STATE_FILTER: String = "state:filter"
    private const val STATE_CACHE_MODE: String = "state:cacheMode"
    private val mPreferenceObservable: Preferences.Observable = new Preferences.Observable()
    private final Observer<Uri> mObserver = uri -> {
        if (uri == null) {
            return
        }
        int toastMessageResId = 0
        if (FavoriteManager.Companion.isAdded(uri)) {
            toastMessageResId = R.string.toast_saved
        } else if (FavoriteManager.Companion.isRemoved(uri)) {
            toastMessageResId = R.string.toast_removed
        }
        if (toastMessageResId == 0) {
            return
        }
        Snackbar.make(mRecyclerView, toastMessageResId, Snackbar.LENGTH_SHORT)
                .setAction(R.string.undo, v -> getAdapter().toggleSave(uri.getLastPathSegment()))
                .show()
    }
    private var mAdapter: StoryRecyclerViewAdapter? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    @Inject @Named(ActivityModule.HN) var mHnItemManager: ItemManager? = null
    @Inject @Named(ActivityModule.ALGOLIA) var mAlgoliaItemManager: ItemManager? = null
    @Inject @Named(ActivityModule.POPULAR) var mPopularItemManager: ItemManager? = null
    @Inject @Named(DataModule.IO_THREAD) var mIoThreadScheduler: Scheduler? = null
    private var mStoryListViewModel: StoryListViewModel? = null
    private var mErrorView: View? = null
    private var mEmptyView: View? = null
    private var mRefreshCallback: RefreshCallback? = null
    private var mFilter: String? = null
    private var mCacheMode: Int = ItemManager.MODE_DEFAULT

    interface RefreshCallback {
        fun onRefreshed()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is RefreshCallback) {
            mRefreshCallback = (RefreshCallback) context
        }
        mPreferenceObservable.subscribe(context, this::onPreferenceChanged,
                R.string.pref_highlight_updated,
                R.string.pref_username,
                R.string.pref_auto_viewed)
    }
    override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            mFilter = savedInstanceState.getString(STATE_FILTER)
            mCacheMode = savedInstanceState.getInt(STATE_CACHE_MODE)
        } else {
            mFilter = getArguments().getString(EXTRA_FILTER)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View {
        final View view = inflater.inflate(R.layout.fragment_list, container, false)
        mErrorView = view.findViewById(R.id.empty)
        mEmptyView = view.findViewById(R.id.empty_search)
        mRecyclerView = view.findViewById(R.id.recycler_view)
        mSwipeRefreshLayout = view.findViewById(R.id.swipe_layout)
        mSwipeRefreshLayout.setColorSchemeResources(R.color.white)
        mSwipeRefreshLayout.setProgressBackgroundColorSchemeResource(
                AppUtils.getThemedResId(getActivity(), R.attr.colorAccent))
        if (savedInstanceState == null) {
            mSwipeRefreshLayout.setRefreshing(true)
        }
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            mCacheMode = ItemManager.MODE_NETWORK
            getAdapter().setCacheMode(mCacheMode)
            refresh()
        })
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle) {
        super.onActivityCreated(savedInstanceState)
        MaterialisticDatabase.getInstance(getContext()).getLiveData().observe(getViewLifecycleOwner(), mObserver)
        String managerClassName = getArguments().getString(EXTRA_ITEM_MANAGER)
        ItemManager itemManager
        if (TextUtils.equals(managerClassName, AlgoliaClient::class.java.getName())) {
            itemManager = mAlgoliaItemManager
        } else if (TextUtils.equals(managerClassName, AlgoliaPopularClient::class.java.getName())) {
            itemManager = mPopularItemManager
        } else {
            itemManager = mHnItemManager
        }
        getAdapter().setHotThresHold(AppUtils.HOT_THRESHOLD_NORMAL)
        if (itemManager == mHnItemManager && mFilter != null) {
            switch (mFilter) {
                case ItemManager.BEST_FETCH_MODE:
                    getAdapter().setHotThresHold(AppUtils.HOT_THRESHOLD_HIGH)
                    break
                case ItemManager.NEW_FETCH_MODE:
                    getAdapter().setHotThresHold(AppUtils.HOT_THRESHOLD_LOW)
                    break
            }
        } else if (itemManager == mPopularItemManager) {
            getAdapter().setHotThresHold(AppUtils.HOT_THRESHOLD_HIGH)
        }
        getAdapter().initDisplayOptions(mRecyclerView)
        getAdapter().setCacheMode(mCacheMode)
        getAdapter().setUpdateListener((showAll, itemCount, actionClickListener) -> {
            if (showAll) {
                Snackbar.make(mRecyclerView,
                        getResources().getQuantityString(R.plurals.new_stories_count,
                                itemCount, itemCount),
                        Snackbar.LENGTH_LONG)
                        .setAction(R.string.show_me, actionClickListener)
                        .show()
            } else {
                final Snackbar snackbar = Snackbar.make(mRecyclerView,
                        getResources().getQuantityString(R.plurals.showing_new_stories,
                                itemCount, itemCount),
                        Snackbar.LENGTH_INDEFINITE)
                snackbar.setAction(R.string.show_all, actionClickListener).show()
            }

        })
        mStoryListViewModel = ViewModelProvider(this).get(StoryListViewModel::class.java)
        mStoryListViewModel.inject(itemManager, mIoThreadScheduler)
        mStoryListViewModel.getStories(mFilter, mCacheMode).observe(getViewLifecycleOwner(), itemLists -> {
            if (itemLists == null) {
                return
            }
            if (itemLists.first != null) {
                onItemsLoaded(itemLists.first)
            }
            if (itemLists.second != null) {
                onItemsLoaded(itemLists.second)
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_FILTER, mFilter)
        outState.putInt(STATE_CACHE_MODE, mCacheMode)
    }

    override fun onDetach() {
        mPreferenceObservable.unsubscribe(getActivity())
        mRefreshCallback = null
        super.onDetach()
    }

    fun filter(filter: String) {
        mFilter = filter
        getAdapter().setHighlightUpdated(false)
        mSwipeRefreshLayout.setRefreshing(true)
        refresh()
    }

    protected override fun getAdapter(): StoryRecyclerViewAdapter {
        if (mAdapter == null) {
            mAdapter = StoryRecyclerViewAdapter(getContext())
        }
        return mAdapter
    }

    private fun onPreferenceChanged(key: Int, contextChanged: Boolean) {
        if (!contextChanged) {
            getAdapter().initDisplayOptions(mRecyclerView)
        }
    }

    private fun refresh() {
        getAdapter().setShowAll(true)
        mStoryListViewModel.refreshStories(mFilter, mCacheMode)
    }

    @Synthetic
    fun onItemsLoaded(items: Array<Item>) {
        if (!isAttached()) {
            return
        }
        if (items == null) {
            mSwipeRefreshLayout.setRefreshing(false)
            if (getAdapter().getItems() == null || getAdapter().getItems().size() == 0) {
                // TODO make refreshing indicator visible in error view
                mEmptyView.setVisibility(View.GONE)
                mRecyclerView.setVisibility(View.INVISIBLE)
                mErrorView.setVisibility(View.VISIBLE)
            } else {
                Toast.makeText(getActivity(), getString(R.string.connection_error),
                        Toast.LENGTH_SHORT).show()
            }
        } else {
            getAdapter().setItems(items)
            if (items.length == 0) {
                mEmptyView.setVisibility(View.VISIBLE)
                mRecyclerView.setVisibility(View.INVISIBLE)
            } else {
                mEmptyView.setVisibility(View.GONE)
                mRecyclerView.setVisibility(View.VISIBLE)
            }
            mErrorView.setVisibility(View.GONE)
            mSwipeRefreshLayout.setRefreshing(false)
            if (mRefreshCallback != null) {
                mRefreshCallback.onRefreshed()
            }
        }
    }
}
