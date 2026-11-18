// SPDX-License-Identifier: AGPL-3.0-or-later

package dev.spradlev.scholarlyfeed

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
class FeedFocusFilteringTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: FeedDatabase
    private val fetcher = NoOpFeedFetcher()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FeedDatabase::class.java).build()
        runBlocking {
            val researchSource = source(level = 1)
            val civicSource = source(
                feedUrl = "https://example.org/civic-feed.xml",
                title = "Civic Digest",
                level = 3,
            )
            database.feedSourceDao().insert(researchSource)
            database.feedSourceDao().insert(civicSource)
            database.feedArticleDao().insertAll(
                listOf(
                    article(researchSource, id = "research", title = "Research finding", order = 0),
                    article(civicSource, id = "civic", title = "Civic report", order = 1),
                ),
            )
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun focusModeShowsOnlyLevelOneAndTurningItOffRestoresAllCachedArticles() {
        composeRule.setContent {
            MaterialTheme {
                ScholarlyFeedScreen(database, fetcher)
            }
        }

        composeRule.onNodeWithText("Research finding", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Civic report", useUnmergedTree = true).assertExists()

        composeRule.onNodeWithTag("bottomFocusModeButton").performClick()
        composeRule.onNodeWithText("Research only").assertExists()
        composeRule.onNodeWithText("Research finding", useUnmergedTree = true).assertExists()
        assertTrue(
            composeRule.onAllNodesWithText("Civic report", useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty(),
        )
        assertEquals(0, fetcher.fetchCount)

        composeRule.onNodeWithTag("bottomFocusModeButton").performClick()
        composeRule.onNodeWithText("Research finding", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Civic report", useUnmergedTree = true).assertExists()
        assertEquals(0, fetcher.fetchCount)
        runBlocking {
            assertEquals(2, database.feedArticleDao().observeArticles().first().size)
        }
    }

    @Test
    fun focusModeExplainsWhenNoLevelOneArticlesAreAvailable() {
        runBlocking {
            database.feedSourceDao().remove("https://example.org/research-feed.xml")
        }
        composeRule.setContent {
            MaterialTheme {
                ScholarlyFeedScreen(database, fetcher)
            }
        }

        composeRule.onNodeWithTag("bottomFocusModeButton").performClick()
        composeRule.onNodeWithText(
            "No Level 1 articles yet. Turn off Focus Mode to see all sources.",
        ).assertExists()
    }

    private fun source(
        feedUrl: String = "https://example.org/research-feed.xml",
        title: String = "Research Digest",
        level: Int,
    ) = FeedSource(
        feedUrl = feedUrl,
        title = title,
        feedTitle = title,
        level = level,
        verifiedAtMillis = 1L,
    )

    private fun article(
        source: FeedSource,
        id: String,
        title: String,
        order: Int,
    ) = CachedFeedArticle(
        sourceUrl = source.feedUrl,
        articleId = id,
        title = title,
        link = "https://example.org/$id",
        publishedAt = null,
        publishedAtMillis = null,
        excerpt = null,
        sourceTitle = source.title,
        sortOrder = order,
    )

    private class NoOpFeedFetcher : FeedFetcher {
        var fetchCount = 0

        override suspend fun fetch(url: String): ParsedFeed {
            fetchCount += 1
            return ParsedFeed("", url, emptyList())
        }
    }
}
