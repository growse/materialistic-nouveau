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
import android.net.Uri
import android.os.Parcel
import androidx.annotation.NonNull
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils

import io.github.hidroh.materialistic.AppUtils
import io.github.hidroh.materialistic.R
import io.github.hidroh.materialistic.annotation.Synthetic

/**
 * Represents a favorite item
 */
open class Favorite : WebItem {
    private var itemId: String? = null
    private var url: String? = null
    private var title: String? = null
    private var time: Long = 0
    private var favorite: Boolean = false
    private var displayedTime: Spannable? = null
    private val displayedAuthor: Spannable = new SpannableString("")

    public static final Creator<Favorite> CREATOR = Creator<Favorite>() {
        public Favorite createFromParcel(Parcel source) {
            return Favorite(source)
        }

        public Favorite[] newArray(int size) {
            return Favorite[size]
        }
    }

    constructor(itemId: String, url: String, title: String, time: Long) {
        this.itemId = itemId
        this.url = url
        this.title = title
        this.time = time
        this.favorite = true
    }

    @Synthetic
    constructor(source: Parcel) {
        itemId = source.readString()
        url = source.readString()
        title = source.readString()
        favorite = source.readInt() != 0
        time = source.readLong()
    }

    override fun getUrl(): String {
        return url
    }

    override fun isStoryType(): Boolean {
        return true
    }

    override fun getId(): String {
        return itemId
    }

    override fun getLongId(): Long {
        return Long.valueOf(itemId)
    }

    override fun getDisplayedTitle(): String {
        return title
    }

    override fun getDisplayedAuthor(context: Context, linkify: Boolean, color: Int): Spannable {
        return displayedAuthor
    }

    override fun getDisplayedTime(context: Context): Spannable {
        if (displayedTime == null) {
            displayedTime = SpannableString(context.getString(R.string.saved,
                    AppUtils.getAbbreviatedTimeSpan(time)))
        }
        return displayedTime
    }

    override fun getSource(): String {
        return TextUtils.isEmpty(url) ? null : Uri.parse(url).getHost()
    }

    @NonNull
    override fun getType(): String {
        // TODO treating all saved items as stories for now
        return STORY_TYPE
    }

    override fun isFavorite(): Boolean {
        return favorite
    }

    override fun setFavorite(favorite: Boolean) {
        this.favorite = favorite
    }

    override fun toString(): String {
        return String.format("%s (%s) - %s", title, url, String.format(HackerNewsClient.WEB_ITEM_PATH, itemId))
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(itemId)
        dest.writeString(url)
        dest.writeString(title)
        dest.writeInt(favorite ? 1 : 0)
        dest.writeLong(time)
    }

    fun getTime(): Long {
        return time
    }
}
