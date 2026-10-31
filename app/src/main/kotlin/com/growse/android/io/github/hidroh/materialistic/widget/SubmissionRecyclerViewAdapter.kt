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
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.growse.android.io.github.hidroh.materialistic.AlertDialogBuilder
import com.growse.android.io.github.hidroh.materialistic.ItemActivity
import com.growse.android.io.github.hidroh.materialistic.R
import com.growse.android.io.github.hidroh.materialistic.ThreadPreviewActivity
import com.growse.android.io.github.hidroh.materialistic.accounts.UserServices
import com.growse.android.io.github.hidroh.materialistic.data.Item
import com.growse.android.io.github.hidroh.materialistic.data.ItemManager
import com.growse.android.io.github.hidroh.materialistic.data.WebItem

class SubmissionRecyclerViewAdapter(
    itemManager: ItemManager,
    userServices: UserServices,
    popupMenu: PopupMenu,
    alertDialogBuilder: AlertDialogBuilder<*>?,
    private val mItems: Array<out Item?>,
) :
    ItemRecyclerViewAdapter<SubmissionViewHolder>(
        itemManager,
        userServices,
        popupMenu,
        alertDialogBuilder,
    ) {
  override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
    super.onAttachedToRecyclerView(recyclerView)
    attach(recyclerView.context, recyclerView)
  }

  override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
    super.onDetachedFromRecyclerView(recyclerView)
    detach(recyclerView.context, recyclerView)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubmissionViewHolder {
    return SubmissionViewHolder(layoutInflater.inflate(R.layout.item_submission, parent, false))
  }

  override fun getItemCount(): Int {
    return mItems.size
  }

  override fun getItem(position: Int): Item? {
    return mItems[position]
  }

  override fun bind(holder: SubmissionViewHolder, item: Item) {
    super.bind(holder, item)
    val isComment = TextUtils.equals(item.type, WebItem.COMMENT_TYPE)
    holder.postedTextView.text = item.getDisplayedTime(context!!)
    holder.postedTextView.append(item.getDisplayedAuthor(context!!, false, 0))
    if (isComment) {
      holder.titleTextView!!.setText(null)
      holder.commentButton.setText(R.string.view_thread)
    } else {
      holder.postedTextView.append(" - ")
      holder.postedTextView.append(
          context!!.resources.getQuantityString(R.plurals.score, item.score, item.score)
      )
      holder.titleTextView!!.setText(item.displayedTitle)
      holder.commentButton.setText(R.string.view_story)
    }
    holder.titleTextView.visibility =
        if (holder.titleTextView.length() > 0) View.VISIBLE else View.GONE
    holder.contentTextView.visibility =
        if (holder.contentTextView.length() > 0) View.VISIBLE else View.GONE
    holder.commentButton.visibility = if (item.isDeleted) View.GONE else View.VISIBLE
    holder.commentButton.setOnClickListener {
      if (isComment) {
        openPreview(item)
      } else {
        openItem(item)
      }
    }
  }

  private fun openItem(item: Item?) {
    context!!.startActivity(
        Intent(context, ItemActivity::class.java).putExtra(ItemActivity.EXTRA_ITEM, item)
    )
  }

  private fun openPreview(item: Item?) {
    context!!.startActivity(
        Intent(context, ThreadPreviewActivity::class.java)
            .putExtra(ThreadPreviewActivity.EXTRA_ITEM, item)
    )
  }
}
