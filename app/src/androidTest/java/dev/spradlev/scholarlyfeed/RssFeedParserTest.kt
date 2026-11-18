// SPDX-License-Identifier: AGPL-3.0-or-later

package dev.spradlev.scholarlyfeed

import com.prof18.rssparser.RssParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Assert.assertNull
import org.junit.Test

class RssFeedParserTest {
    private val parser = RssParser()

    @Test
    fun parsesRssAndSortsDatedArticlesBeforeUndated() = runBlocking {
        val feed = parseFeedDocument(parser, RSS_FEED, "https://feeds.example.org/research.xml")

        assertEquals("Research Digest", feed.title)
        assertEquals(listOf("New finding", "Older finding", "Undated item"), feed.articles.map { it.title })
        assertEquals("A short research summary.", feed.articles.first().excerpt)
        assertNull(feed.articles.last().publishedAtMillis)
    }

    @Test
    fun parsesAtomFeedAndKeepsArticleLink() = runBlocking {
        val feed = parseFeedDocument(parser, ATOM_FEED, "https://feeds.example.org/atom")

        assertEquals("Analysis Journal", feed.title)
        assertEquals(1, feed.articles.size)
        assertEquals("https://journal.example.org/articles/42", feed.articles.single().link)
        assertEquals("A brief excerpt.", feed.articles.single().excerpt)
    }

    @Test
    fun removesDuplicateEntriesByGuidAndCanonicalUrl() = runBlocking {
        val byGuid = parseFeedDocument(parser, DUPLICATE_GUID_FEED, "https://feeds.example.org/research.xml")
        val byUrl = parseFeedDocument(parser, DUPLICATE_URL_FEED, "https://feeds.example.org/research.xml")

        assertEquals(listOf("Keep first GUID entry"), byGuid.articles.map { it.title })
        assertEquals(listOf("Keep first URL entry"), byUrl.articles.map { it.title })
    }

    @Test
    fun preservesDistinctArticlesWithSimilarOrMissingIdentifiers() = runBlocking {
        val feed = parseFeedDocument(parser, DISTINCT_ARTICLES_FEED, "https://feeds.example.org/research.xml")

        assertEquals(
            listOf("https://journal.example.org/articles/one", "https://journal.example.org/articles/two"),
            feed.articles.mapNotNull { it.link },
        )
        val withoutIdentifiers = parseFeedDocument(
            parser,
            ARTICLES_WITHOUT_IDENTIFIERS,
            "https://feeds.example.org/research.xml",
        )
        assertEquals(2, withoutIdentifiers.articles.size)
        assertEquals(2, withoutIdentifiers.articles.map { it.id }.distinct().size)
    }

    @Test
    fun rejectsXmlThatIsNotAFeed() {
        runBlocking {
            try {
                parseFeedDocument(parser, "<html><title>Not a feed</title></html>", "https://example.org")
                fail("Expected invalid XML to be rejected")
            } catch (_: FeedFetchException) {
                // Expected: parser failures are presented as feed verification errors.
            }
        }
    }

    private companion object {
        const val RSS_FEED = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"><channel>
              <title>Research Digest</title>
              <item>
                <guid>new</guid><title>New finding</title>
                <link>https://journal.example.org/new</link>
                <pubDate>Tue, 28 Oct 2025 12:00:00 GMT</pubDate>
                <description><![CDATA[A short research summary.]]></description>
              </item>
              <item>
                <guid>old</guid><title>Older finding</title>
                <pubDate>Mon, 27 Oct 2025 12:00:00 GMT</pubDate>
              </item>
              <item><guid>undated</guid><title>Undated item</title></item>
            </channel></rss>
        """

        const val ATOM_FEED = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
              <id>https://journal.example.org/</id>
              <title>Analysis Journal</title>
              <updated>2025-10-28T12:00:00Z</updated>
              <entry>
                <id>urn:article:42</id>
                <title>New analysis</title>
                <link rel="alternate" href="https://journal.example.org/articles/42"/>
                <updated>2025-10-28T12:00:00Z</updated>
                <summary>A brief excerpt.</summary>
              </entry>
            </feed>
        """

        const val DUPLICATE_GUID_FEED = """
            <rss version="2.0"><channel><title>Research Digest</title>
              <item><guid>shared-id</guid><title>Keep first GUID entry</title><link>https://journal.example.org/one</link></item>
              <item><guid>shared-id</guid><title>Duplicate GUID entry</title><link>https://journal.example.org/two</link></item>
            </channel></rss>
        """

        const val DUPLICATE_URL_FEED = """
            <rss version="2.0"><channel><title>Research Digest</title>
              <item><guid>first-id</guid><title>Keep first URL entry</title><link>https://journal.example.org/article#section-one</link></item>
              <item><guid>second-id</guid><title>Duplicate URL entry</title><link>https://journal.example.org/article#section-two</link></item>
            </channel></rss>
        """

        const val DISTINCT_ARTICLES_FEED = """
            <rss version="2.0"><channel><title>Research Digest</title>
              <item><guid>first-id</guid><title>Same title</title><link>https://journal.example.org/articles/one</link></item>
              <item><guid>second-id</guid><title>Same title</title><link>https://journal.example.org/articles/two</link></item>
            </channel></rss>
        """

        const val ARTICLES_WITHOUT_IDENTIFIERS = """
            <rss version="2.0"><channel><title>Research Digest</title>
              <item><title>Unidentified article</title></item>
              <item><title>Unidentified article</title></item>
            </channel></rss>
        """
    }
}
