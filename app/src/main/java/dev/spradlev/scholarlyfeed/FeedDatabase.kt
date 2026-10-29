package dev.spradlev.scholarlyfeed

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
internal interface FeedSourceDao {
    @Query("SELECT * FROM feed_sources ORDER BY title COLLATE NOCASE")
    fun observeSources(): Flow<List<FeedSource>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(source: FeedSource)

    @Query("DELETE FROM feed_sources WHERE feedUrl = :feedUrl")
    suspend fun remove(feedUrl: String)
}

@Database(
    entities = [FeedSource::class],
    version = 1,
    exportSchema = true,
)
internal abstract class FeedDatabase : RoomDatabase() {
    abstract fun feedSourceDao(): FeedSourceDao
}
