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

package io.github.hidroh.materialistic

import android.accounts.Account
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.Point
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.text.Html
import android.text.Layout
import android.text.Spannable
import android.text.TextUtils
import android.text.format.DateUtils
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.ContextThemeWrapper
import android.view.Display
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.webkit.WebSettings
import android.widget.TextView
import android.widget.Toast

import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton

import java.util.ArrayList
import java.util.List

import androidx.annotation.AttrRes
import androidx.annotation.DimenRes
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StyleRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsSession
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.core.view.GravityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.github.hidroh.materialistic.annotation.PublicApi
import io.github.hidroh.materialistic.data.HackerNewsClient
import io.github.hidroh.materialistic.data.Item
import io.github.hidroh.materialistic.data.WebItem
import io.github.hidroh.materialistic.widget.PopupMenu

@SuppressWarnings("WeakerAccess")
@PublicApi
open class AppUtils {
    private const val ABBR_YEAR: String = "y"
    private const val ABBR_WEEK: String = "w"
    private const val ABBR_DAY: String = "d"
    private const val ABBR_HOUR: String = "h"
    private const val ABBR_MINUTE: String = "m"
    private const val PLAY_STORE_URL: String = "market://details?id=" + BuildConfig.APPLICATION_ID
    private const val FORMAT_HTML_COLOR: String = "%06X"
    const val HOT_THRESHOLD_HIGH: Int = 300
    const val HOT_THRESHOLD_NORMAL: Int = 100
    const val HOT_THRESHOLD_LOW: Int = 10
    const val HOT_FACTOR: Int = 3
    private const val HOST_ITEM: String = "item"
    private const val HOST_USER: String = "user"

    fun openWebUrlExternal(context: Context, item: WebItem, url: String, session: CustomTabsSession) {
        if (!hasConnection(context)) {
            context.startActivity(Intent(context, OfflineWebActivity::class.java)
                    .putExtra(OfflineWebActivity.EXTRA_URL, url))
            return
        }
        Intent intent = createViewIntent(context, item, url, session)
        if (!HackerNewsClient.BASE_WEB_URL.contains(Uri.parse(url).getHost())) {
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent)
            }
            return
        }
        List<ResolveInfo> activities = context.getPackageManager()
                .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        ArrayList<Intent> intents = ArrayList<>()
        for (info in activities) {
            if (info.activityInfo.packageName.equalsIgnoreCase(context.getPackageName())) {
                continue
            }
            intents.add(createViewIntent(context, item, url, session)
                    .setPackage(info.activityInfo.packageName))
        }
        if (intents.isEmpty()) {
            return
        }
        if (intents.size() == 1) {
            context.startActivity(intents.remove(0))
        } else {
            context.startActivity(Intent.createChooser(intents.remove(0),
                    context.getString(R.string.chooser_title))
                    .putExtra(Intent.EXTRA_INITIAL_INTENTS,
                            intents.toArray(Parcelable[intents.size()])))
        }
    }

    fun setTextWithLinks(textView: TextView, html: CharSequence) {
        textView.setText(html)
        // TODO https://code.google.com/p/android/issues/detail?id=191430
        //noinspection Convert2Lambda
        textView.setOnTouchListener(View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction()
                if (action == MotionEvent.ACTION_UP ||
                        action == MotionEvent.ACTION_DOWN) {
                    int x = (int) event.getX()
                    int y = (int) event.getY()

                    TextView widget = (TextView) v
                    x -= widget.getTotalPaddingLeft()
                    y -= widget.getTotalPaddingTop()

                    x += widget.getScrollX()
                    y += widget.getScrollY()

                    Layout layout = widget.getLayout()
                    int line = layout.getLineForVertical(y)
                    int off = layout.getOffsetForHorizontal(line, x)

                    ClickableSpan[] links = Spannable.Factory.getInstance()
                            .newSpannable(widget.getText())
                            .getSpans(off, off, ClickableSpan::class.java)

                    if (links.length != 0) {
                        if (action == MotionEvent.ACTION_UP) {
                            if (links[0] is URLSpan) {
                                openWebUrlExternal(widget.getContext(), null,
                                        ((URLSpan) links[0]).getURL(), null)
                            } else {
                                links[0].onClick(widget)
                            }
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    fun fromHtml(htmlText: String): CharSequence {
        return fromHtml(htmlText, false)
    }

    fun fromHtml(htmlText: String, compact: Boolean): CharSequence {
        if (TextUtils.isEmpty(htmlText)) {
            return null
        }
        CharSequence spanned
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //noinspection InlinedApi
            spanned = Html.fromHtml(htmlText, compact ?
                    Html.FROM_HTML_MODE_COMPACT : Html.FROM_HTML_MODE_LEGACY)
        } else {
            //noinspection deprecation
            spanned = Html.fromHtml(htmlText)
        }
        return trim(spanned)
    }

    fun makeSendIntentChooser(context: Context, data: Uri): Intent {
        // use ACTION_SEND_MULTIPLE instead of ACTION_SEND to filter out
        // share receivers that accept only EXTRA_TEXT but not EXTRA_STREAM
        return Intent.createChooser(Intent(Intent.ACTION_SEND_MULTIPLE)
                        .setType("text/plain")
                        .putParcelableArrayListExtra(Intent.EXTRA_STREAM,
                                ArrayList<Uri>(){{add(data);}}),
                context.getString(R.string.share_file))
    }

    fun openExternal(context: Context, popupMenu: PopupMenu, anchor: View, item: WebItem, session: CustomTabsSession) {
        if (TextUtils.isEmpty(item.getUrl()) ||
                item.getUrl().startsWith(HackerNewsClient.BASE_WEB_URL)) {
            openWebUrlExternal(context,
                    item, String.format(HackerNewsClient.WEB_ITEM_PATH, item.getId()),
                    session)
            return
        }
        popupMenu.create(context, anchor, GravityCompat.END)
                .inflate(R.menu.menu_share)
                .setOnMenuItemClickListener(menuItem -> {
                    openWebUrlExternal(context, item, menuItem.getItemId() == R.id.menu_article ?
                            item.getUrl() :
                            String.format(HackerNewsClient.WEB_ITEM_PATH, item.getId()), session)
                    return true
                })
                .show()
    }

    fun share(context: Context, popupMenu: PopupMenu, anchor: View, item: WebItem) {
        if (TextUtils.isEmpty(item.getUrl()) ||
                item.getUrl().startsWith(HackerNewsClient.BASE_WEB_URL)) {
            share(context, item.getDisplayedTitle(),
                    String.format(HackerNewsClient.WEB_ITEM_PATH, item.getId()))
            return
        }
        popupMenu.create(context, anchor, GravityCompat.END)
                .inflate(R.menu.menu_share)
                .setOnMenuItemClickListener(menuItem -> {
                    share(context, item.getDisplayedTitle(),
                            menuItem.getItemId() == R.id.menu_article ?
                                    item.getUrl() :
                                    String.format(HackerNewsClient.WEB_ITEM_PATH, item.getId()))
                    return true
                })
                .show()
    }

    fun getThemedResId(context: Context, attr: Int): Int {
        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{attr})
        final int resId = a.getResourceId(0, 0)
        a.recycle()
        return resId
    }

    fun getDimension(context: Context, styleResId: Int, attr: Int): Float {
        TypedArray a = context.getTheme().obtainStyledAttributes(styleResId, new int[]{attr})
        float size = a.getDimension(0, 0)
        a.recycle()
        return size
    }

    fun isHackerNewsUrl(item: WebItem): Boolean {
        return !TextUtils.isEmpty(item.getUrl()) &&
                item.getUrl().equals(String.format(HackerNewsClient.WEB_ITEM_PATH, item.getId()))
    }

    fun getDimensionInDp(context: Context, dimenResId: Int): Int {
        return (int) (context.getResources().getDimension(dimenResId) /
                        context.getResources().getDisplayMetrics().density)
    }

    fun restart(activity: Activity, transition: Boolean) {
        activity.recreate()
    }

    fun getAbbreviatedTimeSpan(timeMillis: Long): String {
        long span = Math.max(System.currentTimeMillis() - timeMillis, 0)
        if (span >= DateUtils.YEAR_IN_MILLIS) {
            return (span / DateUtils.YEAR_IN_MILLIS) + ABBR_YEAR
        }
        if (span >= DateUtils.WEEK_IN_MILLIS) {
            return (span / DateUtils.WEEK_IN_MILLIS) + ABBR_WEEK
        }
        if (span >= DateUtils.DAY_IN_MILLIS) {
            return (span / DateUtils.DAY_IN_MILLIS) + ABBR_DAY
        }
        if (span >= DateUtils.HOUR_IN_MILLIS) {
            return (span / DateUtils.HOUR_IN_MILLIS) + ABBR_HOUR
        }
        return (span / DateUtils.MINUTE_IN_MILLIS) + ABBR_MINUTE
    }

    fun isOnWiFi(context: Context): Boolean {
        NetworkInfo activeNetwork = ((ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo()
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting() &&
                activeNetwork.getType() == ConnectivityManager.TYPE_WIFI
    }

    fun hasConnection(context: Context): Boolean {
        NetworkInfo activeNetworkInfo = ((ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo()
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting()
    }

    @SuppressLint("MissingPermission")
    fun getCredentials(context: Context): Pair<String, String> {
        String username = Preferences.getUsername(context)
        if (TextUtils.isEmpty(username)) {
            return null
        }
        AccountManager accountManager = AccountManager.get(context)
        Account[] accounts = accountManager.getAccountsByType(BuildConfig.APPLICATION_ID)
        for (account in accounts) {
            if (TextUtils.equals(username, account.name)) {
                return Pair.create(username, accountManager.getPassword(account))
            }
        }
        return null
    }

    /**
     * Displays UI to allow user to login
     * If no accounts exist in user's device, regardless of login status, prompt to login again
     * If 1 or more accounts in user's device, and already logged in, prompt to update password
     * If 1 or more accounts in user's device, and logged out, show account chooser
     * @param context activity context
     * @param alertDialogBuilder dialog builder
     */
    @SuppressLint("MissingPermission")
    fun showLogin(context: Context, alertDialogBuilder: AlertDialogBuilder) {
        Account[] accounts = AccountManager.get(context).getAccountsByType(BuildConfig.APPLICATION_ID)
        if (accounts.length == 0) { // no accounts, ask to login or re-login
            context.startActivity(Intent(context, LoginActivity::class.java))
        } else if (!TextUtils.isEmpty(Preferences.getUsername(context))) { // stale account, ask to re-login
            context.startActivity(Intent(context, LoginActivity::class.java))
        } else { // logged out, choose from existing accounts to log in
            showAccountChooser(context, alertDialogBuilder, accounts)
        }
    }

    @SuppressLint("MissingPermission")
    fun registerAccountsUpdatedListener(context: Context) {
        AccountManager.get(context).addOnAccountsUpdatedListener(accounts -> {
            String username = Preferences.getUsername(context)
            if (TextUtils.isEmpty(username)) {
                return
            }
            for (account in accounts) {
                if (TextUtils.equals(account.name, username)) {
                    return
                }
            }
            Preferences.setUsername(context, null)
        }, null, true)
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun openPlayStore(context: Context) {
        Intent intent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL))
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK |
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        try {
            context.startActivity(intent)
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.no_playstore, Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    fun showAccountChooser(context: Context, alertDialogBuilder: AlertDialogBuilder, accounts: Array<Account>) {
        String username = Preferences.getUsername(context)
        final String[] items = String[accounts.length]
        int checked = -1
        for (int i = 0; i < accounts.length; i++) {
            String accountName = accounts[i].name
            items[i] = accountName
            if (TextUtils.equals(accountName, username)) {
                checked = i
            }
        }
        int initialSelection = checked
        DialogInterface.OnClickListener clickListener = DialogInterface.OnClickListener() {
            private int selection = initialSelection

            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        Preferences.setUsername(context, items[selection])
                        Toast.makeText(context,
                                context.getString(R.string.welcome, items[selection]),
                                Toast.LENGTH_SHORT)
                                .show()
                        dialog.dismiss()
                        break
                    case DialogInterface.BUTTON_NEGATIVE:
                        Intent intent = Intent(context, LoginActivity::class.java)
                        intent.putExtra(LoginActivity.EXTRA_ADD_ACCOUNT, true)
                        context.startActivity(intent)
                        dialog.dismiss()
                        break
                    case DialogInterface.BUTTON_NEUTRAL:
                        if (selection < 0) {
                            break
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                            AccountManager.get(context).removeAccount(accounts[selection], null, null, null)
                        } else {
                            //noinspection deprecation
                            AccountManager.get(context).removeAccount(accounts[selection], null, null)
                        }
                        dialog.dismiss()
                        break
                    default:
                        selection = which
                        break
                }
            }
        }
        alertDialogBuilder
                .init(context)
                .setTitle(R.string.choose_account)
                .setSingleChoiceItems(items, checked, clickListener)
                .setPositiveButton(android.R.string.ok, clickListener)
                .setNegativeButton(R.string.add_account, clickListener)
                .setNeutralButton(R.string.remove_account, clickListener)
                .show()
    }

    fun toggleFab(fab: FloatingActionButton, visible: Boolean) {
        if (visible) {
            fab.setTag(null)
            fab.show()
        } else {
            fab.setTag(FabAwareScrollBehavior.HIDDEN)
            fab.hide()
        }
    }

    fun toggleFabAction(fab: FloatingActionButton, item: WebItem, commentMode: Boolean) {
        Context context = fab.getContext()
        fab.setImageResource(commentMode ? R.drawable.ic_reply_white_24dp : R.drawable.ic_zoom_out_map_white_24dp)
        fab.setOnClickListener(v -> {
            if (commentMode) {
                context.startActivity(Intent(context, ComposeActivity::class.java)
                        .putExtra(ComposeActivity.EXTRA_PARENT_ID, item.getId())
                        .putExtra(ComposeActivity.EXTRA_PARENT_TEXT,
                                item is Item ? ((Item) item).getText() : null))
            } else {
                LocalBroadcastManager.getInstance(context)
                        .sendBroadcast(Intent(WebFragment.ACTION_FULLSCREEN)
                                .putExtra(WebFragment.EXTRA_FULLSCREEN, true))
            }
        })
    }

    fun toHtmlColor(context: Context, colorAttr: Int): String {
        return String.format(FORMAT_HTML_COLOR, 0xFFFFFF & ContextCompat.getColor(context,
                AppUtils.getThemedResId(context, colorAttr)))
    }

    fun toggleWebViewZoom(webSettings: WebSettings, enabled: Boolean) {
        webSettings.setSupportZoom(enabled)
        webSettings.setBuiltInZoomControls(enabled)
        webSettings.setDisplayZoomControls(false)
    }

    fun setStatusBarDim(window: Window, dim: Boolean) {
        setStatusBarColor(window, dim ? Color.TRANSPARENT :
                ContextCompat.getColor(window.getContext(),
                        AppUtils.getThemedResId(window.getContext(), R.attr.colorPrimaryDark)))
    }

    fun setStatusBarColor(window: Window, color: Int) {
        window.setStatusBarColor(color)
    }

    fun navigate(direction: Int, appBarLayout: AppBarLayout, navigable: Navigable) {
        switch (direction) {
            case Navigable.DIRECTION_DOWN:
            case Navigable.DIRECTION_RIGHT:
                if (appBarLayout.getBottom() == 0) {
                    navigable.onNavigate(direction)
                } else {
                    appBarLayout.setExpanded(false, true)
                }
                break
            default:
                navigable.onNavigate(direction)
                break
        }
    }

    fun getDisplayHeight(context: Context): Int {
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay()
        Point point = Point()
        display.getSize(point)
        return point.y
    }

    fun createLayoutInflater(context: Context): LayoutInflater {
        return LayoutInflater.from(ContextThemeWrapper(context,
                Preferences.Theme.resolvePreferredTextSize(context)))
    }

    fun share(context: Context, subject: String, text: String) {
        Intent intent = Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, subject)
                .putExtra(Intent.EXTRA_TEXT, !TextUtils.isEmpty(subject) ?
                        TextUtils.join(" - ", String[]{subject, text}) : text)
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent)
        }
    }
    fun createItemUri(itemId: String): Uri {
        return Uri.Builder()
                .scheme(BuildConfig.APPLICATION_ID)
                .authority(HOST_ITEM)
                .path(itemId)
                .build()
    }

    fun createUserUri(userId: String): Uri {
        return Uri.Builder()
                .scheme(BuildConfig.APPLICATION_ID)
                .authority(HOST_USER)
                .path(userId)
                .build()
    }

    fun getDataUriId(intent: Intent, altParamId: String): String {
        if (intent.getData() == null) {
            return null
        }
        if (TextUtils.equals(intent.getData().getScheme(), BuildConfig.APPLICATION_ID)) {
            return intent.getData().getLastPathSegment()
        } else { // web URI
            return intent.getData().getQueryParameter(altParamId)
        }
    }

    fun wrapHtml(context: Context, html: String): String {
        return context.getString(R.string.html,
                Preferences.Theme.getReadabilityTypeface(context),
                toHtmlPx(context, Preferences.Theme.resolvePreferredReadabilityTextSize(context)),
                AppUtils.toHtmlColor(context, android.R.attr.textColorPrimary),
                AppUtils.toHtmlColor(context, android.R.attr.textColorLink),
                TextUtils.isEmpty(html) ? context.getString(R.string.empty_text) : html,
                toHtmlPx(context, context.getResources().getDimension(R.dimen.activity_vertical_margin)),
                toHtmlPx(context, context.getResources().getDimension(R.dimen.activity_horizontal_margin)),
                Preferences.getReadabilityLineHeight(context))
    }

    private fun toHtmlPx(context: Context, textStyleAttr: Int): Float {
        return toHtmlPx(context, AppUtils.getDimension(context, textStyleAttr, R.attr.contentTextSize))
    }

    private fun toHtmlPx(context: Context, dimen: Float): Float {
        return dimen / context.getResources().getDisplayMetrics().density
    }

    private fun trim(charSequence: CharSequence): CharSequence {
        if (TextUtils.isEmpty(charSequence)) {
            return charSequence
        }
        int end = charSequence.length() - 1
        while (Character.isWhitespace(charSequence.charAt(end))) {
            end--
        }
        return charSequence.subSequence(0, end + 1)
    }

    @NonNull
    private fun createViewIntent(context: Context, item: WebItem, url: String, session: CustomTabsSession): Intent {
        if (Preferences.customTabsEnabled(context)) {
            CustomTabsIntent.Builder builder = CustomTabsIntent.Builder(session)
                    .setToolbarColor(ContextCompat.getColor(context,
                            AppUtils.getThemedResId(context, R.attr.colorPrimary)))
                    .setShowTitle(true)
                    .enableUrlBarHiding()
                    .addDefaultShareMenuItem()
            if (item != null) {
                builder.addMenuItem(context.getString(R.string.comments),
                        PendingIntent.getActivity(context, 0,
                                Intent(context, ItemActivity::class.java)
                                        .putExtra(ItemActivity.EXTRA_ITEM, item)
                                        .putExtra(ItemActivity.EXTRA_OPEN_COMMENTS, true),
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                                        PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE :
                                        PendingIntent.FLAG_ONE_SHOT))
            }
            return builder.build().intent.setData(Uri.parse(url))
        } else {
            return Intent(Intent.ACTION_VIEW, Uri.parse(url))
        }
    }

    @SuppressLint("InlinedApi")
    fun multiWindowIntent(activity: Activity, intent: Intent): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.isInMultiWindowMode()) {
            intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT |
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }
        return intent
    }

    fun setTextAppearance(textView: TextView, textAppearance: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            textView.setTextAppearance(textAppearance)
        } else {
            //noinspection deprecation
            textView.setTextAppearance(textView.getContext(), textAppearance)
        }
    }

    fun urlEquals(thisUrl: String, thatUrl: String): Boolean {
        if (AndroidUtils.TextUtils.isEmpty(thisUrl) || AndroidUtils.TextUtils.isEmpty(thatUrl)) {
            return false
        }
        thisUrl = thisUrl.endsWith("/") ? thisUrl : thisUrl + "/"
        thatUrl = thatUrl.endsWith("/") ? thatUrl : thatUrl + "/"
        return AndroidUtils.TextUtils.equals(thisUrl, thatUrl)
    }

    open class SystemUiHelper {
        private var window: Window? = null
        private val originalUiFlags: Int = 0
        private var enabled: Boolean = true

        constructor(window: Window) {
            this.window = window
            this.originalUiFlags = window.getDecorView().getSystemUiVisibility()
        }

        @SuppressLint("InlinedApi")
        fun setFullscreen(fullscreen: Boolean) {
            if (!enabled) {
                return
            }
            if (fullscreen) {
                window.getDecorView().setSystemUiVisibility(originalUiFlags |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            } else {
                window.getDecorView().setSystemUiVisibility(originalUiFlags)
            }
        }

        fun setEnabled(enabled: Boolean) {
            this.enabled = enabled
        }
    }
}
