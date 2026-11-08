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
import android.app.SearchManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.growse.android.io.github.hidroh.materialistic.KeyDelegate.BackInterceptor
import com.growse.android.io.github.hidroh.materialistic.Preferences.StoryViewMode
import com.growse.android.io.github.hidroh.materialistic.data.ItemManager
import com.growse.android.io.github.hidroh.materialistic.data.SessionManager
import com.growse.android.io.github.hidroh.materialistic.data.WebItem
import com.growse.android.io.github.hidroh.materialistic.widget.ItemPagerAdapter
import com.growse.android.io.github.hidroh.materialistic.widget.NavFloatingActionButton
import com.growse.android.io.github.hidroh.materialistic.widget.PopupMenu
import com.growse.android.io.github.hidroh.materialistic.widget.ViewPager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** List activity that renders alternative layouts for portrait/landscape */
@AndroidEntryPoint
abstract class BaseListActivity : DrawerActivity(), MultiPaneListener {
  @Inject lateinit var actionViewResolver: ActionViewResolver
  @Inject lateinit var popupMenu: PopupMenu
  @Inject lateinit var sessionManager: SessionManager
  @Inject lateinit var customTabsDelegate: CustomTabsDelegate
  @Inject lateinit var keyDelegate: KeyDelegate

  final override var isMultiPane = false
    private set

  final override var selectedItem: WebItem? = null
    private set

  private var storyViewMode: StoryViewMode? = null
  private var externalBrowser = false
  private var multiWindowEnabled = false
  private var fullscreen = false
  private lateinit var appBar: AppBarLayout

  // multi-pane only
  private lateinit var viewPager: ViewPager
  private lateinit var tabLayout: TabLayout
  private lateinit var replyButton: FloatingActionButton
  private lateinit var navButton: NavFloatingActionButton
  private lateinit var listView: View
  private var adapter: ItemPagerAdapter? = null

  private val preferenceObservable = Preferences.Observable()
  private val receiver: BroadcastReceiver =
      object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
          fullscreen = intent.getBooleanExtra(WebFragment.EXTRA_FULLSCREEN, false)
          setFullscreen()
        }
      }
  private val backPressedCallback =
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          if (isMultiPane && fullscreen) {
            LocalBroadcastManager.getInstance(this@BaseListActivity)
                .sendBroadcast(
                    Intent(WebFragment.ACTION_FULLSCREEN)
                        .putExtra(WebFragment.EXTRA_FULLSCREEN, false),
                )
          } else {
            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
            isEnabled = true
          }
        }
      }

  /**
   * Checks if activity should have search view
   *
   * @return true if is searchable, false otherwise
   */
  protected open val isSearchable: Boolean
    get() = true

  /**
   * Gets default title to be displayed in list-only layout
   *
   * @return displayed title
   */
  protected abstract val defaultTitle: String

  /**
   * Gets cache mode for [ItemManager]
   *
   * @return cache mode
   */
  @get:ItemManager.CacheMode
  protected open val itemCacheMode: Int
    get() = ItemManager.MODE_DEFAULT

  /**
   * Creates list fragment to host list data
   *
   * @return list fragment
   */
  protected abstract fun instantiateListFragment(): Fragment

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    onBackPressedDispatcher.addCallback(this, backPressedCallback)
    setContentView(R.layout.activity_list)
    title = defaultTitle
    val toolbar = findViewById<Toolbar>(R.id.toolbar)
    setSupportActionBar(toolbar)
    supportActionBar.displayOptions =
        ActionBar.DISPLAY_SHOW_HOME or ActionBar.DISPLAY_HOME_AS_UP or ActionBar.DISPLAY_SHOW_TITLE
    AppUtils.applyToolbarTopInset(toolbar)
    toolbar.setOnClickListener { scrollableList?.scrollToTop() }
    appBar = findViewById(R.id.appbar)
    isMultiPane = resources.getBoolean(R.bool.multi_pane)
    if (isMultiPane) {
      LocalBroadcastManager.getInstance(this)
          .registerReceiver(receiver, IntentFilter(WebFragment.ACTION_FULLSCREEN))
      listView = findViewById(android.R.id.list)
      tabLayout = findViewById(R.id.tab_layout)
      tabLayout.visibility = View.GONE
      viewPager = findViewById(R.id.content)
      viewPager.visibility = View.GONE
      replyButton = findViewById(R.id.reply_button)
      navButton = findViewById(R.id.navigation_button)
      navButton.setNavigable { direction ->
        // if callback is fired navigable should not be null
        ((viewPager.adapter as ItemPagerAdapter).getItem(0) as Navigable).onNavigate(direction)
      }
      AppUtils.toggleFab(navButton, false)
      AppUtils.toggleFab(replyButton, false)
    }
    multiWindowEnabled = Preferences.multiWindowEnabled(this)
    storyViewMode = Preferences.getDefaultStoryView(this)
    externalBrowser = Preferences.externalBrowserEnabled(this)
    if (savedInstanceState == null) {
      supportFragmentManager
          .beginTransaction()
          .replace(android.R.id.list, instantiateListFragment(), LIST_FRAGMENT_TAG)
          .commit()
    } else {
      selectedItem =
          BundleCompat.getParcelable(savedInstanceState, STATE_SELECTED_ITEM, WebItem::class.java)
      fullscreen = savedInstanceState.getBoolean(STATE_FULLSCREEN)
      if (isMultiPane) {
        openMultiPaneItem()
      } else {
        unbindViewPager()
      }
    }
    preferenceObservable.subscribe(
        this,
        { key, _ -> onPreferenceChanged(key) },
        R.string.pref_navigation,
        R.string.pref_external,
        R.string.pref_story_display,
        R.string.pref_multi_window,
    )
  }

  override fun onPostCreate(savedInstanceState: Bundle?) {
    super.onPostCreate(savedInstanceState)
    if (Preferences.isReleaseNotesSeen(this)) {
      return
    }
    val snackbar =
        Snackbar.make(
            findViewById(R.id.content_frame),
            R.string.hint_update,
            Snackbar.LENGTH_INDEFINITE,
        )
    snackbar
        .setAction(R.string.title_activity_release) {
          snackbar.dismiss()
          startActivity(Intent(this, ReleaseNotesActivity::class.java))
        }
        .setActionTextColor(ContextCompat.getColor(this, R.color.orange500))
        .addCallback(
            object : Snackbar.Callback() {
              override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                Preferences.setReleaseNotesSeen(this@BaseListActivity)
              }
            },
        )
        .show()
  }

  override fun onStart() {
    super.onStart()
    customTabsDelegate.bindCustomTabsService(this)
    keyDelegate.attach(this)
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    if (isMultiPane) {
      menuInflater.inflate(R.menu.menu_item_compact, menu)
    }
    if (isSearchable) {
      menuInflater.inflate(R.menu.menu_search, menu)
      val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager
      val searchView =
          actionViewResolver.getActionView(menu.findItem(R.id.menu_search)) as SearchView
      searchView.setSearchableInfo(
          searchManager.getSearchableInfo(ComponentName(this, SearchActivity::class.java)),
      )
      searchView.isIconified = true
      searchView.setQuery("", false)
    }
    return super.onCreateOptionsMenu(menu)
  }

  override fun onPrepareOptionsMenu(menu: Menu): Boolean {
    if (isMultiPane) {
      menu.findItem(R.id.menu_share).isVisible = selectedItem != null
      menu.findItem(R.id.menu_external).isVisible = selectedItem != null
    }
    return isSearchable || super.onPrepareOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val selectedItem = selectedItem
    when (item.itemId) {
      R.id.menu_share -> {
        if (selectedItem != null) {
          AppUtils.share(this, popupMenu, menuAnchor(R.id.menu_share), selectedItem)
        }
        return true
      }
      R.id.menu_external -> {
        if (selectedItem != null) {
          AppUtils.openExternal(
              this,
              popupMenu,
              menuAnchor(R.id.menu_external),
              selectedItem,
              customTabsDelegate.session,
          )
        }
        return true
      }
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putParcelable(STATE_SELECTED_ITEM, selectedItem)
    outState.putBoolean(STATE_FULLSCREEN, fullscreen)
  }

  override fun onStop() {
    super.onStop()
    customTabsDelegate.unbindCustomTabsService(this)
    keyDelegate.detach(this)
  }

  override fun onDestroy() {
    super.onDestroy()
    preferenceObservable.unsubscribe(this)
    if (isMultiPane) {
      LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
    keyDelegate.setScrollable(scrollableList, appBar)
    keyDelegate.setBackInterceptor(backInterceptor)
    return keyDelegate.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
  }

  override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean =
      keyDelegate.onKeyUp(keyCode, event) || super.onKeyUp(keyCode, event)

  override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean =
      keyDelegate.onKeyLongPress(keyCode, event) || super.onKeyLongPress(keyCode, event)

  // setSupportActionBar() is always called in onCreate, so subclasses can rely on it
  override fun getSupportActionBar(): ActionBar = super.getSupportActionBar()!!

  override fun onItemSelected(item: WebItem?) {
    val previousItem = selectedItem
    selectedItem = item
    if (isMultiPane) {
      if (previousItem != null && item != null && item.id == previousItem.id) {
        return
      }
      if ((previousItem == null) != (item == null)) {
        invalidateOptionsMenu()
      }
      openMultiPaneItem()
    } else if (item != null) {
      openSinglePaneItem(item)
    }
  }

  private fun setFullscreen() {
    appBar.setExpanded(!fullscreen, true)
    tabLayout.visibility = if (fullscreen) View.GONE else View.VISIBLE
    listView.visibility = if (fullscreen) View.GONE else View.VISIBLE
    keyDelegate.setAppBarEnabled(!fullscreen)
    viewPager.setSwipeEnabled(!fullscreen)
    AppUtils.toggleFab(replyButton, !fullscreen)
  }

  /** Anchors popups to the menu item's view, or the toolbar when it's in the overflow menu. */
  private fun menuAnchor(menuItemId: Int): View =
      findViewById(menuItemId) ?: findViewById(R.id.toolbar)

  // TODO landscape behavior?
  private val scrollableList: Scrollable?
    get() = supportFragmentManager.findFragmentByTag(LIST_FRAGMENT_TAG) as Scrollable?

  private val backInterceptor: BackInterceptor?
    get() {
      if (!isMultiPane || viewPager.currentItem < 0) {
        return null
      }
      val adapter = viewPager.adapter as? ItemPagerAdapter? ?: return null
      return adapter.getItem(viewPager.currentItem) as? BackInterceptor
    }

  private fun openSinglePaneItem(item: WebItem) {
    if (externalBrowser && storyViewMode != StoryViewMode.Comment) {
      AppUtils.openWebUrlExternal(this, item, item.url, customTabsDelegate.session)
    } else {
      val intent =
          Intent(this, ItemActivity::class.java)
              .putExtra(ItemActivity.EXTRA_CACHE_MODE, itemCacheMode)
              .putExtra(ItemActivity.EXTRA_ITEM, item)
      startActivity(if (multiWindowEnabled) AppUtils.multiWindowIntent(this, intent) else intent)
    }
  }

  private fun openMultiPaneItem() {
    val selectedItem = selectedItem
    if (selectedItem == null) {
      title = defaultTitle
      findViewById<View>(R.id.empty_selection).visibility = View.VISIBLE
      tabLayout.visibility = View.GONE
      viewPager.visibility = View.GONE
      viewPager.adapter = null
      AppUtils.toggleFab(navButton, false)
      AppUtils.toggleFab(replyButton, false)
    } else {
      title = selectedItem.displayedTitle
      findViewById<View>(R.id.empty_selection).visibility = View.GONE
      tabLayout.visibility = View.VISIBLE
      viewPager.visibility = View.VISIBLE
      bindViewPager(selectedItem)
      sessionManager.view(selectedItem.id)
    }
  }

  private fun bindViewPager(item: WebItem) {
    adapter?.unbind(tabLayout)
    adapter =
        ItemPagerAdapter(
                this,
                supportFragmentManager,
                ItemPagerAdapter.Builder()
                    .setItem(item)
                    .setCacheMode(itemCacheMode)
                    .setShowArticle(true)
                    .setDefaultViewMode(storyViewMode),
            )
            .also { it.bind(viewPager, tabLayout, navButton, replyButton) }
    if (fullscreen) {
      setFullscreen()
    }
  }

  @SuppressLint("RestrictedApi")
  private fun unbindViewPager() {
    // fragment manager always restores view pager fragments,
    // even when view pager no longer exists (e.g. after rotation),
    // so we have to explicitly remove those with view pager ID
    val transaction = supportFragmentManager.beginTransaction()
    supportFragmentManager.fragments
        .filter { it.id == R.id.content }
        .forEach { transaction.remove(it) }
    transaction.commit()
  }

  private fun onPreferenceChanged(key: Int) {
    when (key) {
      R.string.pref_external -> externalBrowser = Preferences.externalBrowserEnabled(this)
      R.string.pref_story_display -> storyViewMode = Preferences.getDefaultStoryView(this)
      R.string.pref_multi_window -> multiWindowEnabled = Preferences.multiWindowEnabled(this)
      R.string.pref_navigation -> {
        val enabled = Preferences.navigationEnabled(this)
        if (!enabled) {
          NavFloatingActionButton.resetPosition(this)
        }
        if (isMultiPane) {
          AppUtils.toggleFab(navButton, viewPager.currentItem == 0 && enabled)
        }
      }
    }
  }

  companion object {
    @JvmField
    protected val LIST_FRAGMENT_TAG: String =
        BaseListActivity::class.java.name + ".LIST_FRAGMENT_TAG"
    private const val STATE_SELECTED_ITEM = "state:selectedItem"
    private const val STATE_FULLSCREEN = "state:fullscreen"
  }
}
