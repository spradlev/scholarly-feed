package dev.spradlev.scholarlyfeed

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedArticleListPerformanceTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: FeedDatabase

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
            database.feedSourceDao().insert(source)
            database.feedArticleDao().insertAll(
                (0 until ARTICLE_COUNT).map { index ->
                    CachedFeedArticle(
                        sourceUrl = source.feedUrl,
                        articleId = "article-$index",
                        title = "Research article $index",
                        link = "https://example.org/article-$index",
                        publishedAt = null,
                        publishedAtMillis = index.toLong(),
                        excerpt = "Summary $index",
                        sourceTitle = source.title,
                        sortOrder = index,
                    )
                },
            )
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun largeFeedComposesVisibleItemsAndScrollsToOlderArticles() {
        composeRule.setContent {
            MaterialTheme {
                ScholarlyFeedScreen(database, NoOpFeedFetcher)
            }
        }

        val rootWidthDp = with(composeRule.density) {
            composeRule.onRoot().fetchSemanticsNode().boundsInRoot.width.toDp().value
        }
        val contentWidthDp = with(composeRule.density) {
            composeRule.onNodeWithTag("feedArticleList")
                .fetchSemanticsNode().boundsInRoot.width.toDp().value
        }
        assertTrue(contentWidthDp <= READABLE_CONTENT_MAX_WIDTH.value)
        if (rootWidthDp > READABLE_CONTENT_MAX_WIDTH.value) {
            assertEquals(READABLE_CONTENT_MAX_WIDTH.value - 48f, contentWidthDp, 1f)
        }

        assertTrue(
            composeRule.onAllNodesWithText("Research article 0")
                .fetchSemanticsNodes().isEmpty(),
        )
        composeRule.onNodeWithText(
            "Research article ${ARTICLE_COUNT - 1}",
            useUnmergedTree = true,
        ).assertExists()

        composeRule.onNodeWithTag("feedArticleList")
            .performScrollToNode(hasText("Research article 0"))
        composeRule.onNodeWithText("Research article 0", useUnmergedTree = true).assertExists()
    }

    private companion object {
        const val ARTICLE_COUNT = 1_400

        val NoOpFeedFetcher = object : FeedFetcher {
            override suspend fun fetch(url: String): ParsedFeed =
                ParsedFeed("", url, emptyList())
        }
    }
}
