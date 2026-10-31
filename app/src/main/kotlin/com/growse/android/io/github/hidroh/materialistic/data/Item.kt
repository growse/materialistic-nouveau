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

/** Represents an item that can be displayed as story/comment. */
interface Item : WebItem {
  /** Copies the content of [info] into this item. */
  fun populate(info: Item)

  /** Raw item type, parsed into [type]. */
  val rawType: String?

  /** Raw URL, used to build [url]. */
  val rawUrl: String?

  /** IDs of this item's kids, if any. */
  val kids: LongArray?

  /** Author name, if any. */
  val by: String?

  /** Posted time as a Unix timestamp in seconds. */
  val time: Long

  val title: String?

  val text: String?

  /** Number of kids, contained in [kids]. */
  val kidCount: Int

  /** Number of kids before the last [populate]. */
  var lastKidCount: Int

  /** True if the last [populate] brought new kids. */
  fun hasNewKids(): Boolean

  /** Kids, matching the IDs in [kids]. */
  val kidItems: Array<out Item>

  /**
   * Current revision, used to determine whether the item's state is stale and needs updating.
   *
   * @see populate
   */
  var localRevision: Int

  /** Number of descendants, or -1 if unknown. */
  val descendants: Int

  /** Whether this item has been viewed. */
  var isViewed: Boolean

  /** How many ascendants this item has. */
  val level: Int

  /** Parent ID, or "0" if none. */
  val parent: String

  /** Parent item, if any. */
  val parentItem: Item?

  val isDeleted: Boolean

  val isDead: Boolean

  val score: Int

  /** Increments [score] after a user vote, marking the item [isVoted] and [isPendingVoted]. */
  fun incrementScore()

  /** True if the user has voted on this item. */
  val isVoted: Boolean

  /** True if a user vote has not been acknowledged yet. */
  val isPendingVoted: Boolean

  /** Clears [isPendingVoted]. */
  fun clearPendingVoted()

  var isCollapsed: Boolean

  /** Rank among siblings. */
  val rank: Int

  var isContentExpanded: Boolean

  /**
   * ID of the neighbouring item in the given
   * [com.growse.android.io.github.hidroh.materialistic.Navigable] direction, or 0.
   */
  fun getNeighbour(direction: Int): Long

  /** [text] rendered from HTML. */
  val displayedText: CharSequence?
}
