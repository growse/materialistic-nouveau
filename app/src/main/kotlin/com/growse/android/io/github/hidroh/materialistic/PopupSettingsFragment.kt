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

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class PopupSettingsFragment : AppCompatDialogFragment() {
  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View? {
    return inflater.inflate(R.layout.fragment_popup_settings, container, false)
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return BottomSheetDialog(requireActivity(), theme)
  }

  @Deprecated("Deprecated in Java")
  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    if (savedInstanceState == null) {
      @Suppress("DEPRECATION")
      val fragment =
          Fragment.instantiate(
              requireActivity(),
              PreferenceFragment::class.java.name,
              arguments,
          )
      childFragmentManager.beginTransaction().add(R.id.content, fragment).commit()
    }
  }

  class PreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(bundle: Bundle?, s: String?) {
      addPreferencesFromResource(R.xml.preferences_category)
      val category = findPreference<Preference>(getString(R.string.pref_category))
      val title = requireArguments().getInt(EXTRA_TITLE, 0)
      if (title != 0) {
        category?.setTitle(title)
      }
      val summary = requireArguments().getInt(EXTRA_SUMMARY, 0)
      if (summary != 0) {
        category?.setSummary(summary)
      }
      val preferences = requireArguments().getIntArray(EXTRA_XML_PREFERENCES)
      if (preferences != null) {
        for (preference in preferences) {
          addPreferencesFromResource(preference)
        }
      }
    }
  }

  // the supertype is spelled out in full because this class shadows the imported name, exactly
  // as the Java version did
  class BottomSheetDialog(context: Context, @StyleRes theme: Int) :
      com.google.android.material.bottomsheet.BottomSheetDialog(context, theme) {
    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      val width = context.resources.getDimensionPixelSize(R.dimen.bottom_sheet_width)
      window?.setLayout(
          if (width > 0) width else ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT,
      )
    }
  }

  companion object {
    // @JvmField so the Java callers keep seeing static fields rather than getters
    @JvmField val EXTRA_TITLE: String = PopupSettingsFragment::class.java.name + ".EXTRA_TITLE"

    @JvmField val EXTRA_SUMMARY: String = PopupSettingsFragment::class.java.name + ".EXTRA_SUMMARY"

    @JvmField
    val EXTRA_XML_PREFERENCES: String =
        PopupSettingsFragment::class.java.name + ".EXTRA_XML_PREFERENCES"
  }
}
