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

package io.github.hidroh.materialistic.widget

import androidx.lifecycle.Observer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.collection.ArrayMap
import androidx.collection.ArraySet
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.SortedListAdapterCallback
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import java.lang.ref.WeakReference
import java.util.List

import javax.inject.Inject
import javax.inject.Named

import io.github.hidroh.materialistic.ActivityModule
import io.github.hidroh.materialistic.AppUtils
import io.github.hidroh.materialistic.ComposeActivity
import io.github.hidroh.materialistic.Preferences
import io.github.hidroh.materialistic.R
import io.github.hidroh.materialistic.UserActivity
import io.github.hidroh.materialistic.accounts.UserServices
import io.github.hidroh.materialistic.annotation.Synthetic
import io.github.hidroh.materialistic.data.FavoriteManager
import io.github.hidroh.materialistic.data.Item
import io.github.hidroh.materialistic.data.ItemManager
import io.github.hidroh.materialistic.data.MaterialisticDatabase
import io.github.hidroh.materialistic.data.ResponseListener
import io.github.hidroh.materialistic.data.SessionManager

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION
import static io.github.hidroh.materialistic.Preferences.SwipeAction.Save

public class StoryRecyclerViewAdapter extends
        ListRecyclerViewAdapter<ListRecyclerViewAdapter.ItemViewHolder, Item> {
    private static final String STATE_SHOW_ALL = "state:showAll"
    private static final String STATE_USERNAME = "state:username"
    private final Object VOTED = Object()
    private final RecyclerView.OnScrollListener mAutoViewScrollListener = RecyclerView.OnScrollListener() {
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (dy > 0) { // scrolling down
                markAsViewed(((LinearLayoutManager) recyclerView.getLayoutManager())
                        .findFirstVisibleItemPosition() - 1)
            }
        }
    }
    private final Preferences.Observable mPrefObservable = Preferences.Observable()
    private final SortedList.Callback<Item> mSortedListCallback = SortedListAdapterCallback<Item>(this) {
        public int compare(Item o1, Item o2) {
            return o1.getRank() - o2.getRank()
        }

        public boolean areContentsTheSame(Item item1, Item item2) {
            return areItemsTheSame(item1, item2) && item1.getLocalRevision() == item2.getLocalRevision()
        }

        public boolean areItemsTheSame(Item item1, Item item2) {
            return item1.getLongId() == item2.getLongId()
        }
    }
    @Inject @Named(ActivityModule.HN) ItemManager mItemManager
    @Inject SessionManager mSessionManager
    @Synthetic final SortedList<Item> mItems = SortedList<>(Item::class.java, mSortedListCallback)
    @Synthetic final ArraySet<Item> mAdded = ArraySet<>()
    @Synthetic final ArrayMap<String, Integer> mPromoted = ArrayMap<>()
    @Synthetic int mFavoriteRevision = 1
    private String mUsername
    private boolean mHighlightUpdated = true
    private boolean mShowAll = true
    private int mCacheMode = ItemManager.MODE_DEFAULT
    private ItemTouchHelper mItemTouchHelper
    @Synthetic ItemTouchHelperCallback mCallback
    @SuppressLint("NotifyDataSetChanged")
    private final Observer<Uri> mObserver = uri -> {
        if (uri == null) {
            return
        }
        if (FavoriteManager.Companion.isCleared(uri)) {
            mFavoriteRevision++; // invalidate all favorite statuses
            notifyDataSetChanged()
            return
        }
        int position = NO_POSITION
        for (int i = 0; i < mItems.size(); i++) {
            if (TextUtils.equals(mItems.get(i).getId(), uri.getLastPathSegment())) {
                position = i
                break
            }
        }
        if (position == NO_POSITION) {
            return
        }
        val item = mItems.get(position)
        if (FavoriteManager.Companion.isAdded(uri)) {
            item.setFavorite(true)
            item.setLocalRevision(mFavoriteRevision)
        } else if (FavoriteManager.Companion.isRemoved(uri)) {
            item.setFavorite(false)
            item.setLocalRevision(mFavoriteRevision)
        } else {
            item.setIsViewed(true)
        }
        notifyItemChanged(position)
    }
    private UpdateListener mUpdateListener

    interface UpdateListener {
        fun onUpdated(showAll: Boolean, itemCount: Int, actionClickListener: View.OnClickListener)
    }

    public StoryRecyclerViewAdapter(Context context) {
        super(context)
        mCallback = ItemTouchHelperCallback(context,
                Preferences.getListSwipePreferences(context)) {
            public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                val item = getItem(viewHolder.getAdapterPosition())
                if (item == null) {
                    return 0
                }
                mSaved = item.isFavorite()
                return checkSwipeDir(0, ItemTouchHelper.LEFT, mCallback.getLeftSwipeAction(), item) |
                        checkSwipeDir(0, ItemTouchHelper.RIGHT, mCallback.getRightSwipeAction(), item)
            }

            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                Preferences.SwipeAction action = direction == ItemTouchHelper.LEFT ?
                        getLeftSwipeAction() : getRightSwipeAction()
                val item = getItem(viewHolder.getAdapterPosition())
                if (item == null) {
                    return
                }
                switch (action) {
                    case Save:
                        toggleSave(item)
                        break
                    case Refresh:
                        refresh(item, viewHolder)
                        break
                    case Vote:
                        notifyItemChanged(viewHolder.getAdapterPosition())
                        vote(item, viewHolder)
                        break
                    case Share:
                        notifyItemChanged(viewHolder.getAdapterPosition())
                        AppUtils.share(mContext, item.getDisplayedTitle(), item.getUrl())
                        break
                }
            }

            private int checkSwipeDir(int swipeDirs, int swipeDir, Preferences.SwipeAction action, Item item) {
                switch (action) {
                    case None:
                        break
                    case Vote:
                        if (!item.isVoted() && !item.isPendingVoted()) {
                            swipeDirs |= swipeDir
                        }
                        break
                    default:
                        swipeDirs |= swipeDir
                        break
                }
                return swipeDirs
            }
        }
        mItemTouchHelper = ItemTouchHelper(mCallback)
    }

    @SuppressLint("NotifyDataSetChanged")
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        MaterialisticDatabase.getInstance(recyclerView.getContext()).getLiveData().observeForever(mObserver)
        mItemTouchHelper.attachToRecyclerView(recyclerView)
        toggleAutoMarkAsViewed(recyclerView)
        mPrefObservable.subscribe(recyclerView.getContext(),
                (key, contextChanged) -> {
                    mCallback.setSwipePreferences(recyclerView.getContext(),
                            Preferences.getListSwipePreferences(recyclerView.getContext()))
                    notifyDataSetChanged()
                },
                R.string.pref_list_swipe_left,
                R.string.pref_list_swipe_right)
    }

    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        MaterialisticDatabase.getInstance(recyclerView.getContext()).getLiveData().removeObserver(mObserver)
        mItemTouchHelper.attachToRecyclerView(null)
        mPrefObservable.unsubscribe(recyclerView.getContext())
    }

    protected ItemViewHolder create(ViewGroup parent, int viewType) {
        return ItemViewHolder(mInflater.inflate(R.layout.item_story, parent, false))
    }

    public void onBindViewHolder(ItemViewHolder holder, int position, List<Object> payloads) {
        if (payloads.contains(VOTED)) {
            holder.animateVote(getItem(position).getScore())
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    public int getItemCount() {
        if (mShowAll) {
            return mItems.size()
        } else {
            return mAdded.size()
        }
    }

    public Bundle saveState() {
        val savedState = super.saveState()
        savedState.putBoolean(STATE_SHOW_ALL, mShowAll)
        savedState.putString(STATE_USERNAME, mUsername)
        return savedState
    }

    public void restoreState(Bundle savedState) {
        if (savedState == null) {
            return
        }
        super.restoreState(savedState)
        mShowAll = savedState.getBoolean(STATE_SHOW_ALL, true)
        mUsername = savedState.getString(STATE_USERNAME)
    }

    public void setUpdateListener(UpdateListener updateListener) {
        mUpdateListener = updateListener
    }

    public SortedList<Item> getItems() {
        return mItems
    }

    public void setItems(Item[] items) {
        setUpdated(items)
        mItems.clear()
        mItems.addAll(items)
    }

    public void setHighlightUpdated(boolean highlightUpdated) {
        mHighlightUpdated = highlightUpdated
    }

    public void setShowAll(boolean showAll) {
        mShowAll = showAll
    }

    @SuppressLint("NotifyDataSetChanged")
    public void initDisplayOptions(RecyclerView recyclerView) {
        mHighlightUpdated = Preferences.highlightUpdatedEnabled(recyclerView.getContext())
        mUsername = Preferences.getUsername(recyclerView.getContext())
        if (isAttached()) {
            toggleAutoMarkAsViewed(recyclerView)
            notifyDataSetChanged()
        }
    }

    public void toggleSave(String itemId) {
        int position = NO_POSITION
        for (int i = 0; i < mItems.size(); i++) {
            if (TextUtils.equals(mItems.get(i).getId(), itemId)) {
                position = i
                break
            }
        }
        if (position == NO_POSITION) {
            return
        }
        toggleSave(mItems.get(position))
    }

    protected void loadItem(final int adapterPosition) {
        val item = getItem(adapterPosition)
        if (item.getLocalRevision() == 0) {
            return
        }
        item.setLocalRevision(0)
        mItemManager.getItem(item.getId(), getItemCacheMode(), ItemResponseListener(this, item))
    }

    protected void bindItem(final ItemViewHolder holder, int position) {
        val story = getItem(position)
        if (mHighlightUpdated) {
            holder.setUpdated(story,
                    mAdded.contains(story),
                    mPromoted.containsKey(story.getId()) ? mPromoted.get(story.getId()) : 0)
        }
        holder.setChecked(isSelected(story.getId()) ||
                !TextUtils.isEmpty(mUsername) && TextUtils.equals(mUsername, story.getBy()))
        holder.setViewed(story.isViewed())
        if (story.getLocalRevision() < mFavoriteRevision) {
            story.setFavorite(false)
        }
        holder.setFavorite(story.isFavorite())
        holder.bindMoreOptions(anchor -> showMoreOptions(anchor, story, holder), true)
    }

    protected boolean isItemAvailable(Item item) {
        return item != null && item.getLocalRevision() > 0
    }

    protected Item getItem(int position) {
        if (position < 0 || position >= getItems().size()) {
            return null
        }
        return getItems().get(position)
    }

    protected int getItemCacheMode() {
        return mCacheMode
    }

    private void setUpdated(Item[] items) {
        if (!mHighlightUpdated || getItems() == null) {
            return
        }
        if (mItems.size() == 0) {
            return
        }
        mAdded.clear()
        mPromoted.clear()
        DiffUtil.calculateDiff(DiffUtil.Callback() {
            public int getOldListSize() {
                return mItems.size()
            }

            public int getNewListSize() {
                return items.length
            }

            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return mItems.get(oldItemPosition).getLongId() ==
                        items[newItemPosition].getLongId()
            }

            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return areItemsTheSame(oldItemPosition, newItemPosition)
            }
        }).dispatchUpdatesTo(ListUpdateCallback() {
            public void onInserted(int position, int count) {
                mAdded.add(items[position])
                notifyUpdated()
            }

            public void onRemoved(int position, int count) {
                // no-op
            }

            public void onMoved(int fromPosition, int toPosition) {
                if (toPosition < fromPosition) {
                    mPromoted.put(mItems.get(fromPosition).getId(), fromPosition - toPosition)
                }
            }

            public void onChanged(int position, int count, Object payload) {
                // no-op
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    @Synthetic
    void notifyUpdated() {
        if (mUpdateListener != null) {
            mUpdateListener.onUpdated(mShowAll, mAdded.size(), v -> {
                setShowAll(!mShowAll)
                notifyUpdated()
                notifyDataSetChanged()
            })
        }
    }

    @Synthetic
    void onItemLoaded(Item item) {
        int position = getItems().indexOf(item)
        // ignore changes if item was invalidated by refresh / filter
        if (position >= 0 && position < getItemCount()) {
            notifyItemChanged(position)
        }
    }

    @Synthetic
    void showMoreOptions(View v, final Item story, final ItemViewHolder holder) {
        mPopupMenu.create(mContext, v, Gravity.NO_GRAVITY)
                .inflate(R.menu.menu_contextual_story)
                .setMenuItemTitle(R.id.menu_contextual_save,
                        story.isFavorite() ? R.string.unsave : R.string.save)
                .setMenuItemVisible(R.id.menu_contextual_save,
                        !mCallback.hasAction(Preferences.SwipeAction.Save))
                .setMenuItemVisible(R.id.menu_contextual_vote,
                        !mCallback.hasAction(Preferences.SwipeAction.Vote))
                .setMenuItemVisible(R.id.menu_contextual_refresh,
                        !mCallback.hasAction(Preferences.SwipeAction.Refresh))
                .setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.menu_contextual_save) {
                        toggleSave(story)
                        return true
                    }
                    if (item.getItemId() == R.id.menu_contextual_vote) {
                        vote(story, holder)
                        return true
                    }
                    if (item.getItemId() == R.id.menu_contextual_refresh) {
                        refresh(story, holder)
                        return true
                    }
                    if (item.getItemId() == R.id.menu_contextual_comment) {
                        mContext.startActivity(Intent(mContext, ComposeActivity::class.java)
                                .putExtra(ComposeActivity.EXTRA_PARENT_ID, story.getId())
                                .putExtra(ComposeActivity.EXTRA_PARENT_TEXT,
                                        story.getDisplayedTitle()))
                        return true
                    }
                    if (item.getItemId() == R.id.menu_contextual_profile) {
                        mContext.startActivity(Intent(mContext, UserActivity::class.java)
                                .putExtra(UserActivity.EXTRA_USERNAME, story.getBy()))
                        return true
                    }
                    if (item.getItemId() == R.id.menu_contextual_share) {
                        AppUtils.share(mContext, story.getDisplayedTitle(), story.getUrl())
                        return true
                    }
                    return false
                })
                .show()
    }

    @Synthetic
    void toggleSave(final Item story) {
        if (!story.isFavorite()) {
            mFavoriteManager.add(mContext, story)
        } else {
            mFavoriteManager.remove(mContext, story.getId())
        }
    }

    @Synthetic
    void refresh(Item story, RecyclerView.ViewHolder holder) {
        story.setLocalRevision(-1)
        notifyItemChanged(holder.getAdapterPosition())
    }

    @Synthetic
    void vote(final Item story, final RecyclerView.ViewHolder holder) {
        if (!mUserServices.voteUp(mContext, story.getId(),
                VoteCallback(this, holder.getAdapterPosition(), story))) {
            AppUtils.showLogin(mContext, mAlertDialogBuilder)
        }
    }

    @Synthetic
    void onVoted(int position, Boolean successful) {
        if (successful == null) {
            Toast.makeText(mContext, R.string.vote_failed, Toast.LENGTH_SHORT).show()
        } else if (successful) {
            Toast.makeText(mContext, R.string.voted, Toast.LENGTH_SHORT).show()
            if (position < getItemCount()) {
                notifyItemChanged(position, VOTED)
            }
        }
    }

    public void setCacheMode(int cacheMode) {
        mCacheMode = cacheMode
    }

    @Synthetic
    void markAsViewed(int position) {
        if (position < 0) {
            return
        }
        val item = mItems != null && position < mItems.size() ?
                mItems.get(position) : null
        if (item == null || !isItemAvailable(item) || item.isViewed()) {
            return
        }
        mSessionManager.view(item.getId())
    }

    private void toggleAutoMarkAsViewed(RecyclerView recyclerView) {
        if (Preferences.autoMarkAsViewed(recyclerView.getContext())) {
            recyclerView.addOnScrollListener(mAutoViewScrollListener)
        } else {
            recyclerView.removeOnScrollListener(mAutoViewScrollListener)
        }
    }

    open class ItemResponseListener : ResponseListener<Item> {
        private var mAdapter: WeakReference<StoryRecyclerViewAdapter>? = null
        private var mPartialItem: Item? = null

        @Synthetic
        constructor(adapter: StoryRecyclerViewAdapter, partialItem: Item) {
            mAdapter = WeakReference<>(adapter)
            mPartialItem = partialItem
        }

        override fun onResponse(response: Item) {
            if (mAdapter.get() != null && mAdapter.get().isAttached() && response != null) {
                mPartialItem.populate(response)
                mAdapter.get().onItemLoaded(mPartialItem)
            }
        }

        override fun onError(errorMessage: String) {
            // do nothing
        }
    }

    open class VoteCallback : UserServices.Callback() {
        private var mAdapter: WeakReference<StoryRecyclerViewAdapter>? = null
        private val mPosition: Int = 0
        private var mItem: Item? = null

        @Synthetic
        constructor(adapter: StoryRecyclerViewAdapter, position: Int, item: Item) {
            mAdapter = WeakReference<>(adapter)
            mPosition = position
            mItem = item
        }

        override fun onDone(successful: Boolean) {
            // TODO update locally only, as API does not update instantly
            mItem.incrementScore()
            mItem.clearPendingVoted()
            if (mAdapter.get() != null && mAdapter.get().isAttached()) {
                mAdapter.get().onVoted(mPosition, successful)
            }
        }

        override fun onError(throwable: Throwable) {
            if (mAdapter.get() != null && mAdapter.get().isAttached()) {
                mAdapter.get().onVoted(mPosition, null)
            }
        }
    }

    abstract class ItemTouchHelperCallback : PeekabooTouchHelperCallback() {
        private var mSaveText: String? = null
        private var mUnsaveText: String? = null
        var mSaved: Boolean = false
        private var mSwipePreferences: Array<Preferences.SwipeAction>? = null
        private var mTexts: Array<String> = new String[2]
        private var mColors: IntArray = new int[2]

        constructor(context: Context, swipePreferences: Array<Preferences.SwipeAction>) : super(context) {
            mSaveText = context.getString(R.string.save)
            mUnsaveText = context.getString(R.string.unsave)
            setSwipePreferences(context, swipePreferences)
        }

        protected override fun getLeftText(): String {
            return getLeftSwipeAction() == Save ? getSaveText() : mTexts[0]
        }

        protected override fun getRightText(): String {
            return getRightSwipeAction() == Save ? getSaveText() : mTexts[1]
        }

        protected override fun getLeftTextColor(): Int {
            return mColors[0]
        }

        protected override fun getRightTextColor(): Int {
            return mColors[1]
        }

        @Synthetic
        fun setSwipePreferences(context: Context, swipePreferences: Array<Preferences.SwipeAction>) {
            mSwipePreferences = swipePreferences
            for (int i = 0; i < 2; i++) {
                switch (swipePreferences[i]) {
                    case Vote:
                        mTexts[i] = context.getString(R.string.vote_up)
                        mColors[i] = ContextCompat.getColor(context, R.color.greenA700)
                        break
                    case Save:
                        mTexts[i] = null; // dynamic text
                        mColors[i] = ContextCompat.getColor(context, R.color.orange500)
                        break
                    case Refresh:
                        mTexts[i] = context.getString(R.string.refresh)
                        mColors[i] = ContextCompat.getColor(context, R.color.lightBlueA700)
                        break
                    case Share:
                        mTexts[i] = context.getString(R.string.share)
                        mColors[i] = ContextCompat.getColor(context, R.color.lightBlueA700)
                        break
                    default:
                        mTexts[i] = null
                        mColors[i] = 0
                        break
                }
            }
        }

        fun getLeftSwipeAction(): Preferences.SwipeAction {
            return mSwipePreferences == null ? Preferences.SwipeAction.None : mSwipePreferences[0]
        }

        fun getRightSwipeAction(): Preferences.SwipeAction {
            return mSwipePreferences == null ? Preferences.SwipeAction.None : mSwipePreferences[1]
        }

        fun hasAction(action: Preferences.SwipeAction): Boolean {
            return mSwipePreferences != null &&
                    (mSwipePreferences[0] == action || mSwipePreferences[1] == action)
        }

        private fun getSaveText(): String {
            return mSaved ? mUnsaveText : mSaveText
        }
    }
}
