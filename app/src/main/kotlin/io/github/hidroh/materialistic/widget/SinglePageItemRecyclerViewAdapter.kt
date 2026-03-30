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
import android.content.res.TypedArray
import android.graphics.Canvas
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.NonNull
import androidx.collection.LongSparseArray
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import android.view.View
import android.view.ViewGroup

import java.util.ArrayList
import java.util.Arrays
import java.util.HashSet
import java.util.List
import java.util.Set

import javax.inject.Inject

import io.github.hidroh.materialistic.AppUtils
import io.github.hidroh.materialistic.Navigable
import io.github.hidroh.materialistic.Preferences
import io.github.hidroh.materialistic.R
import io.github.hidroh.materialistic.ResourcesProvider
import io.github.hidroh.materialistic.annotation.Synthetic
import io.github.hidroh.materialistic.data.Item
import io.github.hidroh.materialistic.data.ItemManager

public class SinglePageItemRecyclerViewAdapter
        extends ItemRecyclerViewAdapter<ToggleItemViewHolder> {
    private static final int VIEW_TYPE_FOOTER = -1
    private final Object TOGGLE = Object()
    private final RecyclerView.OnScrollListener mScrollListener = RecyclerView.OnScrollListener() {
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                unlockBinding()
            }
        }
    }
    @Inject ResourcesProvider mResourcesProvider
    private int mLevelIndicatorWidth = 0
    private final boolean mAutoExpand
    private boolean mColorCoded = true
    private TypedArray mColors
    private final @NonNull SavedState mState
    private ItemTouchHelper mItemTouchHelper
    private int[] mLock
    private int mColorOpacity = 100

    public SinglePageItemRecyclerViewAdapter(ItemManager itemManager,
                                             @NonNull SavedState state,
                                             boolean autoExpand) {
        super(itemManager)
        this.mState = state
        mAutoExpand = autoExpand
    }

    public void attach(Context context, RecyclerView recyclerView) {
        super.attach(context, recyclerView)
        mLevelIndicatorWidth = AppUtils.getDimensionInDp(mContext, R.dimen.level_indicator_width)
        mColors = mResourcesProvider.obtainTypedArray(R.array.color_codes)
        mItemTouchHelper = ItemTouchHelper(ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.RIGHT) {
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false
            }

            public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                val item = getItem(viewHolder.getAdapterPosition())
                if (item == null || item.getKidCount() == 0) {
                    return 0
                }
                return super.getSwipeDirs(recyclerView, viewHolder)
            }

            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition()
                val item = getItem(position)
                if (item != null) {
                    notifyItemChanged(position)
                    toggleKids(item)
                }
            }

            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                float swipeWidth = viewHolder.itemView.getWidth() * getSwipeThreshold(viewHolder)
                dX = Math.max(dX, -swipeWidth)
                dX = Math.min(dX, swipeWidth)
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }

            public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {
                return 0.1f
            }
        })
        mItemTouchHelper.attachToRecyclerView(recyclerView)
        recyclerView.addOnScrollListener(mScrollListener)
    }

    public void detach(Context context, RecyclerView recyclerView) {
        super.detach(context, recyclerView)
        recyclerView.removeOnScrollListener(mScrollListener)
        mColors.recycle()
        mItemTouchHelper.attachToRecyclerView(null)
    }

    public ToggleItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_FOOTER) {
            return ToggleItemViewHolder(mLayoutInflater.inflate(R.layout.item_footer, parent, false), null)
        }
        val holder =
                ToggleItemViewHolder(mLayoutInflater.inflate(R.layout.item_comment, parent, false))
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                holder.itemView.getLayoutParams()
        params.leftMargin = mLevelIndicatorWidth * viewType
        holder.itemView.setLayoutParams(params)
        return holder
    }

    public void onBindViewHolder(ToggleItemViewHolder holder, int position, List<Object> payloads) {
        if (payloads.contains(TOGGLE)) {
            val item = getItem(position)
            if (item != null) {
                bindToggle(holder, item, mState.isExpanded(item))
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    public void onBindViewHolder(ToggleItemViewHolder holder, int position) {
        if (holder.isFooter()) {
            return
        }
        if (mLock != null && mLock[0] <= position && position <= mLock[1]) {
            clear(holder)
            return
        }
        if (mColorCoded && mColors != null && mColors.length() > 0) {
            holder.mLevel.setVisibility(View.VISIBLE)
            holder.mLevel.setBackgroundColor(getThreadColor(getItemViewType(position)))
            holder.mLevel.setAlpha(mColorOpacity / 100f)
        } else {
            holder.mLevel.setVisibility(View.GONE)
        }
        super.onBindViewHolder(holder, position)
    }

    public int getItemViewType(int position) {
        val item = getItem(position)
        if (item == null) { // footer
            return VIEW_TYPE_FOOTER
        }
        return item.getLevel() - 1
    }

    public int getItemCount() {
        return mState.size()
    }

    public void initDisplayOptions(Context context) {
        mColorCoded = Preferences.colorCodeEnabled(context)
        mColorOpacity = Preferences.colorCodeOpacity(context)
        super.initDisplayOptions(context)
    }

    public void getNextPosition(int position, int direction, PositionCallback callback) {
        if (position < 0) {
            return
        }
        val item = getItem(position)
        if (item == null) {
            return
        }
        long id = item.getNeighbour(direction)
        switch (direction) {
            case Navigable.DIRECTION_UP:
                if (id == 0) { // no more previous sibling, try previous list item
                    setSelectedPosition(position - 1, callback)
                } else {
                    setSelectedPosition(mState.indexOf(id), callback)
                }
                break
            case Navigable.DIRECTION_DOWN:
                if (id == 0) { // no more next sibling, try next list item
                    setSelectedPosition(position + 1, callback)
                } else {
                    setSelectedPosition(mState.indexOf(id), callback)
                }
                break
            case Navigable.DIRECTION_LEFT:
                if (id != 0) {
                    setSelectedPosition(mState.indexOf(id), callback)
                }
                break
            case Navigable.DIRECTION_RIGHT:
                if (id == 0) { // no kids, try next list item
                    setSelectedPosition(position + 1, callback)
                } else if (mState.isExpanded(item)) {
                    setSelectedPosition(mState.indexOf(id), callback)
                } else {
                    expand(item, callback)
                }
                break
        }
    }

    protected Item getItem(int position) {
        if (position < 0 || position >= mState.size()) {
            return null
        }
        return mState.get(position)
    }

    protected void onItemLoaded(int position, Item item) {
        // item position may already be shifted due to expansion, need to get new position
        int index = mState.indexOf(item)
        if (index >= 0 && index < getItemCount()) {
            notifyItemChanged(index)
        }
    }

    protected void clear(ToggleItemViewHolder holder) {
        super.clear(holder)
        holder.mToggleButton.setVisibility(View.GONE)
    }

    protected void bind(ToggleItemViewHolder holder, Item item) {
        super.bind(holder, item)
        if (item == null) {
            return
        }
        holder.mPostedTextView.setText(item.getDisplayedTime(mContext))
        holder.mPostedTextView.append(item.getDisplayedAuthor(mContext, true,
                getThreadColor(getItemViewType(holder.getAdapterPosition()))))
        bindKids(holder, item)
    }

    public void lockBinding(int[] lock) {
        mLock = lock
    }

    @Synthetic
    void unlockBinding() {
        if (mLock != null) {
            notifyItemRangeChanged(mLock[0], mLock[1] - mLock[0] + 1)
            mLock = null
        }
    }

    private int getThreadColor(int itemViewType) {
        return mColorCoded ? mColors.getColor(itemViewType % mColors.length(), 0) : 0
    }

    private void bindKids(final ToggleItemViewHolder holder, final Item item) {
        holder.mToggleButton.setVisibility(View.GONE)
        if (item.getKidCount() == 0) {
            return
        }
        if (!item.isCollapsed() && mAutoExpand) {
            expand(item)
        }
        bindToggle(holder, item, mState.isExpanded(item))
    }

    @Synthetic
    void toggleKids(Item item) {
        boolean expanded = mState.isExpanded(item)
        item.setCollapsed(!item.isCollapsed())
        if (expanded) {
            collapse(item)
        } else {
            expand(item)
        }
    }

    private void bindToggle(ToggleItemViewHolder holder, Item item, boolean expanded) {
        changeToggleState(holder, item, expanded)
        holder.mToggleButton.setVisibility(View.VISIBLE)
        holder.mToggleButton.setOnClickListener(v -> {
            changeToggleState(holder, item, !mState.isExpanded(item))
            toggleKids(item)
        })
    }

    private void changeToggleState(ToggleItemViewHolder holder, Item item, boolean expanded) {
        holder.mToggle.setText(mContext.getResources()
                .getQuantityString(R.plurals.comments_count, item.getKidCount(), item.getKidCount()))
        holder.mToggle.setCompoundDrawablesWithIntrinsicBounds(0, 0, expanded ?
                R.drawable.ic_expand_less_white_24dp : R.drawable.ic_expand_more_white_24dp, 0)
    }

    private void expand(Item item) {
        expand(item, null)
    }

    private void expand(final Item item, PositionCallback callback) {
        if (mState.isExpanded(item)) {
            return
        }
        mRecyclerView.post(() -> {
            if (mRecyclerView == null) {
                return; // adapter detached
            }
            int index = mState.expand(item)
            notifyItemRangeInserted(index, item.getKidCount())
            notifyItemChanged(index - 1, TOGGLE)
            mRecyclerView.getItemAnimator().isRunning(() -> setSelectedPosition(index, callback))
        })
    }

    private void collapse(Item item) {
        int[] collapsedState = mState.collapse(item)
        notifyItemRangeRemoved(collapsedState[0], collapsedState[1])
    }

    private void setSelectedPosition(int position, PositionCallback callback) {
        if (callback != null) {
            callback.onPosition(position)
        }
    }

    open class SavedState : Parcelable {
        public static final Creator<SavedState> CREATOR = Creator<SavedState>() {
            public SavedState createFromParcel(Parcel source) {
                return SavedState(source)
            }

            public SavedState[] newArray(int size) {
                return SavedState[size]
            }
        }

        private val list: ArrayList<Item> = new ArrayList<>()
        private val map: LongSparseArray<Item> = new LongSparseArray<>()
        private val expanded: Set<String> = new HashSet<>()

        constructor(list: ArrayList<Item>) {
            list.add(null); // footer
            addAll(0, list)
        }

        @SuppressWarnings("unchecked")
        @Synthetic
        constructor(source: Parcel) {
            val savedList = source.readArrayList(Item::class.java.getClassLoader())
            addAll(0, savedList)
            expanded.addAll(source.createStringArrayList())
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeList(list)
            dest.writeStringList(ArrayList<>(expanded))
        }

        @Synthetic
        fun size(): Int {
            return list.size()
        }

        @Synthetic
        fun get(position: Int): Item {
            return list.get(position)
        }

        @Synthetic
        fun indexOf(itemId: Long): Int {
            return indexOf(map.get(itemId))
        }

        @Synthetic
        fun indexOf(item: Item): Int {
            return list.indexOf(item)
        }

        @Synthetic
        fun isExpanded(item: Item): Boolean {
            return isExpanded(item.getId())
        }

        @Synthetic
        fun isExpanded(itemId: String): Boolean {
            return expanded.contains(itemId)
        }

        @Synthetic
        fun expand(item: Item): Int {
            expanded.add(item.getId())
            int index = indexOf(item) + 1
            addAll(index, Arrays.asList(item.getKidItems())); // recursive
            return index
        }

        @Synthetic
        fun collapse(item: Item): IntArray {
            int index = indexOf(item) + 1
            int count = recursiveRemove(item)
            return new int[]{index, count}
        }

        private fun addAll(index: Int, items: List<Item>) {
            list.addAll(index, items)
            for (item in items) {
                if (item != null) {
                    map.put(item.getLongId(), item)
                }
            }
        }

        private fun recursiveRemove(item: Item): Int {
            if (!isExpanded(item.getId())) {
                return 0
            }
            // if item is already expanded, its kids must be added, so we need to remove them
            int count = item.getKidCount()
            expanded.remove(item.getId())
            for (kid in item.getKidItems()) {
                count += recursiveRemove(kid)
                remove(kid)
            }
            return count
        }

        private fun remove(item: Item) {
            list.remove(item)
            map.remove(item.getLongId())
        }
    }
}
