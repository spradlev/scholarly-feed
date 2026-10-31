package dev.spradlev.scholarlyfeed

import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

internal data class PublisherArticleText(
    val text: String,
    val isTruncated: Boolean,
)

internal data class PublisherPageContent(
    val summary: String?,
    val articleText: PublisherArticleText?,
)

internal interface PublisherPageLoader {
    suspend fun loadContent(articleUrl: String): PublisherPageContent
}

internal class SafePublisherPageLoader(
    private val client: OkHttpClient = createHttpClient(),
) : PublisherPageLoader {
    override suspend fun loadContent(articleUrl: String): PublisherPageContent {
        val html = loadHtml(articleUrl)
        return PublisherPageContent(
            summary = extractPublisherSummary(html),
            articleText = extractPublisherArticleText(html),
        )
    }

    private suspend fun loadHtml(articleUrl: String): String = withContext(Dispatchers.IO) {
        var currentUrl = validateArticleUrl(articleUrl)

        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            val request = Request.Builder()
                .url(currentUrl)
                .header("Accept", "text/html, application/xhtml+xml")
                .header("User-Agent", USER_AGENT)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code in REDIRECT_CODES) {
                    if (redirectCount == MAX_REDIRECTS) {
                        throw IOException("The publisher page redirected too many times.")
                    }
                    val location = response.header("Location")
                        ?: throw IOException("The publisher redirect did not include a destination.")
                    currentUrl = currentUrl.resolve(location)
                        ?: throw IOException("The publisher redirect URL is invalid.")
                    currentUrl = validateArticleUrl(currentUrl.toString())
                    return@repeat
                }

                if (!response.isSuccessful) {
                    throw IOException("The publisher page returned HTTP ${response.code}.")
                }

                val body = response.body
                val contentType = body.contentType()?.let { "${it.type}/${it.subtype}" }
                if (contentType !in HTML_CONTENT_TYPES) {
                    throw IOException("The publisher page did not return HTML.")
                }
                if (body.contentLength() > MAX_PAGE_BYTES) {
                    throw IOException("The publisher page is too large to inspect.")
                }

                val html = body.byteStream().use(::readBoundedPage)
                    .toString(body.contentType()?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8)
                return@withContext html
            }
        }

        throw IOException("Could not load the publisher page.")
    }

    private fun validateArticleUrl(value: String): HttpUrl {
        val url = value.toHttpUrlOrNull()
            ?: throw IOException("The publisher article URL is invalid.")
        if (url.scheme != "https" || url.username.isNotEmpty() || url.password.isNotEmpty()) {
            throw IOException("Only public HTTPS publisher pages can be checked.")
        }
        return url
    }

    private fun readBoundedPage(input: java.io.InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val remainingWithSentinel = MAX_PAGE_BYTES + 1 - output.size()
            val count = input.read(buffer, 0, minOf(buffer.size, remainingWithSentinel))
            if (count < 0) break
            output.write(buffer, 0, count)
            if (output.size() > MAX_PAGE_BYTES) {
                throw IOException("The publisher page is too large to inspect.")
            }
        }
        return output.toByteArray()
    }

    private companion object {
        const val MAX_PAGE_BYTES = 1024 * 1024
        const val MAX_REDIRECTS = 5
        const val USER_AGENT = "ScholarlyFeed/1.0"
        val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
        val HTML_CONTENT_TYPES = setOf("text/html", "application/xhtml+xml")

        fun createHttpClient(): OkHttpClient =
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

internal fun extractPublisherSummary(html: String): String? {
    val tags = META_TAG.findAll(html)
        .map { match -> parseAttributes(match.value) }
        .toList()

    val content = listOf("og:description", "twitter:description", "description", "summary")
        .firstNotNullOfOrNull { key ->
            tags.firstNotNullOfOrNull { attributes ->
                val name = attributes["property"] ?: attributes["name"]
                attributes["content"]?.takeIf { name.equals(key, ignoreCase = true) }
            }
        }
        ?: return null

    val decodedContent = Html.fromHtml(content, Html.FROM_HTML_MODE_COMPACT).toString()
    val summary = Jsoup.parseBodyFragment(decodedContent)
        .text()
        .replace(WHITESPACE, " ")
        .trim()
    if (summary.isEmpty()) return null
    return if (summary.length <= MAX_SUMMARY_CHARACTERS) {
        summary
    } else {
        summary.take(MAX_SUMMARY_CHARACTERS).trimEnd() + "…"
    }
}

internal fun extractPublisherArticleText(html: String): PublisherArticleText? {
    val document = Jsoup.parse(html)
    document.select(
        "script, style, noscript, svg, iframe, canvas, nav, header, footer, aside, form, button, " +
            "[aria-hidden=true]",
    ).remove()

    val contentRoot = document.select(
        "article, main, [role=main], [itemprop=articleBody], " +
            ".article-body, .article-content, .post-content, #article-body",
    ).maxByOrNull(::articleContentScore) ?: document.body()

    val blocks = contentRoot.select("h1, h2, h3, h4, p, blockquote, li")
        .mapNotNull { element ->
            element.text()
                .replace(WHITESPACE, " ")
                .trim()
                .takeIf { text ->
                    text.length >= MINIMUM_BLOCK_CHARACTERS ||
                        element.tagName().matches(HEADING_TAG)
                }
        }
        .distinct()
    val article = blocks.joinToString("\n\n")
    if (article.length < MINIMUM_ARTICLE_CHARACTERS || blocks.size < MINIMUM_ARTICLE_BLOCKS) {
        return null
    }

    val truncated = article.length > MAX_ARTICLE_CHARACTERS
    val text = if (truncated) {
        article.take(MAX_ARTICLE_CHARACTERS).trimEnd() + "…"
    } else {
        article
    }
    return PublisherArticleText(text, truncated)
}

private fun articleContentScore(element: Element): Int =
    element.select("p, blockquote, li").sumOf { it.text().length }

private fun parseAttributes(tag: String): Map<String, String> =
    ATTRIBUTE.findAll(tag)
        .associate { match ->
            val value = match.groups[2]?.value
                ?: match.groups[3]?.value
                ?: match.groups[4]?.value.orEmpty()
            match.groupValues[1].lowercase() to value
        }

private val META_TAG = Regex("<meta\\b[^>]*>", RegexOption.IGNORE_CASE)
private val ATTRIBUTE = Regex(
    """([^\s=/>]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+))""",
    RegexOption.IGNORE_CASE,
)
private val WHITESPACE = Regex("\\s+")
private val HEADING_TAG = Regex("h[1-4]")
private const val MAX_SUMMARY_CHARACTERS = 2_000
private const val MAX_ARTICLE_CHARACTERS = 60_000
private const val MINIMUM_BLOCK_CHARACTERS = 30
private const val MINIMUM_ARTICLE_CHARACTERS = 120
private const val MINIMUM_ARTICLE_BLOCKS = 2
