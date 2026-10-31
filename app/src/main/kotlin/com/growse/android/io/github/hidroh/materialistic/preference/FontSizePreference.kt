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
package com.growse.android.io.github.hidroh.materialistic.preference

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.growse.android.io.github.hidroh.materialistic.AppUtils
import com.growse.android.io.github.hidroh.materialistic.Preferences.Theme.resolveTextSize
import com.growse.android.io.github.hidroh.materialistic.R

class FontSizePreference(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    SpinnerPreference(context, attrs, defStyleAttr) {
  private val mLayoutInflater: LayoutInflater = LayoutInflater.from(getContext())

  @Suppress("unused") constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

  override fun createDropDownView(position: Int, parent: ViewGroup): View {
    return mLayoutInflater.inflate(R.layout.support_simple_spinner_dropdown_item, parent, false)
  }

  override fun bindDropDownView(position: Int, view: View) {
    val textView = view.findViewById<View?>(android.R.id.text1) as TextView
    val textSize =
        AppUtils.getDimension(
            context,
            resolveTextSize(mEntryValues[position]),
            R.attr.contentTextSize,
        )
    textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
    textView.text = mEntries[position]
  }
}
