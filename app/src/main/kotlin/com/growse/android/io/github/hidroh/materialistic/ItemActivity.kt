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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
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
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.net.toUri
import androidx.core.os.BundleCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.growse.android.io.github.hidroh.materialistic.AppUtils.SystemUiHelper
import com.growse.android.io.github.hidroh.materialistic.KeyDelegate.BackInterceptor
import com.growse.android.io.github.hidroh.materialistic.Preferences.StoryViewMode
import com.growse.android.io.github.hidroh.materialistic.accounts.UserServices
import com.growse.android.io.github.hidroh.materialistic.data.FavoriteManager
import com.growse.android.io.github.hidroh.materialistic.data.Item
import com.growse.android.io.github.hidroh.materialistic.data.ItemManager
import com.growse.android.io.github.hidroh.materialistic.data.ItemManagerQualifiers
import com.growse.android.io.github.hidroh.materialistic.data.MaterialisticDatabase
import com.growse.android.io.github.hidroh.materialistic.data.ResponseListener
import com.growse.android.io.github.hidroh.materialistic.data.SessionManager
import com.growse.android.io.github.hidroh.materialistic.data.WebItem
import com.growse.android.io.github.hidroh.materialistic.widget.ItemPagerAdapter
import com.growse.android.io.github.hidroh.materialistic.widget.NavFloatingActionButton
import com.growse.android.io.github.hidroh.materialistic.widget.PopupMenu
import com.growse.android.io.github.hidroh.materialistic.widget.ViewPager
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class ItemActivity : ThemedActivity(), ItemFragment.ItemChangedListener {
  @Inject @field:Named(ItemManagerQualifiers.HN) lateinit var itemManager: ItemManager
  @Inject lateinit var favoriteManager: FavoriteManager
  @Inject lateinit var alertDialogBuilder: AlertDialogBuilder<*>
  @Inject lateinit var popupMenu: PopupMenu
  @Inject lateinit var userServices: UserServices
  @Inject lateinit var sessionManager: SessionManager
  @Inject lateinit var customTabsDelegate: CustomTabsDelegate
  @Inject lateinit var keyDelegate: KeyDelegate

  private var item: WebItem? = null
  private var itemId: String? = null
  private var externalBrowser = false
  private lateinit var storyViewMode: StoryViewMode
  private var fullscreen = false

  private lateinit var toolbar: Toolbar
  private lateinit var bookmark: ImageView
  private lateinit var tabLayout: TabLayout
  private lateinit var appBar: AppBarLayout
  private lateinit var coordinatorLayout: CoordinatorLayout
  private lateinit var voteButton: ImageButton
  private lateinit var replyButton: FloatingActionButton
  private lateinit var navButton: NavFloatingActionButton
  private lateinit var viewPager: ViewPager
  private lateinit var systemUiHelper: SystemUiHelper
  private var adapter: ItemPagerAdapter? = null

  private val cacheMode: Int
    get() = intent.getIntExtra(EXTRA_CACHE_MODE, ItemManager.MODE_DEFAULT)

  private val favoriteObserver =
      Observer<Uri?> { uri ->
        val item = item ?: return@Observer
        uri ?: return@Observer
        when {
          FavoriteManager.isCleared(uri) -> item.isFavorite = false
          uri.lastPathSegment != itemId -> return@Observer
          FavoriteManager.isAdded(uri) -> item.isFavorite = true
          FavoriteManager.isRemoved(uri) -> item.isFavorite = false
          else -> return@Observer
        }
        bindFavorite()
      }

  private val fullscreenReceiver =
      object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
          fullscreen = intent.getBooleanExtra(WebFragment.EXTRA_FULLSCREEN, false)
          applyFullscreen()
        }
      }

  private val preferenceObservable = Preferences.Observable()

  private val backPressedCallback =
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          if (fullscreen) {
            // leave fullscreen rather than the activity
            LocalBroadcastManager.getInstance(this@ItemActivity)
                .sendBroadcast(
                    Intent(WebFragment.ACTION_FULLSCREEN)
                        .putExtra(WebFragment.EXTRA_FULLSCREEN, false)
                )
          } else {
            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
            isEnabled = true
          }
        }
      }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    onBackPressedDispatcher.addCallback(this, backPressedCallback)
    externalBrowser = Preferences.externalBrowserEnabled(this)
    storyViewMode =
        if (intent.getBooleanExtra(EXTRA_OPEN_COMMENTS, false)) StoryViewMode.Comment
        else Preferences.getDefaultStoryView(this)
    setContentView(R.layout.activity_item)
    toolbar = findViewById(R.id.toolbar)
    setSupportActionBar(toolbar)
    supportActionBar!!.displayOptions = ActionBar.DISPLAY_SHOW_HOME or ActionBar.DISPLAY_HOME_AS_UP
    AppUtils.applyToolbarTopInset(toolbar)
    systemUiHelper = SystemUiHelper(window)
    replyButton = findViewById(R.id.reply_button)
    navButton = findViewById(R.id.navigation_button)
    navButton.setNavigable { direction ->
      // the callback only fires once the pager has an adapter
      navigate(direction, appBar, adapter!!.getItem(0) as Navigable)
    }
    voteButton = findViewById(R.id.vote_button)
    bookmark = findViewById(R.id.bookmarked)
    coordinatorLayout = findViewById(R.id.content_frame)
    appBar = findViewById(R.id.appbar)
    tabLayout = findViewById(R.id.tab_layout)
    viewPager = findViewById(R.id.view_pager)
    AppUtils.toggleFab(navButton, false)
    AppUtils.toggleFab(replyButton, false)
    MaterialisticDatabase.getInstance(this).liveData.observe(this, favoriteObserver)
    LocalBroadcastManager.getInstance(this)
        .registerReceiver(fullscreenReceiver, IntentFilter(WebFragment.ACTION_FULLSCREEN))
    preferenceObservable.subscribe(
        this,
        { _, _ -> onNavigationPreferenceChanged() },
        R.string.pref_navigation,
    )
    if (savedInstanceState != null) {
      item = BundleCompat.getParcelable(savedInstanceState, STATE_ITEM, WebItem::class.java)
      itemId = savedInstanceState.getString(STATE_ITEM_ID)
      fullscreen = savedInstanceState.getBoolean(STATE_FULLSCREEN)
    } else if (Intent.ACTION_VIEW.equals(intent.action, ignoreCase = true)) {
      itemId = AppUtils.getDataUriId(intent, PARAM_ID)
    } else if (intent.hasExtra(EXTRA_ITEM)) {
      item = IntentCompat.getParcelableExtra(intent, EXTRA_ITEM, WebItem::class.java)
      itemId = item?.id
    }

    val item = item
    val itemId = itemId
    if (item != null) {
      bindData(item)
    } else if (!itemId.isNullOrEmpty()) {
      itemManager.getItem(itemId, cacheMode, ItemResponseListener(this))
    }
    if (!AppUtils.hasConnection(this)) {
      Snackbar.make(coordinatorLayout, R.string.offline_notice, Snackbar.LENGTH_LONG).show()
    }
  }

  override fun onStart() {
    super.onStart()
    customTabsDelegate.bindCustomTabsService(this)
    keyDelegate.attach(this)
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_item, menu)
    return super.onCreateOptionsMenu(menu)
  }

  override fun onPrepareOptionsMenu(menu: Menu): Boolean {
    menu.findItem(R.id.menu_share).isVisible = item != null
    return item != null
  }

  override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
    val item = item
    return when {
      menuItem.itemId == android.R.id.home -> {
        finish()
        true
      }
      menuItem.itemId == R.id.menu_external && item != null -> {
        AppUtils.openExternal(
            this,
            popupMenu,
            menuAnchor(R.id.menu_external),
            item,
            customTabsDelegate.session,
        )
        true
      }
      menuItem.itemId == R.id.menu_share && item != null -> {
        AppUtils.share(this, popupMenu, menuAnchor(R.id.menu_share), item)
        true
      }
      else -> super.onOptionsItemSelected(menuItem)
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putParcelable(STATE_ITEM, item)
    outState.putString(STATE_ITEM_ID, itemId)
    outState.putBoolean(STATE_FULLSCREEN, fullscreen)
  }

  override fun onStop() {
    super.onStop()
    customTabsDelegate.unbindCustomTabsService(this)
    keyDelegate.detach(this)
  }

  override fun onDestroy() {
    super.onDestroy()
    LocalBroadcastManager.getInstance(this).unregisterReceiver(fullscreenReceiver)
    preferenceObservable.unsubscribe(this)
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
    keyDelegate.setScrollable(currentPage<Scrollable>(), appBar)
    keyDelegate.setBackInterceptor(currentPage<BackInterceptor>())
    return keyDelegate.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
  }

  override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean =
      keyDelegate.onKeyUp(keyCode, event) || super.onKeyUp(keyCode, event)

  override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean =
      keyDelegate.onKeyLongPress(keyCode, event) || super.onKeyLongPress(keyCode, event)

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    systemUiHelper.setFullscreen(hasFocus && fullscreen)
  }

  override fun onItemChanged(item: Item) {
    this.item = item
    tabLayout.getTabAt(0)?.text =
        resources.getQuantityString(R.plurals.comments_count, item.kidCount, item.kidCount)
  }

  private fun applyFullscreen() {
    systemUiHelper.setFullscreen(fullscreen)
    appBar.setExpanded(!fullscreen, true)
    keyDelegate.setAppBarEnabled(!fullscreen)
    viewPager.setSwipeEnabled(!fullscreen)
    AppUtils.toggleFab(replyButton, !fullscreen)
  }

  private fun onItemLoaded(response: Item?) {
    item = response
    invalidateOptionsMenu()
    response?.let(::bindData)
  }

  private fun bindFavorite() {
    val item = item?.takeIf { it.isStoryType } ?: return
    bookmark.isVisible = true
    bookmark.setImageResource(
        if (item.isFavorite) R.drawable.ic_bookmark_white_24dp
        else R.drawable.ic_bookmark_border_white_24dp
    )
    // undo re-clicks the bookmark, which must not offer another undo
    var undoing = false
    bookmark.setOnClickListener {
      val current = this.item ?: return@setOnClickListener
      val messageResId =
          if (current.isFavorite) {
            favoriteManager.remove(this, current.id)
            R.string.toast_removed
          } else {
            favoriteManager.add(this, current)
            R.string.toast_saved
          }
      if (!undoing) {
        Snackbar.make(coordinatorLayout, messageResId, Snackbar.LENGTH_SHORT)
            .setAction(R.string.undo) {
              undoing = true
              bookmark.performClick()
            }
            .show()
      }
      undoing = false
    }
  }

  private fun bindData(story: WebItem) {
    customTabsDelegate.mayLaunchUrl(story.url?.toUri(), null, null)
    bindFavorite()
    sessionManager.view(story.id)
    voteButton.isVisible = true
    voteButton.setOnClickListener { userServices.voteUp(this, story.id, VoteCallback(this)) }
    bindTitle(story)
    findViewById<TextView>(R.id.posted).apply {
      text = story.getDisplayedTime(this@ItemActivity)
      append(story.getDisplayedAuthor(this@ItemActivity, true, 0))
      movementMethod = LinkMovementMethod.getInstance()
      when (story.type) {
        WebItem.JOB_TYPE ->
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_work_white_18dp, 0, 0, 0)
        WebItem.POLL_TYPE ->
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_poll_white_18dp, 0, 0, 0)
      }
    }
    val hasText = story is Item && !story.text.isNullOrEmpty()
    adapter =
        ItemPagerAdapter(
                this,
                supportFragmentManager,
                ItemPagerAdapter.Builder()
                    .setItem(story)
                    .setShowArticle(hasText || !externalBrowser)
                    .setCacheMode(cacheMode)
                    .setRetainInstance(true)
                    .setDefaultViewMode(storyViewMode),
            )
            .apply { bind(viewPager, tabLayout, navButton, replyButton) }
    tabLayout.addOnTabSelectedListener(
        object : TabLayout.ViewPagerOnTabSelectedListener(viewPager) {
          override fun onTabReselected(tab: TabLayout.Tab) {
            appBar.setExpanded(true, true)
          }
        }
    )
    if (story.isStoryType && externalBrowser && !hasText) {
      findViewById<TextView>(R.id.button_article).apply {
        isVisible = true
        setOnClickListener {
          AppUtils.openWebUrlExternal(
              this@ItemActivity,
              story,
              story.url,
              customTabsDelegate.session,
          )
        }
      }
    }
    if (fullscreen) {
      applyFullscreen()
    }
  }

  private fun bindTitle(story: WebItem) {
    val titleTextView = findViewById<TextView>(android.R.id.text2)
    if (story.isStoryType) {
      titleTextView.text = story.displayedTitle
      setTaskTitle(story.displayedTitle)
      val source = story.source
      if (!source.isNullOrEmpty()) {
        findViewById<TextView>(R.id.source).apply {
          text = source
          isVisible = true
        }
      }
    } else {
      titleTextView.setTextAppearance(R.style.TextAppearance_App_Small)
      titleTextView.setTextSize(
          TypedValue.COMPLEX_UNIT_PX,
          resources.getDimension(AppUtils.getThemedResId(this, R.attr.contentTextSize)),
      )
      val title = AppUtils.fromHtml(story.displayedTitle, true)
      titleTextView.text = title
      setTaskTitle(title)
    }
  }

  /** The toolbar action view to anchor a popup to, or the toolbar if the action is overflowed. */
  private fun menuAnchor(menuItemId: Int): View = findViewById(menuItemId) ?: toolbar

  private inline fun <reified T> currentPage(): T? = adapter?.getItem(viewPager.currentItem) as? T

  private fun onVoted(successful: Boolean?) {
    when (successful) {
      null -> Toast.makeText(this, R.string.vote_failed, Toast.LENGTH_SHORT).show()
      true -> {
        DrawableCompat.setTint(
            DrawableCompat.wrap(voteButton.drawable),
            ContextCompat.getColor(this, R.color.greenA700),
        )
        Toast.makeText(this, R.string.voted, Toast.LENGTH_SHORT).show()
      }
      false -> AppUtils.showLogin(this, alertDialogBuilder)
    }
  }

  private fun onNavigationPreferenceChanged() {
    if (!Preferences.navigationEnabled(this)) {
      NavFloatingActionButton.resetPosition(this)
    }
    AppUtils.toggleFab(
        navButton,
        viewPager.currentItem == 0 && Preferences.navigationEnabled(this),
    )
  }

  private class ItemResponseListener(activity: ItemActivity) : ResponseListener<Item?> {
    private val activity = WeakReference(activity)

    override fun onResponse(response: Item?) {
      activity.ifAlive { onItemLoaded(response) }
    }

    override fun onError(errorMessage: String?) {
      // do nothing
    }
  }

  private class VoteCallback(activity: ItemActivity) : UserServices.Callback() {
    private val activity = WeakReference(activity)

    override fun onDone(successful: Boolean) {
      activity.ifAlive { onVoted(successful) }
    }

    override fun onError(throwable: Throwable?) {
      activity.ifAlive { onVoted(null) }
    }
  }

  companion object {
    @JvmField val EXTRA_ITEM = "${ItemActivity::class.java.name}.EXTRA_ITEM"
    @JvmField val EXTRA_CACHE_MODE = "${ItemActivity::class.java.name}.EXTRA_CACHE_MODE"
    @JvmField val EXTRA_OPEN_COMMENTS = "${ItemActivity::class.java.name}.EXTRA_OPEN_COMMENTS"
    private const val PARAM_ID = "id"
    private const val STATE_ITEM = "state:item"
    private const val STATE_ITEM_ID = "state:itemId"
    private const val STATE_FULLSCREEN = "state:fullscreen"

    /** Collapses the app bar before moving down or right, otherwise navigates straight away. */
    private fun navigate(direction: Int, appBarLayout: AppBarLayout, navigable: Navigable) {
      when (direction) {
        Navigable.DIRECTION_DOWN,
        Navigable.DIRECTION_RIGHT ->
            if (appBarLayout.bottom == 0) navigable.onNavigate(direction)
            else appBarLayout.setExpanded(false, true)
        else -> navigable.onNavigate(direction)
      }
    }

    private inline fun WeakReference<ItemActivity>.ifAlive(block: ItemActivity.() -> Unit) {
      get()?.takeUnless { it.isActivityDestroyed }?.block()
    }
  }
}
