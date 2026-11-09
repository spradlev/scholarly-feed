package dev.spradlev.scholarlyfeed

internal data class FeedArticle(
    val id: String,
    val title: String,
    val link: String?,
    val publishedAt: String?,
    val publishedAtMillis: Long?,
    val excerpt: String?,
    val sourceUrl: String,
    val sourceTitle: String,
    val userVote: Int = 0,
)

internal data class ParsedFeed(
    val title: String,
    val resolvedUrl: String,
    val articles: List<FeedArticle>,
)

internal data class FeedRefreshResult(val errors: List<String>)

internal sealed interface FeedFetchResult {
    data class Updated(
        val feed: ParsedFeed,
        val etag: String?,
        val lastModified: String?,
    ) : FeedFetchResult

    data object NotModified : FeedFetchResult
}

internal class FeedFetchException(message: String, cause: Throwable? = null) : Exception(message, cause)

internal interface FeedFetcher {
    suspend fun fetch(url: String): ParsedFeed

    suspend fun fetchForRefresh(
        url: String,
        etag: String?,
        lastModified: String?,
    ): FeedFetchResult = FeedFetchResult.Updated(fetch(url), etag, lastModified)
}
