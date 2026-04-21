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
package com.growse.android.io.github.hidroh.materialistic.widget

import android.content.Context
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView

internal abstract class RecyclerViewAdapter<VH : RecyclerView.ViewHolder?> :
    RecyclerView.Adapter<VH?>() {
  @JvmField protected var context: Context? = null
  @JvmField protected var recyclerView: RecyclerView? = null

  @CallSuper
  open fun attach(context: Context?, recyclerView: RecyclerView?) {
    this@RecyclerViewAdapter.context = context
    this@RecyclerViewAdapter.recyclerView = recyclerView
  }

  @CallSuper
  open fun detach(context: Context?, recyclerView: RecyclerView?) {
    this@RecyclerViewAdapter.context = null
    this@RecyclerViewAdapter.recyclerView = null
  }
}
