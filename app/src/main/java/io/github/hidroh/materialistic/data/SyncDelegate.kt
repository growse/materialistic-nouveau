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

package io.github.hidroh.materialistic.data

import android.accounts.Account
import android.accounts.AccountManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.PersistableBundle
import android.os.Process
import android.text.TextUtils
import android.text.format.DateUtils
import android.webkit.WebView

import java.io.IOException
import java.util.Set
import java.util.concurrent.Executor

import javax.inject.Inject

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import io.github.hidroh.materialistic.AppUtils
import io.github.hidroh.materialistic.BuildConfig
import io.github.hidroh.materialistic.ItemActivity
import io.github.hidroh.materialistic.Preferences
import io.github.hidroh.materialistic.R
import io.github.hidroh.materialistic.annotation.Synthetic
import io.github.hidroh.materialistic.widget.AdBlockWebViewClient
import io.github.hidroh.materialistic.widget.CacheableWebView
import retrofit2.Call
import retrofit2.Callback

open class SyncDelegate {
    const val SYNC_PREFERENCES_FILE: String = "_syncpreferences"
    private const val NOTIFICATION_GROUP_KEY: String = "group"
    private const val SYNC_ACCOUNT_NAME: String = "Materialistic"
    private const val TIMEOUT_MILLIS: Long = DateUtils.MINUTE_IN_MILLIS
    private const val DOWNLOADS_CHANNEL_ID: String = "downloads"

    private var mHnRestService: HackerNewsClient.RestService? = null
    private var mReadabilityClient: ReadabilityClient? = null
    private var mSharedPreferences: SharedPreferences? = null
    private var mNotificationManager: NotificationManager? = null
    private var mNotificationBuilder: NotificationCompat.Builder? = null
    private val mHandler: Handler = new Handler(Looper.getMainLooper())
    private var mSyncProgress: SyncProgress? = null
    private var mContext: Context? = null
    private var mListener: ProgressListener? = null
    private var mJob: Job? = null
    var mWebView: CacheableWebView? = null

    @Inject
    constructor(context: Context, factory: RestServiceFactory, readabilityClient: ReadabilityClient) {
        mContext = context
        mSharedPreferences = context.getSharedPreferences(
                context.getPackageName() + SYNC_PREFERENCES_FILE, Context.MODE_PRIVATE)
        mHnRestService = factory.create(HackerNewsClient.BASE_API_URL,
                HackerNewsClient.RestService::class.java, BackgroundThreadExecutor())
        mReadabilityClient = readabilityClient
        mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(DOWNLOADS_CHANNEL_ID,
                    context.getString(R.string.notification_channel_downloads),
                    NotificationManager.IMPORTANCE_LOW)
            mNotificationManager.createNotificationChannel(channel)
            mNotificationBuilder = NotificationCompat.Builder(context, DOWNLOADS_CHANNEL_ID)
        } else {
            //noinspection deprecation
            mNotificationBuilder = NotificationCompat.Builder(context)
        }
        mNotificationBuilder
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                        R.mipmap.ic_launcher))
                .setSmallIcon(R.drawable.ic_notification)
                .setGroup(NOTIFICATION_GROUP_KEY)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setAutoCancel(true)
    }

    @UiThread
    fun scheduleSync(context: Context, job: Job) {
        if (!Preferences.Offline.isEnabled(context)) {
            return
        }
        if (!TextUtils.isEmpty(job.id)) {
            JobInfo.Builder builder = JobInfo.Builder(Long.valueOf(job.id).intValue(),
                    ComponentName(context.getPackageName(),
                            ItemSyncJobService::class.java.getName()))
                    .setRequiredNetworkType(Preferences.Offline.isWifiOnly(context) ?
                            JobInfo.NETWORK_TYPE_UNMETERED :
                            JobInfo.NETWORK_TYPE_ANY)
                    .setExtras(job.toPersistableBundle())
            if (Preferences.Offline.currentConnectionEnabled(context)) {
                builder.setOverrideDeadline(0)
            }
            ((JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE))
                    .schedule(builder.build())
        } else {
            val extras = Bundle(job.toBundle())
            extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
            Account syncAccount
            val accountManager = AccountManager.get(context)
            val accounts = accountManager.getAccountsByType(BuildConfig.APPLICATION_ID)
            if (accounts.length == 0) {
                syncAccount = Account(SYNC_ACCOUNT_NAME, BuildConfig.APPLICATION_ID)
                accountManager.addAccountExplicitly(syncAccount, null, null)
            } else {
                syncAccount = accounts[0]
            }
            ContentResolver.requestSync(syncAccount, SyncContentProvider.PROVIDER_AUTHORITY, extras)
        }
    }

    fun subscribe(listener: ProgressListener) {
        mListener = listener
    }

    fun performSync(job: Job) {
        // assume that connection wouldn't change until we finish syncing
        mJob = job
        if (!TextUtils.isEmpty(mJob.id)) {
            val message = Message.obtain(mHandler, this::stopSync)
            message.what = Integer.valueOf(mJob.id)
            mHandler.sendMessageDelayed(message, TIMEOUT_MILLIS)
            mSyncProgress = SyncProgress(mJob)
            sync(mJob.id)
        } else {
            syncDeferredItems()
        }
    }

    private fun syncDeferredItems() {
        val itemIds = mSharedPreferences.getAll().keySet()
        for (itemId in itemIds) {
            scheduleSync(mContext, JobBuilder(mContext, itemId).setNotificationEnabled(false).build())
        }
    }

    private fun sync(itemId: String) {
        if (!mJob.connectionEnabled) {
            defer(itemId)
            return
        }
        HackerNewsItem cachedItem
        if ((cachedItem = getFromCache(itemId)) != null) {
            sync(cachedItem)
        } else {
            updateProgress()
            // TODO defer on low battery as well?
            mHnRestService.networkItem(itemId).enqueue(Callback<HackerNewsItem>() {
                public void onResponse(Call<HackerNewsItem> call,
                                       retrofit2.Response<HackerNewsItem> response) {
                    HackerNewsItem item
                    if ((item = response.body()) != null) {
                        sync(item)
                    }
                }

                public void onFailure(Call<HackerNewsItem> call, Throwable t) {
                    notifyItem(itemId, null)
                }
            })
        }
    }

    @Synthetic
    fun sync(item: HackerNewsItem) {
        mSharedPreferences.edit().remove(item.getId()).apply()
        notifyItem(item.getId(), item)
        syncReadability(item)
        syncArticle(item)
        syncChildren(item)
    }

    private fun syncReadability(item: HackerNewsItem) {
        if (mJob.readabilityEnabled && item.isStoryType()) {
            val itemId = item.getId()
            mReadabilityClient.parse(itemId, item.getRawUrl(), content -> notifyReadability())
        }
    }

    private fun syncArticle(item: HackerNewsItem) {
        if (mJob.articleEnabled && item.isStoryType() && !TextUtils.isEmpty(item.getUrl())) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                loadArticle(item)
            } else {
                mContext.startService(Intent(mContext, WebCacheService::class.java)
                        .putExtra(WebCacheService.EXTRA_URL, item.getUrl()))
                notifyArticle(100)
            }
        }
    }

    private fun loadArticle(item: HackerNewsItem) {
        mWebView = CacheableWebView(mContext)
        mWebView.setWebViewClient(AdBlockWebViewClient(Preferences.adBlockEnabled(mContext)))
        mWebView.setWebChromeClient(CacheableWebView.ArchiveClient() {
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress)
                notifyArticle(newProgress)
            }
        })
        notifyArticle(0)
        mWebView.loadUrl(item.getUrl())
    }

    private fun syncChildren(item: HackerNewsItem) {
        if (mJob.commentsEnabled && item.getKids() != null) {
            for (id in item.getKids()) {
                sync(String.valueOf(id))
            }
        }
    }

    private fun defer(itemId: String) {
        mSharedPreferences.edit().putBoolean(itemId, true).apply()
    }

    private fun getFromCache(itemId: String): HackerNewsItem {
        try {
            return mHnRestService.cachedItem(itemId).execute().body()
        } catch (IOException e) {
            return null
        }
    }

    @Synthetic
    fun notifyItem(id: String, item: HackerNewsItem) {
        mSyncProgress.finishItem(id, item,
                mJob.commentsEnabled && mJob.connectionEnabled,
                mJob.readabilityEnabled && mJob.connectionEnabled)
        updateProgress()
    }

    private fun notifyReadability() {
        mSyncProgress.finishReadability()
        updateProgress()
    }

    @Synthetic
    fun notifyArticle(newProgress: Int) {
        mSyncProgress.updateArticle(newProgress, 100)
        updateProgress()
    }

    private fun updateProgress() {
        if (mSyncProgress.getProgress() >= mSyncProgress.getMax()) { // TODO may never done
            finish(); // TODO finish once only
        } else if (mJob.notificationEnabled) {
            showProgress()
        }
    }

    private fun showProgress() {
        mNotificationManager.notify(Integer.valueOf(mJob.id), mNotificationBuilder
                .setContentTitle(mSyncProgress.title)
                .setContentText(mContext.getString(R.string.download_in_progress))
                .setContentIntent(getItemActivity(mJob.id))
                .setOnlyAlertOnce(true)
                .setProgress(mSyncProgress.getMax(), mSyncProgress.getProgress(), false)
                .setSortKey(mJob.id)
                .build())
    }

    private fun finish() {
        if (mListener != null) {
            mListener.onDone(mJob.id)
            mListener = null
        }
        stopSync()
    }

    fun stopSync() {
        // TODO
        mJob.connectionEnabled = false
        int id = Integer.valueOf(mJob.id)
        mNotificationManager.cancel(id)
        mHandler.removeMessages(id)
    }

    private fun getItemActivity(itemId: String): PendingIntent {
        return PendingIntent.getActivity(mContext, 0,
                Intent(Intent.ACTION_VIEW)
                        .setData(AppUtils.createItemUri(itemId))
                        .putExtra(ItemActivity.EXTRA_CACHE_MODE, ItemManager.MODE_CACHE)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                        PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE :
                        PendingIntent.FLAG_ONE_SHOT)
    }

    private open class SyncProgress {
        private var id: String? = null
        private var self: Boolean = false
        private var maxWebProgress: int totalKids, finishedKids, webProgress,? = null
        private var readability: Boolean = false
        var title: String? = null

        @Synthetic
        constructor(job: Job) {
            this.id = job.id
            if (job.commentsEnabled) {
                totalKids = 1
            }
            if (job.articleEnabled) {
                maxWebProgress = 100
            }
            if (job.readabilityEnabled) {
                readability = false
            }
        }

        fun getMax(): Int {
            return 1 + totalKids + (readability != null ? 1 : 0) + maxWebProgress
        }

        fun getProgress(): Int {
            return (self != null ? 1 : 0) + finishedKids + (readability != null && readability ? 1 :0) + webProgress
        }

        @Synthetic
        fun finishItem(id: String, item: HackerNewsItem, kidsEnabled: Boolean, readabilityEnabled: Boolean) {
            if (TextUtils.equals(id, this.id)) {
                finishSelf(item, kidsEnabled, readabilityEnabled)
            } else {
                finishKid()
            }
        }

        @Synthetic
        fun finishReadability() {
            readability = true
        }

        @Synthetic
        fun updateArticle(webProgress: Int, maxWebProgress: Int) {
            this.webProgress = webProgress
            this.maxWebProgress = maxWebProgress
        }

        private fun finishSelf(item: HackerNewsItem, kidsEnabled: Boolean, readabilityEnabled: Boolean) {
            self = item != null
            title = item != null ? item.getTitle() : null
            if (kidsEnabled && item != null && item.getKids() != null) {
                // fetch recursively but only notify for immediate children
                totalKids = item.getKids().length
            } else {
                totalKids = 0
            }
            if (readabilityEnabled) {
                readability = false
            }
        }

        private fun finishKid() {
            finishedKids++
        }
    }

    private open class BackgroundThreadExecutor : Executor {

        constructor()

        override fun execute(r: Runnable) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            r.run()
        }
    }

    interface ProgressListener {
        fun onDone(token: String)
    }

    open class Job {
        private const val EXTRA_ID: String = "extra:id"
        private const val EXTRA_CONNECTION_ENABLED: String = "extra:connectionEnabled"
        private const val EXTRA_READABILITY_ENABLED: String = "extra:readabilityEnabled"
        private const val EXTRA_ARTICLE_ENABLED: String = "extra:articleEnabled"
        private const val EXTRA_COMMENTS_ENABLED: String = "extra:commentsEnabled"
        private const val EXTRA_NOTIFICATION_ENABLED: String = "extra:notificationEnabled"
        var id: String? = null
        var connectionEnabled: Boolean = false
        var readabilityEnabled: Boolean = false
        var articleEnabled: Boolean = false
        var commentsEnabled: Boolean = false
        var notificationEnabled: Boolean = false

        constructor(id: String) {
            this.id = id
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        constructor(bundle: PersistableBundle) {
            id = bundle.getString(EXTRA_ID)
            connectionEnabled = bundle.getInt(EXTRA_CONNECTION_ENABLED) == 1
            readabilityEnabled = bundle.getInt(EXTRA_READABILITY_ENABLED) == 1
            articleEnabled = bundle.getInt(EXTRA_ARTICLE_ENABLED) == 1
            commentsEnabled = bundle.getInt(EXTRA_COMMENTS_ENABLED) == 1
            notificationEnabled = bundle.getInt(EXTRA_NOTIFICATION_ENABLED) == 1
        }

        constructor(bundle: Bundle) {
            id = bundle.getString(EXTRA_ID)
            connectionEnabled = bundle.getBoolean(EXTRA_CONNECTION_ENABLED)
            readabilityEnabled = bundle.getBoolean(EXTRA_READABILITY_ENABLED)
            articleEnabled = bundle.getBoolean(EXTRA_ARTICLE_ENABLED)
            commentsEnabled = bundle.getBoolean(EXTRA_COMMENTS_ENABLED)
            notificationEnabled = bundle.getBoolean(EXTRA_NOTIFICATION_ENABLED)
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        fun toPersistableBundle(): PersistableBundle {
            val bundle = PersistableBundle()
            bundle.putString(EXTRA_ID, id)
            bundle.putInt(EXTRA_CONNECTION_ENABLED, connectionEnabled ? 1 : 0)
            bundle.putInt(EXTRA_READABILITY_ENABLED, readabilityEnabled ? 1 : 0)
            bundle.putInt(EXTRA_ARTICLE_ENABLED, articleEnabled ? 1 : 0)
            bundle.putInt(EXTRA_COMMENTS_ENABLED, commentsEnabled ? 1 : 0)
            bundle.putInt(EXTRA_NOTIFICATION_ENABLED, notificationEnabled ? 1 : 0)
            return bundle
        }

        fun toBundle(): Bundle {
            val bundle = Bundle()
            bundle.putString(EXTRA_ID, id)
            bundle.putBoolean(EXTRA_CONNECTION_ENABLED, connectionEnabled)
            bundle.putBoolean(EXTRA_READABILITY_ENABLED, readabilityEnabled)
            bundle.putBoolean(EXTRA_ARTICLE_ENABLED, articleEnabled)
            bundle.putBoolean(EXTRA_COMMENTS_ENABLED, commentsEnabled)
            bundle.putBoolean(EXTRA_NOTIFICATION_ENABLED, notificationEnabled)
            return bundle
        }
    }

    open class JobBuilder {
        private var job: Job? = null

        constructor(context: Context, id: String) {
            job = Job(id)
            setConnectionEnabled(Preferences.Offline.currentConnectionEnabled(context))
            setReadabilityEnabled(Preferences.Offline.isReadabilityEnabled(context))
            setArticleEnabled(Preferences.Offline.isArticleEnabled(context))
            setCommentsEnabled(Preferences.Offline.isCommentsEnabled(context))
            setNotificationEnabled(Preferences.Offline.isNotificationEnabled(context))
        }

        fun setConnectionEnabled(connectionEnabled: Boolean): JobBuilder {
            job.connectionEnabled = connectionEnabled
            return this
        }

        fun setReadabilityEnabled(readabilityEnabled: Boolean): JobBuilder {
            job.readabilityEnabled = readabilityEnabled
            return this
        }

        fun setArticleEnabled(articleEnabled: Boolean): JobBuilder {
            job.articleEnabled = articleEnabled
            return this
        }

        fun setCommentsEnabled(commentsEnabled: Boolean): JobBuilder {
            job.commentsEnabled = commentsEnabled
            return this
        }

        fun setNotificationEnabled(notificationEnabled: Boolean): JobBuilder {
            job.notificationEnabled = notificationEnabled
            return this
        }

        fun build(): Job {
            return job
        }
    }
}
