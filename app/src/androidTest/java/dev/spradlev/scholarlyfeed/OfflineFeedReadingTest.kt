package dev.spradlev.scholarlyfeed

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineFeedReadingTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: FeedDatabase
    private val networkUnavailableFetcher = NetworkUnavailableFetcher()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "offline-feed-reading-test.db"
        context.deleteDatabase(databaseName)

        val initialDatabase = openDatabase(context, databaseName)
        runBlocking {
            val source = FeedSource(
                feedUrl = "https://example.org/feed.xml",
                title = "Research Digest",
                feedTitle = "Research Feed",
                level = 1,
                verifiedAtMillis = 1L,
                lastSuccessfulRefreshAtMillis = 2L,
            )
            val article = FeedArticle(
                id = "cached-article",
                title = "Cached research finding",
                link = "https://example.org/article",
                publishedAt = "2026-10-28T12:00:00Z",
                publishedAtMillis = 1793188800000L,
                excerpt = "Saved excerpt available offline.",
                sourceUrl = source.feedUrl,
                sourceTitle = source.title,
            )
            initialDatabase.feedSourceDao().insert(source)
            initialDatabase.feedArticleDao().insertAll(listOf(CachedFeedArticle.from(article, 0)))
        }
        initialDatabase.close()

        database = openDatabase(context, databaseName)
    }

    @After
    fun tearDown() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database.close()
        context.deleteDatabase("offline-feed-reading-test.db")
    }

    @Test
    fun reopenedCacheShowsFeedAndArticleDetailWithoutNetworkAccess() {
        composeRule.setContent {
            MaterialTheme {
                ScholarlyFeedScreen(database, networkUnavailableFetcher)
            }
        }

        composeRule.onNodeWithText("Cached research finding", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithContentDescription(
            "Open article: Cached research finding",
            useUnmergedTree = true,
        ).performClick()
        composeRule.onNodeWithText("Cached research finding").assertExists()
        composeRule.onNodeWithText("Research Digest").assertExists()
        composeRule.onNodeWithText("Saved excerpt available offline.").assertExists()
        assertEquals(0, networkUnavailableFetcher.fetchCount)
    }

    private fun openDatabase(context: Context, databaseName: String) =
        Room.databaseBuilder(context, FeedDatabase::class.java, databaseName)
            .addMigrations(
                FeedDatabase.MIGRATION_1_2,
                FeedDatabase.MIGRATION_2_3,
                FeedDatabase.MIGRATION_3_4,
            )
            .build()

    private class NetworkUnavailableFetcher : FeedFetcher {
        var fetchCount = 0

        override suspend fun fetch(url: String): ParsedFeed {
            fetchCount += 1
            throw FeedFetchException("Network unavailable.")
        }
    }
}
