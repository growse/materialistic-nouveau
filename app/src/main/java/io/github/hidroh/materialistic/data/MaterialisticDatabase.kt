package io.github.hidroh.materialistic.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.sqlite.db.SupportSQLiteDatabase
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
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.VisibleForTesting

import java.util.List

@Database(
        entities = {
                MaterialisticDatabase.SavedStory::class.java,
                MaterialisticDatabase.ReadStory::class.java,
                MaterialisticDatabase.Readable::class.java
        },
        version = 4)
abstract class MaterialisticDatabase : RoomDatabase() {

    private const val BASE_URI: String = "content://io.github.hidroh.materialistic"

    private var sInstance: MaterialisticDatabase? = null
    private val mLiveData: MutableLiveData<Uri> = new MutableLiveData<>()

    fun getInstance(context: Context): MaterialisticDatabase {
        if (sInstance == null) {
            sInstance = setupBuilder(Room.databaseBuilder(context.getApplicationContext(),
                    MaterialisticDatabase::class.java,
                    DbConstants.DB_NAME))
                    .build()
        }
        return sInstance
    }

    @VisibleForTesting
    protected fun setupBuilder(builder: Builder<MaterialisticDatabase>): Builder<MaterialisticDatabase> {
        return builder.addMigrations(Migration(3, 4) {
            public void migrate(@NonNull SupportSQLiteDatabase database) {
                database.execSQL(DbConstants.SQL_CREATE_SAVED_TABLE)
                database.execSQL(DbConstants.SQL_INSERT_FAVORITE_SAVED)
                database.execSQL(DbConstants.SQL_DROP_FAVORITE_TABLE)

                database.execSQL(DbConstants.SQL_CREATE_READ_TABLE)
                database.execSQL(DbConstants.SQL_INSERT_VIEWED_READ)
                database.execSQL(DbConstants.SQL_DROP_VIEWED_TABLE)

                database.execSQL(DbConstants.SQL_CREATE_READABLE_TABLE)
                database.execSQL(DbConstants.SQL_INSERT_READABILITY_READABLE)
                database.execSQL(DbConstants.SQL_DROP_READABILITY_TABLE)
            }
        })
    }

    fun getBaseSavedUri(): Uri {
        return Uri.parse(BASE_URI).buildUpon().appendPath("saved").build()
    }

    fun getBaseReadUri(): Uri {
        return Uri.parse(BASE_URI).buildUpon().appendPath("read").build()
    }

    abstract fun getSavedStoriesDao(): SavedStoriesDao

    abstract fun getReadStoriesDao(): ReadStoriesDao

    abstract fun getReadableDao(): ReadableDao

    fun getLiveData(): LiveData<Uri> {
        return mLiveData
    }

    fun setLiveValue(uri: Uri) {
        mLiveData.setValue(uri)
        // clear notification Uri after notifying all active observers
        mLiveData.setValue(null)
    }

    fun createReadUri(itemId: String): Uri {
        return MaterialisticDatabase.getBaseReadUri().buildUpon().appendPath(itemId).build()
    }

    @Entity(tableName = "read")
    open class ReadStory {
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = "_id")
        private var id: Int = 0
        @ColumnInfo(name = "itemid")
        private var itemId: String? = null

        constructor(itemId: String) {
            this.itemId = itemId
        }

        fun getId(): Int {
            return id
        }

        fun setId(id: Int) {
            this.id = id
        }

        fun getItemId(): String {
            return itemId
        }

        fun setItemId(itemId: String) {
            this.itemId = itemId
        }

        override fun equals(o: Any): Boolean {
            if (this == o) return true
            if (o == null || getClass() != o.getClass()) return false

            ReadStory readStory = (ReadStory) o

            if (id != readStory.id) return false
            return itemId != null ? itemId.equals(readStory.itemId) : readStory.itemId == null
        }

        override fun hashCode(): Int {
            int result = id
            result = 31 * result + (itemId != null ? itemId.hashCode() : 0)
            return result
        }
    }

    @Entity
    open class Readable {
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = "_id")
        private var id: Int = 0
        @ColumnInfo(name = "itemid")
        private var itemId: String? = null
        private var content: String? = null

        constructor(itemId: String, content: String) {
            this.itemId = itemId
            this.content = content
        }

        fun getId(): Int {
            return id
        }

        fun setId(id: Int) {
            this.id = id
        }

        fun getItemId(): String {
            return itemId
        }

        fun setItemId(itemId: String) {
            this.itemId = itemId
        }

        fun getContent(): String {
            return content
        }

        fun setContent(content: String) {
            this.content = content
        }

        override fun equals(o: Any): Boolean {
            if (this == o) return true
            if (o == null || getClass() != o.getClass()) return false

            Readable readable = (Readable) o

            if (id != readable.id) return false
            if (itemId != null ? !itemId.equals(readable.itemId) : readable.itemId != null)
                return false
            return content != null ? content.equals(readable.content) : readable.content == null
        }

        override fun hashCode(): Int {
            int result = id
            result = 31 * result + (itemId != null ? itemId.hashCode() : 0)
            result = 31 * result + (content != null ? content.hashCode() : 0)
            return result
        }
    }

    @Entity(tableName = "saved")
    open class SavedStory {
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = "_id")
        private var id: Int = 0
        @ColumnInfo(name = "itemid")
        private var itemId: String? = null
        private var url: String? = null
        private var title: String? = null
        private var time: String? = null

        fun from(story: WebItem): SavedStory {
            SavedStory savedStory = SavedStory()
            savedStory.itemId = story.getId()
            savedStory.url = story.getUrl()
            savedStory.title = story.getDisplayedTitle()
            savedStory.time = String.valueOf(story is Favorite ?
                    ((Favorite) story).getTime() :
                    String.valueOf(System.currentTimeMillis()))
            return savedStory
        }

        fun getId(): Int {
            return id
        }

        fun setId(id: Int) {
            this.id = id
        }

        fun getItemId(): String {
            return itemId
        }

        fun setItemId(itemId: String) {
            this.itemId = itemId
        }

        fun getUrl(): String {
            return url
        }

        fun setUrl(url: String) {
            this.url = url
        }

        fun getTitle(): String {
            return title
        }

        fun setTitle(title: String) {
            this.title = title
        }

        fun getTime(): String {
            return time
        }

        fun setTime(time: String) {
            this.time = time
        }
    }

    @Dao
    interface SavedStoriesDao {
        @Query("SELECT * FROM saved ORDER BY time DESC")
        fun selectAll(): LiveData<List<SavedStory>>

        @Query("SELECT * FROM saved ORDER BY time DESC")
        fun selectAllToCursor(): Cursor

        @Query("SELECT * FROM saved WHERE title LIKE '%' || :query || '%' ORDER BY time DESC")
        fun searchToCursor(query: String): Cursor

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun insert(savedStories: Array<SavedStory>)

        @Query("DELETE FROM saved")
        fun deleteAll(): Int

        @Query("DELETE FROM saved WHERE itemid = :itemId")
        fun deleteByItemId(itemId: String): Int

        @Query("DELETE FROM saved WHERE title LIKE '%' || :query || '%'")
        fun deleteByTitle(query: String): Int

        @Query("SELECT * FROM saved WHERE itemid = :itemId")
        @Nullable
        fun selectByItemId(itemId: String): SavedStory
    }

    @Dao
    interface ReadStoriesDao {
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun insert(readStory: ReadStory)

        @Query("SELECT * FROM read WHERE itemid = :itemId LIMIT 1")
        fun selectByItemId(itemId: String): ReadStory
    }

    @Dao
    interface ReadableDao {
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun insert(readable: Readable)

        @Query("SELECT * FROM readable WHERE itemid = :itemId LIMIT 1")
        fun selectByItemId(itemId: String): Readable
    }

    open class DbConstants {
        const val DB_NAME: String = "Materialistic.db"
        static final String SQL_CREATE_READ_TABLE =
                fun read(KEY: _id INTEGER NOT NULL PRIMARY, TEXT: itemid): "CREATE TABLE
        static final String SQL_CREATE_READABLE_TABLE =
                fun readable(KEY: _id INTEGER NOT NULL PRIMARY, TEXT: itemid, TEXT: content): "CREATE TABLE
        static final String SQL_CREATE_SAVED_TABLE =
                fun saved(KEY: _id INTEGER NOT NULL PRIMARY, TEXT: itemid, TEXT: url, TEXT: title, TEXT: time): "CREATE TABLE
        const val SQL_INSERT_FAVORITE_SAVED: String = "INSERT INTO saved SELECT * FROM favorite"
        const val SQL_INSERT_VIEWED_READ: String = "INSERT INTO read SELECT * FROM viewed"
        const val SQL_INSERT_READABILITY_READABLE: String = "INSERT INTO readable SELECT * FROM readability"
        const val SQL_DROP_FAVORITE_TABLE: String = "DROP TABLE IF EXISTS favorite"
        const val SQL_DROP_VIEWED_TABLE: String = "DROP TABLE IF EXISTS viewed"
        const val SQL_DROP_READABILITY_TABLE: String = "DROP TABLE IF EXISTS readability"
    }

    interface FavoriteEntry : BaseColumns {
        var COLUMN_NAME_ITEM_ID: String = "itemid"
        var COLUMN_NAME_URL: String = "url"
        var COLUMN_NAME_TITLE: String = "title"
        var COLUMN_NAME_TIME: String = "time"
    }
}
