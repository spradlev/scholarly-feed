package dev.spradlev.scholarlyfeed

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedPersistenceTest {
    @Test
    fun verifiedSourceSurvivesDatabaseReopenAndCanBeRemoved(): Unit {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val databaseName = "feed-source-persistence-test.db"
            context.deleteDatabase(databaseName)
            try {
                val source = FeedSource(
                    feedUrl = "https://example.org/feed.xml",
                    title = "Research Digest",
                    feedTitle = "Research Feed",
                    level = 2,
                    verifiedAtMillis = 1234L,
                )

                val firstDatabase = Room.databaseBuilder(context, FeedDatabase::class.java, databaseName).build()
                firstDatabase.feedSourceDao().insert(source)
                firstDatabase.close()

                val reopenedDatabase = Room.databaseBuilder(context, FeedDatabase::class.java, databaseName).build()
                try {
                    val dao = reopenedDatabase.feedSourceDao()
                    assertEquals(listOf(source), dao.observeSources().first())

                    dao.remove(source.feedUrl)
                    assertTrue(dao.observeSources().first().isEmpty())
                } finally {
                    reopenedDatabase.close()
                }
            } finally {
                context.deleteDatabase(databaseName)
            }
        }
    }
}
