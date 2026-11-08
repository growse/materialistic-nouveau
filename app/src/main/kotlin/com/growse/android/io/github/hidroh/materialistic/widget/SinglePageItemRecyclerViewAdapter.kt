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
import android.content.res.TypedArray
import android.graphics.Canvas
import android.os.Parcel
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import androidx.collection.LongSparseArray
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.growse.android.io.github.hidroh.materialistic.AlertDialogBuilder
import com.growse.android.io.github.hidroh.materialistic.AppUtils.getDimensionInDp
import com.growse.android.io.github.hidroh.materialistic.Navigable
import com.growse.android.io.github.hidroh.materialistic.Preferences.colorCodeEnabled
import com.growse.android.io.github.hidroh.materialistic.Preferences.colorCodeOpacity
import com.growse.android.io.github.hidroh.materialistic.R
import com.growse.android.io.github.hidroh.materialistic.ResourcesProvider
import com.growse.android.io.github.hidroh.materialistic.accounts.UserServices
import com.growse.android.io.github.hidroh.materialistic.data.Item
import com.growse.android.io.github.hidroh.materialistic.data.ItemManager

class SinglePageItemRecyclerViewAdapter(
    itemManager: ItemManager,
    userServices: UserServices,
    popupMenu: PopupMenu,
    alertDialogBuilder: AlertDialogBuilder<*>?,
    private val resourcesProvider: ResourcesProvider,
    private val state: SavedState,
    private val autoExpand: Boolean,
) :
    ItemRecyclerViewAdapter<ToggleItemViewHolder>(
        itemManager,
        userServices,
        popupMenu,
        alertDialogBuilder,
    ) {
  private val TOGGLE = Any()
  private val scrollListener: RecyclerView.OnScrollListener =
      object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
          if (newState == RecyclerView.SCROLL_STATE_IDLE) {
            unlockBinding()
          }
        }
      }
  private var levelIndicatorWidth = 0
  private var colorCoded = true
  private var colors: TypedArray? = null
  private var itemTouchHelper: ItemTouchHelper? = null
  private var lock: IntArray? = null
  private var colorOpacity = 100

  override fun attach(context: Context, recyclerView: RecyclerView) {
    super.attach(context, recyclerView)
    levelIndicatorWidth = getDimensionInDp(context, R.dimen.level_indicator_width)
    colors = resourcesProvider.obtainTypedArray(R.array.color_codes)
    itemTouchHelper =
        ItemTouchHelper(
            object :
                ItemTouchHelper.SimpleCallback(
                    0,
                    ItemTouchHelper.RIGHT,
                ) {
              override fun onMove(
                  recyclerView: RecyclerView,
                  viewHolder: RecyclerView.ViewHolder,
                  target: RecyclerView.ViewHolder,
              ): Boolean {
                return false
              }

              override fun getSwipeDirs(
                  recyclerView: RecyclerView,
                  viewHolder: RecyclerView.ViewHolder,
              ): Int {
                val item = getItem(viewHolder.absoluteAdapterPosition)
                if (item == null || item.kidCount == 0) {
                  return 0
                }
                return super.getSwipeDirs(recyclerView, viewHolder)
              }

              override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.absoluteAdapterPosition
                val item = getItem(position)
                if (item != null) {
                  notifyItemChanged(position)
                  toggleKids(item)
                }
              }

              override fun onChildDraw(
                  c: Canvas,
                  recyclerView: RecyclerView,
                  viewHolder: RecyclerView.ViewHolder,
                  dX: Float,
                  dY: Float,
                  actionState: Int,
                  isCurrentlyActive: Boolean,
              ) {
                val swipeWidth = viewHolder.itemView.width * getSwipeThreshold(viewHolder)
                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX.coerceIn(-swipeWidth, swipeWidth),
                    dY,
                    actionState,
                    isCurrentlyActive,
                )
              }

              override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                return 0.1f
              }
            },
        )
    itemTouchHelper!!.attachToRecyclerView(recyclerView)
    recyclerView.addOnScrollListener(scrollListener)
  }

  override fun detach(context: Context, recyclerView: RecyclerView) {
    super.detach(context, recyclerView)
    recyclerView.removeOnScrollListener(scrollListener)
    colors!!.recycle()
    itemTouchHelper!!.attachToRecyclerView(null)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToggleItemViewHolder {
    if (viewType == VIEW_TYPE_FOOTER) {
      return ToggleItemViewHolder(
          layoutInflater.inflate(R.layout.item_footer, parent, false),
          null,
      )
    }
    val holder = ToggleItemViewHolder(layoutInflater.inflate(R.layout.item_comment, parent, false))
    val params = holder.itemView.layoutParams as RecyclerView.LayoutParams
    params.leftMargin = levelIndicatorWidth * viewType
    holder.itemView.layoutParams = params
    return holder
  }

  override fun onBindViewHolder(
      holder: ToggleItemViewHolder,
      position: Int,
      payloads: MutableList<Any?>,
  ) {
    if (payloads.contains(TOGGLE)) {
      val item = getItem(position)
      if (item != null) {
        bindToggle(holder, item, state.isExpanded(item))
      }
    } else {
      super.onBindViewHolder(holder, position, payloads)
    }
  }

  override fun onBindViewHolder(holder: ToggleItemViewHolder, position: Int) {
    if (holder.isFooter) {
      return
    }
    if (lock != null && lock!![0] <= position && position <= lock!![1]) {
      clear(holder)
      return
    }
    if (colorCoded && colors != null && colors!!.length() > 0) {
      holder.level!!.visibility = View.VISIBLE
      holder.level!!.setBackgroundColor(getThreadColor(getItemViewType(position)))
      holder.level!!.alpha = colorOpacity / 100f
    } else {
      holder.level!!.visibility = View.GONE
    }
    super.onBindViewHolder(holder, position)
  }

  override fun getItemViewType(position: Int): Int {
    val item = getItem(position)
    if (item == null) { // footer
      return VIEW_TYPE_FOOTER
    }
    return item.level - 1
  }

  override fun getItemCount(): Int {
    return state.size()
  }

  override fun initDisplayOptions(context: Context) {
    colorCoded = colorCodeEnabled(context)
    colorOpacity = colorCodeOpacity(context)
    super.initDisplayOptions(context)
  }

  override fun getNextPosition(
      position: Int,
      direction: Int,
      callback: PositionCallback,
  ) {
    if (position < 0) {
      return
    }
    val item = getItem(position) ?: return
    val id = item.getNeighbour(direction)
    when (direction) {
      Navigable.DIRECTION_UP ->
          if (id == 0L) { // no more previous sibling, try previous list item
            setSelectedPosition(position - 1, callback)
          } else {
            setSelectedPosition(state.indexOf(id), callback)
          }

      Navigable.DIRECTION_DOWN ->
          if (id == 0L) { // no more next sibling, try next list item
            setSelectedPosition(position + 1, callback)
          } else {
            setSelectedPosition(state.indexOf(id), callback)
          }

      Navigable.DIRECTION_LEFT ->
          if (id != 0L) {
            setSelectedPosition(state.indexOf(id), callback)
          }

      Navigable.DIRECTION_RIGHT ->
          if (id == 0L) { // no kids, try next list item
            setSelectedPosition(position + 1, callback)
          } else if (state.isExpanded(item)) {
            setSelectedPosition(state.indexOf(id), callback)
          } else {
            expand(item, callback)
          }
    }
  }

  override fun getItem(position: Int): Item? {
    if (position < 0 || position >= state.size()) {
      return null
    }
    return state.get(position)
  }

  override fun onItemLoaded(position: Int, item: Item) {
    // item position may already be shifted due to expansion, need to get new position
    val index = state.indexOf(item)
    if (index in 0..<itemCount) {
      notifyItemChanged(index)
    }
  }

  override fun clear(holder: ToggleItemViewHolder) {
    super.clear(holder)
    holder.toggleButton!!.visibility = View.GONE
  }

  override fun bind(holder: ToggleItemViewHolder, item: Item) {
    super.bind(holder, item)
    holder.postedTextView.text = item.getDisplayedTime(context!!)
    holder.postedTextView.append(
        item.getDisplayedAuthor(
            context!!,
            true,
            getThreadColor(getItemViewType(holder.absoluteAdapterPosition)),
        ),
    )
    bindKids(holder, item)
  }

  override fun lockBinding(lock: IntArray?) {
    this.lock = lock
  }

  fun unlockBinding() {
    if (lock != null) {
      notifyItemRangeChanged(lock!![0], lock!![1] - lock!![0] + 1)
      lock = null
    }
  }

  private fun getThreadColor(itemViewType: Int): Int {
    return if (colorCoded) colors!!.getColor(itemViewType % colors!!.length(), 0) else 0
  }

  private fun bindKids(holder: ToggleItemViewHolder, item: Item) {
    holder.toggleButton!!.visibility = View.GONE
    if (item.kidCount == 0) {
      return
    }
    if (!item.isCollapsed && autoExpand) {
      expand(item)
    }
    bindToggle(holder, item, state.isExpanded(item))
  }

  fun toggleKids(item: Item) {
    val expanded = state.isExpanded(item)
    item.isCollapsed = !item.isCollapsed
    if (expanded) {
      collapse(item)
    } else {
      expand(item)
    }
  }

  private fun bindToggle(holder: ToggleItemViewHolder, item: Item, expanded: Boolean) {
    changeToggleState(holder, item, expanded)
    holder.toggleButton!!.visibility = View.VISIBLE
    holder.toggleButton!!.setOnClickListener {
      changeToggleState(holder, item, !state.isExpanded(item))
      toggleKids(item)
    }
  }

  private fun changeToggleState(holder: ToggleItemViewHolder, item: Item, expanded: Boolean) {
    holder.toggle!!.text =
        context!!
            .resources
            .getQuantityString(R.plurals.comments_count, item.kidCount, item.kidCount)
    holder.toggle!!.setCompoundDrawablesWithIntrinsicBounds(
        0,
        0,
        if (expanded) R.drawable.ic_expand_less_white_24dp
        else R.drawable.ic_expand_more_white_24dp,
        0,
    )
  }

  private fun expand(item: Item, callback: PositionCallback? = null) {
    if (state.isExpanded(item)) {
      return
    }
    recyclerView!!.post {
      if (recyclerView == null) {
        return@post // adapter detached
      }
      // state may have changed since this was posted: a rebind may have queued a duplicate
      // expansion, or an ancestor may have been collapsed and removed this item
      if (state.isExpanded(item) || state.indexOf(item) < 0) {
        return@post
      }
      val index = state.expand(item)
      notifyItemRangeInserted(index, item.kidCount)
      notifyItemChanged(index - 1, TOGGLE)
      recyclerView!!.itemAnimator!!.isRunning { setSelectedPosition(index, callback) }
    }
  }

  private fun collapse(item: Item) {
    val collapsedState = state.collapse(item)
    notifyItemRangeRemoved(collapsedState[0], collapsedState[1])
  }

  private fun setSelectedPosition(position: Int, callback: PositionCallback?) {
    callback?.onPosition(position)
  }

  class SavedState : Parcelable {
    private val list = ArrayList<Item?>()
    private val map = LongSparseArray<Item>()
    private val expanded = HashSet<String>()

    constructor(list: ArrayList<Item?>) {
      list.add(null) // footer
      addAll(0, list)
    }

    internal constructor(source: Parcel) {
      val savedList = ArrayList<Item?>()
      @Suppress("DEPRECATION") source.readList(savedList, Item::class.java.classLoader)
      addAll(0, savedList)
      expanded.addAll(source.createStringArrayList()!!)
    }

    override fun describeContents(): Int {
      return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
      dest.writeList(list)
      dest.writeStringList(ArrayList(expanded))
    }

    fun size(): Int {
      return list.size
    }

    fun get(position: Int): Item? {
      return list.get(position)
    }

    fun indexOf(itemId: Long): Int {
      return indexOf(map.get(itemId))
    }

    fun indexOf(item: Item?): Int {
      return list.indexOf(item)
    }

    fun isExpanded(item: Item): Boolean {
      return isExpanded(item.id)
    }

    fun isExpanded(itemId: String): Boolean {
      return expanded.contains(itemId)
    }

    fun expand(item: Item): Int {
      expanded.add(item.id)
      val index = indexOf(item) + 1
      addAll(index, item.kidItems.asList()) // recursive
      return index
    }

    fun collapse(item: Item): IntArray {
      val index = indexOf(item) + 1
      val count = recursiveRemove(item)
      return intArrayOf(index, count)
    }

    private fun addAll(index: Int, items: List<Item?>) {
      list.addAll(index, items)
      for (item in items) {
        if (item != null) {
          map.put(item.longId, item)
        }
      }
    }

    private fun recursiveRemove(item: Item): Int {
      if (!isExpanded(item.id)) {
        return 0
      }
      // if item is already expanded, its kids must be added, so we need to remove them
      var count = item.kidCount
      expanded.remove(item.id)
      for (kid in item.kidItems) {
        count += recursiveRemove(kid)
        remove(kid)
      }
      return count
    }

    private fun remove(item: Item) {
      list.remove(item)
      map.remove(item.longId)
    }

    companion object {
      @JvmField
      val CREATOR: Parcelable.Creator<SavedState?> =
          object : Parcelable.Creator<SavedState?> {
            override fun createFromParcel(source: Parcel): SavedState {
              return SavedState(source)
            }

            override fun newArray(size: Int): Array<SavedState?> {
              return arrayOfNulls(size)
            }
          }
    }
  }

  companion object {
    private const val VIEW_TYPE_FOOTER = -1
  }
}
