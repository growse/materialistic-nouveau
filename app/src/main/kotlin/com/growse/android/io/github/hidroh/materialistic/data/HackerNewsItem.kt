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
import android.content.Intent
import android.graphics.Typeface
import android.os.Parcel
import android.os.Parcelable
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.view.View
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.gson.annotations.SerializedName
import com.growse.android.io.github.hidroh.materialistic.AppUtils
import com.growse.android.io.github.hidroh.materialistic.Navigable
import com.growse.android.io.github.hidroh.materialistic.R

/**
 * An item from the Hacker News API.
 *
 * Gson creates instances without calling a constructor, so for API items no property initializer
 * runs: fields absent from the JSON stay at JVM defaults. Caches are therefore plain nullable
 * fields rather than `lazy` delegates, which would be null.
 */
class HackerNewsItem(
    // The item's unique id. Required.
    @Keep @SerializedName("id") override val longId: Long,
) : Item {
  // true if the item is deleted.
  @Keep
  @SerializedName("deleted")
  override var isDeleted = false
    private set

  // The type of item. One of "job", "story", "comment", "poll", or "pollopt".
  @Keep
  @SerializedName("type")
  override var rawType: String? = null
    private set

  // The username of the item's author.
  @Keep
  override var by: String? = null
    private set

  // Creation date of the item, in Unix Time.
  @Keep
  override var time = 0L
    private set

  // The comment, Ask HN, or poll text. HTML.
  @Keep
  override var text: String? = null
    private set

  // true if the item is dead.
  @Keep
  @SerializedName("dead")
  override var isDead = false
    private set

  // The item's parent. For comments, either another comment or the relevant story. For pollopts,
  // the relevant poll.
  @Keep @SerializedName("parent") private var parentId = 0L

  // The ids of the item's comments, in ranked display order.
  @Keep
  override var kids: LongArray? = null
    private set

  // The URL of the story.
  @Keep
  @SerializedName("url")
  override var rawUrl: String? = null
    private set

  // The story's score, or the votes for a pollopt.
  @Keep
  override var score = 0
    private set

  // The title of the story or poll.
  @Keep
  override var title: String? = null
    private set

  // A list of related pollopts, in display order.
  @Suppress("unused") @Keep private var parts: LongArray? = null

  // In the case of stories or polls, the total comment count.
  @Keep
  override var descendants = -1
    private set

  // view state
  override var isFavorite = false
  override var isViewed = false
  override var localRevision = -1
  override var level = 0
    private set

  override var isCollapsed = false
  override var isContentExpanded = false
  override var rank = 0
  override var lastKidCount = -1
  private var hasNewDescendants = false
  override var isVoted = false
    private set

  override var isPendingVoted = false
    private set

  private var next = 0L
  private var previous = 0L

  // non parcelable caches
  private var cachedKidItems: Array<HackerNewsItem>? = null
  private var cachedParentItem: HackerNewsItem? = null
  private var cachedDisplayedText: CharSequence? = null
  private var displayedTime: Spannable? = null
  private var displayedAuthor: Spannable? = null
  private var defaultColor = 0

  private constructor(id: Long, level: Int) : this(id) {
    this.level = level
  }

  private constructor(source: Parcel) : this(source.readLong()) {
    title = source.readString()
    time = source.readLong()
    by = source.readString()
    kids = source.createLongArray()
    rawUrl = source.readString()
    text = source.readString()
    rawType = source.readString()
    isFavorite = source.readBool()
    descendants = source.readInt()
    score = source.readInt()
    isViewed = source.readBool()
    localRevision = source.readInt()
    level = source.readInt()
    isDead = source.readBool()
    isDeleted = source.readBool()
    isCollapsed = source.readBool()
    isContentExpanded = source.readBool()
    rank = source.readInt()
    lastKidCount = source.readInt()
    hasNewDescendants = source.readBool()
    parentId = source.readLong()
    isVoted = source.readBool()
    isPendingVoted = source.readBool()
    next = source.readLong()
    previous = source.readLong()
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    dest.writeLong(longId)
    dest.writeString(title)
    dest.writeLong(time)
    dest.writeString(by)
    dest.writeLongArray(kids)
    dest.writeString(rawUrl)
    dest.writeString(text)
    dest.writeString(rawType)
    dest.writeBool(isFavorite)
    dest.writeInt(descendants)
    dest.writeInt(score)
    dest.writeBool(isViewed)
    dest.writeInt(localRevision)
    dest.writeInt(level)
    dest.writeBool(isDead)
    dest.writeBool(isDeleted)
    dest.writeBool(isCollapsed)
    dest.writeBool(isContentExpanded)
    dest.writeInt(rank)
    dest.writeInt(lastKidCount)
    dest.writeBool(hasNewDescendants)
    dest.writeLong(parentId)
    dest.writeBool(isVoted)
    dest.writeBool(isPendingVoted)
    dest.writeLong(next)
    dest.writeLong(previous)
  }

  override fun describeContents() = 0

  override fun populate(info: Item) {
    title = info.title
    time = info.time
    by = info.by
    kids = info.kids
    rawUrl = info.rawUrl
    text = info.text
    cachedDisplayedText = info.displayedText // pre-load, but not part of Parcelable
    rawType = info.rawType
    descendants = info.descendants
    hasNewDescendants = lastKidCount >= 0 && descendants > lastKidCount
    lastKidCount = descendants
    parentId = info.parent.toLong()
    isDeleted = info.isDeleted
    isDead = info.isDead
    score = info.score
    isViewed = info.isViewed
    isFavorite = info.isFavorite
    localRevision = 1
  }

  override val id: String
    get() = longId.toString()

  override val parent: String
    get() = parentId.toString()

  override val type: String
    get() = rawType.takeUnless { it.isNullOrEmpty() } ?: WebItem.STORY_TYPE

  override val isStoryType: Boolean
    get() =
        when (type) {
          WebItem.STORY_TYPE,
          WebItem.POLL_TYPE,
          WebItem.JOB_TYPE -> true
          else -> false
        }

  override val displayedTitle: String
    get() = if (type == WebItem.COMMENT_TYPE) text!! else title!!

  override val url: String
    get() =
        when (type) {
          WebItem.JOB_TYPE,
          WebItem.POLL_TYPE,
          WebItem.COMMENT_TYPE -> itemUrl
          else -> rawUrl.takeUnless { it.isNullOrEmpty() } ?: itemUrl
        }

  override val source: String?
    get() = url.toUri().host

  override val kidCount: Int
    get() = if (descendants > 0) descendants else kids?.size ?: 0

  override val kidItems: Array<HackerNewsItem>
    get() = cachedKidItems ?: createKidItems().also { cachedKidItems = it }

  override val parentItem: HackerNewsItem?
    get() =
        if (parentId == 0L) null
        else cachedParentItem ?: HackerNewsItem(parentId).also { cachedParentItem = it }

  override val displayedText: CharSequence?
    get() = cachedDisplayedText ?: AppUtils.fromHtml(text).also { cachedDisplayedText = it }

  private val itemUrl: String
    get() = HackerNewsClient.WEB_ITEM_PATH.format(id)

  override fun hasNewKids() = hasNewDescendants

  override fun getDisplayedAuthor(context: Context, linkify: Boolean, color: Int): Spannable {
    val author = displayedAuthor ?: createDisplayedAuthor(context, linkify)
    displayedAuthor = author
    if (author.isNotEmpty()) {
      author.setSpan(
          ForegroundColorSpan(if (color != 0) color else defaultColor),
          AUTHOR_SEPARATOR.length,
          author.length,
          Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
      )
    }
    return author
  }

  override fun getDisplayedTime(context: Context): Spannable =
      displayedTime
          ?: SpannableStringBuilder()
              .apply {
                if (isDead) {
                  append(context.getString(R.string.dead_prefix)).append(' ')
                }
                val timeSpan = SpannableString(AppUtils.getAbbreviatedTimeSpan(time * 1000))
                if (isDeleted) {
                  timeSpan.setSpan(
                      StrikethroughSpan(),
                      0,
                      timeSpan.length,
                      Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
                  )
                }
                append(timeSpan)
              }
              .also { displayedTime = it }

  override fun incrementScore() {
    score++
    isVoted = true
    isPendingVoted = true
  }

  override fun clearPendingVoted() {
    isPendingVoted = false
  }

  override fun getNeighbour(direction: Int): Long =
      when (direction) {
        Navigable.DIRECTION_UP -> previous
        Navigable.DIRECTION_DOWN -> next
        Navigable.DIRECTION_LEFT -> if (level > 1) parentId else 0L
        Navigable.DIRECTION_RIGHT -> kids?.firstOrNull() ?: 0L
        else -> 0L
      }

  override fun hashCode() = longId.toInt()

  override fun equals(other: Any?) = other is HackerNewsItem && longId == other.longId

  /** Parses the HTML text and builds the kids up front, off the main thread. */
  fun preload() {
    displayedText
    kidItems
  }

  private fun createKidItems(): Array<HackerNewsItem> {
    val kids = kids ?: return emptyArray()
    return Array(kids.size) { i ->
      HackerNewsItem(kids[i], level + 1).apply {
        rank = i + 1
        previous = kids.getOrElse(i - 1) { 0L }
        next = kids.getOrElse(i + 1) { 0L }
      }
    }
  }

  private fun createDisplayedAuthor(context: Context, linkify: Boolean): Spannable {
    val by = by
    if (by.isNullOrEmpty()) {
      return SpannableString("")
    }
    defaultColor =
        ContextCompat.getColor(
            context,
            AppUtils.getThemedResId(
                context,
                if (linkify) android.R.attr.textColorLink else android.R.attr.textColorSecondary,
            ),
        )
    val author = SpannableString(AUTHOR_SEPARATOR + by)
    if (linkify) {
      val start = AUTHOR_SEPARATOR.length
      author.setSpan(
          StyleSpan(Typeface.BOLD),
          start,
          author.length,
          Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
      )
      author.setSpan(
          object : ClickableSpan() {
            override fun onClick(view: View) {
              view.context.startActivity(
                  Intent(Intent.ACTION_VIEW).setData(AppUtils.createUserUri(by)),
              )
            }

            override fun updateDrawState(ds: TextPaint) {
              super.updateDrawState(ds)
              ds.isUnderlineText = false
            }
          },
          start,
          author.length,
          Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
      )
    }
    return author
  }

  companion object {
    private const val AUTHOR_SEPARATOR = " - "

    @JvmField
    val CREATOR =
        object : Parcelable.Creator<HackerNewsItem> {
          override fun createFromParcel(source: Parcel) = HackerNewsItem(source)

          override fun newArray(size: Int) = arrayOfNulls<HackerNewsItem>(size)
        }

    @JvmStatic
    fun toCachedItem(item: Item) =
        MaterialisticDatabase.CachedItem(item.id).apply {
          type = item.rawType
          by = item.by
          time = item.time
          text = item.text
          parent = item.parent.toLong()
          url = item.rawUrl
          title = item.title
          kids = item.kids?.joinToString(",")
          score = item.score
          descendants = item.descendants
          isDead = item.isDead
          isDeleted = item.isDeleted
          fetchedAt = System.currentTimeMillis()
        }

    @JvmStatic
    fun fromCachedItem(cached: MaterialisticDatabase.CachedItem) =
        HackerNewsItem(cached.itemId.toLong()).apply {
          rawType = cached.type
          by = cached.by
          time = cached.time
          text = cached.text
          parentId = cached.parent
          rawUrl = cached.url
          title = cached.title
          kids =
              cached.kids
                  ?.split(',')
                  ?.filter { it.isNotEmpty() }
                  ?.map { it.toLong() }
                  ?.toLongArray()
                  ?.takeIf { it.isNotEmpty() }
          score = cached.score
          descendants = cached.descendants
          lastKidCount = cached.descendants
          isDead = cached.isDead
          isDeleted = cached.isDeleted
          localRevision = 1
        }

    private fun Parcel.readBool() = readInt() != 0

    private fun Parcel.writeBool(value: Boolean) = writeInt(if (value) 1 else 0)
  }
}
