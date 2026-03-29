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

package io.github.hidroh.materialistic.data

import android.content.Context
import android.os.Parcel
import androidx.annotation.Keep
import androidx.annotation.NonNull
import android.text.format.DateUtils

import io.github.hidroh.materialistic.annotation.Synthetic

open class UserItem : UserManager.User {
    public static final Creator<UserItem> CREATOR = Creator<UserItem>() {
        public UserItem createFromParcel(Parcel source) {
            return UserItem(source)
        }

        public UserItem[] newArray(int size) {
            return UserItem[size]
        }
    }
    private var id: String? = null
    private var delay: Long = 0
    private var created: Long = 0
    private var karma: Long = 0
    private var about: String? = null
    private var submitted: IntArray? = null

    // view state
    private var submittedItems: Array<HackerNewsItem> = new HackerNewsItem[0]

    @Synthetic
    constructor(source: Parcel) {
        id = source.readString()
        delay = source.readLong()
        created = source.readLong()
        karma = source.readLong()
        about = source.readString()
        submitted = source.createIntArray()
        submittedItems = source.createTypedArray(HackerNewsItem.CREATOR)
    }

    override fun getId(): String {
        return id
    }

    override fun getAbout(): String {
        return about
    }

    override fun getKarma(): Long {
        return karma
    }

    override fun getCreated(context: Context): String {
        return DateUtils.formatDateTime(context, created * 1000, DateUtils.FORMAT_SHOW_DATE)
    }

    @NonNull
    override fun getItems(): Array<Item> {
        return submittedItems
    }

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

    fun setSubmittedItems(submittedItems: Array<HackerNewsItem>) {
        this.submittedItems = submittedItems != null ? submittedItems : HackerNewsItem[0]
    }

    fun getSubmitted(): IntArray {
        return submitted
    }
}
