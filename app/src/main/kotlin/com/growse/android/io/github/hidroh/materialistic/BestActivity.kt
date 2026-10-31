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
package com.growse.android.io.github.hidroh.materialistic

import com.growse.android.io.github.hidroh.materialistic.data.ItemManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BestActivity : BaseStoriesActivity() {
  override val fetchMode: String
    get() = ItemManager.BEST_FETCH_MODE

  override val defaultTitle: String
    get() = getString(R.string.title_activity_best)
}
