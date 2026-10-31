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
import android.content.res.TypedArray
import android.database.DataSetObserver
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.SpinnerAdapter
import androidx.core.content.withStyledAttributes
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.growse.android.io.github.hidroh.materialistic.R

/**
 * [Preference] with spinner as custom widget. Entries, entry values and entry layouts should be
 * provided via arrays. Preference value will be persisted as string.
 */
abstract class SpinnerPreference(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    Preference(context, attrs, defStyleAttr) {
  var mEntries: Array<String> = emptyArray()
  var mEntryValues: Array<String> = emptyArray()
  var mSelection: Int = 0

  @Suppress("unused") constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

  init {
    widgetLayoutResource = R.layout.preference_spinner
    init(context, attrs)
  }

  private fun init(context: Context, attrs: AttributeSet?) {
    context.withStyledAttributes(attrs, R.styleable.SpinnerPreference) {
      val entriesResId = getResourceId(R.styleable.SpinnerPreference_entries, 0)
      if (entriesResId != 0) {
        mEntries = context.resources.getStringArray(entriesResId)
      }
      val valuesResId = getResourceId(R.styleable.SpinnerPreference_entryValues, 0)
      if (valuesResId != 0) {
        mEntryValues = context.resources.getStringArray(valuesResId)
      }
    }
  }

  override fun onGetDefaultValue(a: TypedArray, index: Int): Any? {
    return a.getString(index)
  }

  @Deprecated("Deprecated in Java")
  override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
    super.onSetInitialValue(restorePersistedValue, defaultValue)
    val value = if (restorePersistedValue) getPersistedString(null) else defaultValue as String?
    for (i in mEntryValues.indices) {
      if (TextUtils.equals(mEntryValues[i], value)) {
        mSelection = i
        break
      }
    }
  }

  override fun onBindViewHolder(holder: PreferenceViewHolder) {
    super.onBindViewHolder(holder)
    val spinner = holder.findViewById(R.id.spinner) as Spinner
    holder.itemView.setOnClickListener { spinner.performClick() }
    spinner.adapter =
        object : SpinnerAdapter {
          override fun getDropDownView(
              position: Int,
              convertView: View?,
              parent: ViewGroup,
          ): View {
            var convertView = convertView
            if (convertView == null) {
              convertView = createDropDownView(position, parent)
            }
            bindDropDownView(position, convertView)
            return convertView
          }

          override fun registerDataSetObserver(observer: DataSetObserver?) {
            // no op
          }

          override fun unregisterDataSetObserver(observer: DataSetObserver?) {
            // no op
          }

          override fun getCount(): Int {
            return mEntries.size
          }

          override fun getItem(position: Int): Any? {
            return null // not applicable
          }

          override fun getItemId(position: Int): Long {
            return position.toLong()
          }

          override fun hasStableIds(): Boolean {
            return true
          }

          override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            return getDropDownView(position, convertView, parent)
          }

          override fun getItemViewType(position: Int): Int {
            return 0
          }

          override fun getViewTypeCount(): Int {
            return 1
          }

          override fun isEmpty(): Boolean {
            return false
          }
        }
    spinner.setSelection(mSelection)
    spinner.onItemSelectedListener =
        object : AdapterView.OnItemSelectedListener {
          override fun onItemSelected(
              parent: AdapterView<*>?,
              view: View?,
              position: Int,
              id: Long,
          ) {
            mSelection = position
            persistString(mEntryValues[position])
          }

          override fun onNothingSelected(parent: AdapterView<*>?) {
            // no op
          }
        }
  }

  /**
   * Create dropdown view for item at given position
   *
   * @param position item position
   * @param parent parent view
   * @return created view
   */
  protected abstract fun createDropDownView(position: Int, parent: ViewGroup): View

  /**
   * Customize dropdown view for given spinner item
   *
   * @param position item position
   * @param view item view
   */
  protected abstract fun bindDropDownView(position: Int, view: View)
}
