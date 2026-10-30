package dev.spradlev.scholarlyfeed

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleDriveSourceStoreTest {
    private val localSources = listOf(
        FeedSource(
            feedUrl = "https://example.org/shared.xml",
            title = "Local title",
            feedTitle = "Example",
            level = 1,
            verifiedAtMillis = 1L,
        ),
        FeedSource(
            feedUrl = "https://example.org/local.xml",
            title = "Local only",
            feedTitle = "Example",
            level = 2,
            verifiedAtMillis = 1L,
        ),
    )
    private val driveSources = listOf(
        OpmlSource("Drive title", "https://example.org/shared.xml", 3),
        OpmlSource("Drive only", "https://example.org/drive.xml", 4),
    )

    @Test
    fun syncPlanIdentifiesRemoteOnlySourcesAndMetadataConflictsByFeedUrl() {
        val plan = planSourceSync(localSources, driveSources)

        assertEquals(
            listOf(OpmlSource("Drive only", "https://example.org/drive.xml", 4)),
            plan.driveOnly,
        )
        assertEquals(
            listOf(
                SourceSyncConflict(
                    local = OpmlSource("Local title", "https://example.org/shared.xml", 1),
                    drive = OpmlSource("Drive title", "https://example.org/shared.xml", 3),
                ),
            ),
            plan.conflicts,
        )
    }

    @Test
    fun mergeKeepsLocalConflictValuesByDefaultAndUnionsSourcesWithoutDeleting() {
        val merged = mergeSourceLists(localSources, driveSources, useDriveConflictValues = false)

        assertEquals(3, merged.size)
        assertEquals("Local title", merged.first { it.feedUrl == "https://example.org/shared.xml" }.title)
        assertTrue(merged.any { it.feedUrl == "https://example.org/local.xml" })
        assertTrue(merged.any { it.feedUrl == "https://example.org/drive.xml" })
    }

    @Test
    fun mergeUsesDriveConflictValuesOnlyWhenExplicitlyChosen() {
        val merged = mergeSourceLists(localSources, driveSources, useDriveConflictValues = true)

        assertEquals("Drive title", merged.first { it.feedUrl == "https://example.org/shared.xml" }.title)
        assertEquals(3, merged.first { it.feedUrl == "https://example.org/shared.xml" }.level)
    }

    @Test
    fun readSourceFileUsesAppDataSpaceAndBearerToken() = runBlocking {
        val requests = mutableListOf<Request>()
        val opml = createOpml(
            listOf(
                FeedSource(
                    feedUrl = "https://example.org/feed.xml",
                    title = "Example",
                    feedTitle = "Example",
                    level = 2,
                    verifiedAtMillis = 1L,
                ),
            ),
        )
        val store = GoogleDriveSourceStore(
            httpClient = fakeHttpClient(
                requests,
                listOf(
                    """{"files":[{"id":"file-1","name":"scholarly-feed-sources.opml","modifiedTime":"2026-01-01T00:00:00Z"}]}""",
                    opml,
                ),
            ),
        )

        val file = store.readSourceFile("test-access-token")

        assertEquals("file-1", file?.id)
        assertEquals(listOf(OpmlSource("Example", "https://example.org/feed.xml", 2)), file?.sources)
        assertEquals("Bearer test-access-token", requests[0].header("Authorization"))
        assertEquals("appDataFolder", requests[0].url.queryParameter("spaces"))
        assertEquals("media", requests[1].url.queryParameter("alt"))
    }

    @Test
    fun readSourceFileRejectsMalformedListingInsteadOfTreatingItAsMissing() = runBlocking {
        val store = GoogleDriveSourceStore(
            httpClient = fakeHttpClient(mutableListOf(), listOf("{}")),
        )

        try {
            store.readSourceFile("test-access-token")
            throw AssertionError("Expected malformed listing to fail.")
        } catch (exception: GoogleDriveSyncException) {
            assertTrue(exception.message.orEmpty().contains("files array"))
        }
    }

    @Test
    fun readSourceFileReturnsNullForAValidEmptyListing() = runBlocking {
        val store = GoogleDriveSourceStore(
            httpClient = fakeHttpClient(mutableListOf(), listOf("""{"files":[]}""")),
        )

        assertNull(store.readSourceFile("test-access-token"))
    }

    @Test
    fun createsSourceFileInPrivateAppDataFolder() = runBlocking {
        val requests = mutableListOf<Request>()
        val store = GoogleDriveSourceStore(
            httpClient = fakeHttpClient(requests, listOf("""{"id":"new-file"}""")),
        )

        val id = store.writeSourceFile(
            accessToken = "test-access-token",
            fileId = null,
            sources = listOf(OpmlSource("Example", "https://example.org/feed.xml", 1)),
        )

        assertEquals("new-file", id)
        val request = requests.single()
        assertEquals("POST", request.method)
        assertEquals("Bearer test-access-token", request.header("Authorization"))
        assertEquals("multipart", request.url.queryParameter("uploadType"))
        val body = Buffer()
        request.body?.writeTo(body)
        assertTrue(body.readUtf8().contains("\"parents\":[\"appDataFolder\"]"))
    }

    private fun fakeHttpClient(requests: MutableList<Request>, bodies: List<String>): OkHttpClient {
        val remainingBodies = bodies.toMutableList()
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                requests += request
                val responseBody = remainingBodies.removeAt(0)
                    .toResponseBody("application/json".toMediaType())
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(responseBody)
                    .build()
            }
            .build()
    }
}
