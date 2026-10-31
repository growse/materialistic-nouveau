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

import android.content.Intent
import android.os.Handler
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.growse.android.io.github.hidroh.materialistic.AlertDialogBuilder
import com.growse.android.io.github.hidroh.materialistic.AppUtils
import com.growse.android.io.github.hidroh.materialistic.ItemActivity
import com.growse.android.io.github.hidroh.materialistic.R
import com.growse.android.io.github.hidroh.materialistic.accounts.UserServices
import com.growse.android.io.github.hidroh.materialistic.data.Item
import com.growse.android.io.github.hidroh.materialistic.data.ItemManager
import com.growse.android.io.github.hidroh.materialistic.data.WebItem

class ThreadPreviewRecyclerViewAdapter(
    itemManager: ItemManager,
    userServices: UserServices,
    popupMenu: PopupMenu,
    alertDialogBuilder: AlertDialogBuilder<*>?,
    item: Item,
) :
    ItemRecyclerViewAdapter<SubmissionViewHolder>(
        itemManager,
        userServices,
        popupMenu,
        alertDialogBuilder,
    ) {
  private val mItems: MutableList<Item?> = ArrayList<Item?>()
  private val mExpanded: MutableList<String?> = ArrayList<String?>()
  private var mLevelIndicatorWidth = 0
  private val mUsername: String?

  init {
    mItems.add(item)
    mUsername = item.by
  }

  override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
    super.onAttachedToRecyclerView(recyclerView)
    attach(recyclerView.context, recyclerView)
    mLevelIndicatorWidth = AppUtils.getDimensionInDp(context!!, R.dimen.level_indicator_width)
  }

  override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
    super.onDetachedFromRecyclerView(recyclerView)
    detach(recyclerView.context, recyclerView)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubmissionViewHolder {
    val holder =
        SubmissionViewHolder(layoutInflater.inflate(R.layout.item_submission, parent, false))
    val params = holder.itemView.layoutParams as RecyclerView.LayoutParams
    params.leftMargin = mLevelIndicatorWidth * viewType
    holder.itemView.layoutParams = params
    holder.commentButton.visibility = View.GONE
    return holder
  }

  override fun getItemCount(): Int {
    return mItems.size
  }

  override fun getItemViewType(position: Int): Int {
    return position
  }

  override fun bind(holder: SubmissionViewHolder, item: Item) {
    super.bind(holder, item)
    holder.postedTextView.text = item.getDisplayedTime(context!!)
    holder.postedTextView.append(
        item.getDisplayedAuthor(
            context!!,
            !TextUtils.equals(item.by, mUsername),
            0,
        )
    )
    holder.moreButton.visibility = View.GONE
    if (TextUtils.equals(item.type, WebItem.COMMENT_TYPE)) {
      holder.titleTextView!!.setText(null)
      holder.itemView.setOnClickListener(null)
      holder.commentButton.visibility = View.GONE
    } else {
      holder.titleTextView!!.setText(item.displayedTitle)
      holder.commentButton.visibility = View.VISIBLE
      holder.commentButton.setOnClickListener(View.OnClickListener { v: View? -> openItem(item) })
    }
    holder.titleTextView.visibility =
        if (holder.titleTextView.length() > 0) View.VISIBLE else View.GONE
    holder.contentTextView.visibility =
        if (holder.contentTextView.length() > 0) View.VISIBLE else View.GONE
    if (!mExpanded.contains(item.id) && item.parentItem != null) {
      mExpanded.add(item.id)
      Handler()
          .post(
              Runnable {
                mItems.add(0, item.parentItem) // recursive
                notifyItemInserted(0)
                notifyItemRangeChanged(1, mItems.size)
              }
          )
    }
  }

  override fun getItem(position: Int): Item? {
    return mItems.get(position)
  }

  private fun openItem(item: Item?) {
    context!!.startActivity(
        Intent(context, ItemActivity::class.java).putExtra(ItemActivity.EXTRA_ITEM, item)
    )
  }
}
