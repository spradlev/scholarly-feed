package dev.spradlev.scholarlyfeed

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun FeedArticleDetailScreen(
    article: FeedArticle,
    onBack: () -> Unit,
    onOpenOriginalArticle: (String) -> Boolean,
) {
    var linkError by remember(article.id, article.sourceUrl) { mutableStateOf(false) }
    val articleLink = article.link?.takeIf(::isSupportedArticleLink)

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .keyboardFocusTraversal()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextButton(onClick = onBack) {
                Text("Back to feed")
            }
            Text(
                text = article.title,
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(article.sourceTitle, style = MaterialTheme.typography.bodyLarge)
            article.publishedAt?.let { publishedAt ->
                Text(
                    text = "Published $publishedAt",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            HorizontalDivider()

            Text("Excerpt from feed", style = MaterialTheme.typography.titleMedium)
            article.excerpt?.let { excerpt ->
                Text(excerpt, style = MaterialTheme.typography.bodyLarge)
            } ?: Text("This feed did not provide an excerpt.")

            Text(
                text = "Full article text is not included in the cached feed entry. Open the original article to read the full text.",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (articleLink != null) {
                Button(
                    onClick = {
                        linkError = !onOpenOriginalArticle(articleLink)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Open original article")
                }
            } else {
                Text("No web link was provided by the feed.")
            }
            if (linkError) {
                Text(
                    text = "No app is available to open this article link.",
                    color = MaterialTheme.colorScheme.error,
                )
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
