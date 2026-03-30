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

package io.github.hidroh.materialistic.widget.preference

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.SuperscriptSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import io.github.hidroh.materialistic.R
import io.github.hidroh.materialistic.widget.AsteriskSpan

open class HelpListView : ScrollView() {
  constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    addView(LayoutInflater.from(context).inflate(R.layout.include_help_list_view, this, false))
  }

  protected override fun onFinishInflate() {
    super.onFinishInflate()
    ((TextView) findViewById (R.id.item_new).findViewById(R.id.rank)).append(makeAsteriskSpan())
    val spannable = SpannableString("+5")
    spannable.setSpan(SuperscriptSpan(), 0, spannable.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
    spannable.setSpan(
        RelativeSizeSpan(0.6f),
        0,
        spannable.length(),
        Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
    )
    spannable.setSpan(
        ForegroundColorSpan(ContextCompat.getColor(getContext(), R.color.greenA700)),
        0,
        spannable.length(),
        Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
    )
    ((TextView) findViewById (R.id.item_promoted).findViewById(R.id.rank)).append(spannable)
    val comments = (TextView) findViewById (R.id.item_new_comments).findViewById(R.id.comment)
    val sb = SpannableStringBuilder("46")
    sb.append(makeAsteriskSpan())
    comments.setText(sb)
  }

  private fun makeAsteriskSpan(): Spannable {
    val sb = SpannableString("*")
    sb.setSpan(
        AsteriskSpan(getContext()),
        sb.length() - 1,
        sb.length(),
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
    return sb
  }
}
