package dev.spradlev.scholarlyfeed

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.TextLayoutResult
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
    private lateinit var context: Context
    private val fetcher = CountingFeedFetcher()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        AppPreferences.save(context, AppPreferences())
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
        AppPreferences.save(context, AppPreferences())
    }

    @Test
    fun cachedArticleOpensDetailWithoutFetchingAndBackReturnsToFeed() {
        composeRule.setContent {
            MaterialTheme {
                ScholarlyFeedScreen(database, fetcher)
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithContentDescription(
                "Open article: A research finding",
                useUnmergedTree = true,
            ).fetchSemanticsNodes().isNotEmpty()
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
        composeRule.onNodeWithText("Upvoted").assertExists()
        composeRule.onNodeWithTag("upvoteArticleButton")
            .assert(
                androidx.compose.ui.test.SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Selected",
                ),
            )
        assertEquals(
            1,
            runBlocking { database.feedArticleDao().observeArticles().first().single().userVote },
        )
        composeRule.onNodeWithTag("downvoteArticleButton").performClick()
        composeRule.onNodeWithText("Downvoted").assertExists()
        composeRule.onNodeWithTag("downvoteArticleButton")
            .assert(
                androidx.compose.ui.test.SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Selected",
                ),
            )
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
    fun feedSearchFiltersLoadedArticlesByTitleSourceAndExcerpt() {
        composeRule.setContent {
            MaterialTheme {
                ScholarlyFeedScreen(database, fetcher)
            }
        }
        val article = composeRule.onNodeWithContentDescription(
            "Open article: A research finding",
            useUnmergedTree = true,
        )
        article.assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Search feed").performClick()
        val searchField = composeRule.onNodeWithTag("feedSearchField")
        searchField.performTextInput("Research Digest")
        article.assertIsDisplayed()

        searchField.performTextClearance()
        searchField.performTextInput("summary from the feed")
        article.assertIsDisplayed()

        searchField.performTextClearance()
        searchField.performTextInput("no matching articles")
        composeRule.onAllNodesWithContentDescription(
            "Open article: A research finding",
            useUnmergedTree = true,
        ).assertCountEquals(0)
        composeRule.onNodeWithText("No articles match \"no matching articles\".")
            .assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Close feed search").performClick()
        article.assertIsDisplayed()
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
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithContentDescription(
                "Open article: A research finding",
                useUnmergedTree = true,
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription(
            "Open article: A research finding",
            useUnmergedTree = true,
        ).performClick()

        composeRule.onNodeWithText("A short summary from the feed.").assertExists()
        composeRule.onNodeWithTag("articleSummaryMode")
            .assert(
                androidx.compose.ui.test.SemanticsMatcher.expectValue(
                    SemanticsProperties.Selected,
                    true,
                ),
            )
            .assert(
                androidx.compose.ui.test.SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Tab,
                ),
            )
        composeRule.onNodeWithTag("fullArticleMode")
            .assert(
                androidx.compose.ui.test.SemanticsMatcher.expectValue(
                    SemanticsProperties.Selected,
                    false,
                ),
            )
            .assert(
                androidx.compose.ui.test.SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Tab,
                ),
            )
        assertEquals(0, loadCount)

        composeRule.onNodeWithTag("fullArticleMode").performClick()
        composeRule.onNodeWithTag("fullArticleMode")
            .assert(
                androidx.compose.ui.test.SemanticsMatcher.expectValue(
                    SemanticsProperties.Selected,
                    true,
                ),
            )
        composeRule.onNodeWithTag("articleSummaryMode")
            .assert(
                androidx.compose.ui.test.SemanticsMatcher.expectValue(
                    SemanticsProperties.Selected,
                    false,
                ),
            )
        composeRule.onNodeWithText("The complete readable publisher article.").assertExists()
        composeRule.waitUntil(timeoutMillis = 5_000) { loadCount == 1 }
        assertEquals(1, loadCount)
    }

    @Test
    fun similarArticleCardsOpenTheSelectedArticle() {
        val relatedArticle = FeedArticle(
            id = "article-2",
            title = "Research finding about health outcomes",
            link = "https://example.org/article-2",
            publishedAt = "2026-10-29T12:00:00Z",
            publishedAtMillis = 1793275200000L,
            excerpt = "A related research article.",
            sourceUrl = "https://example.org/another-feed.xml",
            sourceTitle = "Health Research",
        )
        runBlocking {
            database.feedSourceDao().insert(
                FeedSource(
                    feedUrl = relatedArticle.sourceUrl,
                    title = relatedArticle.sourceTitle,
                    feedTitle = relatedArticle.sourceTitle,
                    level = 1,
                    verifiedAtMillis = 1L,
                ),
            )
            database.feedArticleDao().insertAll(
                listOf(CachedFeedArticle.from(relatedArticle, 0)),
            )
        }

        composeRule.setContent {
            MaterialTheme {
                ScholarlyFeedScreen(database, fetcher)
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithContentDescription(
                "Open article: A research finding",
                useUnmergedTree = true,
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription(
            "Open article: A research finding",
            useUnmergedTree = true,
        ).performClick()
        composeRule.onNodeWithText("Similar articles").assertExists()
        composeRule.onNodeWithTag("relatedArticle_article-2")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("articleTitle")
            .assertTextContains("Research finding about health outcomes")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("articleReadingContainer")
            .assert(
                androidx.compose.ui.test.SemanticsMatcher.expectValue(
                    SemanticsProperties.PaneTitle,
                    "Article: Research finding about health outcomes",
                ),
            )
        composeRule.onNodeWithText("A related research article.").assertExists()
    }

    @Test
    fun returningFromArticleRestoresThePreviousFeedPosition() {
        val additionalArticles = (1..30).map { index ->
            FeedArticle(
                id = "article-$index",
                title = "Research article $index",
                link = "https://example.org/article-$index",
                publishedAt = null,
                publishedAtMillis = 0L,
                excerpt = "A research summary for article $index.",
                sourceUrl = "https://example.org/feed.xml",
                sourceTitle = "Research Digest",
            )
        }
        runBlocking {
            database.feedArticleDao().insertAll(
                additionalArticles.map { CachedFeedArticle.from(it, 0) },
            )
        }

        composeRule.setContent {
            MaterialTheme {
                ScholarlyFeedScreen(database, fetcher)
            }
        }
        val targetArticle = composeRule.onNodeWithContentDescription(
            "Open article: Research article 30",
            useUnmergedTree = true,
        )
        targetArticle.performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("articleTitle")
            .assertTextContains("Research article 30")
            .assertIsDisplayed()

        composeRule.onNodeWithText("Back to feed").performClick()

        targetArticle.assertIsDisplayed()
    }

    @Test
    fun arrowKeysScrollArticleWhenScreenReaderTouchExplorationIsOff() {
        runBlocking {
            val saved = database.feedArticleDao().observeArticles().first().single()
            database.feedArticleDao().replaceForSource(
                saved.sourceUrl,
                listOf(saved.copy(excerpt = "A readable research sentence. ".repeat(200))),
            )
        }
        composeRule.setContent {
            MaterialTheme {
                ScholarlyFeedScreen(database, fetcher)
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithContentDescription(
                "Open article: A research finding",
                useUnmergedTree = true,
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription(
            "Open article: A research finding",
            useUnmergedTree = true,
        ).performClick()

        composeRule.onNodeWithTag("articleReadingContainer")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput {
                repeat(6) {
                    keyDown(Key.DirectionDown)
                    keyUp(Key.DirectionDown)
                }
            }

        composeRule.onNodeWithTag("articleTitle").assertIsNotDisplayed()
    }

    @Test
    fun mouseWheelScrollsArticleContent() {
        runBlocking {
            val saved = database.feedArticleDao().observeArticles().first().single()
            database.feedArticleDao().replaceForSource(
                saved.sourceUrl,
                listOf(saved.copy(excerpt = "A readable research sentence. ".repeat(200))),
            )
        }
        composeRule.setContent {
            MaterialTheme {
                ScholarlyFeedScreen(database, fetcher)
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithContentDescription(
                "Open article: A research finding",
                useUnmergedTree = true,
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription(
            "Open article: A research finding",
            useUnmergedTree = true,
        ).performClick()

        composeRule.onNodeWithTag("articleReadingContainer")
            .performMouseInput { scroll(80f) }

        composeRule.onNodeWithTag("articleTitle").assertIsNotDisplayed()
    }

    @Test
    fun feedPreviewUsesUpToFiveLines() {
        runBlocking {
            val saved = database.feedArticleDao().observeArticles().first().single()
            database.feedArticleDao().replaceForSource(
                saved.sourceUrl,
                listOf(saved.copy(excerpt = "A readable research preview. ".repeat(100))),
            )
        }
        val textLayoutResults = mutableListOf<TextLayoutResult>()
        composeRule.setContent {
            MaterialTheme {
                ScholarlyFeedScreen(database, fetcher)
            }
        }

        composeRule.onNodeWithTag("feedArticleSummary", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { getLayoutResult ->
                getLayoutResult(textLayoutResults)
            }

        assertEquals(5, textLayoutResults.single().lineCount)
    }

    @Test
    fun pinchGestureIncreasesPersistedReadingScale() {
        val pageLoader = object : PublisherPageLoader {
            override suspend fun loadContent(articleUrl: String): PublisherPageContent =
                PublisherPageContent(
                    summary = null,
                    articleText = PublisherArticleText(
                        "First readable paragraph in the article text.\n\n" +
                            "Second readable paragraph continues the article.",
                        isTruncated = false,
                    ),
                )
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
        composeRule.onNodeWithTag("fullArticleMode").performClick()

        composeRule.onNodeWithTag("articleReadingContainer").performTouchInput {
            val pinchCenter = center
            down(0, pinchCenter - Offset(40f, 0f))
            down(1, pinchCenter + Offset(40f, 0f))
            moveTo(0, pinchCenter - Offset(70f, 0f), delayMillis = 100)
            moveTo(1, pinchCenter + Offset(70f, 0f), delayMillis = 100)
            up(0)
            up(1)
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            AppPreferences.load(context).readingScale > MIN_READING_SCALE
        }
        assertTrue(AppPreferences.load(context).readingScale > MIN_READING_SCALE)
        composeRule.onNodeWithTag("publisherArticleParagraph_0").assertExists()
        composeRule.onNodeWithTag("publisherArticleParagraph_1").assertExists()
    }

    private class CountingFeedFetcher : FeedFetcher {
        var fetchCount = 0

        override suspend fun fetch(url: String): ParsedFeed {
            fetchCount += 1
            error("Opening cached article details must not fetch a feed.")
        }
    }
}
