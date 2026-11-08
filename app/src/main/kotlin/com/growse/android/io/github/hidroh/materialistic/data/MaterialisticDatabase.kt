package com.growse.android.io.github.hidroh.materialistic.data

import android.content.Context
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/*
 * Room derives the schema from these entities and checks it against the tables on open, so column
 * names and nullability must keep matching the SQL in DbConstants and the shipped databases.
 */
@Database(
    entities =
        [
            MaterialisticDatabase.SavedStory::class,
            MaterialisticDatabase.ReadStory::class,
            MaterialisticDatabase.Readable::class,
            MaterialisticDatabase.CachedItem::class,
        ],
    version = 5,
)
abstract class MaterialisticDatabase : RoomDatabase() {
  private val notifications = MutableLiveData<Uri?>()

  abstract val savedStoriesDao: SavedStoriesDao

  abstract val readStoriesDao: ReadStoriesDao

  abstract val readableDao: ReadableDao

  abstract val itemCacheDao: ItemCacheDao

  /** Emits a [Uri] describing each change to saved or read stories, then null. */
  val liveData: LiveData<Uri?>
    get() = notifications

  fun setLiveValue(uri: Uri?) {
    notifications.value = uri
    // clear notification Uri after notifying all active observers
    notifications.value = null
  }

  fun createReadUri(itemId: String?): Uri = baseReadUri.buildUpon().appendPath(itemId).build()

  @Entity(tableName = "read")
  data class ReadStory(
      @ColumnInfo(name = "itemid") val itemId: String?,
      @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") val id: Int = 0,
  )

  @Entity
  data class Readable(
      @ColumnInfo(name = "itemid") val itemId: String?,
      val content: String?,
      @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") val id: Int = 0,
  )

  @Entity(tableName = "saved")
  data class SavedStory(
      @ColumnInfo(name = "itemid") val itemId: String?,
      val url: String?,
      val title: String?,
      val time: String?,
      @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") val id: Int = 0,
  ) {
    companion object {
      fun from(story: WebItem) =
          SavedStory(
              itemId = story.id,
              url = story.url,
              title = story.displayedTitle,
              time = (if (story is Favorite) story.time else System.currentTimeMillis()).toString(),
          )
    }
  }

  @Entity(tableName = "item_cache")
  class CachedItem(@PrimaryKey @ColumnInfo(name = "itemid") var itemId: String) {
    var type: String? = null
    var by: String? = null
    var time = 0L
    var text: String? = null
    var parent = 0L
    var url: String? = null
    var title: String? = null
    var kids: String? = null
    var score = 0
    var descendants = 0

    @ColumnInfo(name = "dead") var isDead = false

    @ColumnInfo(name = "deleted") var isDeleted = false

    @ColumnInfo(name = "fetched_at") var fetchedAt = 0L
  }

  @Dao
  interface SavedStoriesDao {
    @Query("SELECT * FROM saved ORDER BY time DESC") fun selectAll(): LiveData<List<SavedStory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insert(vararg savedStories: SavedStory)

    @Query("DELETE FROM saved") fun deleteAll(): Int

    @Query("DELETE FROM saved WHERE itemid = :itemId") fun deleteByItemId(itemId: String?): Int

    @Query("DELETE FROM saved WHERE title LIKE '%' || :query || '%'")
    fun deleteByTitle(query: String?): Int

    @Query("SELECT * FROM saved WHERE itemid = :itemId")
    fun selectByItemId(itemId: String?): SavedStory?
  }

  @Dao
  interface ReadStoriesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insert(readStory: ReadStory)

    @Query("SELECT * FROM read WHERE itemid = :itemId LIMIT 1")
    fun selectByItemId(itemId: String?): ReadStory?
  }

  @Dao
  interface ReadableDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insert(readable: Readable)

    @Query("SELECT * FROM readable WHERE itemid = :itemId LIMIT 1")
    fun selectByItemId(itemId: String?): Readable?
  }

  @Dao
  interface ItemCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insert(item: CachedItem)

    @Query("SELECT * FROM item_cache WHERE itemid = :itemId LIMIT 1")
    fun selectByItemId(itemId: String?): CachedItem?
  }

  internal object DbConstants {
    const val DB_NAME = "Materialistic.db"
    const val SQL_CREATE_READ_TABLE =
        "CREATE TABLE read (_id INTEGER NOT NULL PRIMARY KEY, itemid TEXT)"
    const val SQL_CREATE_READABLE_TABLE =
        "CREATE TABLE readable (_id INTEGER NOT NULL PRIMARY KEY, itemid TEXT, content TEXT)"
    const val SQL_CREATE_SAVED_TABLE =
        "CREATE TABLE saved (_id INTEGER NOT NULL PRIMARY KEY, itemid TEXT, url TEXT, title TEXT, time TEXT)"
    const val SQL_INSERT_FAVORITE_SAVED = "INSERT INTO saved SELECT * FROM favorite"
    const val SQL_INSERT_VIEWED_READ = "INSERT INTO read SELECT * FROM viewed"
    const val SQL_INSERT_READABILITY_READABLE = "INSERT INTO readable SELECT * FROM readability"
    const val SQL_DROP_FAVORITE_TABLE = "DROP TABLE IF EXISTS favorite"
    const val SQL_DROP_VIEWED_TABLE = "DROP TABLE IF EXISTS viewed"
    const val SQL_DROP_READABILITY_TABLE = "DROP TABLE IF EXISTS readability"
    const val SQL_CREATE_ITEM_CACHE_TABLE =
        "CREATE TABLE IF NOT EXISTS item_cache (" +
            "itemid TEXT NOT NULL PRIMARY KEY, " +
            "type TEXT, " +
            "by TEXT, " +
            "time INTEGER NOT NULL, " +
            "text TEXT, " +
            "parent INTEGER NOT NULL, " +
            "url TEXT, " +
            "title TEXT, " +
            "kids TEXT, " +
            "score INTEGER NOT NULL, " +
            "descendants INTEGER NOT NULL, " +
            "dead INTEGER NOT NULL, " +
            "deleted INTEGER NOT NULL, " +
            "fetched_at INTEGER NOT NULL)"
  }

  /** Column names of the saved stories table, for reading it through a cursor. */
  object FavoriteEntry {
    const val COLUMN_NAME_ITEM_ID = "itemid"
    const val COLUMN_NAME_URL = "url"
    const val COLUMN_NAME_TITLE = "title"
    const val COLUMN_NAME_TIME = "time"
  }

  companion object {
    private const val BASE_URI = "content://io.github.hidroh.materialistic"

    @Volatile private var instance: MaterialisticDatabase? = null

    @JvmStatic
    fun getInstance(context: Context): MaterialisticDatabase =
        instance
            ?: synchronized(this) {
              instance
                  ?: setupBuilder(
                          Room.databaseBuilder(
                              context.applicationContext,
                              MaterialisticDatabase::class.java,
                              DbConstants.DB_NAME,
                          ),
                      )
                      .build()
                      .also { instance = it }
            }

    @VisibleForTesting
    internal fun setupBuilder(
        builder: Builder<MaterialisticDatabase>,
    ): Builder<MaterialisticDatabase> = builder.addMigrations(MIGRATION_3_4, MIGRATION_4_5)

    /** Moves the pre-Room favorite, viewed and readability tables into Room's tables. */
    private val MIGRATION_3_4 =
        object : Migration(3, 4) {
          override fun migrate(db: SupportSQLiteDatabase) {
            with(DbConstants) {
              listOf(
                      SQL_CREATE_SAVED_TABLE,
                      SQL_INSERT_FAVORITE_SAVED,
                      SQL_DROP_FAVORITE_TABLE,
                      SQL_CREATE_READ_TABLE,
                      SQL_INSERT_VIEWED_READ,
                      SQL_DROP_VIEWED_TABLE,
                      SQL_CREATE_READABLE_TABLE,
                      SQL_INSERT_READABILITY_READABLE,
                      SQL_DROP_READABILITY_TABLE,
                  )
                  .forEach(db::execSQL)
            }
          }
        }

    private val MIGRATION_4_5 =
        object : Migration(4, 5) {
          override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(DbConstants.SQL_CREATE_ITEM_CACHE_TABLE)
          }
        }

    val baseSavedUri: Uri
      get() = BASE_URI.toUri().buildUpon().appendPath("saved").build()

    val baseReadUri: Uri
      get() = BASE_URI.toUri().buildUpon().appendPath("read").build()
  }
}
