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
import android.text.format.DateUtils
import androidx.annotation.Keep

internal class UserItem(source: Parcel) : UserManager.User {
  @Keep override val id: String?

  @Keep private val delay: Long

  @Keep private val created: Long

  @Keep override val karma: Long

  @Keep override val about: String?

  @Keep val submitted: IntArray?

  // view state
  private var submittedItems: Array<HackerNewsItem?>

  init {
    id = source.readString()
    delay = source.readLong()
    created = source.readLong()
    karma = source.readLong()
    about = source.readString()
    submitted = source.createIntArray()
    submittedItems = source.createTypedArray(HackerNewsItem.CREATOR) ?: emptyArray()
  }

  override fun getCreated(context: Context?): String? {
    return DateUtils.formatDateTime(context, created * 1000, DateUtils.FORMAT_SHOW_DATE)
  }

  override val items: Array<out Item?>
    get() = submittedItems

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    dest.writeString(id)
    dest.writeLong(delay)
    dest.writeLong(created)
    dest.writeLong(karma)
    dest.writeString(about)
    dest.writeIntArray(submitted)
    dest.writeTypedArray(submittedItems, flags)
  }

  fun setSubmittedItems(submittedItems: Array<HackerNewsItem?>?) {
    this.submittedItems = submittedItems ?: emptyArray()
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<UserItem?> =
        object : Parcelable.Creator<UserItem?> {
          override fun createFromParcel(source: Parcel): UserItem {
            return UserItem(source)
          }

          override fun newArray(size: Int): Array<UserItem?> {
            return arrayOfNulls<UserItem>(size)
          }
        }
  }
}
