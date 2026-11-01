package dev.spradlev.scholarlyfeed

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.io.IOException

@Composable
internal fun FeedArticleDetailScreen(
    article: FeedArticle,
    onBack: () -> Unit,
    onOpenOriginalArticle: (String) -> Boolean,
    onVote: suspend (Int) -> Boolean,
    publisherPageLoader: PublisherPageLoader,
    relatedArticles: List<FeedArticle>,
    onOpenRelatedArticle: (FeedArticle) -> Unit,
    readingScale: Float,
    onReadingScaleChange: (Float) -> Unit,
) {
    var linkError by remember(article.id, article.sourceUrl) { mutableStateOf(false) }
    var userVote by remember(article.id, article.sourceUrl) { mutableStateOf(article.userVote) }
    var voteError by remember(article.id, article.sourceUrl) { mutableStateOf(false) }
    var isSavingVote by remember(article.id, article.sourceUrl) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val articleLink = article.link?.takeIf(::isSupportedArticleLink)
    val feedExcerpt = article.excerpt?.let(::cleanArticleText)?.takeIf(String::isNotEmpty)
    val scrollState = rememberScrollState()
    var showingFullArticle by remember(article.id, article.sourceUrl) { mutableStateOf(false) }
    var publisherPageContent by remember(article.id, article.sourceUrl) {
        mutableStateOf<PublisherPageContent?>(null)
    }
    var isLoadingPublisherPage by remember(article.id, article.sourceUrl) {
        mutableStateOf(false)
    }
    var publisherPageLoadFailed by remember(article.id, article.sourceUrl) {
        mutableStateOf(false)
    }
    var publisherPageRequestStarted by remember(article.id, article.sourceUrl) {
        mutableStateOf(false)
    }

    val requestPublisherPage: (Boolean) -> Unit = { retry ->
        if (articleLink != null && (!publisherPageRequestStarted || retry)) {
            publisherPageRequestStarted = true
            publisherPageLoadFailed = false
            scope.launch {
                isLoadingPublisherPage = true
                try {
                    publisherPageContent = publisherPageLoader.loadContent(articleLink)
                } catch (_: IOException) {
                    publisherPageLoadFailed = true
                } finally {
                    isLoadingPublisherPage = false
                }
            }
        }
    }

    LaunchedEffect(article.id, article.sourceUrl, articleLink, feedExcerpt) {
        if (feedExcerpt == null) requestPublisherPage(false)
    }
    LaunchedEffect(article.id, article.sourceUrl) {
        scrollState.scrollTo(0)
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .widthIn(max = READABLE_CONTENT_MAX_WIDTH)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .align(Alignment.TopCenter)
                    .verticalScroll(scrollState)
                    .pinchToZoom { factor ->
                        onReadingScaleChange(
                            (readingScale * factor).coerceIn(
                                MIN_READING_SCALE,
                                MAX_READING_SCALE,
                            ),
                        )
                    }
                    .keyboardFocusTraversal(scrollState)
                    .padding(24.dp)
                    .semantics { paneTitle = "Article: ${article.title}" }
                    .testTag("articleReadingContainer"),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TextButton(onClick = onBack) {
                    Text("Back to feed")
                }
                Text(
                    text = article.title,
                    style = scaleReadingStyle(
                        MaterialTheme.typography.headlineSmall,
                        readingScale,
                    ),
                    modifier = Modifier
                        .semantics { heading() }
                        .testTag("articleTitle"),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = article.sourceTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    formatArticleDate(article.publishedAtMillis)?.let { date ->
                        Text(date, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                HorizontalDivider()

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !showingFullArticle,
                        onClick = { showingFullArticle = false },
                        label = { Text("Summary") },
                        modifier = Modifier.testTag("articleSummaryMode"),
                    )
                    FilterChip(
                        selected = showingFullArticle,
                        onClick = {
                            showingFullArticle = true
                            requestPublisherPage(false)
                        },
                        enabled = articleLink != null,
                        label = { Text("Full article") },
                        modifier = Modifier.testTag("fullArticleMode"),
                    )
                }
                if (showingFullArticle) {
                    Text("Full article text", style = MaterialTheme.typography.titleMedium)
                    when {
                        publisherPageContent?.articleText != null -> {
                            val articleText = requireNotNull(publisherPageContent?.articleText)
                            articleText.text
                                .split(ARTICLE_PARAGRAPH_SEPARATOR)
                                .filter(String::isNotBlank)
                                .forEachIndexed { index, paragraph ->
                                    Text(
                                        text = paragraph,
                                        style = scaleReadingStyle(
                                            MaterialTheme.typography.bodyLarge,
                                            readingScale,
                                        ),
                                        modifier = Modifier.testTag(
                                            "publisherArticleParagraph_$index",
                                        ),
                                    )
                                }
                            Text(
                                if (articleText.isTruncated) {
                                    "Reader text was shortened to keep reading responsive."
                                } else {
                                    "Text extracted from the publisher page. Some sites may omit " +
                                        "parts of an article or restrict access."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        isLoadingPublisherPage ->
                            Text(
                                "Loading text from the publisher page…",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        publisherPageLoadFailed -> {
                            Text(
                                "Couldn't load the publisher's article text. The page may be " +
                                    "unavailable or restrict access.",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            TextButton(
                                onClick = {
                                    requestPublisherPage(true)
                                },
                            ) {
                                Text("Try loading again")
                            }
                        }
                        else ->
                            Text(
                                "No readable article text was found on the publisher's page.",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                    }
                } else {
                    val summaryLabel = when {
                        feedExcerpt != null -> "Feed excerpt"
                        publisherPageContent?.summary != null -> "Publisher summary"
                        else -> "Article summary"
                    }
                    Text(summaryLabel, style = MaterialTheme.typography.titleMedium)
                    when {
                        feedExcerpt != null ->
                            Text(
                                feedExcerpt,
                                style = scaleReadingStyle(
                                    MaterialTheme.typography.bodyLarge,
                                    readingScale,
                                ),
                                modifier = Modifier.testTag("articleSummaryText"),
                            )
                        publisherPageContent?.summary != null ->
                            Text(
                                requireNotNull(publisherPageContent?.summary),
                                style = scaleReadingStyle(
                                    MaterialTheme.typography.bodyLarge,
                                    readingScale,
                                ),
                                modifier = Modifier.testTag("articleSummaryText"),
                            )
                        isLoadingPublisherPage ->
                            Text(
                                "Checking the publisher page for a summary…",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        publisherPageLoadFailed ->
                            Text(
                                "Couldn't load a publisher summary. You can still open the article.",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        else ->
                            Text(
                                "No summary was available from the feed or publisher page.",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                    }
                }
                if (feedExcerpt == null && articleLink == null) {
                    Text(
                        "This feed did not include a summary or article link.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                if (articleLink != null) {
                    Button(
                        onClick = {
                            linkError = !onOpenOriginalArticle(articleLink)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("openOriginalArticleButton"),
                    ) {
                        Text("Open publisher article")
                    }
                } else {
                    Text("No article link was provided by the feed.")
                }
                if (linkError) {
                    Text(
                        text = "No app is available to open this article link.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                HorizontalDivider()
                Text("Was this useful?", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Your feedback is saved on this device. It won't change your feed yet.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            val nextVote = if (userVote == 1) 0 else 1
                            scope.launch {
                                isSavingVote = true
                                voteError = false
                                try {
                                    if (onVote(nextVote)) {
                                        userVote = nextVote
                                    } else {
                                        voteError = true
                                    }
                                } catch (_: android.database.sqlite.SQLiteException) {
                                    voteError = true
                                } finally {
                                    isSavingVote = false
                                }
                            }
                        },
                        enabled = !isSavingVote,
                        modifier = Modifier
                            .testTag("upvoteArticleButton")
                            .semantics {
                                contentDescription = "Upvote article"
                                stateDescription = if (userVote == 1) "Selected" else "Not selected"
                            },
                    ) {
                        Text(if (userVote == 1) "↑ Upvoted" else "↑ Upvote")
                    }
                    TextButton(
                        onClick = {
                            val nextVote = if (userVote == -1) 0 else -1
                            scope.launch {
                                isSavingVote = true
                                voteError = false
                                try {
                                    if (onVote(nextVote)) {
                                        userVote = nextVote
                                    } else {
                                        voteError = true
                                    }
                                } catch (_: android.database.sqlite.SQLiteException) {
                                    voteError = true
                                } finally {
                                    isSavingVote = false
                                }
                            }
                        },
                        enabled = !isSavingVote,
                        modifier = Modifier
                            .testTag("downvoteArticleButton")
                            .semantics {
                                contentDescription = "Downvote article"
                                stateDescription = if (userVote == -1) "Selected" else "Not selected"
                            },
                    ) {
                        Text(if (userVote == -1) "↓ Downvoted" else "↓ Downvote")
                    }
                }
                if (voteError) {
                    Text(
                        text = "Couldn't save your feedback. Please try again.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (relatedArticles.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        "Similar articles",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    relatedArticles.forEach { relatedArticle ->
                        FeedArticleListItem(
                            article = relatedArticle,
                            onClick = { onOpenRelatedArticle(relatedArticle) },
                            testTag = "relatedArticle_${relatedArticle.id}",
                        )
                    }
                }
            }
        }
    }
}

private fun isSupportedArticleLink(value: String): Boolean {
    val uri = Uri.parse(value)
    return (uri.scheme.equals("https", ignoreCase = true) ||
        uri.scheme.equals("http", ignoreCase = true)) &&
        !uri.host.isNullOrBlank() &&
        uri.userInfo == null
}

private val ARTICLE_PARAGRAPH_SEPARATOR = Regex("\\n\\s*\\n")
