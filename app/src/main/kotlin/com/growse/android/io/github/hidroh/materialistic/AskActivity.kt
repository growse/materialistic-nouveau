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
package com.growse.android.io.github.hidroh.materialistic

import com.growse.android.io.github.hidroh.materialistic.data.ItemManager
import com.growse.android.io.github.hidroh.materialistic.data.ItemManager.FetchMode
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AskActivity : BaseStoriesActivity() {
  override val defaultTitle: String
    get() = getString(R.string.title_activity_ask)

  @FetchMode
  override val fetchMode: String
    get() = ItemManager.ASK_FETCH_MODE
}
