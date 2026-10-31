package dev.spradlev.scholarlyfeed

import android.database.sqlite.SQLiteException
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.input.key.Key
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
    private var failImport = false
    private var failWebSearch = false
    private var searchedQuery: String? = null
    private var importedOpmlSources: List<OpmlSource> = emptyList()

    @Before
    fun setUp() {
        sources.clear()
        failVerification = false
        failImport = false
        failWebSearch = false
        searchedQuery = null
        importedOpmlSources = emptyList()
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
                                lastSuccessfulRefreshAtMillis = 1L,
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
                    onImportOpml = { importedSources ->
                        if (failImport) throw SQLiteException("database is full")
                        importedOpmlSources = importedSources
                        OpmlImportResult(importedSources.size, emptyList())
                    },
                    onUpdateSourceMetadata = {},
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
        composeRule.onNodeWithText("Last refreshed", substring = true).assertExists()
        assertEquals(1, sources.size)

        composeRule.onNodeWithContentDescription("Remove Research Digest")
            .performScrollTo()
            .performClick()
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
        composeRule.onNodeWithTag("sourceDiscoverySearch").performTextInput("preprints")
        composeRule.onNodeWithTag("searchWebButton").performClick()
        assertEquals("preprints RSS feed", searchedQuery)

        composeRule.onNodeWithText("bioRxiv").assertExists()
        assertTrue(composeRule.onAllNodesWithText("Nature").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithTag("selectCandidate-bioRxiv").performClick()

        composeRule.onNodeWithTag("sourceTitleInput").assertTextContains("bioRxiv")
        composeRule.onNodeWithTag("sourceUrlInput")
            .assertTextContains("https://connect.biorxiv.org/biorxiv_xml.php?subject=all")
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

    @Test
    fun sourceScreenOffersOpmlImportAndExport() {
        composeRule.onNodeWithText("OPML source lists").assertExists()
        composeRule.onNodeWithText("Import OPML").assertExists()
        composeRule.onNodeWithText("Export OPML").assertExists()
        assertTrue(composeRule.onAllNodesWithText("Google Drive sync").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText(
            "Add the starter feeds or import an OPML file. Every feed is verified before it is saved. You can also export your verified sources.",
        ).assertExists()
    }

    @Test
    fun shiftTabMovesFocusToPreviousControl() {
        composeRule.onNodeWithTag("sourceUrlInput").performClick()
        composeRule.onNodeWithTag("sourceUrlInput").assertIsFocused()

        composeRule.onNodeWithTag("sourceUrlInput").performKeyInput {
            keyDown(Key.ShiftLeft)
            keyDown(Key.Tab)
            keyUp(Key.Tab)
            keyUp(Key.ShiftLeft)
        }

        composeRule.onNodeWithTag("sourceTitleInput").assertIsFocused()
    }

    @Test
    fun addsBundledStarterSourcesThroughExistingImportFlow() {
        composeRule.onNodeWithTag("addStarterSourcesButton").performScrollTo().performClick()

        composeRule.onNodeWithText("Imported 6 sources.").assertExists()
        assertEquals(
            listOf(
                OpmlSource("ScienceDaily", "https://www.sciencedaily.com/rss/all.xml", 1),
                OpmlSource("arXiv Computer Science", "https://rss.arxiv.org/rss/cs", 1),
                OpmlSource("arXiv AI and Machine Learning", "https://rss.arxiv.org/rss/cs.AI", 1),
                OpmlSource(
                    "The Conversation (US)",
                    "https://theconversation.com/us/articles.atom",
                    2,
                ),
                OpmlSource("NPR News", "https://feeds.npr.org/1001/rss.xml", 3),
                OpmlSource(
                    "Washington State Standard",
                    "https://washingtonstatestandard.com/feed/",
                    4,
                ),
            ),
            importedOpmlSources,
        )
    }

    @Test
    fun starterImportShowsDatabaseFailureInsteadOfLeavingTheScreen() {
        failImport = true
        composeRule.onNodeWithTag("addStarterSourcesButton").performScrollTo().performClick()

        composeRule.onNodeWithText(
            "Couldn't save starter sources. Check available device storage and try again.",
        ).assertExists()
    }
}
