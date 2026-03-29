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
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Parcel
import androidx.annotation.Keep
import androidx.annotation.NonNull
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.view.View

import io.github.hidroh.materialistic.AppUtils
import io.github.hidroh.materialistic.Navigable
import io.github.hidroh.materialistic.R
import io.github.hidroh.materialistic.annotation.Synthetic

open class HackerNewsItem : Item {
    private const val AUTHOR_SEPARATOR: String = " - "

    // The item's unique id. Required.
    private var id: Long = 0
    // true if the item is deleted.
    private var deleted: Boolean = false
    // The type of item. One of "job", "story", "comment", "poll", or "pollopt".
    private var type: String? = null
    // The username of the item's author.
    private var by: String? = null
    // Creation date of the item, in Unix Time.
    private var time: Long = 0
    // The comment, Ask HN, or poll text. HTML.
    private var text: String? = null
    // true if the item is dead.
    private var dead: Boolean = false
    // The item's parent. For comments, either another comment or the relevant story. For pollopts, the relevant poll.
    private var parent: Long = 0
    // The ids of the item's comments, in ranked display order.
    private var kids: LongArray? = null
    // The URL of the story.
    private var url: String? = null
    // The story's score, or the votes for a pollopt.
    private var score: Int = 0
    // The title of the story or poll.
    private var title: String? = null
    // A list of related pollopts, in display order.
    @SuppressWarnings("unused")
    private var parts: LongArray? = null
    // In the case of stories or polls, the total comment count.
    private var descendants: Int = -1

    // view state
    private var favorite: Boolean = false
    private var viewed: Boolean = false
    private var localRevision: Int = -1
    var level: Int = 0
    private var collapsed: Boolean = false
    private var contentExpanded: Boolean = false
    var rank: Int = 0
    private var lastKidCount: Int = -1
    private var hasNewDescendants: Boolean = false
    private var voted: Boolean = false
    private var pendingVoted: Boolean = false
    private var previous: long next,? = null

    // non parcelable fields
    private var kidItems: Array<HackerNewsItem>? = null
    private var parentItem: HackerNewsItem? = null
    private var displayedTime: Spannable? = null
    private var displayedAuthor: Spannable? = null
    private var displayedText: CharSequence? = null
    private var defaultColor: Int = 0

    public static final Creator<HackerNewsItem> CREATOR = Creator<HackerNewsItem>() {
        public HackerNewsItem createFromParcel(Parcel source) {
            return HackerNewsItem(source)
        }

        public HackerNewsItem[] newArray(int size) {
            return HackerNewsItem[size]
        }
    }

    constructor(id: Long) {
        this.id = id
    }

    private constructor(id: Long, level: Int) {
        this(id)
        this.level = level
    }

    @Synthetic
    constructor(source: Parcel) {
        id = source.readLong()
        title = source.readString()
        time = source.readLong()
        by = source.readString()
        kids = source.createLongArray()
        url = source.readString()
        text = source.readString()
        type = source.readString()
        favorite = source.readInt() != 0
        descendants = source.readInt()
        score = source.readInt()
        favorite = source.readInt() == 1
        viewed = source.readInt() == 1
        localRevision = source.readInt()
        level = source.readInt()
        dead = source.readInt() == 1
        deleted = source.readInt() == 1
        collapsed = source.readInt() == 1
        contentExpanded = source.readInt() == 1
        rank = source.readInt()
        lastKidCount = source.readInt()
        hasNewDescendants = source.readInt() == 1
        parent = source.readLong()
        voted = source.readInt() == 1
        pendingVoted = source.readInt() == 1
        next = source.readLong()
        previous = source.readLong()
    }

    override fun populate(info: Item) {
        title = info.getTitle()
        time = info.getTime()
        by = info.getBy()
        kids = info.getKids()
        url = info.getRawUrl()
        text = info.getText()
        displayedText = info.getDisplayedText(); // pre-load, but not part of Parcelable
        type = info.getRawType()
        descendants = info.getDescendants()
        hasNewDescendants = lastKidCount >= 0 && descendants > lastKidCount
        lastKidCount = descendants
        parent = Long.parseLong(info.getParent())
        deleted = info.isDeleted()
        dead = info.isDead()
        score = info.getScore()
        viewed = info.isViewed()
        favorite = info.isFavorite()
        localRevision = 1
    }

    override fun getRawType(): String {
        return type
    }

    override fun getRawUrl(): String {
        return url
    }

    override fun getKids(): LongArray {
        return kids
    }

    override fun getBy(): String {
        return by
    }

    override fun getTime(): Long {
        return time
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeString(title)
        dest.writeLong(time)
        dest.writeString(by)
        dest.writeLongArray(kids)
        dest.writeString(url)
        dest.writeString(text)
        dest.writeString(type)
        dest.writeInt(favorite ? 1 : 0)
        dest.writeInt(descendants)
        dest.writeInt(score)
        dest.writeInt(favorite ? 1 : 0)
        dest.writeInt(viewed ? 1 : 0)
        dest.writeInt(localRevision)
        dest.writeInt(level)
        dest.writeInt(dead ? 1 : 0)
        dest.writeInt(deleted ? 1 : 0)
        dest.writeInt(collapsed ? 1 : 0)
        dest.writeInt(contentExpanded ? 1 : 0)
        dest.writeInt(rank)
        dest.writeInt(lastKidCount)
        dest.writeInt(hasNewDescendants ? 1 : 0)
        dest.writeLong(parent)
        dest.writeInt(voted ? 1 : 0)
        dest.writeInt(pendingVoted ? 1 : 0)
        dest.writeLong(next)
        dest.writeLong(previous)
    }

    override fun getId(): String {
        return String.valueOf(id)
    }

    override fun getLongId(): Long {
        return id
    }

    override fun getTitle(): String {
        return title
    }

    override fun getDisplayedTitle(): String {
        switch (getType()) {
            case COMMENT_TYPE:
                return text
            case JOB_TYPE:
            case STORY_TYPE:
            case POLL_TYPE: // TODO poll need to display options
            default:
                return title
        }
    }

    @NonNull
    override fun getType(): String {
        return !TextUtils.isEmpty(type) ? type : STORY_TYPE
    }

    override fun getDisplayedAuthor(context: Context, linkify: Boolean, color: Int): Spannable {
        if (displayedAuthor == null) {
            if (TextUtils.isEmpty(by)) {
                displayedAuthor = SpannableString("")
            } else {
                defaultColor = ContextCompat.getColor(context, AppUtils.getThemedResId(context,
                        linkify ? android.R.attr.textColorLink : android.R.attr.textColorSecondary))
                displayedAuthor = createAuthorSpannable(linkify)
            }
        }
        if (displayedAuthor.length() == 0) {
            return displayedAuthor
        }
        displayedAuthor.setSpan(ForegroundColorSpan(color != 0 ? color : defaultColor),
                AUTHOR_SEPARATOR.length(), displayedAuthor.length(),
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        return displayedAuthor
    }

    override fun getDisplayedTime(context: Context): Spannable {
        if (displayedTime == null) {
            SpannableStringBuilder builder = SpannableStringBuilder(dead ?
                    context.getString(R.string.dead_prefix) + " " : "")
            SpannableString timeSpannable = SpannableString(
                    AppUtils.getAbbreviatedTimeSpan(time * 1000))
            if (deleted) {
                timeSpannable.setSpan(StrikethroughSpan(), 0, timeSpannable.length(),
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            }
            builder.append(timeSpannable)
            displayedTime = builder
        }
        return displayedTime
    }

    override fun getKidCount(): Int {
        if (descendants > 0) {
            return descendants
        }

        return kids != null ? kids.length : 0
    }

    override fun getLastKidCount(): Int {
        return lastKidCount
    }

    override fun setLastKidCount(lastKidCount: Int) {
        this.lastKidCount = lastKidCount
    }

    override fun hasNewKids(): Boolean {
        return hasNewDescendants
    }

    override fun getUrl(): String {
        switch (getType()) {
            case JOB_TYPE:
            case POLL_TYPE:
            case COMMENT_TYPE:
                return getItemUrl(getId())
            default:
                return TextUtils.isEmpty(url) ? getItemUrl(getId()) : url
        }
    }

    @NonNull
    private fun createAuthorSpannable(authorLink: Boolean): SpannableString {
        SpannableString bySpannable = SpannableString(AUTHOR_SEPARATOR + by)
        if (!authorLink) {
            return bySpannable
        }
        bySpannable.setSpan(StyleSpan(Typeface.BOLD),
                AUTHOR_SEPARATOR.length(), bySpannable.length(),
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        ClickableSpan clickableSpan = ClickableSpan() {
            public void onClick(View view) {
                view.getContext().startActivity(Intent(Intent.ACTION_VIEW)
                        .setData(AppUtils.createUserUri(getBy())))
            }

            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds)
                ds.setUnderlineText(false)
            }
        }
        bySpannable.setSpan(clickableSpan,
                AUTHOR_SEPARATOR.length(), bySpannable.length(),
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        return bySpannable
    }

    private fun getItemUrl(itemId: String): String {
        return String.format(HackerNewsClient.WEB_ITEM_PATH, itemId)
    }

    override fun getSource(): String {
        return TextUtils.isEmpty(getUrl()) ? null : Uri.parse(getUrl()).getHost()
    }

    override fun getKidItems(): Array<HackerNewsItem> {
        if (kids == null || kids.length == 0) {
            return HackerNewsItem[0]
        }

        if (kidItems == null) {
            kidItems = HackerNewsItem[kids.length]
            for (int i = 0; i < kids.length; i++) {
                HackerNewsItem item = HackerNewsItem(kids[i], level + 1)
                item.rank = i + 1
                if (i > 0) {
                    item.previous = kids[i - 1]
                }
                if (i < kids.length - 1) {
                    item.next = kids[i + 1]
                }
                kidItems[i] = item
            }
        }

        return kidItems
    }

    override fun getText(): String {
        return text
    }

    override fun getDisplayedText(): CharSequence {
        if (displayedText == null) {
            displayedText = AppUtils.fromHtml(text)
        }
        return displayedText
    }

    override fun isStoryType(): Boolean {
        switch (getType()) {
            case STORY_TYPE:
            case POLL_TYPE:
            case JOB_TYPE:
                return true
            case COMMENT_TYPE:
            default:
                return false
        }
    }

    override fun isFavorite(): Boolean {
        return favorite
    }

    override fun setFavorite(favorite: Boolean) {
        this.favorite = favorite
    }

    override fun getLocalRevision(): Int {
        return localRevision
    }

    override fun setLocalRevision(localRevision: Int) {
        this.localRevision = localRevision
    }

    override fun getDescendants(): Int {
        return descendants
    }

    override fun isViewed(): Boolean {
        return viewed
    }

    override fun setIsViewed(isViewed: Boolean) {
        viewed = isViewed
    }

    override fun getLevel(): Int {
        return level
    }

    override fun getParent(): String {
        return String.valueOf(parent)
    }

    override fun getParentItem(): Item {
        if (parent == 0) {
            return null
        }
        if (parentItem == null) {
            parentItem = HackerNewsItem(parent)
        }
        return parentItem
    }

    override fun isDeleted(): Boolean {
        return deleted
    }

    override fun isDead(): Boolean {
        return dead
    }

    override fun getScore(): Int {
        return score
    }

    override fun incrementScore() {
        score++
        voted = true
        pendingVoted = true
    }

    override fun isVoted(): Boolean {
        return voted
    }

    override fun isPendingVoted(): Boolean {
        return pendingVoted
    }

    override fun clearPendingVoted() {
        pendingVoted = false
    }

    override fun isCollapsed(): Boolean {
        return collapsed
    }

    override fun setCollapsed(collapsed: Boolean) {
        this.collapsed = collapsed
    }

    override fun getRank(): Int {
        return rank
    }

    override fun isContentExpanded(): Boolean {
        return contentExpanded
    }

    override fun setContentExpanded(expanded: Boolean) {
        contentExpanded = expanded
    }

    override fun getNeighbour(direction: Int): Long {
        switch (direction) {
            case Navigable.DIRECTION_UP:
                return previous
            case Navigable.DIRECTION_DOWN:
                return next
            case Navigable.DIRECTION_LEFT:
                return level > 1 ? parent : 0L
            case Navigable.DIRECTION_RIGHT:
                return kids != null && kids.length > 0 ? kids[0] : 0L
            default:
                return 0L
        }
    }

    override fun hashCode(): Int {
        return (int) id
    }

    override fun equals(o: Any): Boolean {
        return o is HackerNewsItem && id == ((HackerNewsItem) o).id
    }

    fun preload() {
        getDisplayedText(); // pre-load HTML
        getKidItems(); // pre-construct kids
    }
}
