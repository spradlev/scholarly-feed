package dev.spradlev.scholarlyfeed

import kotlinx.coroutines.flow.Flow

internal class FeedRepository(
    private val sourceDao: FeedSourceDao,
    private val fetcher: FeedFetcher,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    fun observeSources(): Flow<List<FeedSource>> = sourceDao.observeSources()

    suspend fun verifyAndAdd(source: FeedSource): ParsedFeed {
        val parsedFeed = fetcher.fetch(source.feedUrl)
        sourceDao.insert(
            source.copy(
                feedTitle = parsedFeed.title,
                verifiedAtMillis = nowMillis(),
            ),
        )
        return parsedFeed
    }

    suspend fun removeSource(source: FeedSource) {
        sourceDao.remove(source.feedUrl)
    }

    suspend fun refreshSources(sources: List<FeedSource>): FeedRefreshResult {
        val articles = mutableListOf<FeedArticle>()
        val errors = mutableListOf<String>()

        sources.forEach { source ->
            try {
                articles += fetcher.fetch(source.feedUrl).articles
                    .map { article ->
                        article.copy(
                            sourceUrl = source.feedUrl,
                            sourceTitle = source.title,
                        )
                    }
            } catch (exception: FeedFetchException) {
                errors += "${source.title}: ${exception.message ?: "Could not load this feed."}"
            }
        }

        return FeedRefreshResult(
            articles = sortArticlesNewestFirst(articles),
            errors = errors,
        )
    }
}
