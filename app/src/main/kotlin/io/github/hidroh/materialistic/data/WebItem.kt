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
import android.os.Parcelable
import androidx.annotation.NonNull
import androidx.annotation.StringDef
import android.text.Spannable


/**
 * Represents an item that can be displayed by a {@link android.webkit.WebView}
 */
interface WebItem : Parcelable {
    @Retention(AnnotationRetention.SOURCE)
    @StringDef({
    /*
      Item types
     */
    annotation class Type {}
    var JOB_TYPE: String = "job"
    var STORY_TYPE: String = "story"
    var COMMENT_TYPE: String = "comment"
    var POLL_TYPE: String = "poll"

    /**
     * Gets formatted title to display
     * @return formatted title or null
     */
    fun getDisplayedTitle(): String

    /**
     * Gets item URL to pass to {@link android.webkit.WebView#loadUrl(String)}
     * @return URL or null
     */
    fun getUrl(): String

    /**
     * Checks if item is not a comment
     * @return true if is not a comment, false otherwise
     */
    fun isStoryType(): Boolean

    /**
     * Gets item ID string
     * @return item ID string
     */
    fun getId(): String

    /**
     * Gets item ID
     * @return item ID
     */
    fun getLongId(): Long

    /**
     * Gets item source
     * @return item source or null
     */
    fun getSource(): String

    /**
     * Gets formatted author for display
     * @param context       an instance of {@link Context}
     * @param linkify       true to display author as a hyperlink, false otherwise
     * @param color         optional decorator color for author, or 0
     * @return  displayed author
     */
    fun getDisplayedAuthor(context: Context, linkify: Boolean, color: Int): Spannable

    /**
     * Gets formatted posted time for display
     * @param context    resources provider
     * @return  displayed time
     */
    fun getDisplayedTime(context: Context): Spannable

    /**
     * Gets item type
     * @return item type
     */
    @NonNull
    @Type
    fun getType(): String

    /**
     * Checks if item is marked as favorite
     * @return true if favorite, false otherwise
     * @see #setFavorite(boolean)
     */
    fun isFavorite(): Boolean

    /**
     * Updates item's favorite status to given status
     * @param favorite true if favorite, false otherwise
     * @see #isFavorite()
     */
    fun setFavorite(favorite: Boolean)
}
