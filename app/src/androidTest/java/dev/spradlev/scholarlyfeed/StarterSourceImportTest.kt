// SPDX-License-Identifier: AGPL-3.0-or-later

package dev.spradlev.scholarlyfeed

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StarterSourceImportTest {
    @Test
    fun importingStarterSourcesFetchesOffCallingThreadAndSavesAllValidSources() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val starterSources = readStarterOpml(context)
        val callingThread = Thread.currentThread()
        val requestThreads = mutableListOf<Thread>()
        val feedXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <title>Verified fixture feed</title>
                <link>https://example.org</link>
                <description>Fixture</description>
              </channel>
            </rss>
        """.trimIndent()
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                requestThreads += Thread.currentThread()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .header("Content-Type", "application/rss+xml")
                    .body(feedXml.toResponseBody("application/rss+xml".toMediaType()))
                    .build()
            }
            .build()
        val database = Room.inMemoryDatabaseBuilder(context, FeedDatabase::class.java).build()
        try {
            val repository = FeedRepository(database, SafeFeedFetcher(client = client))
            val result = repository.importOpml(readStarterOpml(context))

            assertEquals(starterSources.size, result.importedCount)
            assertEquals(emptyList<String>(), result.errors)
            assertEquals(starterSources.size, requestThreads.size)
            requestThreads.forEach { requestThread ->
                assertNotEquals(callingThread, requestThread)
            }
            assertEquals(starterSources.size, repository.observeSources().first().size)
            assertEquals(listOf(8, 9, 10, 4), starterSources.groupingBy { it.level }
                .eachCount().toSortedMap().values.toList())
            assertEquals(starterSources.size, starterSources.map { it.feedUrl }.toSet().size)
        } finally {
            database.close()
        }
    }
}
