package dev.spradlev.scholarlyfeed

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeFeedFetcherSizeLimitTest {
    @Test
    fun acceptsFeedAtFourMiBLimit() = runBlocking {
        val parsed = fetcherWithFeed(feedOfSize(MAX_FEED_BYTES)).fetch(FEED_URL)

        assertEquals("Fixture feed", parsed.title)
    }

    @Test
    fun rejectsFeedAboveFourMiBLimit() = runBlocking {
        try {
            fetcherWithFeed(feedOfSize(MAX_FEED_BYTES + 1)).fetch(FEED_URL)
            throw AssertionError("Expected oversized feed to be rejected")
        } catch (exception: FeedFetchException) {
            assertTrue(exception.message!!.contains("4 MiB"))
        }
    }

    private fun fetcherWithFeed(xml: String): SafeFeedFetcher {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .header("Content-Type", "application/rss+xml")
                    .body(xml.toResponseBody("application/rss+xml".toMediaType()))
                    .build()
            }
            .build()
        return SafeFeedFetcher(client = client)
    }

    private fun feedOfSize(size: Int): String {
        val prefix = "<rss version=\"2.0\"><channel><title>Fixture feed</title>"
        val suffix = "</channel></rss>"
        return prefix + " ".repeat(size - prefix.length - suffix.length) + suffix
    }

    private companion object {
        const val FEED_URL = "https://example.org/feed.xml"
        const val MAX_FEED_BYTES = 4 * 1024 * 1024
    }
}
