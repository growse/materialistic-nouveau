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

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.text.TextUtils

import java.util.HashMap
import java.util.HashSet
import java.util.Locale
import java.util.Map
import java.util.Set

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import io.github.hidroh.materialistic.annotation.PublicApi
import io.github.hidroh.materialistic.annotation.Synthetic
import io.github.hidroh.materialistic.data.AlgoliaPopularClient
import io.github.hidroh.materialistic.preference.ThemePreference

@SuppressWarnings("WeakerAccess")
@PublicApi
open class Preferences {
    private const val DRAFT_PREFIX: String = "draft_%1$s"
    private const val PREFERENCES_DRAFT: String = "_drafts"
    var sReleaseNotesSeen: Boolean = null

    enum class SwipeAction {
        None,
        Vote,
        Save,
        Refresh,
        Share
    }
    enum class StoryViewMode {
        Comment,
        Article,
        Readability
    }

    private static final BoolToStringPref[] PREF_MIGRATION = BoolToStringPref[]{
            BoolToStringPref(R.string.pref_item_click, false,
                    R.string.pref_story_display, R.string.pref_story_display_value_comments),
            BoolToStringPref(R.string.pref_item_search_recent, true,
                    R.string.pref_search_sort, R.string.pref_search_sort_value_default)
    }

    fun sync(preferenceManager: PreferenceManager) {
        val map = preferenceManager.getSharedPreferences().getAll()
        for (key in map.keySet()) {
            sync(preferenceManager, key)
        }
    }

    private fun sync(preferenceManager: PreferenceManager, key: String) {
        val pref = preferenceManager.findPreference(key)
        if (pref is ListPreference) {
            val listPref = (ListPreference) pref
            pref.setSummary(listPref.getEntry())
        }
    }

    /**
     * Migrate from boolean preferences to string preferences. Should be called only once
     * when application is relaunched.
     * If boolean preference has been set before, and value is not default, migrate to the new
     * corresponding string value
     * If boolean preference has been set before, but value is default, simply remove it
     * @param context   application context
     * TODO remove once all users migrated
     */
    fun migrate(context: Context) {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        SharedPreferences.Editor editor = sp.edit()
        for (pref in PREF_MIGRATION) {
            if (pref.isChanged(context, sp)) {
                editor.putString(context.getString(pref.newKey), context.getString(pref.newValue))
            }

            if (pref.hasOldValue(context, sp)) {
                editor.remove(context.getString(pref.oldKey))
            }
        }

        editor.apply()
    }

    fun isListItemCardView(context: Context): Boolean {
        return get(context, R.string.pref_list_item_view, false)
    }

    fun isSortByRecent(context: Context): Boolean {
        return get(context, R.string.pref_search_sort, R.string.pref_search_sort_value_recent)
                .equals(context.getString(R.string.pref_search_sort_value_recent))
    }

    fun setSortByRecent(context: Context, byRecent: Boolean) {
        set(context, R.string.pref_search_sort, context.getString(byRecent ?
                R.string.pref_search_sort_value_recent : R.string.pref_search_sort_value_default))
    }

    fun getDefaultStoryView(context: Context): StoryViewMode {
        val pref = get(context, R.string.pref_story_display,
                        R.string.pref_story_display_value_article)
        if (TextUtils.equals(context.getString(R.string.pref_story_display_value_comments), pref)) {
            return StoryViewMode.Comment
        }
        if (TextUtils.equals(context.getString(R.string.pref_story_display_value_readability), pref)) {
            return StoryViewMode.Readability
        }
        return StoryViewMode.Article
    }

    fun externalBrowserEnabled(context: Context): Boolean {
        return get(context, R.string.pref_external, false)
    }

    fun colorCodeEnabled(context: Context): Boolean {
        return get(context, R.string.pref_color_code, true)
    }

    fun colorCodeOpacity(context: Context): Int {
        return getInt(context, R.string.pref_color_code_opacity, 100)
    }

    fun smoothScrollEnabled(context: Context): Boolean {
        return get(context, R.string.pref_smooth_scroll, true)
    }

    fun threadIndicatorEnabled(context: Context): Boolean {
        return get(context, R.string.pref_thread_indicator, true)
    }

    fun highlightUpdatedEnabled(context: Context): Boolean {
        return get(context, R.string.pref_highlight_updated, true)
    }

    fun autoMarkAsViewed(context: Context): Boolean {
        return get(context, R.string.pref_auto_viewed, false)
    }

    fun navigationEnabled(context: Context): Boolean {
        return get(context, R.string.pref_navigation, false)
    }

    fun navigationVibrationEnabled(context: Context): Boolean {
        return get(context, R.string.pref_navigation_vibrate, true)
    }

    fun customTabsEnabled(context: Context): Boolean {
        return get(context, R.string.pref_custom_tab, true)
    }

    fun isSinglePage(context: Context, displayOption: String): Boolean {
        return !TextUtils.equals(displayOption,
                context.getString(R.string.pref_comment_display_value_multiple))
    }

    fun isAutoExpand(context: Context, displayOption: String): Boolean {
        return TextUtils.equals(displayOption,
                context.getString(R.string.pref_comment_display_value_single))
    }

    fun getCommentDisplayOption(context: Context): String {
        return get(context, R.string.pref_comment_display,
                        R.string.pref_comment_display_value_single)
    }

    fun setPopularRange(context: Context, range: String) {
        set(context, R.string.pref_popular_range, range)
    }

    @NonNull
    fun getPopularRange(context: Context): String {
        return get(context, R.string.pref_popular_range, AlgoliaPopularClient.LAST_24H)
    }

    fun getCommentMaxLines(context: Context): Int {
        val maxLinesString = get(context, R.string.pref_max_lines, null)
        int maxLines = maxLinesString == null ? -1 : Integer.parseInt(maxLinesString)
        if (maxLines < 0) {
            maxLines = Integer.MAX_VALUE
        }
        return maxLines
    }

    fun getLineHeight(context: Context): Float {
        return getFloatFromString(context, R.string.pref_line_height, 1.0f)
    }

    fun getReadabilityLineHeight(context: Context): Float {
        return getFloatFromString(context, R.string.pref_readability_line_height, 1.0f)
    }

    fun shouldLazyLoad(context: Context): Boolean {
        return get(context, R.string.pref_lazy_load, true)
    }

    fun getUsername(context: Context): String {
        return get(context, R.string.pref_username, null)
    }

    fun setUsername(context: Context, username: String) {
        set(context, R.string.pref_username, username)
    }

    @NonNull
    fun getLaunchScreen(context: Context): String {
        return get(context, R.string.pref_launch_screen, R.string.pref_launch_screen_value_top)
    }

    fun isLaunchScreenLast(context: Context): Boolean {
        return TextUtils.equals(context.getString(R.string.pref_launch_screen_value_last),
                getLaunchScreen(context))
    }

    fun adBlockEnabled(context: Context): Boolean {
        return get(context, R.string.pref_ad_block, true)
    }

    fun saveDraft(context: Context, parentId: String, draft: String) {
        context.getSharedPreferences(context.getPackageName() + PREFERENCES_DRAFT, Context.MODE_PRIVATE)
                .edit()
                .putString(String.format(Locale.US, DRAFT_PREFIX, parentId), draft)
                .apply()
    }

    fun getDraft(context: Context, parentId: String): String {
        return context
                .getSharedPreferences(context.getPackageName() + PREFERENCES_DRAFT, Context.MODE_PRIVATE)
                .getString(String.format(Locale.US, DRAFT_PREFIX, parentId), null)
    }

    fun deleteDraft(context: Context, parentId: String) {
        context.getSharedPreferences(context.getPackageName() + PREFERENCES_DRAFT, Context.MODE_PRIVATE)
                .edit()
                .remove(String.format(Locale.US, DRAFT_PREFIX, parentId))
                .apply()
    }

    fun clearDrafts(context: Context) {
        context.getSharedPreferences(context.getPackageName() + PREFERENCES_DRAFT, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
    }

    fun isReleaseNotesSeen(context: Context): Boolean {
        if (sReleaseNotesSeen == null) {
            val info = null
            try {
                info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0)
            } catch (PackageManager.NameNotFoundException e) {
                // no op
            }
            // considered seen if first time install or last seen release is up to date
            if (info != null && info.firstInstallTime == info.lastUpdateTime) {
                setReleaseNotesSeen(context)
            } else {
                sReleaseNotesSeen = getInt(context, R.string.pref_latest_release, 0) >= BuildConfig.LATEST_RELEASE
            }
        }
        return sReleaseNotesSeen
    }

    fun setReleaseNotesSeen(context: Context) {
        sReleaseNotesSeen = true
        setInt(context, R.string.pref_latest_release, BuildConfig.LATEST_RELEASE)
    }

    fun multiWindowEnabled(context: Context): Boolean {
        return !TextUtils.equals(context.getString(R.string.pref_multi_window_value_none),
                get(context, R.string.pref_multi_window, R.string.pref_multi_window_value_none))
    }

    fun getListSwipePreferences(context: Context): Array<SwipeAction> {
        val left = get(context, R.string.pref_list_swipe_left, R.string.swipe_save),
                right = get(context, R.string.pref_list_swipe_right, R.string.swipe_vote)
        return SwipeAction[]{parseSwipeAction(left), parseSwipeAction(right)}
    }

    fun reset(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .clear()
                .apply()
    }

    private fun parseSwipeAction(value: String): SwipeAction {
        try {
            return SwipeAction.valueOf(value)
        } catch (IllegalArgumentException | NullPointerException e) {
            return SwipeAction.None
        }
    }

    @Synthetic
    fun get(context: Context, key: Int, defaultValue: Boolean): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(key), defaultValue)
    }

    private fun getInt(context: Context, key: Int, defaultValue: Int): Int {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(context.getString(key), defaultValue)
    }

    private fun getFloatFromString(context: Context, key: Int, defaultValue: Float): Float {
        val floatValue = get(context, key, null)
        try {
            return Float.parseFloat(floatValue)
        } catch (NumberFormatException | NullPointerException e) {
            return defaultValue
        }
    }

    @Synthetic
    fun get(context: Context, key: Int, defaultValue: String): String {
        return get(context, context.getString(key), defaultValue)
    }

    private fun get(context: Context, key: Int, defaultValue: Int): String {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(key), context.getString(defaultValue))
    }

    private fun get(context: Context, key: String, defaultValue: String): String {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(key, defaultValue)
    }

    private fun setInt(context: Context, key: Int, value: Int) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putInt(context.getString(key), value)
                .apply()
    }

    private fun set(context: Context, key: Int, value: String) {
        set(context, context.getString(key), value)
    }

    private fun set(context: Context, key: String, value: String) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(key, value)
                .apply()
    }

    @Synthetic
    fun set(context: Context, key: Int, value: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(context.getString(key), value)
                .apply()
    }

    open class BoolToStringPref {
        val oldKey: Int = 0
        private val oldDefault: Boolean = false
        val newKey: Int = 0
        val newValue: Int = 0

        @Synthetic
        constructor(oldKey: Int, oldDefault: Boolean, newKey: Int, newValue: Int) {
            this.oldKey = oldKey
            this.oldDefault = oldDefault
            this.newKey = newKey
            this.newValue = newValue
        }

        @Synthetic
        fun isChanged(context: Context, sp: SharedPreferences): Boolean {
            return hasOldValue(context, sp) &&
                    sp.getBoolean(context.getString(oldKey), oldDefault) != oldDefault
        }

        @Synthetic
        fun hasOldValue(context: Context, sp: SharedPreferences): Boolean {
            return sp.contains(context.getString(oldKey))
        }
    }

    @PublicApi
    open class Theme {

        fun apply(context: Context, dialogTheme: Boolean, isTranslucent: Boolean) {
            ThemePreference.ThemeSpec themeSpec = getTheme(context, isTranslucent)
            context.setTheme(themeSpec.theme)
            if (themeSpec.themeOverrides >= 0) {
                context.getTheme().applyStyle(themeSpec.themeOverrides, true)
            }
            if (dialogTheme) {
                context.setTheme(AppUtils.getThemedResId(context, R.attr.alertDialogTheme))
            }
        }

        fun getTypeface(context: Context): String {
            return get(context, R.string.pref_font, null)
        }

        fun getReadabilityTypeface(context: Context): String {
            val typefaceName = get(context, R.string.pref_readability_font, null)
            if (TextUtils.isEmpty(typefaceName)) {
                return getTypeface(context)
            }
            return typefaceName
        }

        fun resolveTextSize(choice: String): Int {
            switch (Integer.parseInt(choice)) {
                case -1:
                    return R.style.AppTextSize_XSmall
                case 0:
                default:
                    return R.style.AppTextSize
                case 1:
                    return R.style.AppTextSize_Medium
                case 2:
                    return R.style.AppTextSize_Large
                case 3:
                    return R.style.AppTextSize_XLarge
            }
        }

        fun resolvePreferredTextSize(context: Context): Int {
            return resolveTextSize(getPreferredTextSize(context))
        }

        fun resolvePreferredReadabilityTextSize(context: Context): Int {
            return resolveTextSize(getPreferredReadabilityTextSize(context))
        }

        fun getAutoDayNightMode(context: Context): Int {
            return getTheme(context, false) is ThemePreference.DayNightSpec &&
                    get(context, R.string.pref_daynight_auto, false) ?
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM : AppCompatDelegate.MODE_NIGHT_NO
        }

        fun disableAutoDayNight(context: Context) {
            set(context, R.string.pref_daynight_auto, false)
        }

        private fun getPreferredReadabilityTextSize(context: Context): String {
            val choice = get(context, R.string.pref_readability_text_size, null)
            if (TextUtils.isEmpty(choice)) {
                return getPreferredTextSize(context)
            }
            return choice
        }

        private fun getPreferredTextSize(context: Context): String {
            return get(context, R.string.pref_text_size, String.valueOf(0))
        }

        private fun getTheme(context: Context, isTransulcent: Boolean): ThemePreference.ThemeSpec {
            return ThemePreference.getTheme(get(context, R.string.pref_theme, null), isTransulcent)
        }
    }

    @PublicApi
    open class Offline {

        fun isEnabled(context: Context): Boolean {
            return get(context, R.string.pref_saved_item_sync, false)
        }

        fun isCommentsEnabled(context: Context): Boolean {
            return isEnabled(context) &&
                    get(context, R.string.pref_offline_comments, true)
        }

        fun isArticleEnabled(context: Context): Boolean {
            return isEnabled(context) &&
                    get(context, R.string.pref_offline_article, true)
        }

        fun isReadabilityEnabled(context: Context): Boolean {
            return isEnabled(context) &&
                    get(context, R.string.pref_offline_readability, true)
        }

        fun currentConnectionEnabled(context: Context): Boolean {
            return !isWifiOnly(context) || AppUtils.isOnWiFi(context)
        }

        fun isNotificationEnabled(context: Context): Boolean {
            return get(context, R.string.pref_offline_notification, false)
        }

        fun isWifiOnly(context: Context): Boolean {
            val wifiValue = context.getString(R.string.offline_data_wifi)
            return TextUtils.equals(wifiValue, get(context, R.string.pref_offline_data, wifiValue))
        }
    }

    open class Observable {
        private var CONTEXT_KEYS: Set<String>? = null
        private val mSubscribedKeys: Map<String, Int> = new HashMap<>()
        private final SharedPreferences.OnSharedPreferenceChangeListener mListener = (sharedPreferences, key) -> {
            if (mSubscribedKeys.containsKey(key)) {
                notifyChanged(mSubscribedKeys.get(key), CONTEXT_KEYS.contains(key))
            }
        }
        private var mObserver: Observer? = null

        fun subscribe(context: Context, observer: Observer, preferenceKeys: IntArray) {
            ensureContextKeys(context)
            setSubscription(context, preferenceKeys)
            mObserver = observer
            PreferenceManager.getDefaultSharedPreferences(context)
                    .registerOnSharedPreferenceChangeListener(mListener)
        }

        fun unsubscribe(context: Context) {
            PreferenceManager.getDefaultSharedPreferences(context)
                    .unregisterOnSharedPreferenceChangeListener(mListener)
        }

        private fun setSubscription(context: Context, preferenceKeys: IntArray) {
            mSubscribedKeys.clear()
            for (key in preferenceKeys) {
                mSubscribedKeys.put(context.getString(key), key)
            }
        }

        private fun notifyChanged(key: Int, contextChanged: Boolean) {
            if (mObserver != null) {
                mObserver.onPreferenceChanged(key, contextChanged)
            }
        }

        @SuppressLint("UseSparseArrays")
        private fun ensureContextKeys(context: Context) {
            if (CONTEXT_KEYS != null) {
                return
            }
            CONTEXT_KEYS = HashSet<>()
            CONTEXT_KEYS.add(context.getString(R.string.pref_theme))
            CONTEXT_KEYS.add(context.getString(R.string.pref_text_size))
            CONTEXT_KEYS.add(context.getString(R.string.pref_font))
            CONTEXT_KEYS.add(context.getString(R.string.pref_daynight_auto))
        }
    }

    interface Observer {
        fun onPreferenceChanged(key: Int, contextChanged: Boolean)
    }
}
