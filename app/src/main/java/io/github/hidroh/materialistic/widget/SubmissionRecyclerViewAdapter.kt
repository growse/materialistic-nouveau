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

import android.content.Intent
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup

import io.github.hidroh.materialistic.ItemActivity
import io.github.hidroh.materialistic.R
import io.github.hidroh.materialistic.ThreadPreviewActivity
import io.github.hidroh.materialistic.data.Item
import io.github.hidroh.materialistic.data.ItemManager

open class SubmissionRecyclerViewAdapter : ItemRecyclerViewAdapter<SubmissionViewHolder>() {
    private var mItems: Array<Item>? = null

    constructor(itemManager: ItemManager, items: Array<Item>) {
        super(itemManager)
        mItems = items
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        attach(recyclerView.getContext(), recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        detach(recyclerView.getContext(), recyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubmissionViewHolder {
        return SubmissionViewHolder(mLayoutInflater.inflate(R.layout.item_submission, parent, false))
    }

    override fun getItemCount(): Int {
        return mItems.length
    }

    protected override fun getItem(position: Int): Item {
        return mItems[position]
    }

    protected override fun bind(holder: SubmissionViewHolder, item: Item) {
        super.bind(holder, item)
        if (item == null) {
            return
        }
        final boolean isComment = TextUtils.equals(item.getType(), Item.COMMENT_TYPE)
        holder.mPostedTextView.setText(item.getDisplayedTime(mContext))
        holder.mPostedTextView.append(item.getDisplayedAuthor(mContext, false, 0))
        if (isComment) {
            holder.mTitleTextView.setText(null)
            holder.mCommentButton.setText(R.string.view_thread)
        } else {
            holder.mPostedTextView.append(" - ")
            holder.mPostedTextView.append(mContext.getResources()
                    .getQuantityString(R.plurals.score, item.getScore(), item.getScore()))
            holder.mTitleTextView.setText(item.getDisplayedTitle())
            holder.mCommentButton.setText(R.string.view_story)
        }
        holder.mTitleTextView.setVisibility(holder.mTitleTextView.length() > 0 ?
                View.VISIBLE : View.GONE)
        holder.mContentTextView.setVisibility(holder.mContentTextView.length() > 0 ?
                View.VISIBLE : View.GONE)
        holder.mCommentButton.setVisibility(item.isDeleted() ? View.GONE : View.VISIBLE)
        holder.mCommentButton.setOnClickListener(v -> {
            if (isComment) {
                openPreview(item)
            } else {
                openItem(item)
            }
        })
    }

    private fun openItem(item: Item) {
        mContext.startActivity(Intent(mContext, ItemActivity::class.java)
                .putExtra(ItemActivity.EXTRA_ITEM, item))
    }

    private fun openPreview(item: Item) {
        mContext.startActivity(Intent(mContext, ThreadPreviewActivity::class.java)
                .putExtra(ThreadPreviewActivity.EXTRA_ITEM, item))
    }
}
