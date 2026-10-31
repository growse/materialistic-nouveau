/*
 * Copyright (c) 2017 Ha Duy Trung
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
import com.growse.android.io.github.hidroh.materialistic.data.SyncDelegate.JobBuilder

class SyncScheduler {
  fun scheduleSync(context: Context, itemId: String) {
    SyncDelegate.scheduleSync(context, JobBuilder(context, itemId).build())
  }

  /**
   * Re-syncs a story that is already saved, going to the network for every item rather than reusing
   * the stored copy, so that comments posted since the last sync are downloaded.
   */
  fun scheduleRefresh(context: Context, itemId: String) {
    SyncDelegate.scheduleSync(
        context,
        JobBuilder(context, itemId).setRefresh(true).build(),
    )
  }
}
