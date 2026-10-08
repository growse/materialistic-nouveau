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

package com.growse.android.io.github.hidroh.materialistic;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.graphics.Typeface;
import android.os.StrictMode;
import androidx.appcompat.app.AppCompatDelegate;

import com.growse.android.io.github.hidroh.materialistic.data.AlgoliaClient;
import dagger.hilt.android.HiltAndroidApp;
import rx.schedulers.Schedulers;

@HiltAndroidApp
public class Application extends android.app.Application {

    public static Typeface TYPE_FACE = null;
    private static final String LEGACY_SYNC_ACCOUNT_NAME = "Materialistic";

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(Preferences.Theme.getAutoDayNightMode(this));
        AlgoliaClient.sSortByTime = Preferences.isSortByRecent(this);
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyFlashScreen()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }
        Preferences.migrate(this);
        TYPE_FACE = FontCache.get(this, Preferences.Theme.getTypeface(this));
        removeLegacySyncAccount(this);
        registerAccountsUpdatedListener(this);
        AdBlocker.init(this, Schedulers.io());
    }

    /**
     * Removes the placeholder account that the old sync adapter created to request syncs against.
     * It shares the login accounts' type, so it was offered in the account chooser. Unlike a real
     * login it was never given a password, which tells it apart from a user named the same.
     */
    @SuppressLint("MissingPermission")
    private static void removeLegacySyncAccount(Context context) {
        AccountManager accountManager = AccountManager.get(context);
        for (Account account : accountManager.getAccountsByType(BuildConfig.APPLICATION_ID)) {
            if (TextUtils.equals(account.name, LEGACY_SYNC_ACCOUNT_NAME)
                    && accountManager.getPassword(account) == null) {
                accountManager.removeAccount(account, null, null, null);
            }
        }
    }

    @SuppressLint("MissingPermission")
    private static void registerAccountsUpdatedListener(final Context context) {
        AccountManager.get(context).addOnAccountsUpdatedListener(accounts -> {
            String username = Preferences.getUsername(context);
            if (TextUtils.isEmpty(username)) {
                return;
            }
            for (Account account : accounts) {
                if (TextUtils.equals(account.name, username)) {
                    return;
                }
            }
            Preferences.setUsername(context, null);
        }, null, true);
    }
}
