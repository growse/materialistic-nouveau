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
import android.app.SearchManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View

import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout

import javax.inject.Inject

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.github.hidroh.materialistic.annotation.Synthetic
import io.github.hidroh.materialistic.data.ItemManager
import io.github.hidroh.materialistic.data.SessionManager
import io.github.hidroh.materialistic.data.WebItem
import io.github.hidroh.materialistic.widget.ItemPagerAdapter
import io.github.hidroh.materialistic.widget.NavFloatingActionButton
import io.github.hidroh.materialistic.widget.PopupMenu
import io.github.hidroh.materialistic.widget.ViewPager

/**
 * List activity that renders alternative layouts for portrait/landscape
 */
abstract class BaseListActivity : DrawerActivity(), MultiPaneListener {

    protected static final String LIST_FRAGMENT_TAG = BaseListActivity::class.java.getName() +
            ".LIST_FRAGMENT_TAG"
    private const val STATE_SELECTED_ITEM: String = "state:selectedItem"
    private const val STATE_FULLSCREEN: String = "state:fullscreen"
    private var mIsMultiPane: Boolean = false
    protected var mSelectedItem: WebItem? = null
    private var mStoryViewMode: Preferences.StoryViewMode? = null
    private var mExternalBrowser: Boolean = false
    private var mViewPager: ViewPager? = null
    @Inject var mActionViewResolver: ActionViewResolver? = null
    @Inject var mPopupMenu: PopupMenu? = null
    @Inject var mSessionManager: SessionManager? = null
    @Inject var mCustomTabsDelegate: CustomTabsDelegate? = null
    @Inject var mKeyDelegate: KeyDelegate? = null
    private var mAppBar: AppBarLayout? = null
    private var mTabLayout: TabLayout? = null
    private var mReplyButton: FloatingActionButton? = null
    private var mNavButton: NavFloatingActionButton? = null
    private var mListView: View? = null
    var mFullscreen: Boolean = false
    private var mMultiWindowEnabled: Boolean = false
    private val mPreferenceObservable: Preferences.Observable = new Preferences.Observable()
    private fun BroadcastReceiver(): BroadcastReceiver mReceiver = new {
        public void onReceive(Context context, Intent intent) {
            mFullscreen = intent.getBooleanExtra(WebFragment.EXTRA_FULLSCREEN, false)
            setFullscreen()
        }
    }
    private var mAdapter: ItemPagerAdapter? = null

    @SuppressWarnings("ConstantConditions")
    protected override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        setTitle(getDefaultTitle())
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar))
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE)
        findViewById(R.id.toolbar).setOnClickListener(v -> {
            val scrollable = getScrollableList()
            if (scrollable != null) {
                scrollable.scrollToTop()
            }
        })
        mAppBar = (AppBarLayout) findViewById(R.id.appbar)
        mIsMultiPane = getResources().getBoolean(R.bool.multi_pane)
        if (mIsMultiPane) {
            LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver,
                    IntentFilter(WebFragment.ACTION_FULLSCREEN))
            mListView = findViewById(android.R.id.list)
            mTabLayout = (TabLayout) findViewById(R.id.tab_layout)
            mTabLayout.setVisibility(View.GONE)
            mViewPager = (ViewPager) findViewById(R.id.content)
            mViewPager.setVisibility(View.GONE)
            mReplyButton = (FloatingActionButton) findViewById(R.id.reply_button)
            mNavButton = (NavFloatingActionButton) findViewById(R.id.navigation_button)
            mNavButton.setNavigable(direction ->
                    // if callback is fired navigable should not be null
                    ((Navigable) ((ItemPagerAdapter) mViewPager.getAdapter()).getItem(0))
                            .onNavigate(direction))
            AppUtils.toggleFab(mNavButton, false)
            AppUtils.toggleFab(mReplyButton, false)
        }
        mMultiWindowEnabled = Preferences.multiWindowEnabled(this)
        mStoryViewMode = Preferences.getDefaultStoryView(this)
        mExternalBrowser = Preferences.externalBrowserEnabled(this)
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.list,
                            instantiateListFragment(),
                            LIST_FRAGMENT_TAG)
                    .commit()
        } else {
            mSelectedItem = savedInstanceState.getParcelable(STATE_SELECTED_ITEM)
            mFullscreen = savedInstanceState.getBoolean(STATE_FULLSCREEN)
            if (mIsMultiPane) {
                openMultiPaneItem()
            } else {
                unbindViewPager()
            }
        }
        mPreferenceObservable.subscribe(this, this::onPreferenceChanged,
                R.string.pref_navigation,
                R.string.pref_external,
                R.string.pref_story_display,
                R.string.pref_multi_window)
    }

    protected override fun onPostCreate(savedInstanceState: Bundle) {
        super.onPostCreate(savedInstanceState)
        if (!Preferences.isReleaseNotesSeen(this)) {
            val snackbar = Snackbar.make(findViewById(R.id.content_frame),
                    R.string.hint_update, Snackbar.LENGTH_INDEFINITE)
            snackbar.setAction(R.string.title_activity_release,
                    v -> {
                        snackbar.dismiss()
                        startActivity(Intent(BaseListActivity.this, ReleaseNotesActivity::class.java))
                    })
                    .setActionTextColor(ContextCompat.getColor(this, R.color.orange500))
                    .addCallback(Snackbar.Callback() {
                        public void onDismissed(Snackbar transientBottomBar, int event) {
                            Preferences.setReleaseNotesSeen(BaseListActivity.this)
                        }
                    })
                    .show()
        }
    }

    protected override fun onStart() {
        super.onStart()
        mCustomTabsDelegate.bindCustomTabsService(this)
        mKeyDelegate.attach(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (mIsMultiPane) {
            getMenuInflater().inflate(R.menu.menu_item_compact, menu)
        }
        if (isSearchable()) {
            getMenuInflater().inflate(R.menu.menu_search, menu)
            val menuSearch = menu.findItem(R.id.menu_search)
            val searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE)
            val searchView = (SearchView) mActionViewResolver.getActionView(menuSearch)
            searchView.setSearchableInfo(searchManager.getSearchableInfo(
                    ComponentName(this, SearchActivity::class.java)))
            searchView.setIconified(true)
            searchView.setQuery("", false)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (mIsMultiPane) {
            menu.findItem(R.id.menu_share).setVisible(mSelectedItem != null)
            menu.findItem(R.id.menu_external).setVisible(mSelectedItem != null)
        }
        return isSearchable() || super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getItemId() == R.id.menu_share) {
            val anchor = findViewById(R.id.menu_share)
            AppUtils.share(this, mPopupMenu, anchor == null ?
                    findViewById(R.id.toolbar) : anchor, mSelectedItem)
            return true
        }
        if (item.getItemId() == R.id.menu_external) {
            val anchor = findViewById(R.id.menu_external)
            AppUtils.openExternal(this, mPopupMenu, anchor == null ?
                    findViewById(R.id.toolbar) : anchor,
                    mSelectedItem, mCustomTabsDelegate.getSession())
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    protected override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(STATE_SELECTED_ITEM, mSelectedItem)
        outState.putBoolean(STATE_FULLSCREEN, mFullscreen)
    }

    protected override fun onStop() {
        super.onStop()
        mCustomTabsDelegate.unbindCustomTabsService(this)
        mKeyDelegate.detach(this)
    }

    protected override fun onDestroy() {
        super.onDestroy()
        mPreferenceObservable.unsubscribe(this)
        if (mIsMultiPane) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver)
        }
    }
    override fun onBackPressed() {
        if (!mIsMultiPane || !mFullscreen) {
            super.onBackPressed()
        } else {
            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(
                    WebFragment.ACTION_FULLSCREEN).putExtra(WebFragment.EXTRA_FULLSCREEN, false))
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        mKeyDelegate.setScrollable(getScrollableList(), mAppBar)
        mKeyDelegate.setBackInterceptor(getBackInterceptor())
        return mKeyDelegate.onKeyDown(keyCode, event) ||
                super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return mKeyDelegate.onKeyUp(keyCode, event) ||
                super.onKeyUp(keyCode, event)
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        return mKeyDelegate.onKeyLongPress(keyCode, event) ||
                super.onKeyLongPress(keyCode, event)
    }

    @NonNull
    override fun getSupportActionBar(): ActionBar {
        //noinspection ConstantConditions
        return super.getSupportActionBar()
    }

    override fun onItemSelected(item: WebItem) {
        val previousItem = mSelectedItem
        mSelectedItem = item
        if (mIsMultiPane) {
            if (previousItem != null && item != null &&
                    TextUtils.equals(item.getId(), previousItem.getId())) {
                return
            }
            if (previousItem == null && item != null ||
                    previousItem != null && item == null) {
                supportInvalidateOptionsMenu()
            }
            openMultiPaneItem()
        } else if (item != null) {
            openSinglePaneItem()
        }
    }

    override fun getSelectedItem(): WebItem {
        return mSelectedItem
    }

    override fun isMultiPane(): Boolean {
        return mIsMultiPane
    }

    /**
     * Checks if activity should have search view
     * @return true if is searchable, false otherwise
     */
    protected fun isSearchable(): Boolean {
        return true
    }

    /**
     * Gets default title to be displayed in list-only layout
     * @return displayed title
     */
    protected abstract fun getDefaultTitle(): String

    /**
     * Creates list fragment to host list data
     * @return list fragment
     */
    protected abstract fun instantiateListFragment(): Fragment

    /**
     * Gets cache mode for {@link ItemManager}
     * @return  cache mode
     */
    @ItemManager.CacheMode
    protected fun getItemCacheMode(): Int {
        return ItemManager.MODE_DEFAULT
    }

    @Synthetic
    fun setFullscreen() {
        mAppBar.setExpanded(!mFullscreen, true)
        mTabLayout.setVisibility(mFullscreen ? View.GONE : View.VISIBLE)
        mListView.setVisibility(mFullscreen ? View.GONE : View.VISIBLE)
        mKeyDelegate.setAppBarEnabled(!mFullscreen)
        mViewPager.setSwipeEnabled(!mFullscreen)
        AppUtils.toggleFab(mReplyButton, !mFullscreen)
    }

    private fun getScrollableList(): Scrollable {
        // TODO landscape behavior?
        return (Scrollable) getSupportFragmentManager().findFragmentByTag(LIST_FRAGMENT_TAG)
    }

    private fun getBackInterceptor(): KeyDelegate.BackInterceptor {
        if (mViewPager == null ||
                mViewPager.getAdapter() == null ||
                mViewPager.getCurrentItem() < 0) {
            return null
        }
        val item = ((ItemPagerAdapter) mViewPager.getAdapter())
                .getItem(mViewPager.getCurrentItem())
        if (item is KeyDelegate.BackInterceptor) {
            return (KeyDelegate.BackInterceptor) item
        } else {
            return null
        }
    }

    private fun openSinglePaneItem() {
        if (mExternalBrowser && mStoryViewMode != Preferences.StoryViewMode.Comment) {
            AppUtils.openWebUrlExternal(this, mSelectedItem, mSelectedItem.getUrl(), mCustomTabsDelegate.getSession())
        } else {
            val intent = Intent(this, ItemActivity::class.java)
                    .putExtra(ItemActivity.EXTRA_CACHE_MODE, getItemCacheMode())
                    .putExtra(ItemActivity.EXTRA_ITEM, mSelectedItem)
            startActivity(mMultiWindowEnabled ? AppUtils.multiWindowIntent(this, intent) : intent)
        }
    }

    private fun openMultiPaneItem() {
        if (mSelectedItem == null) {
            setTitle(getDefaultTitle())
            findViewById(R.id.empty_selection).setVisibility(View.VISIBLE)
            mTabLayout.setVisibility(View.GONE)
            mViewPager.setVisibility(View.GONE)
            mViewPager.setAdapter(null)
            AppUtils.toggleFab(mNavButton, false)
            AppUtils.toggleFab(mReplyButton, false)
        } else {
            setTitle(mSelectedItem.getDisplayedTitle())
            findViewById(R.id.empty_selection).setVisibility(View.GONE)
            mTabLayout.setVisibility(View.VISIBLE)
            mViewPager.setVisibility(View.VISIBLE)
            bindViewPager()
            mSessionManager.view(mSelectedItem.getId())
        }
    }

    private fun bindViewPager() {
        if (mAdapter != null) {
            mAdapter.unbind(mTabLayout)
        }
        mAdapter = ItemPagerAdapter(this, getSupportFragmentManager(), ItemPagerAdapter.Builder()
                .setItem(mSelectedItem)
                .setCacheMode(getItemCacheMode())
                .setShowArticle(true)
                .setDefaultViewMode(mStoryViewMode))
        mAdapter.bind(mViewPager, mTabLayout, mNavButton, mReplyButton)
        if (mFullscreen) {
            setFullscreen()
        }
    }

    @SuppressLint("RestrictedApi")
    private fun unbindViewPager() {
        // fragment manager always restores view pager fragments,
        // even when view pager no longer exists (e.g. after rotation),
        // so we have to explicitly remove those with view pager ID
        val transaction = getSupportFragmentManager().beginTransaction()
        //noinspection Convert2streamapi
        for (fragment in getSupportFragmentManager().getFragments()) {
            if (fragment != null && fragment.getId() == R.id.content) {
                transaction.remove(fragment)
            }
        }
        transaction.commit()
    }

    private fun onPreferenceChanged(key: Int, contextChanged: Boolean) {
        if (key == R.string.pref_external) {
            mExternalBrowser = Preferences.externalBrowserEnabled(this)
        } else if (key == R.string.pref_story_display) {
            mStoryViewMode = Preferences.getDefaultStoryView(this)
        } else if (key == R.string.pref_navigation) {
            boolean enabled = Preferences.navigationEnabled(this)
            if (!enabled) {
                NavFloatingActionButton.resetPosition(this)
            }
            if (mNavButton != null) {
                AppUtils.toggleFab(mNavButton, mViewPager.getCurrentItem() == 0 && enabled)
            }
        } else if (key == R.string.pref_multi_window) {
            mMultiWindowEnabled = Preferences.multiWindowEnabled(this)
        }
    }
}
