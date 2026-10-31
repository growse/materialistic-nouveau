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
package com.growse.android.io.github.hidroh.materialistic.widget

import android.view.View
import android.widget.TextView
import com.growse.android.io.github.hidroh.materialistic.R

class ToggleItemViewHolder : ItemRecyclerViewAdapter.ItemViewHolder {
  @JvmField var toggleButton: View? = null
  @JvmField var toggle: TextView? = null
  @JvmField var level: View? = null

  constructor(itemView: View) : super(itemView) {
    toggleButton = itemView.findViewById(R.id.button_toggle)
    toggle = itemView.findViewById<View?>(R.id.toggle) as TextView?
    level = itemView.findViewById(R.id.level)
  }

  constructor(itemView: View, payload: Any?) : super(itemView, payload)
}
