// SPDX-License-Identifier: AGPL-3.0-or-later

package dev.spradlev.scholarlyfeed

import android.database.sqlite.SQLiteConstraintException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import androidx.room.withTransaction

internal class FeedRepository(
    private val database: FeedDatabase,
    private val fetcher: FeedFetcher,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val sourceDao = database.feedSourceDao()
    private val articleDao = database.feedArticleDao()

    fun observeSources(): Flow<List<FeedSource>> = sourceDao.observeSources()

    fun observeArticles(): Flow<List<FeedArticle>> =
        articleDao.observeArticles().map { articles -> articles.map(CachedFeedArticle::toFeedArticle) }

    suspend fun verifyAndAdd(source: FeedSource): ParsedFeed {
        val fetchResult = fetcher.fetchForRefresh(source.feedUrl, null, null)
        val updated = fetchResult as? FeedFetchResult.Updated
            ?: throw FeedFetchException("The feed was not available for verification.")
        val parsedFeed = updated.feed
        val verifiedAtMillis = nowMillis()
        val savedSource = source.copy(
            feedTitle = parsedFeed.title,
            verifiedAtMillis = verifiedAtMillis,
            lastSuccessfulRefreshAtMillis = verifiedAtMillis,
            etag = updated.etag,
            lastModified = updated.lastModified,
        )
        val articles = parsedFeed.articles
            .map { it.copy(sourceUrl = source.feedUrl, sourceTitle = source.title) }
            .mapIndexed { index, article -> CachedFeedArticle.from(article, index) }

        database.withTransaction {
            sourceDao.insert(savedSource)
            articleDao.replaceForSource(source.feedUrl, articles)
        }
        return parsedFeed
    }

    suspend fun removeSource(source: FeedSource) {
        sourceDao.remove(source.feedUrl)
    }

    suspend fun updateSourceMetadata(source: FeedSource) {
        database.withTransaction {
            sourceDao.updateMetadata(source.feedUrl, source.title, source.level)
            articleDao.updateSourceTitle(source.feedUrl, source.title)
        }
    }

    suspend fun updateArticleVote(article: FeedArticle, userVote: Int): Boolean {
        require(userVote in -1..1) { "Article vote must be -1, 0, or 1." }
        return articleDao.updateUserVote(article.sourceUrl, article.id, userVote) == 1
    }

    suspend fun importOpml(sources: List<OpmlSource>): OpmlImportResult {
        val existingUrls = sourceDao.observeSources().first()
            .map(FeedSource::feedUrl)
            .toMutableSet()
        val errors = mutableListOf<String>()
        var importedCount = 0

        sources.forEach { opmlSource ->
            if (!existingUrls.add(opmlSource.feedUrl)) {
                errors += "${opmlSource.title}: this feed URL is already in your list."
                return@forEach
            }

            try {
                verifyAndAdd(
                    FeedSource(
                        feedUrl = opmlSource.feedUrl,
                        title = opmlSource.title,
                        feedTitle = "",
                        level = opmlSource.level,
                        verifiedAtMillis = 0L,
                    ),
                )
                importedCount++
            } catch (exception: FeedFetchException) {
                existingUrls.remove(opmlSource.feedUrl)
                errors += "${opmlSource.title}: ${exception.message ?: "Could not verify this feed."}"
            } catch (exception: SQLiteConstraintException) {
                errors += "${opmlSource.title}: this feed URL is already in your list."
            }
        }

        return OpmlImportResult(importedCount, errors)
    }

    suspend fun refreshSources(sources: List<FeedSource>): FeedRefreshResult {
        val errors = mutableListOf<String>()

        sources.forEach { source ->
            try {
                when (
                    val fetchResult = fetcher.fetchForRefresh(
                        source.feedUrl,
                        source.etag,
                        source.lastModified,
                    )
                ) {
                    is FeedFetchResult.Updated -> {
                        val fetchedArticles = fetchResult.feed.articles
                            .map { article ->
                                article.copy(
                                    sourceUrl = source.feedUrl,
                                    sourceTitle = source.title,
                                )
                            }
                            .mapIndexed { index, article -> CachedFeedArticle.from(article, index) }
                        database.withTransaction {
                            articleDao.replaceForSource(source.feedUrl, fetchedArticles)
                            sourceDao.updateRefreshMetadata(
                                source.feedUrl,
                                nowMillis(),
                                fetchResult.etag,
                                fetchResult.lastModified,
                            )
                        }
                    }
                    FeedFetchResult.NotModified -> {
                        sourceDao.updateLastSuccessfulRefresh(source.feedUrl, nowMillis())
                    }
                }
            } catch (exception: FeedFetchException) {
                errors += "${source.title}: ${exception.message ?: "Could not load this feed."}"
            }
        }

        return FeedRefreshResult(errors = errors)
    }
}

internal data class OpmlImportResult(
    val importedCount: Int,
    val errors: List<String>,
)
