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

/**
 * Data repository for local items
 *
 * @param <T> item type </T>
 */
interface LocalItemManager<T> {
  /**
   * Gets number of items
   *
   * @return number of items, 0 if none
   */
  val size: Int

  /**
   * Gets item at given position
   *
   * @param position item position
   * @return item at given position, or null if none
   */
  fun getItem(position: Int): T?

  /**
   * Initiates an async query for local items
   *
   * @param observer listener that will be informed on changes
   * @param filter query filter if any
   */
  fun attach(observer: Observer, filter: String)

  /** Cleans up any extra state created by [.attach] */
  fun detach()

  /** Callback interface for local items change events */
  interface Observer {
    /** Fired when local items change (added/removed/edited) */
    fun onChanged()
  }
}
