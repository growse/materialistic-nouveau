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
package com.growse.android.io.github.hidroh.materialistic

import android.accounts.Account
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import android.text.TextUtils
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.growse.android.io.github.hidroh.materialistic.AdBlocker.init
import com.growse.android.io.github.hidroh.materialistic.FontCache.get
import com.growse.android.io.github.hidroh.materialistic.Preferences.Theme.getAutoDayNightMode
import com.growse.android.io.github.hidroh.materialistic.Preferences.Theme.getTypeface
import com.growse.android.io.github.hidroh.materialistic.Preferences.getUsername
import com.growse.android.io.github.hidroh.materialistic.Preferences.isSortByRecent
import com.growse.android.io.github.hidroh.materialistic.Preferences.migrate
import com.growse.android.io.github.hidroh.materialistic.Preferences.setUsername
import com.growse.android.io.github.hidroh.materialistic.data.AlgoliaClient
import com.growse.android.io.github.hidroh.materialistic.data.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import rx.schedulers.Schedulers

@HiltAndroidApp
class Application : Application() {
  override fun onCreate() {
    super.onCreate()
    AppCompatDelegate.setDefaultNightMode(getAutoDayNightMode(this))
    AlgoliaClient.sSortByTime = isSortByRecent(this)
    if (BuildConfig.DEBUG) {
      StrictMode.setThreadPolicy(ThreadPolicy.Builder().detectAll().penaltyFlashScreen().build())
      StrictMode.setVmPolicy(VmPolicy.Builder().detectAll().penaltyLog().build())
    }
    migrate(this)
    TYPE_FACE = get(this, getTypeface(this))
    removeLegacySyncAccount(this)
    rescheduleLegacyDeferredSyncs(this)
    registerAccountsUpdatedListener(this)
    init(this, Schedulers.io())
  }

  companion object {
    var TYPE_FACE: Typeface? = null
    private const val LEGACY_SYNC_ACCOUNT_NAME = "Materialistic"
    private const val LEGACY_SYNC_PREFERENCES_SUFFIX = "_syncpreferences"

    /**
     * Removes the placeholder account that the old sync adapter created to request syncs against.
     * It shares the login accounts' type, so it was offered in the account chooser. Unlike a real
     * login it was never given a password, which tells it apart from a user named the same.
     */
    @SuppressLint("MissingPermission")
    private fun removeLegacySyncAccount(context: Context?) {
      val accountManager = AccountManager.get(context)
      for (account in accountManager.getAccountsByType(BuildConfig.APPLICATION_ID)) {
        if (
            TextUtils.equals(account.name, LEGACY_SYNC_ACCOUNT_NAME) &&
                accountManager.getPassword(account) == null
        ) {
          accountManager.removeAccount(account, null, null, null)
        }
      }
    }

    /**
     * Reschedules stories that older versions deferred until Wi-Fi was available. Nothing retries
     * them any more, as sync jobs now wait for a permitted network themselves.
     */
    private fun rescheduleLegacyDeferredSyncs(context: Context) {
      val name = context.packageName + LEGACY_SYNC_PREFERENCES_SUFFIX
      val preferences = context.getSharedPreferences(name, MODE_PRIVATE)
      val itemIds = preferences.all.keys
      if (itemIds.isEmpty()) {
        return
      }
      val syncScheduler = SyncScheduler()
      for (itemId in itemIds) {
        syncScheduler.scheduleSync(context, itemId)
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.deleteSharedPreferences(name)
      } else {
        preferences.edit { clear() }
      }
    }

    @SuppressLint("MissingPermission")
    private fun registerAccountsUpdatedListener(context: Context) {
      AccountManager.get(context)
          .addOnAccountsUpdatedListener(
              { accounts: Array<Account>? ->
                val username = getUsername(context)
                if (TextUtils.isEmpty(username)) {
                  return@addOnAccountsUpdatedListener
                }
                for (account in accounts!!) {
                  if (TextUtils.equals(account.name, username)) {
                    return@addOnAccountsUpdatedListener
                  }
                }
                setUsername(context, null)
              },
              null,
              true,
          )
    }
  }
}
