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
import android.app.Activity
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.text.Spannable
import android.text.format.DateUtils
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.Window
import android.webkit.WebSettings
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.DimenRes
import androidx.annotation.StyleRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsSession
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.growse.android.io.github.hidroh.materialistic.Preferences.Theme.resolvePreferredTextSize
import com.growse.android.io.github.hidroh.materialistic.Preferences.customTabsEnabled
import com.growse.android.io.github.hidroh.materialistic.Preferences.getUsername
import com.growse.android.io.github.hidroh.materialistic.Preferences.setUsername
import com.growse.android.io.github.hidroh.materialistic.data.HackerNewsClient
import com.growse.android.io.github.hidroh.materialistic.data.Item
import com.growse.android.io.github.hidroh.materialistic.data.WebItem
import com.growse.android.io.github.hidroh.materialistic.widget.PopupMenu
import kotlin.math.abs

object AppUtils {
  private const val PLAY_STORE_URL = "market://details?id=" + BuildConfig.APPLICATION_ID
  const val HOT_THRESHOLD_HIGH: Int = 300
  const val HOT_THRESHOLD_NORMAL: Int = 100
  const val HOT_THRESHOLD_LOW: Int = 10
  const val HOT_FACTOR: Int = 3
  private const val HOST_ITEM = "item"
  private const val HOST_USER = "user"
  private val HN_WEB_HOST by lazy { HackerNewsClient.BASE_WEB_URL.toUri().host }

  @JvmStatic
  fun openWebUrlExternal(
      context: Context,
      item: WebItem?,
      url: String?,
      session: CustomTabsSession?,
  ) {
    if (!hasConnection(context)) {
      context.startActivity(
          Intent(context, OfflineWebActivity::class.java)
              .putExtra(OfflineWebActivity.EXTRA_URL, url)
      )
      return
    }
    val intent = createViewIntent(context, item, url, session)
    if (url?.toUri()?.host != HN_WEB_HOST) {
      // Don't gate on resolveActivity(): package visibility hides apps that only claim
      // specific hosts (e.g. YouTube for youtube.com), so it returns null even though
      // startActivity would happily launch them.
      try {
        context.startActivity(intent)
      } catch (_: ActivityNotFoundException) {}
      return
    }
    // HN links resolve to this app too, so offer every other handler instead
    val intents =
        context.packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .map { it.activityInfo.packageName }
            .filterNot { it.equals(context.packageName, ignoreCase = true) }
            .map { createViewIntent(context, item, url, session).setPackage(it) }
    when (intents.size) {
      0 -> return
      1 -> context.startActivity(intents[0])
      else ->
          context.startActivity(
              Intent.createChooser(intents[0], context.getString(R.string.chooser_title))
                  .putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.drop(1).toTypedArray())
          )
    }
  }

  @JvmStatic
  @SuppressLint("ClickableViewAccessibility")
  fun setTextWithLinks(textView: TextView, html: CharSequence?) {
    textView.text = html
    // TODO https://code.google.com/p/android/issues/detail?id=191430
    textView.setOnTouchListener(
        object : View.OnTouchListener {
          private var downSpan: ClickableSpan? = null
          private var downX = 0f
          private var downY = 0f

          override fun onTouch(v: View, event: MotionEvent): Boolean {
            val action = event.action
            if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_UP) {
              return false
            }
            val widget = v as TextView
            var span = findSpanAt(widget, event.x, event.y)
            if (action == MotionEvent.ACTION_DOWN) {
              downSpan = span
              downX = event.x
              downY = event.y
              if (span != null) {
                // otherwise the enclosing RecyclerView can steal the gesture as a scroll
                // on the tiny finger drift a real tap always has, turning the up event
                // into a CANCEL before it ever reaches the check below
                widget.parent?.requestDisallowInterceptTouchEvent(true)
              }
              return span != null
            }
            // A real finger practically never releases on the exact pixel it pressed, so a
            // release that slips off a one-line-tall link's span would otherwise be dropped
            // silently - fall back to the span under the initial press within touch slop.
            if (span == null && isWithinTouchSlop(widget.context, downX, downY, event.x, event.y)) {
              span = downSpan
            }
            downSpan = null
            when (span) {
              null -> return false
              is URLSpan -> openWebUrlExternal(widget.context, null, span.url, null)
              else -> span.onClick(widget)
            }
            return true
          }
        }
    )
  }

  private fun findSpanAt(widget: TextView, rawX: Float, rawY: Float): ClickableSpan? {
    val layout = widget.layout ?: return null
    val x = rawX.toInt() - widget.totalPaddingLeft + widget.scrollX
    val y = rawY.toInt() - widget.totalPaddingTop + widget.scrollY
    val offset = layout.getOffsetForHorizontal(layout.getLineForVertical(y), x.toFloat())
    return Spannable.Factory.getInstance()
        .newSpannable(widget.text)
        .getSpans(offset, offset, ClickableSpan::class.java)
        .firstOrNull()
  }

  private fun isWithinTouchSlop(context: Context, x1: Float, y1: Float, x2: Float, y2: Float) =
      ViewConfiguration.get(context).scaledTouchSlop.let {
        abs(x1 - x2) <= it && abs(y1 - y2) <= it
      }

  @JvmStatic
  @JvmOverloads
  fun fromHtml(htmlText: String?, compact: Boolean = false): CharSequence? {
    if (htmlText.isNullOrEmpty()) {
      return null
    }
    val mode = if (compact) HtmlCompat.FROM_HTML_MODE_COMPACT else HtmlCompat.FROM_HTML_MODE_LEGACY
    return HtmlCompat.fromHtml(htmlText, mode).trimEnd()
  }

  @JvmStatic
  fun openExternal(
      context: Context,
      popupMenu: PopupMenu,
      anchor: View,
      item: WebItem,
      session: CustomTabsSession?,
  ) {
    withChosenUrl(context, popupMenu, anchor, item) { url ->
      openWebUrlExternal(context, item, url, session)
    }
  }

  @JvmStatic
  fun share(context: Context, popupMenu: PopupMenu, anchor: View, item: WebItem) {
    withChosenUrl(context, popupMenu, anchor, item) { url ->
      share(context, item.displayedTitle, url)
    }
  }

  /**
   * Calls [action] with the HN discussion URL for self posts, otherwise asks via a popup menu
   * whether to use the article or the discussion URL.
   */
  private fun withChosenUrl(
      context: Context,
      popupMenu: PopupMenu,
      anchor: View,
      item: WebItem,
      action: (String?) -> Unit,
  ) {
    val commentsUrl = String.format(HackerNewsClient.WEB_ITEM_PATH, item.id)
    val articleUrl = item.url
    if (articleUrl.isNullOrEmpty() || articleUrl.startsWith(HackerNewsClient.BASE_WEB_URL)) {
      action(commentsUrl)
      return
    }
    popupMenu
        .create(context, anchor, GravityCompat.END)
        .inflate(R.menu.menu_share)
        .setOnMenuItemClickListener { menuItem ->
          action(if (menuItem.itemId == R.id.menu_article) articleUrl else commentsUrl)
          true
        }
        .show()
  }

  @JvmStatic
  fun getThemedResId(context: Context, @AttrRes attr: Int): Int =
      context.theme.obtainStyledAttributes(intArrayOf(attr)).use { it.getResourceId(0, 0) }

  @JvmStatic
  fun getDimension(context: Context, @StyleRes styleResId: Int, @AttrRes attr: Int): Float =
      context.theme.obtainStyledAttributes(styleResId, intArrayOf(attr)).use {
        it.getDimension(0, 0f)
      }

  @JvmStatic
  fun getDimensionInDp(context: Context, @DimenRes dimenResId: Int): Int =
      (context.resources.getDimension(dimenResId) / context.resources.displayMetrics.density)
          .toInt()

  @JvmStatic
  fun getAbbreviatedTimeSpan(timeMillis: Long): String {
    val span = (System.currentTimeMillis() - timeMillis).coerceAtLeast(0)
    return when {
      span >= DateUtils.YEAR_IN_MILLIS -> "${span / DateUtils.YEAR_IN_MILLIS}y"
      span >= DateUtils.WEEK_IN_MILLIS -> "${span / DateUtils.WEEK_IN_MILLIS}w"
      span >= DateUtils.DAY_IN_MILLIS -> "${span / DateUtils.DAY_IN_MILLIS}d"
      span >= DateUtils.HOUR_IN_MILLIS -> "${span / DateUtils.HOUR_IN_MILLIS}h"
      else -> "${span / DateUtils.MINUTE_IN_MILLIS}m"
    }
  }

  @JvmStatic
  @Suppress("DEPRECATION")
  fun hasConnection(context: Context): Boolean =
      (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
          .activeNetworkInfo
          ?.isConnectedOrConnecting == true

  /**
   * Displays UI to allow user to login If no accounts exist in user's device, regardless of login
   * status, prompt to login again If 1 or more accounts in user's device, and already logged in,
   * prompt to update password If 1 or more accounts in user's device, and logged out, show account
   * chooser
   *
   * @param context activity context
   * @param alertDialogBuilder dialog builder
   */
  @JvmStatic
  @SuppressLint("MissingPermission")
  fun showLogin(context: Context, alertDialogBuilder: AlertDialogBuilder<*>) {
    val accounts = AccountManager.get(context).getAccountsByType(BuildConfig.APPLICATION_ID)
    if (accounts.isEmpty() || getUsername(context).isNotEmpty()) {
      // no accounts or stale account, ask to login or re-login
      context.startActivity(Intent(context, LoginActivity::class.java))
    } else {
      // logged out, choose from existing accounts to log in
      showAccountChooser(context, alertDialogBuilder, accounts)
    }
  }

  @JvmStatic
  @Suppress("DEPRECATION")
  fun openPlayStore(context: Context) {
    val intent =
        Intent(Intent.ACTION_VIEW, PLAY_STORE_URL.toUri())
            .addFlags(
                Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT
            )
    try {
      context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
      Toast.makeText(context, R.string.no_playstore, Toast.LENGTH_SHORT).show()
    }
  }

  @JvmStatic
  @SuppressLint("MissingPermission")
  fun showAccountChooser(
      context: Context,
      alertDialogBuilder: AlertDialogBuilder<*>,
      accounts: Array<Account>,
  ) {
    val names = accounts.map { it.name }
    val checked = names.indexOf(getUsername(context))
    val clickListener =
        object : DialogInterface.OnClickListener {
          private var selection = checked

          override fun onClick(dialog: DialogInterface, which: Int) {
            when (which) {
              DialogInterface.BUTTON_POSITIVE -> {
                if (selection >= 0) {
                  val name = names[selection]
                  setUsername(context, name)
                  Toast.makeText(
                          context,
                          context.getString(R.string.welcome, name),
                          Toast.LENGTH_SHORT,
                      )
                      .show()
                }
                dialog.dismiss()
              }
              DialogInterface.BUTTON_NEGATIVE -> {
                context.startActivity(
                    Intent(context, LoginActivity::class.java)
                        .putExtra(LoginActivity.EXTRA_ADD_ACCOUNT, true)
                )
                dialog.dismiss()
              }
              DialogInterface.BUTTON_NEUTRAL -> {
                if (selection >= 0) {
                  AccountManager.get(context).removeAccount(accounts[selection], null, null, null)
                  dialog.dismiss()
                }
              }
              else -> selection = which
            }
          }
        }
    alertDialogBuilder
        .init(context)
        .setTitle(R.string.choose_account)
        .setSingleChoiceItems(names.toTypedArray<CharSequence?>(), checked, clickListener)
        .setPositiveButton(android.R.string.ok, clickListener)
        .setNegativeButton(R.string.add_account, clickListener)
        .setNeutralButton(R.string.remove_account, clickListener)
        .show()
  }

  @JvmStatic
  fun toggleFab(fab: FloatingActionButton, visible: Boolean) {
    if (visible) {
      fab.tag = null
      fab.show()
    } else {
      fab.tag = FabAwareScrollBehavior.HIDDEN
      fab.hide()
    }
  }

  @JvmStatic
  fun toggleFabAction(fab: FloatingActionButton, item: WebItem, commentMode: Boolean) {
    val context = fab.context
    fab.setImageResource(
        if (commentMode) R.drawable.ic_reply_white_24dp else R.drawable.ic_zoom_out_map_white_24dp
    )
    fab.setOnClickListener {
      if (commentMode) {
        context.startActivity(
            Intent(context, ComposeActivity::class.java)
                .putExtra(ComposeActivity.EXTRA_PARENT_ID, item.id)
                .putExtra(ComposeActivity.EXTRA_PARENT_TEXT, (item as? Item)?.text)
        )
      } else {
        LocalBroadcastManager.getInstance(context)
            .sendBroadcast(
                Intent(WebFragment.ACTION_FULLSCREEN).putExtra(WebFragment.EXTRA_FULLSCREEN, true)
            )
      }
    }
  }

  @JvmStatic
  fun toHtmlColor(context: Context, @AttrRes colorAttr: Int): String =
      "%06X"
          .format(0xFFFFFF and ContextCompat.getColor(context, getThemedResId(context, colorAttr)))

  @JvmStatic
  fun toggleWebViewZoom(webSettings: WebSettings, enabled: Boolean) {
    webSettings.setSupportZoom(enabled)
    webSettings.builtInZoomControls = enabled
    webSettings.displayZoomControls = false
  }

  /**
   * Pads a scrolling AppBarLayout's first child down below the status bar without setting
   * `fitsSystemWindows` on it or its AppBarLayout. That flag makes AppBarLayout reserve the status
   * bar's height as space it will never scroll away, leaving a colored sliver stuck under the
   * status bar when the bar is supposed to fully hide.
   */
  @JvmStatic
  fun applyToolbarTopInset(toolbar: View) {
    val initialPaddingTop = toolbar.paddingTop
    ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, windowInsets ->
      val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
      v.setPadding(
          v.paddingLeft,
          initialPaddingTop + systemBars.top,
          v.paddingRight,
          v.paddingBottom,
      )
      windowInsets
    }
    ViewCompat.requestApplyInsets(toolbar)
  }

  @JvmStatic
  fun createLayoutInflater(context: Context): LayoutInflater =
      LayoutInflater.from(ContextThemeWrapper(context, resolvePreferredTextSize(context)))

  @JvmStatic
  fun share(context: Context, subject: String?, text: String?) {
    val intent =
        Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, subject)
            .putExtra(Intent.EXTRA_TEXT, if (subject.isNullOrEmpty()) text else "$subject - $text")
    if (intent.resolveActivity(context.packageManager) != null) {
      context.startActivity(intent)
    }
  }

  @JvmStatic fun createItemUri(itemId: String): Uri = createAppUri(HOST_ITEM, itemId)

  @JvmStatic fun createUserUri(userId: String): Uri = createAppUri(HOST_USER, userId)

  private fun createAppUri(host: String, path: String): Uri =
      Uri.Builder().scheme(BuildConfig.APPLICATION_ID).authority(host).path(path).build()

  @JvmStatic
  fun getDataUriId(intent: Intent, altParamId: String?): String? {
    val data = intent.data ?: return null
    return if (data.scheme == BuildConfig.APPLICATION_ID) {
      data.lastPathSegment
    } else { // web URI
      data.getQueryParameter(altParamId)
    }
  }

  private fun createViewIntent(
      context: Context,
      item: WebItem?,
      url: String?,
      session: CustomTabsSession?,
  ): Intent {
    val uri = url?.toUri()
    if (!customTabsEnabled(context)) {
      return Intent(Intent.ACTION_VIEW, uri)
    }
    val builder =
        CustomTabsIntent.Builder(session)
            .setToolbarColor(
                ContextCompat.getColor(context, getThemedResId(context, R.attr.colorPrimary))
            )
            .setShowTitle(true)
            .enableUrlBarHiding()
            .addDefaultShareMenuItem()
    if (item != null) {
      builder.addMenuItem(
          context.getString(R.string.comments),
          PendingIntent.getActivity(
              context,
              0,
              Intent(context, ItemActivity::class.java)
                  .putExtra(ItemActivity.EXTRA_ITEM, item)
                  .putExtra(ItemActivity.EXTRA_OPEN_COMMENTS, true),
              PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
          ),
      )
    }
    return builder.build().intent.setData(uri)
  }

  @JvmStatic
  @SuppressLint("InlinedApi")
  fun multiWindowIntent(activity: Activity, intent: Intent): Intent {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.isInMultiWindowMode) {
      intent.addFlags(
          Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or
              Intent.FLAG_ACTIVITY_NEW_TASK or
              Intent.FLAG_ACTIVITY_MULTIPLE_TASK
      )
    }
    return intent
  }

  /** Compares two URLs, ignoring a trailing slash. Empty or null URLs never match. */
  @JvmStatic
  fun urlEquals(thisUrl: String?, thatUrl: String?): Boolean =
      !thisUrl.isNullOrEmpty() &&
          !thatUrl.isNullOrEmpty() &&
          thisUrl.removeSuffix("/") == thatUrl.removeSuffix("/")

  internal class SystemUiHelper(private val window: Window) {
    @Suppress("DEPRECATION") private val originalUiFlags = window.decorView.systemUiVisibility
    private var enabled = true

    @Suppress("DEPRECATION")
    fun setFullscreen(fullscreen: Boolean) {
      if (!enabled) {
        return
      }
      window.decorView.systemUiVisibility =
          if (fullscreen) {
            originalUiFlags or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
          } else {
            originalUiFlags
          }
    }

    fun setEnabled(enabled: Boolean) {
      this.enabled = enabled
    }
  }
}
