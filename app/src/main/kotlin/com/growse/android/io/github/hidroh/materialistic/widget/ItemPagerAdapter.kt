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
package com.growse.android.io.github.hidroh.materialistic.widget

import android.content.Context
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.growse.android.io.github.hidroh.materialistic.AppUtils.toggleFab
import com.growse.android.io.github.hidroh.materialistic.AppUtils.toggleFabAction
import com.growse.android.io.github.hidroh.materialistic.ItemFragment
import com.growse.android.io.github.hidroh.materialistic.LazyLoadFragment
import com.growse.android.io.github.hidroh.materialistic.Preferences.StoryViewMode
import com.growse.android.io.github.hidroh.materialistic.Preferences.navigationEnabled
import com.growse.android.io.github.hidroh.materialistic.R
import com.growse.android.io.github.hidroh.materialistic.Scrollable
import com.growse.android.io.github.hidroh.materialistic.WebFragment
import com.growse.android.io.github.hidroh.materialistic.data.Item
import com.growse.android.io.github.hidroh.materialistic.data.WebItem
import kotlin.math.min

class ItemPagerAdapter(private val context: Context, fm: FragmentManager, builder: Builder) :
    FragmentStatePagerAdapter(fm) {
  private val fragments = arrayOfNulls<Fragment>(3)
  private val item: WebItem = builder.item!!
  private val showArticle = builder.showArticle
  private val cacheMode = builder.cacheMode
  private val retainInstance = builder.retainInstance
  private val defaultItem =
      min(count - 1, if (builder.defaultViewMode == StoryViewMode.Comment) 0 else 1)
  private var tabListener: OnTabSelectedListener? = null

  override fun getItem(position: Int): Fragment {
    fragments[position]?.let {
      return it
    }
    val args =
        bundleOf(
            LazyLoadFragment.EXTRA_EAGER_LOAD to (defaultItem == position),
            LazyLoadFragment.EXTRA_RETAIN_INSTANCE to retainInstance,
        )
    return if (position == 0) {
      args.putParcelable(ItemFragment.EXTRA_ITEM, item)
      args.putInt(ItemFragment.EXTRA_CACHE_MODE, cacheMode)
      ItemFragment().apply { arguments = args }
    } else {
      args.putParcelable(WebFragment.EXTRA_ITEM, item)
      WebFragment().apply { arguments = args }
    }
  }

  override fun instantiateItem(container: ViewGroup, position: Int): Any =
      (super.instantiateItem(container, position) as Fragment).also { fragments[position] = it }

  override fun getCount(): Int = if (item.isStoryType && !showArticle) 1 else 2

  override fun getPageTitle(position: Int): CharSequence {
    if (position == 0) {
      if (item is Item) {
        val count = item.kidCount
        return context.resources.getQuantityString(R.plurals.comments_count, count, count)
      }
      return context.getString(R.string.title_activity_item)
    }
    return context.getString(if (item.isStoryType) R.string.article else R.string.full_text)
  }

  fun bind(
      viewPager: ViewPager,
      tabLayout: TabLayout,
      navigationFab: FloatingActionButton,
      genericFab: FloatingActionButton,
  ) {
    viewPager.setPageMargin(viewPager.resources.getDimensionPixelOffset(R.dimen.divider))
    viewPager.setPageMarginDrawable(R.color.blackT12)
    viewPager.setOffscreenPageLimit(2)
    viewPager.setAdapter(this)
    tabLayout.setupWithViewPager(viewPager)
    tabListener =
        object : TabLayout.ViewPagerOnTabSelectedListener(viewPager) {
          override fun onTabSelected(tab: TabLayout.Tab) {
            super.onTabSelected(tab)
            toggleFabs(viewPager.currentItem == 0, navigationFab, genericFab)
            (getItem(viewPager.currentItem) as LazyLoadFragment).loadNow()
          }

          override fun onTabReselected(tab: TabLayout.Tab?) {
            (getItem(viewPager.currentItem) as Scrollable).scrollToTop()
          }
        }
    tabLayout.addOnTabSelectedListener(tabListener)
    viewPager.setCurrentItem(defaultItem)
    toggleFabs(defaultItem == 0, navigationFab, genericFab)
  }

  fun toggleFabs(
      isComments: Boolean,
      navigationFab: FloatingActionButton,
      genericFab: FloatingActionButton,
  ) {
    toggleFab(
        navigationFab,
        isComments && navigationEnabled(navigationFab.context),
    )
    toggleFab(genericFab, true)
    toggleFabAction(genericFab, item, isComments)
  }

  fun unbind(tabLayout: TabLayout) {
    tabListener?.let { tabLayout.removeOnTabSelectedListener(it) }
  }

  class Builder {
    var item: WebItem? = null
    var showArticle: Boolean = false
    var cacheMode: Int = 0
    var defaultViewMode: StoryViewMode? = null
    var retainInstance: Boolean = false

    fun setItem(item: WebItem): Builder {
      this.item = item
      return this
    }

    fun setShowArticle(showArticle: Boolean): Builder {
      this.showArticle = showArticle
      return this
    }

    fun setCacheMode(cacheMode: Int): Builder {
      this.cacheMode = cacheMode
      return this
    }

    fun setDefaultViewMode(viewMode: StoryViewMode?): Builder {
      this.defaultViewMode = viewMode
      return this
    }

    fun setRetainInstance(retainInstance: Boolean): Builder {
      this.retainInstance = retainInstance
      return this
    }
  }
}
