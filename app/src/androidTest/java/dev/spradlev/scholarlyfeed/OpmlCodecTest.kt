// SPDX-License-Identifier: AGPL-3.0-or-later

package dev.spradlev.scholarlyfeed

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpmlCodecTest {
    private lateinit var database: FeedDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FeedDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun exportAndImportPreservesNamesUrlsAndSourceLevels() {
        val sources = listOf(
            source("Research & Science", "https://example.org/research.xml", 1),
            source("Local affairs", "https://example.org/local.xml", 4),
        )

        val imported = parseOpml(createOpml(sources))

        assertEquals(
            listOf(
                OpmlSource("Research & Science", "https://example.org/research.xml", 1),
                OpmlSource("Local affairs", "https://example.org/local.xml", 4),
            ),
            imported,
        )
    }

    @Test
    fun standardOpmlWithoutScholarlyLevelDefaultsToLevelOne() {
        val imported = parseOpml(
            """
            <opml version="2.0">
              <head><title>Feeds</title></head>
              <body>
                <outline text="Science Source" type="rss" xmlUrl="https://example.org/feed.xml" />
              </body>
            </opml>
            """.trimIndent(),
        )

        assertEquals(
            listOf(OpmlSource("Science Source", "https://example.org/feed.xml", 1)),
            imported,
        )
    }

    @Test
    fun rejectsNonOpmlDocumentsAndInsecureFeedUrls() {
        assertThrows(OpmlFormatException::class.java) {
            parseOpml("<feeds><body/></feeds>")
        }
        assertThrows(OpmlFormatException::class.java) {
            parseOpml(
                """
                <opml version="2.0">
                  <body><outline text="Insecure" xmlUrl="http://example.org/feed.xml" /></body>
                </opml>
                """.trimIndent(),
            )
        }
    }

    @Test
    fun rejectsInvalidSourceLevels() {
        assertThrows(OpmlFormatException::class.java) {
            parseOpml(
                """
                <opml version="2.0">
                  <body>
                    <outline text="Invalid" xmlUrl="https://example.org/feed.xml"
                      scholarlyFeedLevel="5" />
                  </body>
                </opml>
                """.trimIndent(),
            )
        }
    }

    @Test
    fun importingOpmlVerifiesEachFeedBeforeSavingAndReportsFailures() {
        runBlocking {
            val requestedUrls = mutableListOf<String>()
            val repository = FeedRepository(
                database,
                object : FeedFetcher {
                    override suspend fun fetch(url: String): ParsedFeed {
                        requestedUrls += url
                        if (url == "https://example.org/broken.xml") {
                            throw FeedFetchException("The response is not a valid RSS or Atom feed.")
                        }
                        return ParsedFeed("Verified feed", url, emptyList())
                    }
                },
                nowMillis = { 1234L },
            )

            val result = repository.importOpml(
                listOf(
                    OpmlSource("Research Digest", "https://example.org/research.xml", 1),
                    OpmlSource("Broken feed", "https://example.org/broken.xml", 3),
                ),
            )

            assertEquals(1, result.importedCount)
            assertEquals(1, result.errors.size)
            assertTrue(result.errors.single().contains("Broken feed"))
            assertEquals(
                listOf(
                    "https://example.org/research.xml",
                    "https://example.org/broken.xml",
                ),
                requestedUrls,
            )
            val savedSource = database.feedSourceDao().observeSources().first().single()
            assertEquals("Research Digest", savedSource.title)
            assertEquals(1, savedSource.level)
            assertEquals(1234L, savedSource.lastSuccessfulRefreshAtMillis)
        }
    }

    @Test
    fun importingOpmlSkipsDuplicateFeedUrls() {
        runBlocking {
            var fetchCount = 0
            val repository = FeedRepository(
                database,
                object : FeedFetcher {
                    override suspend fun fetch(url: String): ParsedFeed {
                        fetchCount++
                        return ParsedFeed("Verified feed", url, emptyList())
                    }
                },
            )
            val feed = OpmlSource("Research Digest", "https://example.org/research.xml", 1)

            val result = repository.importOpml(listOf(feed, feed.copy(title = "Duplicate name")))

            assertEquals(1, result.importedCount)
            assertEquals(1, result.errors.size)
            assertTrue(result.errors.single().contains("Duplicate name"))
            assertEquals(1, fetchCount)
        }
    }

    private fun source(title: String, feedUrl: String, level: Int) = FeedSource(
        feedUrl = feedUrl,
        title = title,
        feedTitle = title,
        level = level,
        verifiedAtMillis = 1L,
    )
}
