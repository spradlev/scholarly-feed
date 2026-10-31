package dev.spradlev.scholarlyfeed

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
                excerpt = "<p>A short summary <strong>from</strong> the feed.</p>",
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

        composeRule.onNodeWithTag("feedArticleSummary", useUnmergedTree = true)
            .assertTextContains("A short summary from the feed.")
        composeRule.onNodeWithTag("feedArticleDate", useUnmergedTree = true)
            .assertTextContains(requireNotNull(formatArticleDate(1793188800000L)))
        composeRule.onNodeWithContentDescription(
            "Open article: A research finding",
            useUnmergedTree = true,
        ).performClick()
        composeRule.onNodeWithText("A research finding").assertExists()
        composeRule.onNodeWithText("Research Digest").assertExists()
        val publishedDate = requireNotNull(formatArticleDate(1793188800000L))
        composeRule.onNodeWithText(publishedDate).assertExists()
        composeRule.onNodeWithText("Feed excerpt").assertExists()
        composeRule.onNodeWithText("A short summary from the feed.").assertExists()
        composeRule.onNodeWithText("Was this useful?").assertExists()
        composeRule.onNodeWithText("Your feedback is saved on this device. It won't change your feed yet.")
            .assertExists()
        composeRule.onNodeWithTag("upvoteArticleButton").performClick()
        composeRule.onNodeWithText("↑ Upvoted").assertExists()
        assertEquals(
            1,
            runBlocking { database.feedArticleDao().observeArticles().first().single().userVote },
        )
        composeRule.onNodeWithTag("downvoteArticleButton").performClick()
        composeRule.onNodeWithText("↓ Downvoted").assertExists()
        assertEquals(
            -1,
            runBlocking { database.feedArticleDao().observeArticles().first().single().userVote },
        )
        composeRule.onNodeWithTag("openOriginalArticleButton").assertExists()
        composeRule.onNodeWithText("Open publisher article").assertExists()
        assertEquals(0, fetcher.fetchCount)

        composeRule.onNodeWithText("Back to feed").performClick()
        composeRule.onNodeWithText("Latest articles").assertExists()
        assertEquals(0, fetcher.fetchCount)
    }

    @Test
    fun missingFeedExcerptLoadsPublisherSummaryOnArticleDetail() {
        runBlocking {
            val saved = database.feedArticleDao().observeArticles().first().single()
            database.feedArticleDao().replaceForSource(
                saved.sourceUrl,
                listOf(saved.copy(excerpt = null)),
            )
        }
        var requestedUrl: String? = null
        val pageLoader = object : PublisherPageLoader {
            override suspend fun loadContent(articleUrl: String): PublisherPageContent {
                requestedUrl = articleUrl
                return PublisherPageContent("A summary from the publisher page.", null)
            }
        }

        composeRule.setContent {
            MaterialTheme {
                ScholarlyFeedScreen(database, fetcher, pageLoader)
            }
        }

        composeRule.onNodeWithContentDescription(
            "Open article: A research finding",
            useUnmergedTree = true,
        ).performClick()
        composeRule.onNodeWithText("Publisher summary").assertExists()
        composeRule.onNodeWithText("A summary from the publisher page.").assertExists()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            requestedUrl == "https://example.org/article-1"
        }
        assertTrue(requestedUrl!!.startsWith("https://"))
    }

    @Test
    fun fullArticleModeLoadsPublisherTextOnDemand() {
        var loadCount = 0
        val pageLoader = object : PublisherPageLoader {
            override suspend fun loadContent(articleUrl: String): PublisherPageContent {
                loadCount += 1
                return PublisherPageContent(
                    summary = null,
                    articleText = PublisherArticleText(
                        "The complete readable publisher article.",
                        isTruncated = false,
                    ),
                )
            }
        }

        composeRule.setContent {
            MaterialTheme {
                ScholarlyFeedScreen(database, fetcher, pageLoader)
            }
        }
        composeRule.onNodeWithContentDescription(
            "Open article: A research finding",
            useUnmergedTree = true,
        ).performClick()

        composeRule.onNodeWithText("A short summary from the feed.").assertExists()
        assertEquals(0, loadCount)

        composeRule.onNodeWithTag("fullArticleMode").performClick()
        composeRule.onNodeWithText("The complete readable publisher article.").assertExists()
        composeRule.waitUntil(timeoutMillis = 5_000) { loadCount == 1 }
        assertEquals(1, loadCount)
    }

    private class CountingFeedFetcher : FeedFetcher {
        var fetchCount = 0

        override suspend fun fetch(url: String): ParsedFeed {
            fetchCount += 1
            error("Opening cached article details must not fetch a feed.")
        }
    }
}
