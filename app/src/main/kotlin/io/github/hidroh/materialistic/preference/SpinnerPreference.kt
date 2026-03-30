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
import android.content.res.TypedArray
import android.database.DataSetObserver
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.SpinnerAdapter

import io.github.hidroh.materialistic.R
import io.github.hidroh.materialistic.annotation.Synthetic

/**
 * {@link Preference} with spinner as custom widget.
 * Entries, entry values and entry layouts should be provided via arrays.
 * Preference value will be persisted as string.
 */
abstract class SpinnerPreference : Preference() {
    var mEntries: Array<String> = new String[0]
    var mEntryValues: Array<String> = new String[0]
    var mSelection: Int = 0

    @SuppressWarnings("unused")
    constructor(context: Context, attrs: AttributeSet) {
        this(context, attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setWidgetLayoutResource(R.layout.preference_spinner)
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.SpinnerPreference)
        int entriesResId = ta.getResourceId(R.styleable.SpinnerPreference_entries, 0)
        if (entriesResId != 0) {
            mEntries = context.getResources().getStringArray(entriesResId)
        }
        int valuesResId = ta.getResourceId(R.styleable.SpinnerPreference_entryValues, 0)
        if (valuesResId != 0) {
            mEntryValues = context.getResources().getStringArray(valuesResId)
        }
        ta.recycle()
    }

    protected override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getString(index)
    }

    protected override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any) {
        super.onSetInitialValue(restorePersistedValue, defaultValue)
        val value = restorePersistedValue ? getPersistedString(null) : (String) defaultValue
        for (int i = 0; i < mEntryValues.length; i++) {
            if (TextUtils.equals(mEntryValues[i], value)) {
                mSelection = i
                break
            }
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val spinner = (Spinner) holder.findViewById(R.id.spinner)
        holder.itemView.setOnClickListener(v -> spinner.performClick())
        spinner.setAdapter(SpinnerAdapter() {
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = createDropDownView(position, parent)
                }
                bindDropDownView(position, convertView)
                return convertView
            }

            public void registerDataSetObserver(DataSetObserver observer) {
                // no op
            }

            public void unregisterDataSetObserver(DataSetObserver observer) {
                // no op
            }

            public int getCount() {
                return mEntries.length
            }

            public Object getItem(int position) {
                return null; // not applicable
            }

            public long getItemId(int position) {
                return position
            }

            public boolean hasStableIds() {
                return true
            }

            public View getView(int position, View convertView, ViewGroup parent) {
                return getDropDownView(position, convertView, parent)
            }

            public int getItemViewType(int position) {
                return 0
            }

            public int getViewTypeCount() {
                return 1
            }

            public boolean isEmpty() {
                return false
            }
        })
        spinner.setSelection(mSelection)
        spinner.setOnItemSelectedListener(AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSelection = position
                persistString(mEntryValues[position])
            }

            public void onNothingSelected(AdapterView<?> parent) {
                // no op
            }
        })
    }

    /**
     * Create dropdown view for item at given position
     * @param position    item position
     * @param parent      parent view
     * @return  created view
     */
    protected abstract fun createDropDownView(position: Int, parent: ViewGroup): View

    /**
     * Customize dropdown view for given spinner item
     * @param position  item position
     * @param view      item view
     */
    protected abstract fun bindDropDownView(position: Int, view: View)
}
