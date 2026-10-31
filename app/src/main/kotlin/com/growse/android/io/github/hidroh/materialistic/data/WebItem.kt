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
import android.os.Parcelable
import android.text.Spannable
import androidx.annotation.StringDef

/** Represents an item that can be displayed by a [android.webkit.WebView]. */
interface WebItem : Parcelable {
  /** Item types. */
  @Retention(AnnotationRetention.SOURCE)
  @StringDef(JOB_TYPE, STORY_TYPE, COMMENT_TYPE, POLL_TYPE)
  annotation class Type

  /** Formatted title to display, if any. */
  val displayedTitle: String

  /** URL to pass to [android.webkit.WebView.loadUrl], if any. */
  val url: String

  /** True if this item is not a comment. */
  val isStoryType: Boolean

  /** Item ID as a string. */
  val id: String

  /** Item ID. */
  val longId: Long

  /** Host the item links to, if any. */
  val source: String?

  /**
   * Formats the author for display.
   *
   * @param linkify true to display the author as a hyperlink
   * @param color optional decorator color for the author, or 0
   */
  fun getDisplayedAuthor(context: Context, linkify: Boolean, color: Int): Spannable

  /** Formats the posted time for display. */
  fun getDisplayedTime(context: Context): Spannable

  /** Item type. */
  @get:Type val type: String

  /** Whether the item is saved as a favorite. */
  var isFavorite: Boolean

  companion object {
    const val JOB_TYPE = "job"
    const val STORY_TYPE = "story"
    const val COMMENT_TYPE = "comment"
    const val POLL_TYPE = "poll"
  }
}
