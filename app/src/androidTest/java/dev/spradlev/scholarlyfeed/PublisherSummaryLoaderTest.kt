package dev.spradlev.scholarlyfeed

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class PublisherSummaryLoaderTest {
    @Test
    fun prefersOpenGraphSummaryAndDecodesMarkup() {
        val summary = extractPublisherSummary(
            """
                <html><head>
                <meta name="description" content="Fallback summary">
                <meta content="A &amp; B &lt;strong&gt;finding&lt;/strong&gt;" property="og:description">
                </head></html>
            """.trimIndent(),
        )

        assertEquals("A & B finding", summary)
    }

    @Test
    fun returnsNullWhenPublisherPageHasNoSummaryMetadata() {
        assertNull(extractPublisherSummary("<html><head><title>Article</title></head></html>"))
    }

    @Test
    fun publisherPageLoaderOnlyRequestsPublicHttpsPages() {
        val loader = SafePublisherPageLoader(successfulHtmlClient("<meta name='description' content='A summary'>"))

        try {
            runBlocking { loader.loadContent("http://example.org/article") }
            throw AssertionError("Expected insecure publisher URL to be rejected")
        } catch (exception: IOException) {
            assertTrue(exception.message!!.contains("HTTPS"))
        }
    }

    @Test
    fun loadsSummaryFromPublisherHtmlResponse() = runBlocking {
        val loader = SafePublisherPageLoader(
            successfulHtmlClient("<meta property='og:description' content='Publisher summary'>"),
        )

        assertEquals("Publisher summary", loader.loadContent("https://example.org/article").summary)
    }

    @Test
    fun extractsReadableArticleBlocksWithoutPageNavigation() {
        val articleText = extractPublisherArticleText(
            """
                <html><body>
                <nav><p>Navigation should not appear in the reader.</p></nav>
                <article>
                  <h1>Research findings</h1>
                  <p>The first article paragraph describes a meaningful finding and its context.</p>
                  <p>The second article paragraph explains the study approach and its limitations.</p>
                </article>
                <script>function irrelevant() { return false; }</script>
                </body></html>
            """.trimIndent(),
        )

        assertEquals(
            "Research findings\n\nThe first article paragraph describes a meaningful finding and its context.\n\n" +
                "The second article paragraph explains the study approach and its limitations.",
            articleText?.text,
        )
        assertTrue(articleText?.text?.contains("Navigation") == false)
    }

    @Test
    fun refusesToPresentShortPageTextAsAnArticle() {
        assertNull(extractPublisherArticleText("<article><p>Only a small teaser.</p></article>"))
    }

    private fun successfulHtmlClient(html: String): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .header("Content-Type", "text/html; charset=utf-8")
                    .body(html.toResponseBody("text/html; charset=utf-8".toMediaType()))
                    .build()
            }
            .build()
}
