package dev.spradlev.scholarlyfeed

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.Index
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
internal interface FeedSourceDao {
    @Query("SELECT * FROM feed_sources ORDER BY title COLLATE NOCASE")
    fun observeSources(): Flow<List<FeedSource>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(source: FeedSource)

    @Query(
        "UPDATE feed_sources SET lastSuccessfulRefreshAtMillis = :refreshedAtMillis WHERE feedUrl = :feedUrl",
    )
    suspend fun updateLastSuccessfulRefresh(feedUrl: String, refreshedAtMillis: Long)

    @Query("DELETE FROM feed_sources WHERE feedUrl = :feedUrl")
    suspend fun remove(feedUrl: String)
}

@Entity(
    tableName = "feed_articles",
    primaryKeys = ["sourceUrl", "articleId"],
    foreignKeys = [
        ForeignKey(
            entity = FeedSource::class,
            parentColumns = ["feedUrl"],
            childColumns = ["sourceUrl"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("sourceUrl"),
        Index("publishedAtMillis"),
    ],
)
internal data class CachedFeedArticle(
    val sourceUrl: String,
    val articleId: String,
    val title: String,
    val link: String?,
    val publishedAt: String?,
    val publishedAtMillis: Long?,
    val excerpt: String?,
    val sourceTitle: String,
    val sortOrder: Int,
) {
    fun toFeedArticle() = FeedArticle(
        id = articleId,
        title = title,
        link = link,
        publishedAt = publishedAt,
        publishedAtMillis = publishedAtMillis,
        excerpt = excerpt,
        sourceUrl = sourceUrl,
        sourceTitle = sourceTitle,
    )

    companion object {
        fun from(article: FeedArticle, sortOrder: Int) = CachedFeedArticle(
            sourceUrl = article.sourceUrl,
            articleId = article.id,
            title = article.title,
            link = article.link,
            publishedAt = article.publishedAt,
            publishedAtMillis = article.publishedAtMillis,
            excerpt = article.excerpt,
            sourceTitle = article.sourceTitle,
            sortOrder = sortOrder,
        )
    }
}

@Dao
internal interface FeedArticleDao {
    @Query(
        """
        SELECT * FROM feed_articles
        ORDER BY
            publishedAtMillis IS NULL,
            publishedAtMillis DESC,
            sourceTitle COLLATE NOCASE,
            sourceUrl,
            sortOrder
        """,
    )
    fun observeArticles(): Flow<List<CachedFeedArticle>>

    @Query("DELETE FROM feed_articles WHERE sourceUrl = :sourceUrl")
    suspend fun removeForSource(sourceUrl: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(articles: List<CachedFeedArticle>)

    @Transaction
    suspend fun replaceForSource(sourceUrl: String, articles: List<CachedFeedArticle>) {
        removeForSource(sourceUrl)
        if (articles.isNotEmpty()) insertAll(articles)
    }
}

@Database(
    entities = [FeedSource::class, CachedFeedArticle::class],
    version = 3,
    exportSchema = true,
)
internal abstract class FeedDatabase : RoomDatabase() {
    abstract fun feedSourceDao(): FeedSourceDao
    abstract fun feedArticleDao(): FeedArticleDao

    companion object {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `feed_articles` (
                        `sourceUrl` TEXT NOT NULL,
                        `articleId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `link` TEXT,
                        `publishedAt` TEXT,
                        `publishedAtMillis` INTEGER,
                        `excerpt` TEXT,
                        `sourceTitle` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        PRIMARY KEY(`sourceUrl`, `articleId`),
                        FOREIGN KEY(`sourceUrl`) REFERENCES `feed_sources`(`feedUrl`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_feed_articles_sourceUrl` ON `feed_articles` (`sourceUrl`)")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_feed_articles_publishedAtMillis` ON `feed_articles` (`publishedAtMillis`)",
                )
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `feed_sources` ADD COLUMN `lastSuccessfulRefreshAtMillis` INTEGER",
                )
            }
        }
    }
}
