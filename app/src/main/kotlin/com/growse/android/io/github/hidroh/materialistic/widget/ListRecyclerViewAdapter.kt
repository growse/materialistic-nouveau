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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.growse.android.io.github.hidroh.materialistic.AlertDialogBuilder
import com.growse.android.io.github.hidroh.materialistic.AppUtils
import com.growse.android.io.github.hidroh.materialistic.AppUtils.multiWindowIntent
import com.growse.android.io.github.hidroh.materialistic.CustomTabsDelegate
import com.growse.android.io.github.hidroh.materialistic.ItemActivity
import com.growse.android.io.github.hidroh.materialistic.MultiPaneListener
import com.growse.android.io.github.hidroh.materialistic.Preferences
import com.growse.android.io.github.hidroh.materialistic.R
import com.growse.android.io.github.hidroh.materialistic.accounts.UserServices
import com.growse.android.io.github.hidroh.materialistic.data.FavoriteManager
import com.growse.android.io.github.hidroh.materialistic.data.Item
import com.growse.android.io.github.hidroh.materialistic.data.ItemManager
import com.growse.android.io.github.hidroh.materialistic.data.WebItem

/**
 * Base [RecyclerView.Adapter] class for list items
 *
 * @param <VH> view holder type, should contain title, posted, source and comment views
 * @param <T> item type, should provide title, posted, source </T></VH>
 */
abstract class ListRecyclerViewAdapter<VH : ListRecyclerViewAdapter.ItemViewHolder, T : WebItem>(
    protected var context: Context,
    val mPopupMenu: PopupMenu,
    val mAlertDialogBuilder: AlertDialogBuilder<*>,
    val mUserServices: UserServices,
    val mFavoriteManager: FavoriteManager,
) : RecyclerView.Adapter<VH?>() {
  private var mCustomTabsDelegate: CustomTabsDelegate? = null
  private val mMultiPaneListener: MultiPaneListener = this.context as MultiPaneListener
  var mInflater: LayoutInflater = AppUtils.createLayoutInflater(this.context)
  private var mLastSelectedPosition = -1
  private var mCardViewEnabled = true
  private var mHotThreshold = Int.MAX_VALUE
  private val mPreferenceObservable = Preferences.Observable()
  private var mMultiWindowEnabled: Boolean

  init {
    mMultiWindowEnabled = Preferences.multiWindowEnabled(this.context)
  }

  override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
    super.onAttachedToRecyclerView(recyclerView)
    mPreferenceObservable.subscribe(
        context,
        { _: Int, _: Boolean -> mMultiWindowEnabled = Preferences.multiWindowEnabled(context) },
        R.string.pref_multi_window,
    )
  }

  override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
    super.onDetachedFromRecyclerView(recyclerView)
    mPreferenceObservable.unsubscribe(context)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
    val holder = create(parent, viewType)
    if (viewType == VIEW_TYPE_FLAT) {
      holder.flatten()
    }
    return holder
  }

  override fun onBindViewHolder(holder: VH, position: Int) {
    val item = getItem(position)
    clearViewHolder(holder)
    if (!isItemAvailable(item!!)) {
      loadItem(holder.adapterPosition)
      return
    }
    // TODO naive launch priority for now
    mCustomTabsDelegate!!.mayLaunchUrl(Uri.parse(item.url), null, null)
    holder.bind(
        item,
        mHotThreshold,
        mCardViewEnabled,
        isSelected(item.id),
        { handleItemClick(item, holder) },
        { openItem(item) },
    )
    bindItem(holder, position)
  }

  override fun getItemViewType(position: Int): Int {
    return if (mCardViewEnabled) VIEW_TYPE_CARD else VIEW_TYPE_FLAT
  }

  override fun getItemId(position: Int): Long {
    return getItem(position)!!.longId
  }

  @set:SuppressLint("NotifyDataSetChanged")
  var isCardViewEnabled: Boolean
    get() = mCardViewEnabled
    set(cardViewEnabled) {
      this.mCardViewEnabled = cardViewEnabled
      notifyDataSetChanged()
    }

  fun setCustomTabsDelegate(customTabsDelegate: CustomTabsDelegate) {
    mCustomTabsDelegate = customTabsDelegate
  }

  fun setHotThresHold(hotThresHold: Int) {
    mHotThreshold = hotThresHold
  }

  open fun saveState(): Bundle {
    val savedState = Bundle()
    savedState.putInt(STATE_LAST_SELECTION_POSITION, mLastSelectedPosition)
    return savedState
  }

  open fun restoreState(savedState: Bundle?) {
    if (savedState == null) {
      return
    }
    mLastSelectedPosition = savedState.getInt(STATE_LAST_SELECTION_POSITION)
  }

  val isAttached: Boolean
    get() = true

  protected abstract fun create(parent: ViewGroup?, viewType: Int): VH

  protected open fun loadItem(adapterPosition: Int) {
    // override to load item if needed
  }

  protected abstract fun bindItem(holder: VH, position: Int)

  protected abstract fun isItemAvailable(item: T): Boolean

  private fun clearViewHolder(holder: VH) {
    holder.clear()
  }

  /**
   * Checks if item with given ID has been selected
   *
   * @param itemId item ID to check
   * @return true if selected, false otherwise or if selection is disabled
   */
  protected open fun isSelected(itemId: String): Boolean {
    return mMultiPaneListener.isMultiPane &&
        mMultiPaneListener.selectedItem != null &&
        itemId == mMultiPaneListener.selectedItem!!.id
  }

  /**
   * Gets item at position
   *
   * @param position item position
   * @return item at given position or null
   */
  protected abstract fun getItem(position: Int): T?

  /**
   * Handles item click
   *
   * @param item clicked item
   * @param holder clicked item view holder
   */
  protected open fun handleItemClick(item: T, holder: VH) {
    mMultiPaneListener.onItemSelected(item)
    if (isSelected(item.id)) {
      notifyItemChanged(holder.adapterPosition)
      if (mLastSelectedPosition >= 0) {
        notifyItemChanged(mLastSelectedPosition)
      }
      mLastSelectedPosition = holder.adapterPosition
    }
  }

  @get:ItemManager.CacheMode protected abstract val itemCacheMode: Int

  private fun openItem(item: T?) {
    val intent =
        Intent(context, ItemActivity::class.java)
            .putExtra(ItemActivity.EXTRA_CACHE_MODE, this.itemCacheMode)
            .putExtra(ItemActivity.EXTRA_ITEM, item)
            .putExtra(ItemActivity.EXTRA_OPEN_COMMENTS, true)
    context.startActivity(
        if (mMultiWindowEnabled)
            multiWindowIntent(
                context as Activity,
                intent,
            )
        else intent,
    )
  }

  /** Base [RecyclerView.ViewHolder] class for list item view */
  open class ItemViewHolder internal constructor(itemView: View) :
      RecyclerView.ViewHolder(itemView) {
    private val mStoryView: StoryView = itemView.findViewById(R.id.story_view)
    private val mCardView: FlatCardView = itemView as FlatCardView
    private val mCardElevation: Int =
        itemView.context.resources.getDimensionPixelSize(R.dimen.cardview_elevation)

    interface ShowMoreOptionsListener {
      fun showMoreOptions(anchor: View)
    }

    fun bind(
        item: WebItem,
        hotThreshold: Int,
        selected: Boolean,
        cardViewEnabled: Boolean,
        itemClickListener: View.OnClickListener?,
        commentClickListener: View.OnClickListener?,
    ) {
      mCardView.cardElevation =
          (if (selected) mCardElevation * 2 else (if (cardViewEnabled) mCardElevation else 0))
              .toFloat()
      mStoryView.setStory(item, hotThreshold)
      mStoryView.setChecked(selected)
      itemView.setOnClickListener(itemClickListener)
      mStoryView.setOnCommentClickListener(commentClickListener)
    }

    fun clear() {
      mCardView.cardElevation = 0f
      mStoryView.reset()
      itemView.setOnClickListener(null)
      itemView.setOnLongClickListener(null)
    }

    fun flatten() {
      mCardView.flatten()
    }

    fun animateVote(score: Int) {
      mStoryView.animateVote(score)
    }

    fun setViewed(viewed: Boolean) {
      mStoryView.setViewed(viewed)
    }

    fun setFavorite(favorite: Boolean) {
      mStoryView.setFavorite(favorite)
    }

    fun setUpdated(story: Item, updated: Boolean, change: Int) {
      mStoryView.setUpdated(story, updated, change)
    }

    fun setChecked(checked: Boolean) {
      mStoryView.setChecked(checked)
    }

    fun setOnLongClickListener(longClickListener: OnLongClickListener?) {
      itemView.setOnLongClickListener(longClickListener)
    }

    fun bindMoreOptions(listener: ShowMoreOptionsListener, allowLongClick: Boolean) {
      if (allowLongClick) {
        itemView.setOnLongClickListener {
          listener.showMoreOptions(mStoryView.moreOptions)
          true
        }
      }
      mStoryView.moreOptions.setOnClickListener { v: View -> listener.showMoreOptions(v) }
    }
  }

  companion object {
    private const val STATE_LAST_SELECTION_POSITION = "state:lastSelectedPosition"
    private const val VIEW_TYPE_CARD = 0
    private const val VIEW_TYPE_FLAT = 1
  }
}
