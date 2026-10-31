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

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.core.net.toUri
import java.lang.ref.WeakReference

class CustomTabsDelegate {
  private var customTabsSession: CustomTabsSession? = null
  private var client: CustomTabsClient? = null
  private var connection: CustomTabsServiceConnection? = null

  /**
   * Binds the Activity to the Custom Tabs Service.
   *
   * @param activity the activity to be binded to the service.
   */
  fun bindCustomTabsService(activity: Activity) {
    if (client != null) {
      return
    }
    if (TextUtils.isEmpty(getPackageNameToUse(activity))) {
      return
    }
    connection = ServiceConnection(this)
    CustomTabsClient.bindCustomTabsService(
        activity,
        getPackageNameToUse(activity)!!,
        connection!!,
    )
  }

  /**
   * Unbinds the Activity from the Custom Tabs Service.
   *
   * @param activity the activity that is connected to the service.
   */
  fun unbindCustomTabsService(activity: Activity) {
    if (connection == null) {
      return
    }
    activity.unbindService(connection!!)
    client = null
    customTabsSession = null
    connection = null
  }

  /**
   * @return true if call to mayLaunchUrl was accepted.
   * @see CustomTabsSession.mayLaunchUrl
   */
  fun mayLaunchUrl(
      uri: Uri?,
      extras: Bundle?,
      otherLikelyBundles: MutableList<Bundle?>?,
  ): Boolean {
    if (client == null) {
      return false
    }
    val session = this.session
    return session != null && session.mayLaunchUrl(uri, extras, otherLikelyBundles)
  }

  val session: CustomTabsSession?
    /**
     * Creates or retrieves an exiting CustomTabsSession.
     *
     * @return a CustomTabsSession.
     */
    get() {
      if (client == null) {
        customTabsSession = null
      } else if (customTabsSession == null) {
        customTabsSession = client!!.newSession(null)
      }
      return customTabsSession
    }

  fun onServiceConnected(client: CustomTabsClient?) {
    this.client = client
    client!!.warmup(0L)
  }

  fun onServiceDisconnected() {
    client = null
    customTabsSession = null
  }

  internal class ServiceConnection(delegate: CustomTabsDelegate?) : CustomTabsServiceConnection() {
    private val delegate: WeakReference<CustomTabsDelegate?> =
        WeakReference<CustomTabsDelegate?>(delegate)

    override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
      val delegate = this.delegate.get()
      delegate?.onServiceConnected(client)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
      val delegate = this.delegate.get()
      delegate?.onServiceDisconnected()
    }
  }

  companion object {
    private const val ACTION_CUSTOM_TABS_CONNECTION =
        "android.support.customtabs.action.CustomTabsService"

    private fun getPackageNameToUse(context: Context): String? {
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

      return browsersWithCustomTabsSupport[0]
    }

    /**
     * Returns the package name of the default browser on the device
     *
     * @param context Context
     * @return The package name of the default browser
     */
    private fun getDefaultBrowser(context: Context): String? {
      val browserIntent = Intent(Intent.ACTION_VIEW, "http://example.com/".toUri())
      val pm = context.packageManager
      val resolveInfo =
          pm.resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)
              ?: // If no default browser could be found
              return null

      return resolveInfo.activityInfo.packageName
    }

    /**
     * Returns all browsers that support custom tabs
     *
     * @param context Context
     * @return List of all Packages with custom Tabs support
     */
    private fun getBrowsersWithCustomTabsSupport(context: Context): MutableList<String?> {
      val packagesSupportingCustomTabs: MutableList<String?> = ArrayList()
      val pm = context.packageManager
      val resolvedActivityList =
          pm.queryIntentActivities(
              Intent(Intent.ACTION_VIEW, "http://www.example.com".toUri()),
              0,
          )
      for (info in resolvedActivityList) {
        if (
            pm.resolveService(
                Intent()
                    .setAction(ACTION_CUSTOM_TABS_CONNECTION)
                    .setPackage(info.activityInfo.packageName),
                0,
            ) != null
        ) {
          packagesSupportingCustomTabs.add(info.activityInfo.packageName)
        }
      }
      return packagesSupportingCustomTabs
    }
  }
}
