/*
 * Copyright (c) 2016 Ha Duy Trung
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
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.collection.ArrayMap
import androidx.collection.ArraySet
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
import com.growse.android.io.github.hidroh.materialistic.ActivityModule
import com.growse.android.io.github.hidroh.materialistic.AppUtils
import com.growse.android.io.github.hidroh.materialistic.ComposeActivity
import com.growse.android.io.github.hidroh.materialistic.Preferences
import com.growse.android.io.github.hidroh.materialistic.Preferences.SwipeAction
import com.growse.android.io.github.hidroh.materialistic.R
import com.growse.android.io.github.hidroh.materialistic.UserActivity
import com.growse.android.io.github.hidroh.materialistic.accounts.UserServices
import com.growse.android.io.github.hidroh.materialistic.annotation.Synthetic
import com.growse.android.io.github.hidroh.materialistic.data.FavoriteManager.Companion.isAdded
import com.growse.android.io.github.hidroh.materialistic.data.FavoriteManager.Companion.isCleared
import com.growse.android.io.github.hidroh.materialistic.data.FavoriteManager.Companion.isRemoved
import com.growse.android.io.github.hidroh.materialistic.data.Item
import com.growse.android.io.github.hidroh.materialistic.data.ItemManager
import com.growse.android.io.github.hidroh.materialistic.data.MaterialisticDatabase
import com.growse.android.io.github.hidroh.materialistic.data.ResponseListener
import com.growse.android.io.github.hidroh.materialistic.data.SessionManager
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Named

class StoryRecyclerViewAdapter(context: Context) :
    ListRecyclerViewAdapter<ListRecyclerViewAdapter.ItemViewHolder?, Item?>(context) {
  private val VOTED = Any()
  private val mAutoViewScrollListener: RecyclerView.OnScrollListener =
      object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
          if (dy > 0) { // scrolling down
            markAsViewed(
                (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition() -
                    1
            )
          }
        }
      }
  private val mPrefObservable = Preferences.Observable()
  private val mSortedListCallback: SortedList.Callback<Item?> =
      object : SortedListAdapterCallback<Item>(this) {
        override fun compare(o1: Item, o2: Item): Int {
          return o1.getRank() - o2.getRank()
        }

        override fun areContentsTheSame(item1: Item, item2: Item): Boolean {
          return areItemsTheSame(item1, item2) &&
              item1.getLocalRevision() == item2.getLocalRevision()
        }

        override fun areItemsTheSame(item1: Item, item2: Item): Boolean {
          return item1.getLongId() == item2.getLongId()
        }
      }

  @JvmField @Inject @Named(ActivityModule.HN) var mItemManager: ItemManager? = null

  @JvmField @Inject var mSessionManager: SessionManager? = null

  @Synthetic val items: SortedList<Item> = SortedList<Item>(Item::class.java, mSortedListCallback)

  @Synthetic val mAdded: ArraySet<Item?> = ArraySet<Item?>()

  @Synthetic val mPromoted: ArrayMap<String?, Int?> = ArrayMap<String?, Int?>()

  @Synthetic var mFavoriteRevision: Int = 1
  private var mUsername: String? = null
  private var mHighlightUpdated = true
  private var mShowAll = true
  private var mCacheMode = ItemManager.MODE_DEFAULT
  private val mItemTouchHelper: ItemTouchHelper

  @Synthetic internal var callback: ItemTouchHelperCallback

  @SuppressLint("NotifyDataSetChanged")
  private val mObserver = Observer { uri: Uri? ->
    if (uri == null) {
      return@Observer
    }
    if (isCleared(uri)) {
      mFavoriteRevision++ // invalidate all favorite statuses
      notifyDataSetChanged()
      return@Observer
    }
    var position = RecyclerView.NO_POSITION
    for (i in 0..<items!!.size()) {
      if (TextUtils.equals(items.get(i).getId(), uri.lastPathSegment)) {
        position = i
        break
      }
    }
    if (position == RecyclerView.NO_POSITION) {
      return@Observer
    }
    val item = items.get(position)
    if (isAdded(uri)) {
      item.setFavorite(true)
      item.setLocalRevision(mFavoriteRevision)
    } else if (isRemoved(uri)) {
      item.setFavorite(false)
      item.setLocalRevision(mFavoriteRevision)
    } else {
      item.setIsViewed(true)
    }
    notifyItemChanged(position)
  }
  private var mUpdateListener: UpdateListener? = null

  interface UpdateListener {
    fun onUpdated(showAll: Boolean, itemCount: Int, actionClickListener: View.OnClickListener?)
  }

  init {
    callback =
        object : ItemTouchHelperCallback(context, Preferences.getListSwipePreferences(context)) {
          override fun getSwipeDirs(
              recyclerView: RecyclerView,
              viewHolder: RecyclerView.ViewHolder,
          ): Int {
            val item = getItem(viewHolder.absoluteAdapterPosition)
            saved = item?.isFavorite() == true
            return checkSwipeDir(0, ItemTouchHelper.LEFT, callback.leftSwipeAction, item) or
                checkSwipeDir(0, ItemTouchHelper.RIGHT, callback.rightSwipeAction, item)
          }

          override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val action =
                if (direction == ItemTouchHelper.LEFT) this.leftSwipeAction
                else this.rightSwipeAction
            val item = getItem(viewHolder.absoluteAdapterPosition) ?: return
            when (action) {
              SwipeAction.Save -> toggleSave(item)
              SwipeAction.Refresh -> refresh(item, viewHolder)
              SwipeAction.Vote -> {
                notifyItemChanged(viewHolder.absoluteAdapterPosition)
                vote(item, viewHolder)
              }

              SwipeAction.Share -> {
                notifyItemChanged(viewHolder.absoluteAdapterPosition)
                AppUtils.share(context, item.getDisplayedTitle(), item.getUrl())
              }

              else -> {}
            }
          }

          private fun checkSwipeDir(
              swipeDirs: Int,
              swipeDir: Int,
              action: SwipeAction,
              item: Item?,
          ): Int {
            var swipeDirs = swipeDirs
            when (action) {
              SwipeAction.None -> {}
              SwipeAction.Vote ->
                  if (!item?.isVoted()!! && !item.isPendingVoted()) {
                    swipeDirs = swipeDirs or swipeDir
                  }

              else -> swipeDirs = swipeDirs or swipeDir
            }
            return swipeDirs
          }
        }
    mItemTouchHelper = ItemTouchHelper(callback)
  }

  @SuppressLint("NotifyDataSetChanged")
  override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
    super.onAttachedToRecyclerView(recyclerView)
    MaterialisticDatabase.getInstance(recyclerView.context).liveData.observeForever(mObserver)
    mItemTouchHelper.attachToRecyclerView(recyclerView)
    toggleAutoMarkAsViewed(recyclerView)
    mPrefObservable.subscribe(
        recyclerView.context,
        { _: Int, _: Boolean ->
          callback.setSwipePreferences(
              recyclerView.context,
              Preferences.getListSwipePreferences(recyclerView.context),
          )
          notifyDataSetChanged()
        },
        R.string.pref_list_swipe_left,
        R.string.pref_list_swipe_right,
    )
  }

  override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
    super.onDetachedFromRecyclerView(recyclerView)
    MaterialisticDatabase.getInstance(recyclerView.context).liveData.removeObserver(mObserver)
    mItemTouchHelper.attachToRecyclerView(null)
    mPrefObservable.unsubscribe(recyclerView.context)
  }

  override fun create(parent: ViewGroup?, viewType: Int): ItemViewHolder {
    return ItemViewHolder(mInflater.inflate(R.layout.item_story, parent, false))
  }

  override fun onBindViewHolder(
      holder: ItemViewHolder,
      position: Int,
      payloads: MutableList<Any?>,
  ) {
    if (payloads.contains(VOTED)) {
      holder.animateVote(getItem(position)!!.getScore())
    } else {
      super.onBindViewHolder(holder, position, payloads)
    }
  }

  override fun getItemCount(): Int {
    if (mShowAll) {
      return items.size()
    } else {
      return mAdded.size
    }
  }

  override fun saveState(): Bundle {
    val savedState = super.saveState()
    savedState.putBoolean(STATE_SHOW_ALL, mShowAll)
    savedState.putString(STATE_USERNAME, mUsername)
    return savedState
  }

  override fun restoreState(savedState: Bundle?) {
    if (savedState == null) {
      return
    }
    super.restoreState(savedState)
    mShowAll = savedState.getBoolean(STATE_SHOW_ALL, true)
    mUsername = savedState.getString(STATE_USERNAME)
  }

  fun setUpdateListener(updateListener: UpdateListener?) {
    mUpdateListener = updateListener
  }

  fun setItems(items: MutableList<Item>) {
    setUpdated(items)
    items.clear()
    items.addAll(items)
  }

  fun setHighlightUpdated(highlightUpdated: Boolean) {
    mHighlightUpdated = highlightUpdated
  }

  fun setShowAll(showAll: Boolean) {
    mShowAll = showAll
  }

  @SuppressLint("NotifyDataSetChanged")
  fun initDisplayOptions(recyclerView: RecyclerView) {
    mHighlightUpdated = Preferences.highlightUpdatedEnabled(recyclerView.context)
    mUsername = Preferences.getUsername(recyclerView.context)
    if (isAttached) {
      toggleAutoMarkAsViewed(recyclerView)
      notifyDataSetChanged()
    }
  }

  fun toggleSave(itemId: String?) {
    var position = RecyclerView.NO_POSITION
    for (i in 0..<items.size()) {
      if (TextUtils.equals(items.get(i).getId(), itemId)) {
        position = i
        break
      }
    }
    if (position == RecyclerView.NO_POSITION) {
      return
    }
    toggleSave(items.get(position))
  }

  override fun loadItem(adapterPosition: Int) {
    val item = getItem(adapterPosition)
    if (item == null || item.getLocalRevision() == 0) {
      return
    }
    item.setLocalRevision(0)
    mItemManager!!.getItem(item.getId(), itemCacheMode, ItemResponseListener(this, item))
  }

  override fun bindItem(holder: ItemViewHolder?, position: Int) {
    val story = getItem(position) ?: return
    if (mHighlightUpdated) {
      holder!!.setUpdated(
          story,
          mAdded.contains(story),
          (if (mPromoted.containsKey(story.getId())) mPromoted[story.getId()] else 0)!!,
      )
    }
    holder!!.setChecked(
        isSelected(story.getId()) ||
            !TextUtils.isEmpty(mUsername) && TextUtils.equals(mUsername, story.getBy())
    )
    holder.setViewed(story.isViewed())
    if (story.getLocalRevision() < mFavoriteRevision) {
      story.setFavorite(false)
    }
    holder.setFavorite(story.isFavorite())
    holder.bindMoreOptions({ anchor: View? -> showMoreOptions(anchor, story, holder) }, true)
  }

  override fun isItemAvailable(item: Item?): Boolean {
    return item != null && item.getLocalRevision() > 0
  }

  override fun getItem(position: Int): Item? {
    if (position < 0 || position >= this.items.size()) {
      return null
    }
    return this.items.get(position)
  }

  override fun getItemCacheMode(): Int {
    return mCacheMode
  }

  private fun setUpdated(items: List<Item?>) {
    if (!mHighlightUpdated) {
      return
    }
    if (items.isEmpty()) {
      return
    }
    mAdded.clear()
    mPromoted.clear()
    DiffUtil.calculateDiff(
            object : DiffUtil.Callback() {
              override fun getOldListSize(): Int {
                return items.size
              }

              override fun getNewListSize(): Int {
                return items.size
              }

              override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items.get(oldItemPosition)!!.getLongId() ==
                    items[newItemPosition]!!.getLongId()
              }

              override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return areItemsTheSame(oldItemPosition, newItemPosition)
              }
            }
        )
        .dispatchUpdatesTo(
            object : ListUpdateCallback {
              override fun onInserted(position: Int, count: Int) {
                mAdded.add(items[position])
                notifyUpdated()
              }

              override fun onRemoved(position: Int, count: Int) {
                // no-op
              }

              override fun onMoved(fromPosition: Int, toPosition: Int) {
                if (toPosition < fromPosition) {
                  mPromoted[items[fromPosition]!!.getId()] = fromPosition - toPosition
                }
              }

              override fun onChanged(position: Int, count: Int, payload: Any?) {
                // no-op
              }
            }
        )
  }

  @SuppressLint("NotifyDataSetChanged")
  @Synthetic
  fun notifyUpdated() {
    if (mUpdateListener != null) {
      mUpdateListener!!.onUpdated(mShowAll, mAdded.size) { _: View? ->
        setShowAll(!mShowAll)
        notifyUpdated()
        notifyDataSetChanged()
      }
    }
  }

  @Synthetic
  fun onItemLoaded(item: Item?) {
    val position = this.items.indexOf(item)
    // ignore changes if item was invalidated by refresh / filter
    if (position >= 0 && position < itemCount) {
      notifyItemChanged(position)
    }
  }

  @Synthetic
  fun showMoreOptions(v: View?, story: Item, holder: ItemViewHolder) {
    mPopupMenu
        .create(context, v, Gravity.NO_GRAVITY)
        .inflate(R.menu.menu_contextual_story)
        .setMenuItemTitle(
            R.id.menu_contextual_save,
            if (story.isFavorite()) R.string.unsave else R.string.save,
        )
        .setMenuItemVisible(R.id.menu_contextual_save, !callback.hasAction(SwipeAction.Save))
        .setMenuItemVisible(R.id.menu_contextual_vote, !callback.hasAction(SwipeAction.Vote))
        .setMenuItemVisible(R.id.menu_contextual_refresh, !callback.hasAction(SwipeAction.Refresh))
        .setOnMenuItemClickListener { item: MenuItem? ->
          if (item!!.itemId == R.id.menu_contextual_save) {
            toggleSave(story)
            return@setOnMenuItemClickListener true
          }
          if (item.itemId == R.id.menu_contextual_vote) {
            vote(story, holder)
            return@setOnMenuItemClickListener true
          }
          if (item.itemId == R.id.menu_contextual_refresh) {
            refresh(story, holder)
            return@setOnMenuItemClickListener true
          }
          if (item.itemId == R.id.menu_contextual_comment) {
            context.startActivity(
                Intent(context, ComposeActivity::class.java)
                    .putExtra(ComposeActivity.EXTRA_PARENT_ID, story.getId())
                    .putExtra(ComposeActivity.EXTRA_PARENT_TEXT, story.getDisplayedTitle())
            )
            return@setOnMenuItemClickListener true
          }
          if (item.itemId == R.id.menu_contextual_profile) {
            context.startActivity(
                Intent(context, UserActivity::class.java)
                    .putExtra(UserActivity.EXTRA_USERNAME, story.getBy())
            )
            return@setOnMenuItemClickListener true
          }
          if (item.itemId == R.id.menu_contextual_share) {
            AppUtils.share(context, story.getDisplayedTitle(), story.getUrl())
            return@setOnMenuItemClickListener true
          }
          false
        }
        .show()
  }

  @Synthetic
  fun toggleSave(story: Item) {
    if (!story.isFavorite()) {
      mFavoriteManager.add(context, story)
    } else {
      mFavoriteManager.remove(context, story.getId())
    }
  }

  @Synthetic
  fun refresh(story: Item, holder: RecyclerView.ViewHolder) {
    story.setLocalRevision(-1)
    notifyItemChanged(holder.absoluteAdapterPosition)
  }

  @Synthetic
  fun vote(story: Item, holder: RecyclerView.ViewHolder) {
    if (
        !mUserServices.voteUp(
            context,
            story.getId(),
            VoteCallback(this, holder.absoluteAdapterPosition, story),
        )
    ) {
      AppUtils.showLogin(context, mAlertDialogBuilder)
    }
  }

  @Synthetic
  fun onVoted(position: Int, successful: Boolean?) {
    if (successful == null) {
      Toast.makeText(context, R.string.vote_failed, Toast.LENGTH_SHORT).show()
    } else if (successful) {
      Toast.makeText(context, R.string.voted, Toast.LENGTH_SHORT).show()
      if (position < itemCount) {
        notifyItemChanged(position, VOTED)
      }
    }
  }

  fun setCacheMode(cacheMode: Int) {
    mCacheMode = cacheMode
  }

  @Synthetic
  fun markAsViewed(position: Int) {
    if (position < 0) {
      return
    }
    val item = if (position < items.size()) items.get(position) else null
    if (item == null || !isItemAvailable(item) || item.isViewed()) {
      return
    }
    mSessionManager!!.view(item.getId())
  }

  private fun toggleAutoMarkAsViewed(recyclerView: RecyclerView) {
    if (Preferences.autoMarkAsViewed(recyclerView.context)) {
      recyclerView.addOnScrollListener(mAutoViewScrollListener)
    } else {
      recyclerView.removeOnScrollListener(mAutoViewScrollListener)
    }
  }

  internal class ItemResponseListener
  @Synthetic
  constructor(adapter: StoryRecyclerViewAdapter?, private val mPartialItem: Item) :
      ResponseListener<Item?> {
    private val mAdapter: WeakReference<StoryRecyclerViewAdapter?>

    init {
      mAdapter = WeakReference<StoryRecyclerViewAdapter?>(adapter)
    }

    override fun onResponse(response: Item?) {
      if (mAdapter.get() != null && mAdapter.get()!!.isAttached && response != null) {
        mPartialItem.populate(response)
        mAdapter.get()!!.onItemLoaded(mPartialItem)
      }
    }

    override fun onError(errorMessage: String?) {
      // do nothing
    }
  }

  internal class VoteCallback
  @Synthetic
  constructor(
      adapter: StoryRecyclerViewAdapter?,
      private val mPosition: Int,
      private val mItem: Item,
  ) : UserServices.Callback() {
    private val mAdapter: WeakReference<StoryRecyclerViewAdapter?>

    init {
      mAdapter = WeakReference<StoryRecyclerViewAdapter?>(adapter)
    }

    override fun onDone(successful: Boolean) {
      // TODO update locally only, as API does not update instantly
      mItem.incrementScore()
      mItem.clearPendingVoted()
      if (mAdapter.get() != null && mAdapter.get()!!.isAttached) {
        mAdapter.get()!!.onVoted(mPosition, successful)
      }
    }

    override fun onError(throwable: Throwable?) {
      if (mAdapter.get() != null && mAdapter.get()!!.isAttached) {
        mAdapter.get()!!.onVoted(mPosition, null)
      }
    }
  }

  internal abstract class ItemTouchHelperCallback(
      context: Context,
      swipePreferences: List<SwipeAction>,
  ) : PeekabooTouchHelperCallback(context) {
    private val saveTextPARP: String = context.getString(R.string.save)
    private val unsaveText: String = context.getString(R.string.unsave)
    var saved: Boolean = false
    private var swipePreferences: List<SwipeAction> = emptyList()
    private val mTexts = arrayOfNulls<String>(2)
    private val mColors = IntArray(2)

    init {
      setSwipePreferences(context, swipePreferences)
    }

    override fun getLeftText(): String? {
      return if (this.leftSwipeAction == SwipeAction.Save) this.saveTextPARP else mTexts[0]
    }

    override fun getRightText(): String? {
      return if (this.rightSwipeAction == SwipeAction.Save) this.saveTextPARP else mTexts[1]
    }

    override fun getLeftTextColor(): Int {
      return mColors[0]
    }

    override fun getRightTextColor(): Int {
      return mColors[1]
    }

    @Synthetic
    fun setSwipePreferences(context: Context, swipePreferences: List<SwipeAction>) {
      this@ItemTouchHelperCallback.swipePreferences = swipePreferences
      for (i in 0..1) {
        when (swipePreferences[i]) {
          SwipeAction.Vote -> {
            mTexts[i] = context.getString(R.string.vote_up)
            mColors[i] = ContextCompat.getColor(context, R.color.greenA700)
          }

          SwipeAction.Save -> {
            mTexts[i] = null // dynamic text
            mColors[i] = ContextCompat.getColor(context, R.color.orange500)
          }

          SwipeAction.Refresh -> {
            mTexts[i] = context.getString(R.string.refresh)
            mColors[i] = ContextCompat.getColor(context, R.color.lightBlueA700)
          }

          SwipeAction.Share -> {
            mTexts[i] = context.getString(R.string.share)
            mColors[i] = ContextCompat.getColor(context, R.color.lightBlueA700)
          }

          else -> {
            mTexts[i] = null
            mColors[i] = 0
          }
        }
      }
    }

    val leftSwipeAction: SwipeAction
      get() = swipePreferences[0]

    val rightSwipeAction: SwipeAction
      get() = swipePreferences[1]

    fun hasAction(action: SwipeAction): Boolean {
      return swipePreferences!![0] == action || swipePreferences!![1] == action
    }

    private val saveText: String?
      get() = if (saved) unsaveText else saveTextPARP
  }

  companion object {
    private const val STATE_SHOW_ALL = "state:showAll"
    private const val STATE_USERNAME = "state:username"
  }
}
