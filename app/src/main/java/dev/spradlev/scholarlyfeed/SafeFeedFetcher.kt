package dev.spradlev.scholarlyfeed

import com.prof18.rssparser.RssParser
import com.prof18.rssparser.exception.RssParsingException
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

internal class SafeFeedFetcher(
    private val parser: RssParser = RssParser(),
    private val client: OkHttpClient = createHttpClient(),
) : FeedFetcher {
    override suspend fun fetch(url: String): ParsedFeed = withContext(Dispatchers.IO) {
        var currentUrl = validateFeedUrl(url)

        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            val request = Request.Builder()
                .url(currentUrl)
                .header("Accept", ACCEPTED_CONTENT_TYPES.joinToString(", "))
                .header("User-Agent", USER_AGENT)
                .get()
                .build()

            val response = try {
                client.newCall(request).execute()
            } catch (exception: IOException) {
                throw FeedFetchException("Network error while loading the feed.", exception)
            }

            response.use {
                if (it.code in REDIRECT_CODES) {
                    if (redirectCount == MAX_REDIRECTS) {
                        throw FeedFetchException("The feed redirected too many times.")
                    }
                    val location = it.header("Location")
                        ?: throw FeedFetchException("The feed redirect did not include a destination.")
                    currentUrl = currentUrl.resolve(location)
                        ?: throw FeedFetchException("The feed redirect URL is invalid.")
                    currentUrl = validateFeedUrl(currentUrl.toString())
                    return@repeat
                }

                if (!it.isSuccessful) {
                    throw FeedFetchException("The feed server returned HTTP ${it.code}.")
                }

                val body = it.body
                val contentType = body.contentType()?.let { type -> "${type.type}/${type.subtype}" }
                if (contentType !in ACCEPTED_CONTENT_TYPES) {
                    throw FeedFetchException("The server did not return an RSS or Atom XML content type.")
                }
                if (body.contentLength() > MAX_FEED_BYTES) {
                    throw FeedFetchException("The feed is larger than the 4 MiB limit.")
                }

                val xml = try {
                    body.byteStream().use(::readBoundedFeed)
                        .toString(body.contentType()?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8)
                } catch (exception: FeedFetchException) {
                    throw exception
                } catch (exception: IOException) {
                    throw FeedFetchException("Could not read the feed response.", exception)
                }

                return@withContext parseFeedDocument(parser, xml, currentUrl.toString())
            }
        }

        throw FeedFetchException("Could not load the feed.")
    }

    private fun readBoundedFeed(input: java.io.InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val remainingWithSentinel = MAX_FEED_BYTES + 1 - output.size()
            val count = input.read(buffer, 0, minOf(buffer.size, remainingWithSentinel))
            if (count < 0) break
            output.write(buffer, 0, count)
            if (output.size() > MAX_FEED_BYTES) {
                throw FeedFetchException("The feed is larger than the 4 MiB limit.")
            }
        }
        return output.toByteArray()
    }

    private fun validateFeedUrl(value: String): HttpUrl {
        val url = value.toHttpUrlOrNull()
            ?: throw FeedFetchException("Enter a valid HTTPS feed URL.")
        if (url.scheme != "https" || url.username.isNotEmpty() || url.password.isNotEmpty()) {
            throw FeedFetchException("Only public HTTPS feed URLs are allowed.")
        }
        return url
    }

    companion object {
        private const val MAX_FEED_BYTES = 4 * 1024 * 1024
        private const val MAX_REDIRECTS = 5
        private const val USER_AGENT = "ScholarlyFeed/1.0"
        private val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
        private val ACCEPTED_CONTENT_TYPES = setOf(
            "application/atom+xml",
            "application/rss+xml",
            "application/rdf+xml",
            "application/xml",
            "text/xml",
            "text/plain",
        )

        private fun createHttpClient(): OkHttpClient =
            OkHttpClient.Builder()
                .dns(PublicAddressDns)
                .followRedirects(false)
                .followSslRedirects(false)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .build()

    }
}

internal suspend fun parseFeedDocument(
    parser: RssParser,
    xml: String,
    resolvedUrl: String,
): ParsedFeed {
    val channel = try {
        parser.parse(xml)
    } catch (exception: RssParsingException) {
        throw FeedFetchException("The response is not a valid RSS or Atom feed.", exception)
    } catch (exception: IllegalArgumentException) {
        throw FeedFetchException("The response is not a valid RSS or Atom feed.", exception)
    }

    val title = channel.title?.trim().orEmpty()
    if (title.isBlank()) {
        throw FeedFetchException("The feed did not provide a title.")
    }

    val seenArticleKeys = mutableSetOf<String>()
    val articles = channel.items.mapIndexedNotNull { index, item ->
        val articleTitle = item.title?.trim()?.takeIf(String::isNotEmpty)
            ?: return@mapIndexedNotNull null
        val link = item.link?.trim()?.takeIf(String::isNotEmpty)
        val guid = item.guid?.trim()?.takeIf(String::isNotEmpty)
        val deduplicationKeys = articleDeduplicationKeys(guid, link)
        val isDuplicate = deduplicationKeys.any(seenArticleKeys::contains)
        seenArticleKeys.addAll(deduplicationKeys)
        if (isDuplicate) return@mapIndexedNotNull null

        val id = guid ?: link?.let(::canonicalArticleUrl)
            ?: "$articleTitle:${item.pubDate.orEmpty()}:$index"

        FeedArticle(
            id = id,
            title = articleTitle,
            link = link,
            publishedAt = item.pubDate,
            publishedAtMillis = parsePublishedAt(item.pubDate),
            excerpt = item.description?.trim()?.takeIf(String::isNotEmpty)
                ?: item.content?.trim()?.takeIf(String::isNotEmpty),
            sourceUrl = resolvedUrl,
            sourceTitle = title,
        )
    }
    return ParsedFeed(
        title = title,
        resolvedUrl = resolvedUrl,
        articles = sortArticlesNewestFirst(articles),
    )
}

private fun articleDeduplicationKeys(guid: String?, link: String?): Set<String> = buildSet {
    guid?.let { add("id:$it") }
    link?.let { add("url:${canonicalArticleUrl(it)}") }
}

private fun canonicalArticleUrl(value: String): String {
    val url = value.toHttpUrlOrNull() ?: return value
    return url.newBuilder()
        .fragment(null)
        .build()
        .toString()
}

private fun parsePublishedAt(value: String?): Long? {
    if (value.isNullOrBlank()) return null
    try {
        return Instant.parse(value).toEpochMilli()
    } catch (_: DateTimeParseException) {
        // Continue with the other standard feed date formats.
    }
    try {
        return OffsetDateTime.parse(value).toInstant().toEpochMilli()
    } catch (_: DateTimeParseException) {
        // Continue with RFC 822 dates commonly used in RSS.
    }

    val formats = listOf(
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm:ss z",
        "EEE, dd MMM yyyy HH:mm Z",
        "dd MMM yyyy HH:mm:ss Z",
    )
    return formats.firstNotNullOfOrNull { pattern ->
        try {
            SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
                timeZone = TimeZone.getTimeZone("GMT")
            }.parse(value)?.time
        } catch (_: ParseException) {
            null
        }
    }
}

private object PublicAddressDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val addresses = try {
            Dns.SYSTEM.lookup(hostname)
        } catch (exception: UnknownHostException) {
            throw exception
        }
        if (addresses.isEmpty() || addresses.any { !it.isPublicAddress() }) {
            throw UnknownHostException("Feed host resolves to a non-public network address.")
        }
        return addresses
    }
}

internal fun InetAddress.isPublicAddress(): Boolean {
    if (isAnyLocalAddress || isLoopbackAddress || isLinkLocalAddress ||
        isSiteLocalAddress || isMulticastAddress
    ) {
        return false
    }

    val bytes = address
    if (this is Inet6Address) {
        val isIpv4Mapped = bytes.take(10).all { it == 0.toByte() } &&
            bytes[10] == 0xff.toByte() && bytes[11] == 0xff.toByte()
        if (isIpv4Mapped) {
            return bytes.copyOfRange(12, 16).isPublicIpv4()
        }
        return bytes.isPublicIpv6()
    }
    return bytes.isPublicIpv4()
}

private fun ByteArray.isPublicIpv6(): Boolean {
    val first = this[0].toInt() and 0xff
    val second = this[1].toInt() and 0xff
    val third = this[2].toInt() and 0xff
    val fourth = this[3].toInt() and 0xff
    if (first !in 0x20..0x3f) return false

    return when {
        first == 0x20 && second == 0x01 && third == 0x0d && fourth == 0xb8 -> false
        first == 0x20 && second == 0x01 && third == 0 && fourth == 0 -> false
        first == 0x20 && second == 0x01 && third == 0 && fourth == 2 -> false
        first == 0x20 && second == 0x01 && third == 0 && fourth == 0x10 -> false
        first == 0x20 && second == 2 -> false
        first == 0x3f && second == 0xff && third and 0xf0 == 0 -> false
        else -> true
    }
}

private fun ByteArray.isPublicIpv4(): Boolean {
    val first = this[0].toInt() and 0xff
    val second = this[1].toInt() and 0xff
    val third = this[2].toInt() and 0xff
    return when {
        first == 0 || first == 10 || first == 127 || first >= 224 -> false
        first == 100 && second in 64..127 -> false
        first == 169 && second == 254 -> false
        first == 172 && second in 16..31 -> false
        first == 192 && second == 168 -> false
        first == 192 && second == 0 && third == 0 -> false
        first == 192 && second == 0 && third == 2 -> false
        first == 192 && second == 88 && third == 99 -> false
        first == 198 && second in 18..19 -> false
        first == 198 && second == 51 && third == 100 -> false
        first == 203 && second == 0 && third == 113 -> false
        else -> true
    }
}

internal fun sortArticlesNewestFirst(articles: List<FeedArticle>): List<FeedArticle> =
    articles.withIndex()
        .sortedWith { first, second ->
            val firstDate = first.value.publishedAtMillis
            val secondDate = second.value.publishedAtMillis
            when {
                firstDate != null && secondDate != null -> secondDate.compareTo(firstDate)
                firstDate != null -> -1
                secondDate != null -> 1
                else -> first.index.compareTo(second.index)
            }
        }
        .map { it.value }
