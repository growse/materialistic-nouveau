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
package com.growse.android.io.github.hidroh.materialistic.data

import android.text.format.DateUtils
import androidx.annotation.StringDef
import javax.inject.Inject
import retrofit2.Call
import rx.Observable

class AlgoliaPopularClient @Inject constructor(factory: RestServiceFactory) :
    AlgoliaClient(factory) {
  @Retention(AnnotationRetention.SOURCE)
  @StringDef(LAST_24H, PAST_WEEK, PAST_MONTH, PAST_YEAR)
  annotation class Range

  override fun searchRx(@Range filter: String): Observable<AlgoliaHits?>? {
    return mRestService.searchByMinTimestampRx(MIN_CREATED_AT + toTimestamp(filter) / 1000)
  }

  override fun search(@Range filter: String): Call<AlgoliaHits?>? {
    return mRestService.searchByMinTimestamp(MIN_CREATED_AT + toTimestamp(filter) / 1000)
  }

  private fun toTimestamp(@Range filter: String): Long =
      System.currentTimeMillis() -
          when (filter) {
            PAST_WEEK -> DateUtils.WEEK_IN_MILLIS
            PAST_MONTH -> DateUtils.WEEK_IN_MILLIS * 4
            PAST_YEAR -> DateUtils.YEAR_IN_MILLIS
            else -> DateUtils.DAY_IN_MILLIS
          }

  companion object {
    const val LAST_24H = "last_24h"
    const val PAST_WEEK = "past_week"
    const val PAST_MONTH = "past_month"
    const val PAST_YEAR = "past_year"
  }
}
