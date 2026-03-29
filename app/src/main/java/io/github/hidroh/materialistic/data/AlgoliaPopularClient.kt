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

package io.github.hidroh.materialistic.data

import android.text.format.DateUtils


import javax.inject.Inject

import androidx.annotation.StringDef
import retrofit2.Call
import rx.Observable

open class AlgoliaPopularClient : AlgoliaClient() {

    @Inject
    constructor(factory: RestServiceFactory) {
        super(factory)
    }

    @Retention(AnnotationRetention.SOURCE)
    @StringDef({
    annotation class Range {}
    const val LAST_24H: String = "last_24h"
    const val PAST_WEEK: String = "past_week"
    const val PAST_MONTH: String = "past_month"
    const val PAST_YEAR: String = "past_year"

    protected override fun searchRx(filter: String): Observable<AlgoliaHits> {
        return mRestService.searchByMinTimestampRx(MIN_CREATED_AT + toTimestamp(filter) / 1000)
    }

    protected override fun search(filter: String): Call<AlgoliaHits> {
        return mRestService.searchByMinTimestamp(MIN_CREATED_AT + toTimestamp(filter) / 1000)
    }

    private fun toTimestamp(filter: String): Long {
        long timestamp = System.currentTimeMillis()
        switch (filter) {
            case LAST_24H:
            default:
                timestamp -= DateUtils.DAY_IN_MILLIS
                break
            case PAST_WEEK:
                timestamp -= DateUtils.WEEK_IN_MILLIS
                break
            case PAST_MONTH:
                timestamp -= DateUtils.WEEK_IN_MILLIS * 4
                break
            case PAST_YEAR:
                timestamp -= DateUtils.YEAR_IN_MILLIS
                break
        }
        return timestamp
    }
}
