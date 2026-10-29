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
class FeedArticleDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: FeedDatabase
    private val fetcher = CountingFeedFetcher()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FeedDatabase::class.java).build()
        runBlocking {
            val source = FeedSource(
                feedUrl = "https://example.org/feed.xml",
                title = "Research Digest",
                feedTitle = "Research Feed",
                level = 1,
                verifiedAtMillis = 1L,
            )
            val article = FeedArticle(
                id = "article-1",
                title = "A research finding",
                link = "https://example.org/article-1",
                publishedAt = "2026-10-28T12:00:00Z",
                publishedAtMillis = 1793188800000L,
                excerpt = "A short summary from the feed.",
                sourceUrl = source.feedUrl,
                sourceTitle = source.title,
            )
            database.feedSourceDao().insert(source)
            database.feedArticleDao().insertAll(listOf(CachedFeedArticle.from(article, 0)))
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun cachedArticleOpensDetailWithoutFetchingAndBackReturnsToFeed() {
        composeRule.setContent {
            MaterialTheme {
                ScholarlyFeedScreen(database, fetcher)
            }
        }

        composeRule.onNodeWithContentDescription(
            "Open article: A research finding",
            useUnmergedTree = true,
        ).performClick()
        composeRule.onNodeWithText("A research finding").assertExists()
        composeRule.onNodeWithText("Research Digest").assertExists()
        composeRule.onNodeWithText("Published 2026-10-28T12:00:00Z").assertExists()
        composeRule.onNodeWithText("Excerpt from feed").assertExists()
        composeRule.onNodeWithText("A short summary from the feed.").assertExists()
        composeRule.onNodeWithText(
            "Full article text is not included in the cached feed entry. Open the original article to read the full text.",
        ).assertExists()
        composeRule.onNodeWithText("Open original article").assertExists()
        assertEquals(0, fetcher.fetchCount)

        composeRule.onNodeWithText("Back to feed").performClick()
        composeRule.onNodeWithText("Latest articles").assertExists()
        assertEquals(0, fetcher.fetchCount)
    }

    private class CountingFeedFetcher : FeedFetcher {
        var fetchCount = 0

        override suspend fun fetch(url: String): ParsedFeed {
            fetchCount += 1
            error("Opening cached article details must not fetch a feed.")
        }
    }
}
