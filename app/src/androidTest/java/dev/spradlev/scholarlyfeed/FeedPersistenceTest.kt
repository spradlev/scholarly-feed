package dev.spradlev.scholarlyfeed

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedPersistenceTest {
   @Test
   fun verifiedSourceAndFetchedArticlesSurviveDatabaseReopenAndRemoval(): Unit {
       runBlocking {
           val context = ApplicationProvider.getApplicationContext<Context>()
           val databaseName = "feed-source-persistence-test.db"
           context.deleteDatabase(databaseName)
           val source = sampleSource()
           val article = sampleArticle(source)
           val firstDatabase = Room.databaseBuilder(context, FeedDatabase::class.java, databaseName)
               .addMigrations(FeedDatabase.MIGRATION_1_2, FeedDatabase.MIGRATION_2_3)
               .build()
           try {
               FeedRepository(
                   firstDatabase,
                   SingleFeedFetcher(ParsedFeed("Research Feed", source.feedUrl, listOf(article))),
                   nowMillis = { 1234L },
               ).verifyAndAdd(source)
           } finally {
               firstDatabase.close()
           }

           val reopenedDatabase = Room.databaseBuilder(context, FeedDatabase::class.java, databaseName)
               .addMigrations(FeedDatabase.MIGRATION_1_2, FeedDatabase.MIGRATION_2_3)
               .build()
           try {
               val dao = reopenedDatabase.feedSourceDao()
               val articleDao = reopenedDatabase.feedArticleDao()
               assertEquals(
                   listOf(
                       source.copy(
                           feedTitle = "Research Feed",
                           verifiedAtMillis = 1234L,
                           lastSuccessfulRefreshAtMillis = 1234L,
                       ),
                   ),
                   dao.observeSources().first(),
               )
               assertEquals(listOf(article), articleDao.observeArticles().first().map(CachedFeedArticle::toFeedArticle))

               FeedRepository(reopenedDatabase, SingleFeedFetcher()).removeSource(source)
               assertTrue(dao.observeSources().first().isEmpty())
               assertTrue(articleDao.observeArticles().first().isEmpty())
           } finally {
               reopenedDatabase.close()
               context.deleteDatabase(databaseName)
           }
       }
   }

   @Test
   fun migrationFromVersionOnePreservesSavedSources() {
       val context = ApplicationProvider.getApplicationContext<Context>()
       val databaseName = "feed-source-migration-test.db"
       context.deleteDatabase(databaseName)
       val legacyDatabase = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
       try {
           legacyDatabase.execSQL(
               """
               CREATE TABLE feed_sources (
                   feedUrl TEXT NOT NULL PRIMARY KEY,
                   title TEXT NOT NULL,
                   feedTitle TEXT NOT NULL,
                   level INTEGER NOT NULL,
                   verifiedAtMillis INTEGER NOT NULL
               )
               """.trimIndent(),
           )
           legacyDatabase.execSQL(
               "CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)",
           )
           legacyDatabase.execSQL(
               "INSERT INTO room_master_table (id, identity_hash) VALUES (42, '080f0be87c05f3ccc427322c8b36bbbb')",
           )
           legacyDatabase.execSQL(
               """
               INSERT INTO feed_sources (feedUrl, title, feedTitle, level, verifiedAtMillis)
               VALUES ('https://example.org/feed.xml', 'Research Digest', 'Research Feed', 2, 1234)
               """.trimIndent(),
           )
           legacyDatabase.version = 1
       } finally {
           legacyDatabase.close()
       }

       val migratedDatabase = Room.databaseBuilder(context, FeedDatabase::class.java, databaseName)
           .addMigrations(FeedDatabase.MIGRATION_1_2, FeedDatabase.MIGRATION_2_3)
           .build()
       runBlocking {
           try {
               assertEquals(
                   "Research Digest",
                   migratedDatabase.feedSourceDao().observeSources().first().single().title,
               )
               assertEquals(
                   null,
                   migratedDatabase.feedSourceDao().observeSources().first().single().lastSuccessfulRefreshAtMillis,
               )
               assertTrue(migratedDatabase.feedArticleDao().observeArticles().first().isEmpty())
           } finally {
               migratedDatabase.close()
               context.deleteDatabase(databaseName)
           }
       }
   }

   @Test
   fun failedRefreshKeepsCachedArticlesAndSuccessfulRefreshReplacesThem() {
       runBlocking {
           val context = ApplicationProvider.getApplicationContext<Context>()
           val database = Room.inMemoryDatabaseBuilder(context, FeedDatabase::class.java).build()
           val source = sampleSource()
           val oldArticle = sampleArticle(source)
           val fetcher = SingleFeedFetcher(ParsedFeed("Research Feed", source.feedUrl, listOf(oldArticle)))
           var currentTimeMillis = 1000L
           try {
               val repository = FeedRepository(database, fetcher) { currentTimeMillis }
               repository.verifyAndAdd(source)
               assertEquals(
                   1000L,
                   database.feedSourceDao().observeSources().first().single().lastSuccessfulRefreshAtMillis,
               )

               currentTimeMillis = 2000L
               fetcher.failure = true
               val failedRefresh = repository.refreshSources(listOf(source))
               assertEquals(1, failedRefresh.errors.size)
               assertEquals(listOf(oldArticle), repository.observeArticles().first())
               assertEquals(
                   1000L,
                   database.feedSourceDao().observeSources().first().single().lastSuccessfulRefreshAtMillis,
               )

               val newArticle = oldArticle.copy(id = "new", title = "New finding")
               currentTimeMillis = 3000L
               fetcher.failure = false
               fetcher.feed = ParsedFeed("Research Feed", source.feedUrl, listOf(newArticle))
               val successfulRefresh = repository.refreshSources(listOf(source))
               assertTrue(successfulRefresh.errors.isEmpty())
               assertEquals(listOf(newArticle), repository.observeArticles().first())
               assertEquals(
                   3000L,
                   database.feedSourceDao().observeSources().first().single().lastSuccessfulRefreshAtMillis,
               )
           } finally {
               database.close()
           }
       }
   }

   private fun sampleSource() = FeedSource(
       feedUrl = "https://example.org/feed.xml",
       title = "Research Digest",
       feedTitle = "",
       level = 2,
       verifiedAtMillis = 0L,
   )

   private fun sampleArticle(source: FeedSource) = FeedArticle(
       id = "article-1",
       title = "A research finding",
       link = "https://example.org/article-1",
       publishedAt = "2026-10-28T12:00:00Z",
       publishedAtMillis = 1793188800000L,
       excerpt = "A short summary.",
       sourceUrl = source.feedUrl,
       sourceTitle = source.title,
   )

   private class SingleFeedFetcher(
       var feed: ParsedFeed = ParsedFeed("", "", emptyList()),
   ) : FeedFetcher {
       var failure = false

       override suspend fun fetch(url: String): ParsedFeed {
           if (failure) throw FeedFetchException("Network unavailable.")
           return feed
       }
   }
}
