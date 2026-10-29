package dev.spradlev.scholarlyfeed

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class FeedSourceManagementScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val sources = mutableStateListOf<FeedSource>()
    private var failVerification = false
    private var failWebSearch = false
    private var searchedQuery: String? = null

    @Before
    fun setUp() {
        sources.clear()
        failVerification = false
        failWebSearch = false
        searchedQuery = null
        composeRule.setContent {
            MaterialTheme {
                FeedSourceManagementScreen(
                    sources = sources,
                    onVerifyAndAdd = { source ->
                        if (failVerification) {
                            throw FeedFetchException("The response is not a valid RSS or Atom feed.")
                        }
                        val parsedFeed = ParsedFeed(
                            title = "Research Feed",
                            resolvedUrl = source.feedUrl,
                            articles = emptyList(),
                        )
                        sources.add(
                            source.copy(
                                feedTitle = parsedFeed.title,
                                verifiedAtMillis = 1L,
                            ),
                        )
                        parsedFeed
                    },
                    onRemoveSource = { source -> sources.remove(source) },
                    onBack = {},
                    onSearchWeb = { query ->
                        searchedQuery = createWebFeedSearchIntent(query)
                            .getStringExtra(android.app.SearchManager.QUERY)
                        !failWebSearch
                    },
                )
            }
        }
    }

    @Test
    fun verifiedSourceCanBeAddedWithLevelAndRemoved() {
        composeRule.onNodeWithTag("sourceTitleInput").performTextInput("Research Digest")
        composeRule.onNodeWithTag("sourceUrlInput").performTextInput("https://example.org/feed.xml")
        composeRule.onNodeWithTag("sourceLevelSelector").performClick()
        composeRule.onNodeWithText("Level 2: Academic commentary and analysis").performClick()
        composeRule.onNodeWithText("Verify and add").performClick()

        composeRule.onNodeWithText("Verified Research Feed and added it.").assertExists()
        composeRule.onNodeWithText("Feed: Research Feed").assertExists()
        composeRule.onNodeWithText("Level 2: Academic commentary and analysis").assertExists()
        assertEquals(1, sources.size)

        composeRule.onNodeWithContentDescription("Remove Research Digest").performClick()
        composeRule.onNodeWithText("No verified sources yet.").assertExists()
        assertEquals(0, sources.size)
    }

    @Test
    fun rejectsNonHttpsSourceWithoutVerifying() {
        composeRule.onNodeWithTag("sourceTitleInput").performTextInput("Research Digest")
        composeRule.onNodeWithTag("sourceUrlInput").performTextInput("http://example.org/feed.xml")
        composeRule.onNodeWithText("Verify and add").performClick()

        composeRule.onNodeWithText("Enter a valid HTTPS URL with a host.").assertExists()
        composeRule.onNodeWithText("No verified sources yet.").assertExists()
        assertEquals(0, sources.size)
    }

    @Test
    fun failedFeedValidationDoesNotAddSource() {
        failVerification = true
        composeRule.onNodeWithTag("sourceTitleInput").performTextInput("Not a feed")
        composeRule.onNodeWithTag("sourceUrlInput").performTextInput("https://example.org/not-a-feed")
        composeRule.onNodeWithText("Verify and add").performClick()

        composeRule.onNodeWithText("The response is not a valid RSS or Atom feed.").assertExists()
        composeRule.onNodeWithText("No verified sources yet.").assertExists()
        assertEquals(0, sources.size)
    }

    @Test
    fun discoverySearchSelectsCandidateAndPrefillsSourceForm() {
        composeRule.onNodeWithTag("discoverSourcesButton").performClick()
        composeRule.onNodeWithText(
            "Search this starter directory or search the web for feed links. Directory entries are candidates, not verified feeds.",
        ).assertExists()
        composeRule.onNodeWithTag("sourceDiscoverySearch").performTextInput("biology")
        composeRule.onNodeWithTag("searchWebButton").performClick()
        assertEquals("biology RSS feed", searchedQuery)

        composeRule.onNodeWithText("bioRxiv").assertExists()
        assertTrue(composeRule.onAllNodesWithText("Nature").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithTag("selectCandidate-bioRxiv").performClick()

        composeRule.onNodeWithTag("sourceTitleInput").assertTextContains("bioRxiv")
        composeRule.onNodeWithTag("sourceLevelSelector").assertTextContains(
            "Level 1: Research and scientific reporting",
        )
        composeRule.onNodeWithTag("sourceUrlInput").assertExists()
        composeRule.onNodeWithText("No verified sources yet.").assertExists()
    }

    @Test
    fun showsRecoveryWhenNoWebSearchAppCanHandleRequest() {
        failWebSearch = true
        composeRule.onNodeWithTag("discoverSourcesButton").performClick()
        composeRule.onNodeWithTag("sourceDiscoverySearch").performTextInput("climate research")
        composeRule.onNodeWithTag("searchWebButton").performClick()

        composeRule.onNodeWithText(
            "No web search app is available. You can still search the built-in directory or enter a feed URL.",
        ).assertExists()
    }
}
