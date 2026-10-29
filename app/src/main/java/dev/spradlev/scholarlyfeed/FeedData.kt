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
)

internal data class ParsedFeed(
    val title: String,
    val resolvedUrl: String,
    val articles: List<FeedArticle>,
)

internal data class FeedRefreshResult(val errors: List<String>)

internal class FeedFetchException(message: String, cause: Throwable? = null) : Exception(message, cause)

internal interface FeedFetcher {
    suspend fun fetch(url: String): ParsedFeed
}
