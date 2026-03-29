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

package io.github.hidroh.materialistic.widget

import android.content.Context
import android.os.Bundle
import androidx.annotation.NonNull
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import android.view.ViewGroup

import io.github.hidroh.materialistic.AppUtils
import io.github.hidroh.materialistic.ItemFragment
import io.github.hidroh.materialistic.LazyLoadFragment
import io.github.hidroh.materialistic.Preferences
import io.github.hidroh.materialistic.R
import io.github.hidroh.materialistic.Scrollable
import io.github.hidroh.materialistic.WebFragment
import io.github.hidroh.materialistic.annotation.Synthetic
import io.github.hidroh.materialistic.data.Item
import io.github.hidroh.materialistic.data.WebItem

open class ItemPagerAdapter : FragmentStatePagerAdapter() {
    private val mFragments: Array<Fragment> = new Fragment[3]
    private var mContext: Context? = null
    private var mItem: WebItem? = null
    private val mShowArticle: Boolean = false
    private val mCacheMode: Int = 0
    private val mDefaultItem: Int = 0
    private val mRetainInstance: Boolean = false
    private var mTabListener: TabLayout.OnTabSelectedListener? = null

    constructor(context: Context, fm: FragmentManager, builder: Builder) {
        super(fm)
        mContext = context
        mItem = builder.item
        mShowArticle = builder.showArticle
        mCacheMode = builder.cacheMode
        mRetainInstance = builder.retainInstance
        mDefaultItem = Math.min(getCount()-1,
                builder.defaultViewMode == Preferences.StoryViewMode.Comment ? 0 : 1)
    }

    override fun getItem(position: Int): Fragment {
        if (mFragments[position] != null) {
            return mFragments[position]
        }
        String fragmentName
        Bundle args = Bundle()
        args.putBoolean(LazyLoadFragment.EXTRA_EAGER_LOAD, mDefaultItem == position)
        if (position == 0) {
            args.putParcelable(ItemFragment.EXTRA_ITEM, mItem)
            args.putInt(ItemFragment.EXTRA_CACHE_MODE, mCacheMode)
            args.putBoolean(ItemFragment.EXTRA_RETAIN_INSTANCE, mRetainInstance)
            fragmentName = ItemFragment::class.java.getName()
        } else {
            args.putParcelable(WebFragment.EXTRA_ITEM, mItem)
            args.putBoolean(WebFragment.EXTRA_RETAIN_INSTANCE, mRetainInstance)
            fragmentName = WebFragment::class.java.getName()
        }
        return Fragment.instantiate(mContext, fragmentName, args)
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        mFragments[position] = (Fragment) super.instantiateItem(container, position)
        return mFragments[position]
    }

    override fun getCount(): Int {
        return mItem.isStoryType() && !mShowArticle ? 1 : 2
    }

    override fun getPageTitle(position: Int): CharSequence {
        if (position == 0) {
            if (mItem is Item) {
                int count = ((Item) mItem).getKidCount()
                return mContext.getResources()
                        .getQuantityString(R.plurals.comments_count, count, count)
            }
            return mContext.getString(R.string.title_activity_item)
        }
        return mContext.getString(mItem.isStoryType() ? R.string.article : R.string.full_text)
    }

    fun bind(viewPager: ViewPager, tabLayout: TabLayout, navigationFab: FloatingActionButton, genericFab: FloatingActionButton) {
        viewPager.setPageMargin(viewPager.getResources().getDimensionPixelOffset(R.dimen.divider))
        viewPager.setPageMarginDrawable(R.color.blackT12)
        viewPager.setOffscreenPageLimit(2)
        viewPager.setAdapter(this)
        tabLayout.setupWithViewPager(viewPager)
        mTabListener = TabLayout.ViewPagerOnTabSelectedListener(viewPager) {
            public void onTabSelected(TabLayout.Tab tab) {
                super.onTabSelected(tab)
                toggleFabs(viewPager.getCurrentItem() == 0, navigationFab, genericFab)
                Fragment fragment = getItem(viewPager.getCurrentItem())
                if (fragment != null) {
                    ((LazyLoadFragment) fragment).loadNow()
                }
            }

            public void onTabReselected(TabLayout.Tab tab) {
                Fragment fragment = getItem(viewPager.getCurrentItem())
                if (fragment != null) {
                    ((Scrollable) fragment).scrollToTop()
                }
            }
        }
        tabLayout.addOnTabSelectedListener(mTabListener)
        viewPager.setCurrentItem(mDefaultItem)
        toggleFabs(mDefaultItem == 0, navigationFab, genericFab)

    }

    @Synthetic
    fun toggleFabs(isComments: Boolean, navigationFab: FloatingActionButton, genericFab: FloatingActionButton) {
        AppUtils.toggleFab(navigationFab, isComments &&
                Preferences.navigationEnabled(navigationFab.getContext()))
        AppUtils.toggleFab(genericFab, true)
        AppUtils.toggleFabAction(genericFab, mItem, isComments)
    }

    fun unbind(tabLayout: TabLayout) {
        if (mTabListener != null) {
            tabLayout.removeOnTabSelectedListener(mTabListener)
        }
    }

    open class Builder {
        var item: WebItem? = null
        var showArticle: Boolean = false
        var cacheMode: Int = 0
        var defaultViewMode: Preferences.StoryViewMode? = null
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

        fun setDefaultViewMode(viewMode: Preferences.StoryViewMode): Builder {
            this.defaultViewMode = viewMode
            return this
        }

        fun setRetainInstance(retainInstance: Boolean): Builder {
            this.retainInstance = retainInstance
            return this
        }
    }
}
