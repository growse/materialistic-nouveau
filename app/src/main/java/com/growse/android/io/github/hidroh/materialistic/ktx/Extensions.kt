/*
 * Copyright (c) 2018 Ha Duy Trung
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

package com.growse.android.io.github.hidroh.materialistic.ktx

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.growse.android.io.github.hidroh.materialistic.R
import java.io.Closeable
import java.io.File

fun Closeable.closeQuietly() {
  try {
    close()
  } catch (_: Exception) {}
}

fun File.getUri(context: Context, authority: String) =
    FileProvider.getUriForFile(context, authority, this)!!

// use ACTION_SEND_MULTIPLE instead of ACTION_SEND to filter out
// share receivers that accept only EXTRA_TEXT but not EXTRA_STREAM
fun Uri.toSendIntentChooser(context: Context): Intent =
    Intent.createChooser(
        Intent(Intent.ACTION_SEND_MULTIPLE)
            .setType("text/plain")
            .putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(this)),
        context.getString(R.string.share_file),
    )

fun NotificationCompat.Builder.setChannel(
    context: Context,
    channelId: String,
    name: CharSequence,
): NotificationCompat.Builder {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        .createNotificationChannel(
            NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_LOW)
        )
    this.setChannelId(channelId)
  }
  return this
}
