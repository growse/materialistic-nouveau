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

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle

import androidx.annotation.Nullable
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession

import android.text.TextUtils

import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.List

import io.github.hidroh.materialistic.annotation.Synthetic


open class CustomTabsDelegate {
    private static final String ACTION_CUSTOM_TABS_CONNECTION =
            "android.support.customtabs.action.CustomTabsService"
    private var mCustomTabsSession: CustomTabsSession? = null
    private var mClient: CustomTabsClient? = null
    private var mConnection: CustomTabsServiceConnection? = null

    /**
     * Binds the Activity to the Custom Tabs Service.
     *
     * @param activity the activity to be binded to the service.
     */
    fun bindCustomTabsService(activity: Activity) {
        if (mClient != null) {
            return
        }
        if (TextUtils.isEmpty(getPackageNameToUse(activity))) {
            return
        }
        mConnection = ServiceConnection(this)
        CustomTabsClient.bindCustomTabsService(activity, getPackageNameToUse(activity), mConnection)
    }

    /**
     * Unbinds the Activity from the Custom Tabs Service.
     *
     * @param activity the activity that is connected to the service.
     */
    fun unbindCustomTabsService(activity: Activity) {
        if (mConnection == null) {
            return
        }
        activity.unbindService(mConnection)
        mClient = null
        mCustomTabsSession = null
        mConnection = null
    }

    /**
     * @return true if call to mayLaunchUrl was accepted.
     * @see CustomTabsSession#mayLaunchUrl(Uri, Bundle, List)
     */
    fun mayLaunchUrl(uri: Uri, extras: Bundle, otherLikelyBundles: List<Bundle>): Boolean {
        if (mClient == null) {
            return false
        }
        val session = getSession()
        return session != null && session.mayLaunchUrl(uri, extras, otherLikelyBundles)
    }

    /**
     * Creates or retrieves an exiting CustomTabsSession.
     *
     * @return a CustomTabsSession.
     */
    fun getSession(): CustomTabsSession {
        if (mClient == null) {
            mCustomTabsSession = null
        } else if (mCustomTabsSession == null) {
            mCustomTabsSession = mClient.newSession(null)
        }
        return mCustomTabsSession
    }

    @Synthetic
    fun onServiceConnected(client: CustomTabsClient) {
        mClient = client
        mClient.warmup(0L)
    }

    @Synthetic
    fun onServiceDisconnected() {
        mClient = null
        mCustomTabsSession = null
    }

    @Nullable
    private fun getPackageNameToUse(context: Context): String {
        val browsersWithCustomTabsSupport = getBrowsersWithCustomTabsSupport(context)
        val defaultBrowser = getDefaultBrowser(context)

        for (browser in browsersWithCustomTabsSupport) {
            if (TextUtils.equals(browser, defaultBrowser)) {
                return browser
            }
        }

        if (browsersWithCustomTabsSupport.isEmpty()) { // If no supported browser were found
            return null
        }

        return browsersWithCustomTabsSupport.get(0)
    }

    /**
     * Returns the package name of the default browser on the device
     *
     * @param context Context
     * @return The package name of the default browser
     */
    @Nullable
    private fun getDefaultBrowser(context: Context): String {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://example.com/"))
        val pm = context.getPackageManager()
        val resolveInfo = pm.resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)

        if (resolveInfo == null) { // If no default browser could be found
            return null
        }

        return resolveInfo.activityInfo.packageName
    }

    /**
     * Returns all browsers that support custom tabs
     *
     * @param context Context
     * @return List of all Packages with custom Tabs support
     */
    private fun getBrowsersWithCustomTabsSupport(context: Context): List<String> {
        val packagesSupportingCustomTabs = ArrayList<>()
        val pm = context.getPackageManager()
        val resolvedActivityList = pm.queryIntentActivities(
                Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com")), 0)
        //noinspection Convert2streamapi
        for (info in resolvedActivityList) {
            if (pm.resolveService(Intent()
                    .setAction(ACTION_CUSTOM_TABS_CONNECTION)
                    .setPackage(info.activityInfo.packageName), 0) != null) {
                packagesSupportingCustomTabs.add(info.activityInfo.packageName)
            }
        }
        return packagesSupportingCustomTabs
    }

    @Synthetic
    open class ServiceConnection : CustomTabsServiceConnection() {
        private var mDelegate: WeakReference<CustomTabsDelegate>? = null

        @Synthetic
        constructor(delegate: CustomTabsDelegate) {
            mDelegate = WeakReference<>(delegate)
        }

        override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
            val delegate = mDelegate.get()
            if (delegate != null) {
                delegate.onServiceConnected(client)
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            val delegate = mDelegate.get()
            if (delegate != null) {
                delegate.onServiceDisconnected()
            }
        }
    }
}
