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

/**
 * Represents an item that can be displayed as story/comment
 */
interface Item : WebItem {

    /**
     * Sets information from given item
     * @param info source item
     */
    fun populate(info: Item)

    /**
     * Gets raw item type, used to be parsed by {@link #getType()}
     * @return string type or null
     * @see Type
     */
    fun getRawType(): String

    /**
     * Gets raw URL
     * @return string URL or null
     * @see #getUrl()
     */
    fun getRawUrl(): String

    /**
     * Gets array of kid IDs
     * @return array of kid IDs or null
     * @see #getKidCount()
     * @see #getKidItems()
     */
    fun getKids(): LongArray

    /**
     * Gets author name
     * @return author name or null
     * @see WebItem#getDisplayedAuthor(Context, boolean, int)
     */
    fun getBy(): String

    /**
     * Gets posted time
     * @return posted time as Unix timestamp in seconds
     * @see WebItem#getDisplayedAuthor(Context, boolean, int)
     */
    fun getTime(): Long

    /**
     * Gets title
     * @return title or null
     * @see #getDisplayedTitle()
     */
    fun getTitle(): String

    /**
     * Gets item text
     * @return item text or null
     * @see #getDisplayedTitle()
     */
    fun getText(): String

    /**
     * Gets number of kids, contained in {@link #getKids()}
     * @return number of kids
     * @see #getKids()
     * @see #getKidItems()
     */
    fun getKidCount(): Int

    /**
     * Gets previous number of kids, before {@link #populate(Item)} is called
     * @return previous number of kids
     * @see #setLastKidCount(int)
     */
    fun getLastKidCount(): Int

    /**
     * Sets previous number of kids, before {@link #populate(Item)} is called
     * @param lastKidCount previous number of kids
     */
    fun setLastKidCount(lastKidCount: Int)

    /**
     * Checks if item has new kids after {@link #populate(Item)}
     * @return true if has new kids, false otherwise
     */
    fun hasNewKids(): Boolean

    /**
     * Gets array of kids, with corresponding IDs in {@link #getKids()}
     * @return array of kids or null
     * @see #getKids()
     * @see #getKidCount()
     */
    fun getKidItems(): Array<Item>

    /**
     * Gets item's current revision. A revision can be used to determined if item state is stale
     * and needs updated
     * @return current revision
     * @see #setLocalRevision(int)
     * @see #populate(Item)
     * @see #setFavorite(boolean)
     */
    fun getLocalRevision(): Int

    /**
     * Updates item's current revision to new one
     * @param localRevision new item revision
     * @see #getLocalRevision()
     */
    fun setLocalRevision(localRevision: Int)

    /**
     * Gets item's descendants if any
     * @return  item's descendants or -1 if none
     */
    fun getDescendants(): Int

    /**
     * Indicates if this item has been viewed
     * @return true if viewed, falst if not, null if unknown
     */
    fun isViewed(): Boolean

    /**
     * Sets item view status
     * @param isViewed  true if has been viewed, false otherwise
     */
    fun setIsViewed(isViewed: Boolean)

    /**
     * Gets item level, i.e. how many ascendants it has
     * @return item level
     */
    fun getLevel(): Int

    /**
     * Gets parent ID if any
     * @return parent ID or 0 if none
     */
    fun getParent(): String

    /**
     * Gets parent item if any
     * @return parent item or null
     */
    fun getParentItem(): Item

    /**
     * Checks if item has been deleted
     * @return true if deleted, false otherwise
     */
    fun isDeleted(): Boolean

    /**
     * Checks if item is dead
     * @return true if dead, false otherwise
     */
    fun isDead(): Boolean

    /**
     * Gets item's score
     * @return item's score
     */
    fun getScore(): Int

    /**
     * Increments item's score
     */
    fun incrementScore()

    /**
     * Checks if item has been voted via a user action
     * @return true if voted, false otherwise
     * @see #incrementScore()
     */
    fun isVoted(): Boolean

    /**
     * Checks if item has pending vote via a user action
     * @return true if pending voted, false otherwise
     * @see #incrementScore()
     */
    fun isPendingVoted(): Boolean

    /**
     * Clears pending voted status
     * @see #isPendingVoted()
     * @see #incrementScore()
     */
    fun clearPendingVoted()

    /**
     * Checks if item is collapsed
     * @return true if collapsed, false otherwise
     */
    fun isCollapsed(): Boolean

    /**
     * Sets item collapsed state
     * @param collapsed true to collapse, false otherwise
     */
    fun setCollapsed(collapsed: Boolean)

    /**
     * Gets item's rank among its siblings
     * @return item's rank
     */
    fun getRank(): Int

    /**
     * Checks if item content is expanded
     * @return true if expanded, false otherwise
     */
    fun isContentExpanded(): Boolean

    /**
     * Sets item content expanded state
     * @param expanded true to expand, false otherwise
     */
    fun setContentExpanded(expanded: Boolean)

    fun getNeighbour(direction: Int): Long

    fun getDisplayedText(): CharSequence
}
