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
import com.growse.android.io.github.hidroh.materialistic.AlertDialogBuilder
import com.growse.android.io.github.hidroh.materialistic.AppUtils
import com.growse.android.io.github.hidroh.materialistic.ComposeActivity
import com.growse.android.io.github.hidroh.materialistic.Preferences
import com.growse.android.io.github.hidroh.materialistic.Preferences.SwipeAction
import com.growse.android.io.github.hidroh.materialistic.R
import com.growse.android.io.github.hidroh.materialistic.UserActivity
import com.growse.android.io.github.hidroh.materialistic.accounts.UserServices
import com.growse.android.io.github.hidroh.materialistic.data.FavoriteManager
import com.growse.android.io.github.hidroh.materialistic.data.FavoriteManager.Companion.isAdded
import com.growse.android.io.github.hidroh.materialistic.data.FavoriteManager.Companion.isCleared
import com.growse.android.io.github.hidroh.materialistic.data.FavoriteManager.Companion.isRemoved
import com.growse.android.io.github.hidroh.materialistic.data.Item
import com.growse.android.io.github.hidroh.materialistic.data.ItemManager
import com.growse.android.io.github.hidroh.materialistic.data.MaterialisticDatabase
import com.growse.android.io.github.hidroh.materialistic.data.ResponseListener
import com.growse.android.io.github.hidroh.materialistic.data.SessionManager
import java.lang.ref.WeakReference

class StoryRecyclerViewAdapter(
    context: Context,
    private val itemManager: ItemManager,
    private val sessionManager: SessionManager,
    popupMenu: PopupMenu,
    alertDialogBuilder: AlertDialogBuilder<*>,
    userServices: UserServices,
    favoriteManager: FavoriteManager,
) :
    ListRecyclerViewAdapter<ListRecyclerViewAdapter.ItemViewHolder, Item>(
        context,
        popupMenu,
        alertDialogBuilder,
        userServices,
        favoriteManager,
    ) {
  private val VOTED = Any()
  private val autoViewScrollListener: RecyclerView.OnScrollListener =
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
  private val prefObservable = Preferences.Observable()
  private val sortedListCallback: SortedList.Callback<Item?> =
      object : SortedListAdapterCallback<Item>(this) {
        override fun compare(o1: Item, o2: Item): Int {
          return o1.rank - o2.rank
        }

        override fun areContentsTheSame(item1: Item, item2: Item): Boolean {
          return areItemsTheSame(item1, item2) && item1.localRevision == item2.localRevision
        }

        override fun areItemsTheSame(item1: Item, item2: Item): Boolean {
          return item1.longId == item2.longId
        }
      }

  val items: SortedList<Item> = SortedList<Item>(Item::class.java, sortedListCallback)

  val added: ArraySet<Item?> = ArraySet<Item?>()

  val promoted: ArrayMap<String?, Int?> = ArrayMap<String?, Int?>()

  var favoriteRevision: Int = 1
  private var username: String? = null
  private var highlightUpdated = true
  private var showAll = true
  private var cacheMode = ItemManager.MODE_DEFAULT
  private val itemTouchHelper: ItemTouchHelper

  internal var callback: ItemTouchHelperCallback

  @SuppressLint("NotifyDataSetChanged")
  private val observer = Observer { uri: Uri? ->
    if (uri == null) {
      return@Observer
    }
    if (isCleared(uri)) {
      favoriteRevision++ // invalidate all favorite statuses
      notifyDataSetChanged()
      return@Observer
    }
    var position = RecyclerView.NO_POSITION
    for (i in 0..<items.size()) {
      if (TextUtils.equals(items.get(i).id, uri.lastPathSegment)) {
        position = i
        break
      }
    }
    if (position == RecyclerView.NO_POSITION) {
      return@Observer
    }
    val item = items.get(position)
    if (isAdded(uri)) {
      item.isFavorite = true
      item.localRevision = favoriteRevision
    } else if (isRemoved(uri)) {
      item.isFavorite = false
      item.localRevision = favoriteRevision
    } else {
      item.isViewed = true
    }
    notifyItemChanged(position)
  }
  private var updateListener: UpdateListener? = null

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
            saved = item?.isFavorite == true
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
                AppUtils.share(context, item.displayedTitle, item.url)
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
                  if (!item?.isVoted!! && !item.isPendingVoted) {
                    swipeDirs = swipeDirs or swipeDir
                  }

              else -> swipeDirs = swipeDirs or swipeDir
            }
            return swipeDirs
          }
        }
    itemTouchHelper = ItemTouchHelper(callback)
  }

  @SuppressLint("NotifyDataSetChanged")
  override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
    super.onAttachedToRecyclerView(recyclerView)
    MaterialisticDatabase.getInstance(recyclerView.context).liveData.observeForever(observer)
    itemTouchHelper.attachToRecyclerView(recyclerView)
    toggleAutoMarkAsViewed(recyclerView)
    prefObservable.subscribe(
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
    MaterialisticDatabase.getInstance(recyclerView.context).liveData.removeObserver(observer)
    itemTouchHelper.attachToRecyclerView(null)
    prefObservable.unsubscribe(recyclerView.context)
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
      holder.animateVote(getItem(position)!!.score)
    } else {
      super.onBindViewHolder(holder, position, payloads)
    }
  }

  override fun getItemCount(): Int {
    return if (showAll) {
      items.size()
    } else {
      added.size
    }
  }

  override fun saveState(): Bundle {
    val savedState = super.saveState()
    savedState.putBoolean(STATE_SHOW_ALL, showAll)
    savedState.putString(STATE_USERNAME, username)
    return savedState
  }

  override fun restoreState(savedState: Bundle?) {
    if (savedState == null) {
      return
    }
    super.restoreState(savedState)
    showAll = savedState.getBoolean(STATE_SHOW_ALL, true)
    username = savedState.getString(STATE_USERNAME)
  }

  fun setUpdateListener(updateListener: UpdateListener?) {
    this.updateListener = updateListener
  }

  fun setItems(newItems: List<Item>) {
    setUpdated(newItems)
    items.beginBatchedUpdates()
    items.clear()
    newItems.forEach { items.add(it) }
    items.endBatchedUpdates()
  }

  fun setHighlightUpdated(highlightUpdated: Boolean) {
    this.highlightUpdated = highlightUpdated
  }

  fun setShowAll(showAll: Boolean) {
    this.showAll = showAll
  }

  @SuppressLint("NotifyDataSetChanged")
  fun initDisplayOptions(recyclerView: RecyclerView) {
    highlightUpdated = Preferences.highlightUpdatedEnabled(recyclerView.context)
    username = Preferences.getUsername(recyclerView.context)
    if (isAttached) {
      toggleAutoMarkAsViewed(recyclerView)
      notifyDataSetChanged()
    }
  }

  fun toggleSave(itemId: String?) {
    var position = RecyclerView.NO_POSITION
    for (i in 0..<items.size()) {
      if (TextUtils.equals(items.get(i).id, itemId)) {
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
    if (item == null || item.localRevision == 0) {
      return
    }
    item.localRevision = 0
    itemManager.getItem(item.id, itemCacheMode, ItemResponseListener(this, item))
  }

  override fun bindItem(holder: ItemViewHolder, position: Int) {
    val story = getItem(position) ?: return
    if (highlightUpdated) {
      holder.setUpdated(
          story,
          added.contains(story),
          (if (promoted.containsKey(story.id)) promoted[story.id] else 0)!!,
      )
    }
    holder.setChecked(
        isSelected(story.id) || !TextUtils.isEmpty(username) && TextUtils.equals(username, story.by)
    )
    holder.setViewed(story.isViewed)
    if (story.localRevision < favoriteRevision) {
      story.isFavorite = false
    }
    holder.setFavorite(story.isFavorite)
    holder.bindMoreOptions(
        { anchor: View ->
          showMoreOptions(anchor, story, holder)
        }
            as ItemViewHolder.ShowMoreOptionsListener,
        true,
    )
  }

  override fun isItemAvailable(item: Item): Boolean {
    return item.localRevision > 0
  }

  override fun getItem(position: Int): Item? {
    if (position < 0 || position >= this.items.size()) {
      return null
    }
    return this.items.get(position)
  }

  override val itemCacheMode: Int
    get() = cacheMode

  private fun setUpdated(items: List<Item?>) {
    if (!highlightUpdated) {
      return
    }
    if (items.isEmpty()) {
      return
    }
    added.clear()
    promoted.clear()
    DiffUtil.calculateDiff(
            object : DiffUtil.Callback() {
              override fun getOldListSize(): Int {
                return items.size
              }

              override fun getNewListSize(): Int {
                return items.size
              }

              override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition]!!.longId == items[newItemPosition]!!.longId
              }

              override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return areItemsTheSame(oldItemPosition, newItemPosition)
              }
            }
        )
        .dispatchUpdatesTo(
            object : ListUpdateCallback {
              override fun onInserted(position: Int, count: Int) {
                added.add(items[position])
                notifyUpdated()
              }

              override fun onRemoved(position: Int, count: Int) {
                // no-op
              }

              override fun onMoved(fromPosition: Int, toPosition: Int) {
                if (toPosition < fromPosition) {
                  promoted[items[fromPosition]!!.id] = fromPosition - toPosition
                }
              }

              override fun onChanged(position: Int, count: Int, payload: Any?) {
                // no-op
              }
            }
        )
  }

  @SuppressLint("NotifyDataSetChanged")
  fun notifyUpdated() {
    if (updateListener != null) {
      updateListener!!.onUpdated(showAll, added.size) { _: View? ->
        setShowAll(!showAll)
        notifyUpdated()
        notifyDataSetChanged()
      }
    }
  }

  fun onItemLoaded(item: Item?) {
    val position = this.items.indexOf(item)
    // ignore changes if item was invalidated by refresh / filter
    if (position in 0..<itemCount) {
      notifyItemChanged(position)
    }
  }

  fun showMoreOptions(v: View, story: Item, holder: ItemViewHolder) {
    mPopupMenu
        .create(context, v, Gravity.NO_GRAVITY)
        .inflate(R.menu.menu_contextual_story)
        .setMenuItemTitle(
            R.id.menu_contextual_save,
            if (story.isFavorite) R.string.unsave else R.string.save,
        )
        .setMenuItemVisible(R.id.menu_contextual_save, !callback.hasAction(SwipeAction.Save))
        .setMenuItemVisible(R.id.menu_contextual_vote, !callback.hasAction(SwipeAction.Vote))
        .setMenuItemVisible(R.id.menu_contextual_refresh, !callback.hasAction(SwipeAction.Refresh))
        .setOnMenuItemClickListener { item: MenuItem ->
          if (item.itemId == R.id.menu_contextual_save) {
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
                    .putExtra(ComposeActivity.EXTRA_PARENT_ID, story.id)
                    .putExtra(ComposeActivity.EXTRA_PARENT_TEXT, story.displayedTitle)
            )
            return@setOnMenuItemClickListener true
          }
          if (item.itemId == R.id.menu_contextual_profile) {
            context.startActivity(
                Intent(context, UserActivity::class.java)
                    .putExtra(UserActivity.EXTRA_USERNAME, story.by)
            )
            return@setOnMenuItemClickListener true
          }
          if (item.itemId == R.id.menu_contextual_share) {
            AppUtils.share(context, story.displayedTitle, story.url)
            return@setOnMenuItemClickListener true
          }
          false
        }
        .show()
  }

  fun toggleSave(story: Item) {
    if (!story.isFavorite) {
      mFavoriteManager.add(context, story)
    } else {
      mFavoriteManager.remove(context, story.id)
    }
  }

  fun refresh(story: Item, holder: RecyclerView.ViewHolder) {
    story.localRevision = -1
    notifyItemChanged(holder.absoluteAdapterPosition)
  }

  fun vote(story: Item, holder: RecyclerView.ViewHolder) {
    if (
        !mUserServices.voteUp(
            context,
            story.id,
            VoteCallback(this, holder.absoluteAdapterPosition, story),
        )
    ) {
      AppUtils.showLogin(context, mAlertDialogBuilder)
    }
  }

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
    this.cacheMode = cacheMode
  }

  fun markAsViewed(position: Int) {
    if (position < 0) {
      return
    }
    val item = if (position < items.size()) items.get(position) else null
    if (item == null || !isItemAvailable(item) || item.isViewed) {
      return
    }
    sessionManager.view(item.id)
  }

  private fun toggleAutoMarkAsViewed(recyclerView: RecyclerView) {
    if (Preferences.autoMarkAsViewed(recyclerView.context)) {
      recyclerView.addOnScrollListener(autoViewScrollListener)
    } else {
      recyclerView.removeOnScrollListener(autoViewScrollListener)
    }
  }

  internal class ItemResponseListener(
      adapter: StoryRecyclerViewAdapter?,
      private val partialItem: Item,
  ) : ResponseListener<Item?> {
    private val adapter: WeakReference<StoryRecyclerViewAdapter?> =
        WeakReference<StoryRecyclerViewAdapter?>(adapter)

    override fun onResponse(response: Item?) {
      if (adapter.get() != null && adapter.get()!!.isAttached && response != null) {
        partialItem.populate(response)
        adapter.get()!!.onItemLoaded(partialItem)
      }
    }

    override fun onError(errorMessage: String?) {
      // do nothing
    }
  }

  internal class VoteCallback(
      adapter: StoryRecyclerViewAdapter?,
      private val position: Int,
      private val item: Item,
  ) : UserServices.Callback() {
    private val adapter: WeakReference<StoryRecyclerViewAdapter?> =
        WeakReference<StoryRecyclerViewAdapter?>(adapter)

    override fun onDone(successful: Boolean) {
      // TODO update locally only, as API does not update instantly
      item.incrementScore()
      item.clearPendingVoted()
      if (adapter.get() != null && adapter.get()!!.isAttached) {
        adapter.get()!!.onVoted(position, successful)
      }
    }

    override fun onError(throwable: Throwable?) {
      if (adapter.get() != null && adapter.get()!!.isAttached) {
        adapter.get()!!.onVoted(position, null)
      }
    }
  }

  internal abstract class ItemTouchHelperCallback(
      context: Context,
      swipePreferences: List<SwipeAction>,
  ) : PeekabooTouchHelperCallback(context) {
    private val saveText = context.getString(R.string.save)
    private val unsaveText = context.getString(R.string.unsave)
    var saved: Boolean = false
    private var swipePreferences: List<SwipeAction> = emptyList()
    private val texts = arrayOfNulls<String>(2)
    private val colors = IntArray(2)

    init {
      setSwipePreferences(context, swipePreferences)
    }

    fun setSwipePreferences(context: Context, swipePreferences: List<SwipeAction>) {
      this@ItemTouchHelperCallback.swipePreferences = swipePreferences
      for (i in 0..1) {
        when (swipePreferences[i]) {
          SwipeAction.Vote -> {
            texts[i] = context.getString(R.string.vote_up)
            colors[i] = ContextCompat.getColor(context, R.color.greenA700)
          }

          SwipeAction.Save -> {
            texts[i] = null // dynamic text
            colors[i] = ContextCompat.getColor(context, R.color.orange500)
          }

          SwipeAction.Refresh -> {
            texts[i] = context.getString(R.string.refresh)
            colors[i] = ContextCompat.getColor(context, R.color.lightBlueA700)
          }

          SwipeAction.Share -> {
            texts[i] = context.getString(R.string.share)
            colors[i] = ContextCompat.getColor(context, R.color.lightBlueA700)
          }

          else -> {
            texts[i] = null
            colors[i] = 0
          }
        }
      }
    }

    val leftSwipeAction: SwipeAction
      get() = swipePreferences[0]

    val rightSwipeAction: SwipeAction
      get() = swipePreferences[1]

    fun hasAction(action: SwipeAction): Boolean {
      return swipePreferences[0] == action || swipePreferences[1] == action
    }

    override val leftText: String?
      get() = if (leftSwipeAction == SwipeAction.Save) currentSaveText else texts[0]

    override val rightText: String?
      get() = if (rightSwipeAction == SwipeAction.Save) currentSaveText else texts[1]

    override val leftTextColor: Int
      get() = colors[0]

    override val rightTextColor: Int
      get() = colors[1]

    private val currentSaveText: String
      get() = if (saved) unsaveText else saveText
  }

  companion object {
    private const val STATE_SHOW_ALL = "state:showAll"
    private const val STATE_USERNAME = "state:username"
  }
}
