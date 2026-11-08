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
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Canvas
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.growse.android.io.github.hidroh.materialistic.AlertDialogBuilder
import com.growse.android.io.github.hidroh.materialistic.AppUtils.share
import com.growse.android.io.github.hidroh.materialistic.AppUtils.showLogin
import com.growse.android.io.github.hidroh.materialistic.ComposeActivity
import com.growse.android.io.github.hidroh.materialistic.MenuTintDelegate
import com.growse.android.io.github.hidroh.materialistic.R
import com.growse.android.io.github.hidroh.materialistic.accounts.UserServices
import com.growse.android.io.github.hidroh.materialistic.data.Favorite
import com.growse.android.io.github.hidroh.materialistic.data.FavoriteManager
import com.growse.android.io.github.hidroh.materialistic.data.ItemManager
import com.growse.android.io.github.hidroh.materialistic.data.SyncScheduler
import com.growse.android.io.github.hidroh.materialistic.widget.ListRecyclerViewAdapter.ItemViewHolder.ShowMoreOptionsListener
import java.lang.ref.WeakReference
import kotlin.math.abs

class FavoriteRecyclerViewAdapter(
    context: Context,
    popupMenu: PopupMenu,
    alertDialogBuilder: AlertDialogBuilder<*>,
    userServices: UserServices,
    favoriteManager: FavoriteManager,
    private val mSyncScheduler: SyncScheduler,
    val mActionModeDelegate: ActionModeDelegate,
) :
    ListRecyclerViewAdapter<ListRecyclerViewAdapter.ItemViewHolder, Favorite>(
        context,
        popupMenu,
        alertDialogBuilder,
        userServices,
        favoriteManager,
    ) {
  interface ActionModeDelegate {
    fun startActionMode(callback: ActionMode.Callback): Boolean

    val isInActionMode: Boolean

    fun stopActionMode()
  }

  private val mItemTouchHelper: ItemTouchHelper
  private val mActionModeCallback: ActionMode.Callback =
      object : ActionMode.Callback {
        private var mPendingClear = false

        override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
          actionMode.menuInflater.inflate(R.menu.menu_favorite_action, menu)
          mMenuTintDelegate.onOptionsMenuCreated(menu)
          return true
        }

        override fun onPrepareActionMode(actionMode: ActionMode?, menu: Menu?): Boolean {
          return false
        }

        override fun onActionItemClicked(actionMode: ActionMode, menuItem: MenuItem): Boolean {
          if (menuItem.itemId == R.id.menu_clear) {
            mAlertDialogBuilder
                .init(context)
                .setMessage(R.string.confirm_clear_selected)
                .setPositiveButton(
                    android.R.string.ok,
                ) { _: DialogInterface?, _: Int ->
                  mPendingClear = true
                  removeSelection()
                  actionMode.finish()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show()
            return true
          }
          if (menuItem.itemId == R.id.menu_refresh) {
            refreshSelection()
            actionMode.finish()
          }
          return false
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onDestroyActionMode(actionMode: ActionMode?) {
          if (!isAttached) {
            return
          }
          mActionModeDelegate.stopActionMode()
          if (mPendingClear) {
            mPendingClear = false
          } else {
            mSelected.clear()
          }
          notifyDataSetChanged()
        }
      }
  val mMenuTintDelegate: MenuTintDelegate = MenuTintDelegate()
  val mSelected = mutableMapOf<Int, String>()
  private var mPendingAdd = -1

  init {
    mMenuTintDelegate.onActivityCreated(this.context)
    mItemTouchHelper =
        ItemTouchHelper(
            object : ItemTouchHelperCallback(this@FavoriteRecyclerViewAdapter.context) {
              override fun getSwipeDirs(
                  recyclerView: RecyclerView,
                  viewHolder: RecyclerView.ViewHolder,
              ): Int {
                if (mActionModeDelegate.isInActionMode) {
                  return 0
                }
                return super.getSwipeDirs(recyclerView, viewHolder)
              }

              override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (direction == ItemTouchHelper.LEFT) {
                  dismiss(viewHolder.itemView, viewHolder.adapterPosition)
                } else {
                  val item = getItem(viewHolder.adapterPosition)
                  mSyncScheduler.scheduleSync(
                      this@FavoriteRecyclerViewAdapter.context,
                      item.id,
                  )
                  notifyItemChanged(viewHolder.adapterPosition)
                }
              }
            },
        )
  }

  override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
    super.onAttachedToRecyclerView(recyclerView)
    mItemTouchHelper.attachToRecyclerView(recyclerView)
  }

  override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
    super.onDetachedFromRecyclerView(recyclerView)
    mItemTouchHelper.attachToRecyclerView(null)
  }

  override fun create(parent: ViewGroup?, viewType: Int): ItemViewHolder {
    return ItemViewHolder(mInflater.inflate(R.layout.item_favorite, parent, false))
  }

  override fun getItemCount(): Int {
    return mFavoriteManager.size
  }

  override fun bindItem(holder: ItemViewHolder, position: Int) {
    val favorite = getItem(holder.adapterPosition)
    holder.setOnLongClickListener {
      if (mActionModeDelegate.startActionMode(mActionModeCallback)) {
        toggle(favorite.id, holder.adapterPosition)
        return@setOnLongClickListener true
      }
      false
    }
    holder.bindMoreOptions(
        object : ShowMoreOptionsListener {
          override fun showMoreOptions(anchor: View) {
            showMoreOptions(anchor, favorite)
          }
        },
        false,
    )
  }

  override fun isItemAvailable(item: Favorite): Boolean {
    return true
  }

  override fun handleItemClick(item: Favorite, holder: ItemViewHolder) {
    if (!mActionModeDelegate.isInActionMode) {
      super.handleItemClick(item, holder)
    } else {
      toggle(item.id, holder.adapterPosition)
    }
  }

  override fun getItem(position: Int): Favorite {
    return mFavoriteManager.getItem(position)!!
  }

  override fun isSelected(itemId: String): Boolean {
    return super.isSelected(itemId) || mSelected.containsValue(itemId)
  }

  override val itemCacheMode: Int
    get() = ItemManager.MODE_CACHE

  @SuppressLint("NotifyDataSetChanged")
  fun notifyChanged() {
    if (itemCount == 0) {
      notifyDataSetChanged()
      return
    }
    if (mSelected.isNotEmpty()) { // has pending removals, notify removed
      val positions = ArrayList(mSelected.keys)
      positions.sort()
      mSelected.clear()
      for (i in positions.indices.reversed()) {
        notifyItemRemoved(positions[i]!!)
      }
    } else if (mPendingAdd >= 0) { // has pending insertion, notify inserted
      notifyItemInserted(mPendingAdd)
      mPendingAdd = -1
    } else { // no pending changes, simply refresh list
      notifyDataSetChanged()
    }
  }

  fun removeSelection() {
    mFavoriteManager.remove(context, mSelected.values)
  }

  fun refreshSelection() {
    for (id in mSelected.values) {
      mSyncScheduler.scheduleSync(context, id)
    }
  }

  fun dismiss(view: View, position: Int) {
    val item = getItem(position)
    mSelected[position] = item.id
    mFavoriteManager.remove(context, mSelected.values)
    Snackbar.make(view, R.string.toast_removed, Snackbar.LENGTH_LONG)
        .setAction(
            R.string.undo,
        ) {
          mPendingAdd = position
          mFavoriteManager.add(context, item)
        }
        .show()
  }

  private fun toggle(itemId: String, position: Int) {
    if (mSelected.containsValue(itemId)) {
      mSelected.remove(position)
    } else {
      mSelected[position] = itemId
    }
    notifyItemChanged(position)
  }

  private fun showMoreOptions(v: View, item: Favorite) {
    mPopupMenu
        .create(context, v, Gravity.NO_GRAVITY)
        .inflate(R.menu.menu_contextual_favorite)
        .setOnMenuItemClickListener { menuItem: MenuItem ->
          if (menuItem.itemId == R.id.menu_contextual_vote) {
            vote(item)
            return@setOnMenuItemClickListener true
          }
          if (menuItem.itemId == R.id.menu_contextual_comment) {
            context.startActivity(
                Intent(context, ComposeActivity::class.java)
                    .putExtra(ComposeActivity.EXTRA_PARENT_ID, item.id)
                    .putExtra(ComposeActivity.EXTRA_PARENT_TEXT, item.displayedTitle),
            )
            return@setOnMenuItemClickListener true
          }
          if (menuItem.itemId == R.id.menu_contextual_share) {
            share(context, item.displayedTitle, item.url)
            return@setOnMenuItemClickListener true
          }
          false
        }
        .show()
  }

  private fun vote(item: Favorite) {
    mUserServices.voteUp(context, item.id, VoteCallback(this))
  }

  fun onVoted(successful: Boolean?) {
    if (successful == null) {
      Toast.makeText(context, R.string.vote_failed, Toast.LENGTH_SHORT).show()
    } else if (successful) {
      Toast.makeText(context, R.string.voted, Toast.LENGTH_SHORT).show()
    } else {
      showLogin(context, mAlertDialogBuilder)
    }
  }

  internal class VoteCallback(adapter: FavoriteRecyclerViewAdapter?) : UserServices.Callback() {
    private val mAdapter: WeakReference<FavoriteRecyclerViewAdapter?>

    init {
      mAdapter = WeakReference<FavoriteRecyclerViewAdapter?>(adapter)
    }

    override fun onDone(successful: Boolean) {
      if (mAdapter.get() != null && mAdapter.get()!!.isAttached) {
        mAdapter.get()!!.onVoted(successful)
      }
    }

    override fun onError(throwable: Throwable?) {
      if (mAdapter.get() != null && mAdapter.get()!!.isAttached) {
        mAdapter.get()!!.onVoted(null)
      }
    }
  }

  internal abstract class ItemTouchHelperCallback(context: Context) :
      PeekabooTouchHelperCallback(context) {
    override val leftText: String = context.getString(R.string.delete)
    override val rightText: String = context.getString(R.string.refresh)
    override val leftTextColor = ContextCompat.getColor(context, R.color.red500)
    override val rightTextColor = ContextCompat.getColor(context, R.color.lightBlueA700)

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean,
    ) {
      viewHolder.itemView.alpha = 1 - abs(dX) / viewHolder.itemView.width
      super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
  }
}
