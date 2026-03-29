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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.google.android.material.appbar.AppBarLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.core.graphics.drawable.DrawableCompat
import androidx.appcompat.app.ActionBar
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

import java.lang.ref.WeakReference

import javax.inject.Inject
import javax.inject.Named

import io.github.hidroh.materialistic.accounts.UserServices
import io.github.hidroh.materialistic.annotation.Synthetic
import io.github.hidroh.materialistic.data.FavoriteManager
import io.github.hidroh.materialistic.data.Item
import io.github.hidroh.materialistic.data.ItemManager
import io.github.hidroh.materialistic.data.MaterialisticDatabase
import io.github.hidroh.materialistic.data.ResponseListener
import io.github.hidroh.materialistic.data.SessionManager
import io.github.hidroh.materialistic.data.WebItem
import io.github.hidroh.materialistic.widget.ItemPagerAdapter
import io.github.hidroh.materialistic.widget.NavFloatingActionButton
import io.github.hidroh.materialistic.widget.PopupMenu
import io.github.hidroh.materialistic.widget.ViewPager

open class ItemActivity : InjectableActivity(), ItemFragment.ItemChangedListener {

    const val EXTRA_ITEM: String = ItemActivity.class.getName() + ".EXTRA_ITEM"
    const val EXTRA_CACHE_MODE: String = ItemActivity.class.getName() + ".EXTRA_CACHE_MODE"
    const val EXTRA_OPEN_COMMENTS: String = ItemActivity.class.getName() + ".EXTRA_OPEN_COMMENTS"
    private const val PARAM_ID: String = "id"
    private const val STATE_ITEM: String = "state:item"
    private const val STATE_ITEM_ID: String = "state:itemId"
    private const val STATE_FULLSCREEN: String = "state:fullscreen"
    var mItem: WebItem? = null
    var mItemId: String? = null
    var mBookmark: ImageView? = null
    private var mExternalBrowser: Boolean = false
    private var mStoryViewMode: Preferences.StoryViewMode? = null
    @Inject @Named(ActivityModule.HN) var mItemManager: ItemManager? = null
    @Inject var mFavoriteManager: FavoriteManager? = null
    @Inject var mAlertDialogBuilder: AlertDialogBuilder? = null
    @Inject var mPopupMenu: PopupMenu? = null
    @Inject var mUserServices: UserServices? = null
    @Inject var mSessionManager: SessionManager? = null
    @Inject var mCustomTabsDelegate: CustomTabsDelegate? = null
    @Inject var mKeyDelegate: KeyDelegate? = null
    private var mTabLayout: TabLayout? = null
    var mAppBar: AppBarLayout? = null
    var mCoordinatorLayout: CoordinatorLayout? = null
    private var mVoteButton: ImageButton? = null
    private var mReplyButton: FloatingActionButton? = null
    private var mNavButton: NavFloatingActionButton? = null
    private var mAdapter: ItemPagerAdapter? = null
    private var mViewPager: ViewPager? = null
    var mFullscreen: Boolean = false
    private final Observer<Uri> mObserver = uri -> {
        if (mItem == null) {
            return
        }
        if (uri == null) {
            return
        }
        if (FavoriteManager.Companion.isCleared(uri)) {
            mItem.setFavorite(false)
            bindFavorite()
            return
        }
        if (!TextUtils.equals(mItemId, uri.getLastPathSegment())) {
            return
        }
        if (FavoriteManager.Companion.isAdded(uri) || FavoriteManager.Companion.isRemoved(uri)) {
            mItem.setFavorite(FavoriteManager.Companion.isAdded(uri))
            bindFavorite()
        }
    }
    private fun BroadcastReceiver(): BroadcastReceiver mReceiver = new {
        public void onReceive(Context context, Intent intent) {
            mFullscreen = intent.getBooleanExtra(WebFragment.EXTRA_FULLSCREEN, false)
            setFullscreen()
        }
    }
    private val mPreferenceObservable: Preferences.Observable = new Preferences.Observable()
    private var mSystemUiHelper: AppUtils.SystemUiHelper? = null

    protected override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        mExternalBrowser = Preferences.externalBrowserEnabled(this)
        if (getIntent().getBooleanExtra(EXTRA_OPEN_COMMENTS, false)) {
            mStoryViewMode = Preferences.StoryViewMode.Comment
        } else {
            mStoryViewMode = Preferences.getDefaultStoryView(this)
        }
        setContentView(R.layout.activity_item)
        setSupportActionBar(findViewById(R.id.toolbar))
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_HOME_AS_UP)
        mSystemUiHelper = AppUtils.SystemUiHelper(getWindow())
        mReplyButton = findViewById(R.id.reply_button)
        mNavButton = findViewById(R.id.navigation_button)
        mNavButton.setNavigable(direction ->
                // if callback is fired navigable should not be null
                AppUtils.navigate(direction, mAppBar, (Navigable) mAdapter.getItem(0)))
        mVoteButton = findViewById(R.id.vote_button)
        mBookmark = findViewById(R.id.bookmarked)
        mCoordinatorLayout = findViewById(R.id.content_frame)
        mAppBar = findViewById(R.id.appbar)
        mTabLayout = findViewById(R.id.tab_layout)
        mViewPager = findViewById(R.id.view_pager)
        AppUtils.toggleFab(mNavButton, false)
        AppUtils.toggleFab(mReplyButton, false)
        final Intent intent = getIntent()
        MaterialisticDatabase.getInstance(this).getLiveData().observe(this, mObserver)
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver,
                IntentFilter(WebFragment.ACTION_FULLSCREEN))
        mPreferenceObservable.subscribe(this, this::onPreferenceChanged,
                R.string.pref_navigation)
        if (savedInstanceState != null) {
            mItem = savedInstanceState.getParcelable(STATE_ITEM)
            mItemId = savedInstanceState.getString(STATE_ITEM_ID)
            mFullscreen = savedInstanceState.getBoolean(STATE_FULLSCREEN)
        } else {
            if (Intent.ACTION_VIEW.equalsIgnoreCase(intent.getAction())) {
                mItemId = AppUtils.getDataUriId(intent, PARAM_ID)
            } else if (intent.hasExtra(EXTRA_ITEM)) {
                mItem = intent.getParcelableExtra(EXTRA_ITEM)
                mItemId = mItem.getId()
            }
        }

        if (mItem != null) {
            bindData(mItem)
        } else if (!TextUtils.isEmpty(mItemId)) {
            mItemManager.getItem(mItemId,
                    getIntent().getIntExtra(EXTRA_CACHE_MODE, ItemManager.MODE_DEFAULT),
                    ItemResponseListener(this))
        }
        if (!AppUtils.hasConnection(this)) {
            Snackbar.make(mCoordinatorLayout, R.string.offline_notice, Snackbar.LENGTH_LONG).show()
        }
    }

    protected override fun onStart() {
        super.onStart()
        mCustomTabsDelegate.bindCustomTabsService(this)
        mKeyDelegate.attach(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        getMenuInflater().inflate(R.menu.menu_item, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_share).setVisible(mItem != null)
        return mItem != null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getItemId() == android.R.id.home) {
            finish()
            return true
        }
        if (item.getItemId() == R.id.menu_external) {
            View anchor = findViewById(R.id.menu_external)
            AppUtils.openExternal(this, mPopupMenu, anchor == null ?
                    findViewById(R.id.toolbar) : anchor, mItem,
                    mCustomTabsDelegate.getSession())
            return true
        }
        if (item.getItemId() == R.id.menu_share) {
            View anchor = findViewById(R.id.menu_share)
            AppUtils.share(this, mPopupMenu, anchor == null ?
                    findViewById(R.id.toolbar) : anchor, mItem)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    protected override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(STATE_ITEM, mItem)
        outState.putString(STATE_ITEM_ID, mItemId)
        outState.putBoolean(STATE_FULLSCREEN, mFullscreen)
    }

    protected override fun onStop() {
        super.onStop()
        mCustomTabsDelegate.unbindCustomTabsService(this)
        mKeyDelegate.detach(this)
    }

    protected override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver)
        mPreferenceObservable.unsubscribe(this)
    }

    override fun onBackPressed() {
        if (!mFullscreen) {
            super.onBackPressed()
        } else {
            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(
                    WebFragment.ACTION_FULLSCREEN).putExtra(WebFragment.EXTRA_FULLSCREEN, false))
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        mKeyDelegate.setScrollable(getCurrent(Scrollable::class.java), mAppBar)
        mKeyDelegate.setBackInterceptor(getCurrent(KeyDelegate.BackInterceptor::class.java))
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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        mSystemUiHelper.setFullscreen(hasFocus && mFullscreen)
    }

    override fun onItemChanged(item: Item) {
        mItem = item
        if (mTabLayout.getTabCount() > 0) {
            //noinspection ConstantConditions
            mTabLayout.getTabAt(0).setText(getResources()
                    .getQuantityString(R.plurals.comments_count, item.getKidCount(), item.getKidCount()))
        }
    }

    @Synthetic
    fun setFullscreen() {
        mSystemUiHelper.setFullscreen(mFullscreen)
        mAppBar.setExpanded(!mFullscreen, true)
        mKeyDelegate.setAppBarEnabled(!mFullscreen)
        mViewPager.setSwipeEnabled(!mFullscreen)
        AppUtils.toggleFab(mReplyButton, !mFullscreen)
    }

    @Synthetic
    fun onItemLoaded(response: Item) {
        mItem = response
        supportInvalidateOptionsMenu()
        bindData(mItem)
    }

    @Synthetic
    fun bindFavorite() {
        if (mItem == null) {
            return
        }
        if (!mItem.isStoryType()) {
            return
        }
        mBookmark.setVisibility(View.VISIBLE)
        decorateFavorite(mItem.isFavorite())
        mBookmark.setOnClickListener(View.OnClickListener() {
            private boolean mUndo

            public void onClick(View v) {
                final int toastMessageResId
                if (!mItem.isFavorite()) {
                    mFavoriteManager.add(ItemActivity.this, mItem)
                    toastMessageResId = R.string.toast_saved
                } else {
                    mFavoriteManager.remove(ItemActivity.this, mItem.getId())
                    toastMessageResId = R.string.toast_removed
                }
                if (!mUndo) {
                    Snackbar.make(mCoordinatorLayout, toastMessageResId, Snackbar.LENGTH_SHORT)
                            .setAction(R.string.undo, v1 -> {
                                mUndo = true
                                mBookmark.performClick()
                            })
                            .show()
                }
                mUndo = false
            }
        })
    }

    @SuppressWarnings("ConstantConditions")
    private fun bindData(story: WebItem) {
        if (story == null) {
            return
        }
        mCustomTabsDelegate.mayLaunchUrl(Uri.parse(story.getUrl()), null, null)
        bindFavorite()
        mSessionManager.view(story.getId())
        mVoteButton.setVisibility(View.VISIBLE)
        mVoteButton.setOnClickListener(v -> vote(story))
        final TextView titleTextView = findViewById(android.R.id.text2)
        if (story.isStoryType()) {
            titleTextView.setText(story.getDisplayedTitle())
            setTaskTitle(story.getDisplayedTitle())
            if (!TextUtils.isEmpty(story.getSource())) {
                TextView sourceTextView = findViewById(R.id.source)
                sourceTextView.setText(story.getSource())
                sourceTextView.setVisibility(View.VISIBLE)
            }
        } else {
            AppUtils.setTextAppearance(titleTextView, R.style.TextAppearance_App_Small)
            titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimension(AppUtils.getThemedResId(this, R.attr.contentTextSize)))
            CharSequence title = AppUtils.fromHtml(story.getDisplayedTitle(), true)
            titleTextView.setText(title)
            setTaskTitle(title)
        }

        final TextView postedTextView = findViewById(R.id.posted)
        postedTextView.setText(story.getDisplayedTime(this))
        postedTextView.append(story.getDisplayedAuthor(this, true, 0))
        postedTextView.setMovementMethod(LinkMovementMethod.getInstance())
        switch (story.getType()) {
            case Item.JOB_TYPE:
                postedTextView.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_work_white_18dp, 0, 0, 0)
                break
            case Item.POLL_TYPE:
                postedTextView.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_poll_white_18dp, 0, 0, 0)
                break
        }
        boolean hasText = story is Item && !TextUtils.isEmpty(((Item) story).getText())
        mAdapter = ItemPagerAdapter(this, getSupportFragmentManager(),
                ItemPagerAdapter.Builder()
                        .setItem(story)
                        .setShowArticle(hasText || !mExternalBrowser)
                        .setCacheMode(getIntent().getIntExtra(EXTRA_CACHE_MODE, ItemManager.MODE_DEFAULT))
                        .setRetainInstance(true)
                        .setDefaultViewMode(mStoryViewMode))
        mAdapter.bind(mViewPager, mTabLayout, mNavButton, mReplyButton)
        mTabLayout.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(mViewPager) {
            public void onTabReselected(TabLayout.Tab tab) {
                mAppBar.setExpanded(true, true)
            }
        })
        if (story.isStoryType() && mExternalBrowser && !hasText) {
            TextView buttonArticle = (TextView) findViewById(R.id.button_article)
            buttonArticle.setVisibility(View.VISIBLE)
            buttonArticle.setOnClickListener(v ->
                    AppUtils.openWebUrlExternal(ItemActivity.this,
                            story, story.getUrl(), mCustomTabsDelegate.getSession()))
        }
        if (mFullscreen) {
            setFullscreen()
        }
    }

    private fun decorateFavorite(isFavorite: Boolean) {
        mBookmark.setImageResource(isFavorite ?
                R.drawable.ic_bookmark_white_24dp : R.drawable.ic_bookmark_border_white_24dp)
    }

    private fun getCurrent(clazz: Class<T>): <T> T {
        if (mAdapter == null) {
            return null
        }
        Fragment currentItem = mAdapter.getItem(mViewPager.getCurrentItem())
        if (clazz.isInstance(currentItem)) {
            //noinspection unchecked
            return (T) currentItem
        } else {
            return null
        }
    }

    private fun vote(story: WebItem) {
        mUserServices.voteUp(ItemActivity.this, story.getId(), VoteCallback(this))
    }

    @Synthetic
    fun onVoted(successful: Boolean) {
        if (successful == null) {
            Toast.makeText(this, R.string.vote_failed, Toast.LENGTH_SHORT).show()
        } else if (successful) {
            Drawable drawable = DrawableCompat.wrap(mVoteButton.getDrawable())
            DrawableCompat.setTint(drawable, ContextCompat.getColor(this, R.color.greenA700))
            Toast.makeText(this, R.string.voted, Toast.LENGTH_SHORT).show()
        } else {
            AppUtils.showLogin(this, mAlertDialogBuilder)
        }
    }

    private fun onPreferenceChanged(key: Int, contextChanged: Boolean) {
        if (!Preferences.navigationEnabled(this)) {
            NavFloatingActionButton.resetPosition(this)
        }
        AppUtils.toggleFab(mNavButton, navigationVisible())
    }

    private fun navigationVisible(): Boolean {
        return mViewPager.getCurrentItem() == 0 && Preferences.navigationEnabled(this)
    }

    open class ItemResponseListener : ResponseListener<Item> {
        private var mItemActivity: WeakReference<ItemActivity>? = null

        @Synthetic
        constructor(itemActivity: ItemActivity) {
            mItemActivity = WeakReference<>(itemActivity)
        }

        override fun onResponse(response: Item) {
            if (mItemActivity.get() != null && !mItemActivity.get().isActivityDestroyed()) {
                mItemActivity.get().onItemLoaded(response)
            }
        }

        override fun onError(errorMessage: String) {
            // do nothing
        }
    }

    open class VoteCallback : UserServices.Callback() {
        private var mItemActivity: WeakReference<ItemActivity>? = null

        @Synthetic
        constructor(itemActivity: ItemActivity) {
            mItemActivity = WeakReference<>(itemActivity)
        }

        override fun onDone(successful: Boolean) {
            if (mItemActivity.get() != null && !mItemActivity.get().isActivityDestroyed()) {
                mItemActivity.get().onVoted(successful)
            }
        }

        override fun onError(throwable: Throwable) {
            if (mItemActivity.get() != null && !mItemActivity.get().isActivityDestroyed()) {
                mItemActivity.get().onVoted(null)
            }
        }
    }
}
