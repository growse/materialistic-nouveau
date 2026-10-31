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
package com.growse.android.io.github.hidroh.materialistic.accounts

import android.content.Context
import androidx.annotation.StringRes
import java.io.IOException

interface UserServices {
  open class Callback {
    open fun onDone(successful: Boolean) {}

    open fun onError(throwable: Throwable?) {}
  }

  class Exception : IOException {
    @StringRes @JvmField val messageResId: Int
    /** Id of the existing item when a submission is a duplicate. */
    @JvmField var itemId: String? = null

    constructor(@StringRes messageResId: Int) {
      this.messageResId = messageResId
    }

    internal constructor(message: String?) : super(message) {
      this.messageResId = 0
    }
  }

  fun login(username: String, password: String, createAccount: Boolean, callback: Callback)

  fun voteUp(context: Context, itemId: String, callback: Callback): Boolean

  fun reply(context: Context, parentId: String, text: String, callback: Callback)

  fun submit(
      context: Context,
      title: String,
      content: String,
      isUrl: Boolean,
      callback: Callback,
  )
}
