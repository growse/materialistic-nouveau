/*
 * Copyright (c) 2015 Ha Duy Trung
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

import androidx.annotation.IntDef
import androidx.annotation.StringDef
import androidx.annotation.WorkerThread

/** Data repository for {@link Item} */
interface ItemManager {

  @Retention(AnnotationRetention.SOURCE)
  @StringDef(
      TOP_FETCH_MODE,
      NEW_FETCH_MODE,
      ASK_FETCH_MODE,
      SHOW_FETCH_MODE,
      JOBS_FETCH_MODE,
      BEST_FETCH_MODE,
  )
  annotation class FetchMode

  @Retention(AnnotationRetention.SOURCE)
  @IntDef(MODE_DEFAULT, MODE_CACHE, MODE_NETWORK)
  annotation class CacheMode

  companion object {
    const val TOP_FETCH_MODE = "top"
    const val NEW_FETCH_MODE = "new"
    const val ASK_FETCH_MODE = "ask"
    const val SHOW_FETCH_MODE = "show"
    const val JOBS_FETCH_MODE = "jobs"
    const val BEST_FETCH_MODE = "best"
    const val MODE_DEFAULT = 0
    const val MODE_CACHE = 1
    const val MODE_NETWORK = 2
  }

  /**
   * Gets array of top stories
   *
   * @param filter filter of stories to fetch
   * @param cacheMode cache mode
   * @param listener callback to be notified on response
   */
  fun getStories(
      filter: String,
      @CacheMode cacheMode: Int,
      listener: ResponseListener<Array<Item>>?,
  )

  /**
   * Gets individual item by ID
   *
   * @param itemId item ID
   * @param cacheMode cache mode
   * @param listener callback to be notified on response
   */
  fun getItem(itemId: String, @CacheMode cacheMode: Int, listener: ResponseListener<Item>?)

  /**
   * Gets array of stories
   *
   * @param filter filter of stories to fetch
   * @param cacheMode cache mode
   * @return array of stories
   */
  @WorkerThread fun getStories(filter: String, @CacheMode cacheMode: Int): Array<Item>?

  /**
   * Gets individual item by ID
   *
   * @param itemId item ID
   * @param cacheMode cache mode
   * @return item or null
   */
  @WorkerThread fun getItem(itemId: String, @CacheMode cacheMode: Int): Item?
}
