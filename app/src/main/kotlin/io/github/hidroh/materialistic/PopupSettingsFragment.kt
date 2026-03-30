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

package io.github.hidroh.materialistic

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StyleRes
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.google.android.material.bottomsheet.BottomSheetDialog

open class PopupSettingsFragment : AppCompatDialogFragment() {
    val EXTRA_TITLE: String = PopupSettingsFragment::class.java.name + ".EXTRA_TITLE"
    val EXTRA_SUMMARY: String = PopupSettingsFragment::class.java.name + ".EXTRA_SUMMARY"
    static final String EXTRA_XML_PREFERENCES = PopupSettingsFragment::class.java.getName() +
            ".EXTRA_XML_PREFERENCES"

    @Nullable
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View {
        return inflater.inflate(R.layout.fragment_popup_settings, container, false)
    }

    @NonNull
    override fun onCreateDialog(savedInstanceState: Bundle): Dialog {
        return BottomSheetDialog(getActivity(), getTheme())
    }

    override fun onActivityCreated(savedInstanceState: Bundle) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState == null) {
            val fragment = Fragment.instantiate(getActivity(),
                    PreferenceFragment::class.java.getName(), getArguments())
            getChildFragmentManager()
                    .beginTransaction()
                    .add(R.id.content, fragment)
                    .commit()
        }
    }

    open class PreferenceFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(bundle: Bundle, s: String) {
            addPreferencesFromResource(R.xml.preferences_category)
            val category = findPreference(getString(R.string.pref_category))
            int summary, title
            if ((title = getArguments().getInt(EXTRA_TITLE, 0)) != 0) {
                category.setTitle(title)
            }
            if ((summary = getArguments().getInt(EXTRA_SUMMARY, 0)) != 0) {
                category.setSummary(summary)
            }
            int[] preferences = getArguments().getIntArray(EXTRA_XML_PREFERENCES)
            if (preferences != null) {
                for (preference in preferences) {
                    addPreferencesFromResource(preference)
                }
            }
        }
    }

    open class BottomSheetDialog : com.google.android.material.bottomsheet.BottomSheetDialog() {
        constructor(context: Context, theme: Int) : super(context, theme) {
        }

        protected override fun onCreate(savedInstanceState: Bundle) {
            super.onCreate(savedInstanceState)
            int width = getContext().getResources().getDimensionPixelSize(R.dimen.bottom_sheet_width)
            //noinspection ConstantConditions
            getWindow().setLayout(
                    width > 0 ? width : ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT)
        }

    }
}
