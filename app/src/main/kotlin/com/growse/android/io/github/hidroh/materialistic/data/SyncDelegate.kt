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
package com.growse.android.io.github.hidroh.materialistic.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.PersistableBundle
import android.os.Process
import android.text.TextUtils
import android.text.format.DateUtils
import android.webkit.WebView
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import com.growse.android.io.github.hidroh.materialistic.AppUtils.createItemUri
import com.growse.android.io.github.hidroh.materialistic.ItemActivity
import com.growse.android.io.github.hidroh.materialistic.Preferences.Offline.isArticleEnabled
import com.growse.android.io.github.hidroh.materialistic.Preferences.Offline.isCommentsEnabled
import com.growse.android.io.github.hidroh.materialistic.Preferences.Offline.isEnabled
import com.growse.android.io.github.hidroh.materialistic.Preferences.Offline.isNotificationEnabled
import com.growse.android.io.github.hidroh.materialistic.Preferences.Offline.isReadabilityEnabled
import com.growse.android.io.github.hidroh.materialistic.Preferences.Offline.isWifiOnly
import com.growse.android.io.github.hidroh.materialistic.Preferences.adBlockEnabled
import com.growse.android.io.github.hidroh.materialistic.R
import com.growse.android.io.github.hidroh.materialistic.data.HackerNewsItem.Companion.fromCachedItem
import com.growse.android.io.github.hidroh.materialistic.data.HackerNewsItem.Companion.toCachedItem
import com.growse.android.io.github.hidroh.materialistic.widget.AdBlockWebViewClient
import com.growse.android.io.github.hidroh.materialistic.widget.CacheableWebView
import com.growse.android.io.github.hidroh.materialistic.widget.CacheableWebView.ArchiveClient
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlin.concurrent.Volatile
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SyncDelegate
@Inject
internal constructor(
    @param:ApplicationContext private val mContext: Context,
    factory: RestServiceFactory,
    private val mReadabilityClient: ReadabilityClient,
    private val mLocalCache: LocalCache,
) {
  private val mHnRestService: HackerNewsClient.RestService
  private val mNotificationManager: NotificationManager
  private val mNotificationBuilder: NotificationCompat.Builder
  private val mHandler = Handler(Looper.getMainLooper())

  /**
   * Number of fetches started but not yet completed. The sync is done when this hits zero, which is
   * the only reliable completion signal - the notification progress bar cannot be one, as its
   * maximum grows as the tree is discovered.
   */
  private val mOutstanding = AtomicInteger()
  private val mFinished = AtomicBoolean()
  private var mSyncProgress: SyncProgress? = null
  private var mListener: ProgressListener? = null
  private var mJob: Job? = null

  @VisibleForTesting var mWebView: CacheableWebView? = null

  init {
    mHnRestService =
        factory.create<HackerNewsClient.RestService>(
            HackerNewsClient.BASE_API_URL,
            HackerNewsClient.RestService::class.java,
            BackgroundThreadExecutor(),
        )
    mNotificationManager =
        mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
          NotificationChannel(
              DOWNLOADS_CHANNEL_ID,
              mContext.getString(R.string.notification_channel_downloads),
              NotificationManager.IMPORTANCE_LOW,
          )
      mNotificationManager.createNotificationChannel(channel)
      mNotificationBuilder = NotificationCompat.Builder(mContext, DOWNLOADS_CHANNEL_ID)
    } else {
      mNotificationBuilder = NotificationCompat.Builder(mContext)
    }
    mNotificationBuilder
        .setLargeIcon(
            BitmapFactory.decodeResource(
                mContext.resources,
                R.mipmap.ic_launcher,
            )
        )
        .setSmallIcon(R.drawable.ic_notification)
        .setGroup(NOTIFICATION_GROUP_KEY)
        .setOnlyAlertOnce(true)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setAutoCancel(true)
  }

  fun subscribe(listener: ProgressListener?) {
    mListener = listener
  }

  fun performSync(job: Job) {
    mJob = job
    mSyncProgress = SyncProgress(mJob!!)
    resetTimeout()
    mOutstanding.set(1) // the story itself
    SYNC_EXECUTOR.execute { sync(mJob!!.id!!) }
  }

  /**
   * Abandons the sync if nothing completes for [.TIMEOUT_MILLIS]. Rescheduled on every completion
   * so that a large but healthy comment tree is allowed to finish.
   */
  private fun resetTimeout() {
    val id = mJob!!.id!!.toInt()
    mHandler.removeMessages(id)
    val message = Message.obtain(mHandler) { this.finish() }
    message.what = id
    mHandler.sendMessageDelayed(message, TIMEOUT_MILLIS)
  }

  /** Marks one fetch as complete, finishing the sync once none remain outstanding. */
  private fun completeUnit() {
    if (mOutstanding.decrementAndGet() <= 0) {
      finish()
    } else {
      resetTimeout()
    }
  }

  /**
   * Syncs a single item. Every exit path must account for exactly one unit of work, either by
   * completing it here or by handing it to [.sync].
   */
  private fun sync(itemId: String) {
    if (mJob!!.stopped) {
      completeUnit()
      return
    }
    if (!mJob!!.refresh) {
      val stored = mLocalCache.getCachedItem(itemId)
      if (stored != null) {
        if (System.currentTimeMillis() - stored.fetchedAt < CACHE_TTL_MILLIS) {
          sync(fromCachedItem(stored), false)
          return
        }
        // stale, so fall through to the network - the HTTP cache is written from the same
        // responses and so is no fresher
      } else {
        val httpCached = getHttpCached(itemId)
        if (httpCached != null) {
          sync(httpCached, true)
          return
        }
      }
    }
    updateProgress()
    // TODO defer on low battery as well?
    mHnRestService
        .networkItem(itemId)
        .enqueue(
            object : Callback<HackerNewsItem?> {
              override fun onResponse(
                  call: Call<HackerNewsItem?>,
                  response: Response<HackerNewsItem?>,
              ) {
                val item: HackerNewsItem?
                if ((response.body().also { item = it }) != null) {
                  sync(item!!, true)
                } else {
                  notifyItem(itemId, null)
                  completeUnit()
                }
              }

              override fun onFailure(call: Call<HackerNewsItem?>, t: Throwable) {
                notifyItem(itemId, null)
                completeUnit()
              }
            }
        )
  }

  /**
   * @param store whether to write the item to the cache. Never set for an item that was read back
   *   out of it - that would refresh the row's timestamp without refreshing its content, and
   *   [.CACHE_TTL_MILLIS] would never elapse.
   */
  fun sync(item: HackerNewsItem, store: Boolean) {
    if (store) {
      mLocalCache.putCachedItem(toCachedItem(item))
    }
    notifyItem(item.id, item)
    syncReadability(item)
    syncArticle(item)
    // children are counted before this item completes, so the outstanding count cannot reach
    // zero while there is still tree left to walk
    syncChildren(item)
    completeUnit()
  }

  private fun syncReadability(item: HackerNewsItem) {
    if (mJob!!.readabilityEnabled && item.isStoryType) {
      val itemId = item.id
      mOutstanding.incrementAndGet()
      mReadabilityClient.parse(
          itemId,
          item.rawUrl,
      ) {
        notifyReadability()
        completeUnit()
      }
    }
  }

  private fun syncArticle(item: HackerNewsItem) {
    if (mJob!!.articleEnabled && item.isStoryType && !TextUtils.isEmpty(item.url)) {
      if (Looper.myLooper() == Looper.getMainLooper()) {
        loadArticle(item)
      } else {
        mContext.startService(
            Intent(mContext, WebCacheService::class.java)
                .putExtra(WebCacheService.EXTRA_URL, item.url)
        )
        notifyArticle(100)
      }
    }
  }

  private fun loadArticle(item: HackerNewsItem) {
    mWebView = CacheableWebView(mContext)
    mWebView!!.webViewClient = AdBlockWebViewClient(adBlockEnabled(mContext))
    mWebView!!.setWebChromeClient(
        object : ArchiveClient() {
          override fun onProgressChanged(view: WebView, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            notifyArticle(newProgress)
          }
        }
    )
    notifyArticle(0)
    mWebView!!.loadUrl(item.url)
  }

  private fun syncChildren(item: HackerNewsItem) {
    if (mJob!!.commentsEnabled && item.kids != null) {
      val kids = item.kids
      mSyncProgress!!.addKids(kids!!.size)
      mOutstanding.addAndGet(kids.size)
      for (id in kids) {
        sync(id.toString())
      }
    }
  }

  /**
   * @return whatever the HTTP cache still holds for an item with no stored copy, so that items
   *   predating the dedicated cache are not all re-downloaded, or null if it holds nothing.
   */
  private fun getHttpCached(itemId: String?): HackerNewsItem? {
    return try {
      mHnRestService.cachedItem(itemId).execute().body()
    } catch (_: IOException) {
      null
    }
  }

  fun notifyItem(id: String, item: HackerNewsItem?) {
    mSyncProgress!!.finishItem(
        id,
        item,
        mJob!!.readabilityEnabled && !mJob!!.stopped,
    )
    updateProgress()
  }

  private fun notifyReadability() {
    mSyncProgress!!.finishReadability()
    updateProgress()
  }

  fun notifyArticle(newProgress: Int) {
    mSyncProgress!!.updateArticle(newProgress, 100)
    updateProgress()
  }

  /**
   * Updates the notification only. Completion is decided by [.completeUnit], since the progress bar
   * maximum keeps growing as more of the comment tree is discovered.
   */
  private fun updateProgress() {
    if (mJob!!.notificationEnabled && !mFinished.get()) {
      showProgress()
    }
  }

  private fun showProgress() {
    mNotificationManager.notify(
        mJob!!.id!!.toInt(),
        mNotificationBuilder
            .setContentTitle(mSyncProgress!!.title)
            .setContentText(mContext.getString(R.string.download_in_progress))
            .setContentIntent(getItemActivity(mJob!!.id!!))
            .setOnlyAlertOnce(true)
            .setProgress(mSyncProgress!!.max, mSyncProgress!!.progress, false)
            .setSortKey(mJob!!.id)
            .build(),
    )
  }

  fun finish() {
    if (!mFinished.compareAndSet(false, true)) {
      return // already finished, or timed out
    }
    if (mListener != null) {
      mListener!!.onDone(mJob!!.id)
      mListener = null
    }
    stopSync()
  }

  fun stopSync() {
    mJob!!.stopped = true
    val id = mJob!!.id!!.toInt()
    mNotificationManager.cancel(id)
    mHandler.removeMessages(id)
  }

  private fun getItemActivity(itemId: String): PendingIntent? {
    return PendingIntent.getActivity(
        mContext,
        0,
        Intent(Intent.ACTION_VIEW)
            .setData(createItemUri(itemId))
            .putExtra(ItemActivity.EXTRA_CACHE_MODE, ItemManager.MODE_CACHE)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  private class SyncProgress(job: Job) {
    private val id = job.id
    private var self: Boolean? = null
    private var totalKids = 0
    private var finishedKids = 0
    private var webProgress = 0
    private var maxWebProgress = 0
    private var readability: Boolean? = null
    var title: String? = null

    init {
      if (job.articleEnabled) {
        maxWebProgress = 100
      }
      if (job.readabilityEnabled) {
        readability = false
      }
    }

    @get:Synchronized
    val max: Int
      get() = 1 + totalKids + (if (readability != null) 1 else 0) + maxWebProgress

    @get:Synchronized
    val progress: Int
      get() =
          (if (self != null) 1 else 0) +
              finishedKids +
              (if (readability != null && readability == true) 1 else 0) +
              webProgress

    /**
     * Counts descendants as they are discovered. Every comment in the tree is counted, at every
     * depth, to match [.finishKid] - counting only immediate children made the progress overtake
     * the maximum as soon as nested replies started completing.
     */
    @Synchronized
    fun addKids(count: Int) {
      totalKids += count
    }

    @Synchronized
    fun finishItem(
        id: String,
        item: HackerNewsItem?,
        readabilityEnabled: Boolean,
    ) {
      if (TextUtils.equals(id, this.id)) {
        finishSelf(item, readabilityEnabled)
      } else {
        finishKid()
      }
    }

    @Synchronized
    fun finishReadability() {
      readability = true
    }

    @Synchronized
    fun updateArticle(webProgress: Int, maxWebProgress: Int) {
      this.webProgress = webProgress
      this.maxWebProgress = maxWebProgress
    }

    fun finishSelf(item: HackerNewsItem?, readabilityEnabled: Boolean) {
      self = item != null
      title = item?.title
      if (readabilityEnabled) {
        readability = false
      }
    }

    fun finishKid() {
      finishedKids++
    }
  }

  private class BackgroundThreadExecutor : Executor {
    override fun execute(r: Runnable) {
      Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
      r.run()
    }
  }

  fun interface ProgressListener {
    fun onDone(token: String?)
  }

  class Job {
    val id: String?

    /**
     * Set when the system stops the job, so that the remaining tree is abandoned rather than
     * fetched. Not persisted: a stopped job is rescheduled and starts over.
     */
    @Volatile var stopped: Boolean = false
    var readabilityEnabled: Boolean = false
    var articleEnabled: Boolean = false
    var commentsEnabled: Boolean = false
    var notificationEnabled: Boolean = false

    /**
     * Re-walk the tree against the network instead of reusing stored items, so that replies posted
     * since the last sync are picked up.
     */
    var refresh: Boolean = false

    constructor(id: String?) {
      this.id = id
    }

    constructor(bundle: PersistableBundle) {
      id = bundle.getString(EXTRA_ID)
      readabilityEnabled = bundle.getInt(EXTRA_READABILITY_ENABLED) == 1
      articleEnabled = bundle.getInt(EXTRA_ARTICLE_ENABLED) == 1
      commentsEnabled = bundle.getInt(EXTRA_COMMENTS_ENABLED) == 1
      notificationEnabled = bundle.getInt(EXTRA_NOTIFICATION_ENABLED) == 1
      refresh = bundle.getInt(EXTRA_REFRESH) == 1
    }

    fun toPersistableBundle(): PersistableBundle {
      val bundle = PersistableBundle()
      bundle.putString(EXTRA_ID, id)
      bundle.putInt(EXTRA_READABILITY_ENABLED, if (readabilityEnabled) 1 else 0)
      bundle.putInt(EXTRA_ARTICLE_ENABLED, if (articleEnabled) 1 else 0)
      bundle.putInt(EXTRA_COMMENTS_ENABLED, if (commentsEnabled) 1 else 0)
      bundle.putInt(EXTRA_NOTIFICATION_ENABLED, if (notificationEnabled) 1 else 0)
      bundle.putInt(EXTRA_REFRESH, if (refresh) 1 else 0)
      return bundle
    }

    companion object {
      private const val EXTRA_ID = "extra:id"
      private const val EXTRA_READABILITY_ENABLED = "extra:readabilityEnabled"
      private const val EXTRA_ARTICLE_ENABLED = "extra:articleEnabled"
      private const val EXTRA_COMMENTS_ENABLED = "extra:commentsEnabled"
      private const val EXTRA_NOTIFICATION_ENABLED = "extra:notificationEnabled"
      private const val EXTRA_REFRESH = "extra:refresh"
    }
  }

  class JobBuilder(context: Context, id: String?) {
    private val job: Job = Job(id)

    init {
      setReadabilityEnabled(isReadabilityEnabled(context))
      setArticleEnabled(isArticleEnabled(context))
      setCommentsEnabled(isCommentsEnabled(context))
      setNotificationEnabled(isNotificationEnabled(context))
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

    fun setRefresh(refresh: Boolean): JobBuilder {
      job.refresh = refresh
      return this
    }

    fun build(): Job {
      return job
    }
  }

  companion object {
    private const val NOTIFICATION_GROUP_KEY = "group"

    /**
     * Idle timeout: the sync is abandoned only after this long with no unit of work completing,
     * rather than after a fixed budget for the whole comment tree.
     */
    private const val TIMEOUT_MILLIS = DateUtils.MINUTE_IN_MILLIS
    private const val DOWNLOADS_CHANNEL_ID = "downloads"

    /**
     * How long a cached item is trusted while syncing. An item's own fields are effectively
     * immutable, but its kid list grows as replies arrive, so the tree has to be re-walked against
     * the network to discover them. A refreshing job ignores this and always goes to the network.
     */
    private const val CACHE_TTL_MILLIS = 30 * DateUtils.MINUTE_IN_MILLIS

    /**
     * Sync walks the comment tree with blocking cache reads, so it must never run on the main
     * thread - Room refuses main thread access outright.
     */
    private val SYNC_EXECUTOR: ExecutorService = Executors.newCachedThreadPool()

    @UiThread
    fun scheduleSync(context: Context, job: Job) {
      if (!isEnabled(context)) {
        return
      }
      // the network constraint holds the job until a permitted connection is available, so the
      // sync itself never has to check
      val jobInfo =
          JobInfo.Builder(
                  job.id!!.toLong().toInt(),
                  ComponentName(context.packageName, ItemSyncJobService::class.java.name),
              )
              .setRequiredNetworkType(
                  if (isWifiOnly(context)) JobInfo.NETWORK_TYPE_UNMETERED
                  else JobInfo.NETWORK_TYPE_ANY
              )
              .setExtras(job.toPersistableBundle())
              .build()
      (context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler).schedule(jobInfo)
    }
  }
}
