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
import android.view.View
import android.view.ViewGroup
import com.growse.android.io.github.hidroh.materialistic.AlertDialogBuilder
import com.growse.android.io.github.hidroh.materialistic.ItemActivity
import com.growse.android.io.github.hidroh.materialistic.R
import com.growse.android.io.github.hidroh.materialistic.accounts.UserServices
import com.growse.android.io.github.hidroh.materialistic.data.Item
import com.growse.android.io.github.hidroh.materialistic.data.ItemManager

class MultiPageItemRecyclerViewAdapter(
    itemManager: ItemManager,
    userServices: UserServices,
    popupMenu: PopupMenu,
    alertDialogBuilder: AlertDialogBuilder<*>,
    items: Array<out Item>,
) :
    ItemRecyclerViewAdapter<ItemRecyclerViewAdapter.ItemViewHolder>(
        itemManager,
        userServices,
        popupMenu,
        alertDialogBuilder,
    ) {
  private val mItems: Array<Item?> = arrayOf(*items, null) // null footer

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
    if (viewType == VIEW_TYPE_FOOTER) {
      return ItemViewHolder(layoutInflater.inflate(R.layout.item_footer, parent, false), null)
    }
    return ItemViewHolder(layoutInflater.inflate(R.layout.item_comment, parent, false))
  }

  override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
    if (!holder.isFooter) {
      super.onBindViewHolder(holder, position)
    }
  }

  override fun getItemViewType(position: Int): Int {
    if (getItem(position) == null) {
      return VIEW_TYPE_FOOTER
    }
    return super.getItemViewType(position)
  }

  override fun getItem(position: Int): Item? {
    return mItems[position]
  }

  override fun bind(holder: ItemViewHolder, item: Item) {
    super.bind(holder, item)
    holder.postedTextView.text = item.getDisplayedTime(context!!)
    holder.postedTextView.append(item.getDisplayedAuthor(context!!, true, 0))
    if (item.kidCount > 0) {
      holder.commentButton.text =
          context!!
              .resources
              .getQuantityString(R.plurals.comments_count, item.kidCount, item.kidCount)
      holder.commentButton.visibility = View.VISIBLE
      holder.commentButton.setOnClickListener { openItem(item) }
    }
  }

  override fun getItemCount(): Int {
    return mItems.size
  }

  private fun openItem(item: Item?) {
    context!!.startActivity(
        Intent(context, ItemActivity::class.java)
            .putExtra(ItemActivity.EXTRA_ITEM, item)
            .putExtra(ItemActivity.EXTRA_OPEN_COMMENTS, true)
    )
  }

  companion object {
    private const val VIEW_TYPE_FOOTER = -1
  }
}
