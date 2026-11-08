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
package com.growse.android.io.github.hidroh.materialistic.data

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import androidx.core.net.toUri
import com.growse.android.io.github.hidroh.materialistic.AppUtils.getAbbreviatedTimeSpan
import com.growse.android.io.github.hidroh.materialistic.R

/** Represents a favorite item */
class Favorite : WebItem {
  private val itemId: String
  override val url: String
  override val displayedTitle: String
  val time: Long
  override var isFavorite: Boolean
  private var displayedTime: Spannable? = null
  private val displayedAuthor: Spannable = SpannableString("")

  internal constructor(itemId: String, url: String, title: String, time: Long) {
    this.itemId = itemId
    this.url = url
    this.displayedTitle = title
    this.time = time
    this.isFavorite = true
  }

  internal constructor(source: Parcel) {
    itemId = source.readString()!!
    url = source.readString()!!
    this.displayedTitle = source.readString()!!
    this.isFavorite = source.readInt() != 0
    time = source.readLong()
  }

  override val isStoryType: Boolean
    get() = true

  override val id: String
    get() = itemId

  override val longId: Long
    get() = itemId.toLong()

  override fun getDisplayedAuthor(context: Context, linkify: Boolean, color: Int): Spannable {
    return displayedAuthor
  }

  override fun getDisplayedTime(context: Context): Spannable {
    if (displayedTime == null) {
      displayedTime =
          SpannableString(
              context.getString(
                  R.string.saved,
                  getAbbreviatedTimeSpan(time),
              ),
          )
    }
    return displayedTime!!
  }

  override val source: String?
    get() = if (TextUtils.isEmpty(url)) null else url.toUri().host

  override val type: String
    get() = // TODO treating all saved items as stories for now
        WebItem.STORY_TYPE

  override fun toString(): String {
    return String.format(
        "%s (%s) - %s",
        this.displayedTitle,
        url,
        String.format(HackerNewsClient.WEB_ITEM_PATH, itemId),
    )
  }

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    dest.writeString(itemId)
    dest.writeString(url)
    dest.writeString(this.displayedTitle)
    dest.writeInt(if (this.isFavorite) 1 else 0)
    dest.writeLong(time)
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<Favorite?> =
        object : Parcelable.Creator<Favorite?> {
          override fun createFromParcel(source: Parcel): Favorite {
            return Favorite(source)
          }

          override fun newArray(size: Int): Array<Favorite?> {
            return arrayOfNulls(size)
          }
        }
  }
}
