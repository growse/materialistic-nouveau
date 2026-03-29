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

package io.github.hidroh.materialistic.preference

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import io.github.hidroh.materialistic.Application
import io.github.hidroh.materialistic.FontCache
import io.github.hidroh.materialistic.R

open class FontPreference : SpinnerPreference() {
    private var mLayoutInflater: LayoutInflater? = null

    @SuppressWarnings("unused")
    constructor(context: Context, attrs: AttributeSet) {
        this(context, attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) {
        super(context, attrs, defStyleAttr)
        mLayoutInflater = LayoutInflater.from(getContext())
    }

    protected override fun createDropDownView(position: Int, parent: ViewGroup): View {
        return mLayoutInflater.inflate(R.layout.support_simple_spinner_dropdown_item, parent, false)
    }

    protected override fun bindDropDownView(position: Int, view: View) {
        TextView textView = (TextView) view.findViewById(android.R.id.text1)
        textView.setTypeface(FontCache.getInstance().get(getContext(), mEntryValues[position]))
        textView.setText(mEntries[position])
    }

    protected override fun persistString(value: String): Boolean {
        if (TextUtils.equals(getKey(), getContext().getString(R.string.pref_font))) {
            Application.TYPE_FACE = FontCache.getInstance().get(getContext(), value)
        }
        return super.persistString(value)
    }

}
