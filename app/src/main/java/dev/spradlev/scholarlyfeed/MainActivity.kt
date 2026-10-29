package dev.spradlev.scholarlyfeed

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.room.Room
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sourceDao = Room.databaseBuilder(
            applicationContext,
            FeedDatabase::class.java,
            "scholarly-feed.db",
        ).build().feedSourceDao()
        setContent {
            MaterialTheme {
                ScholarlyFeedScreen(sourceDao)
            }
        }
    }
}

internal fun createWebFeedSearchIntent(query: String): Intent =
    Intent(Intent.ACTION_WEB_SEARCH).apply {
        putExtra(SearchManager.QUERY, "$query RSS feed")
    }

@Composable
private fun ScholarlyFeedScreen(sourceDao: FeedSourceDao) {
    val context = LocalContext.current
    var focusModeEnabled by rememberSaveable { mutableStateOf(false) }
    var showingSources by rememberSaveable { mutableStateOf(false) }
    val repository = remember(sourceDao) { FeedRepository(sourceDao, SafeFeedFetcher()) }
    val sources by repository.observeSources().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var articles by remember { mutableStateOf(emptyList<FeedArticle>()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshMessage by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = showingSources) {
        showingSources = false
    }

    if (showingSources) {
        FeedSourceManagementScreen(
            sources = sources,
            onVerifyAndAdd = { source ->
                val parsedFeed = repository.verifyAndAdd(source)
                articles = parsedFeed.articles.map { article ->
                    article.copy(
                        sourceUrl = source.feedUrl,
                        sourceTitle = source.title,
                    )
                }
                parsedFeed
            },
            onRemoveSource = { source ->
                scope.launch {
                    repository.removeSource(source)
                    articles = articles.filterNot { it.sourceUrl == source.feedUrl }
                }
            },
            onBack = { showingSources = false },
            onSearchWeb = { query ->
                try {
                    context.startActivity(createWebFeedSearchIntent(query))
                    true
                } catch (_: ActivityNotFoundException) {
                    false
                }
            },
        )
    } else {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Scholarly Feed",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    IconButton(
                        onClick = { showingSources = true },
                        modifier = Modifier.semantics {
                            contentDescription = "Settings"
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = null,
                        )
                    }
                }
                if (articles.isEmpty() && sources.isEmpty()) {
                    Text(
                        text = "Your feed is empty. Add sources to set up your reading list.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                } else if (articles.isNotEmpty()) {
                    Text("Latest articles", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = "Articles load when you verify or refresh a feed. Offline article storage isn't available yet.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Focus Mode",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.clearAndSetSemantics {},
                    )
                    Switch(
                        checked = focusModeEnabled,
                        onCheckedChange = { focusModeEnabled = it },
                        modifier = Modifier.semantics {
                            contentDescription = "Focus Mode"
                            stateDescription = if (focusModeEnabled) "On" else "Off"
                            liveRegion = LiveRegionMode.Polite
                        },
                    )
                }
                if (sources.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                isRefreshing = true
                                refreshMessage = null
                                try {
                                    val result = repository.refreshSources(sources)
                                    articles = result.articles
                                    refreshMessage = result.errors
                                        .takeIf(List<String>::isNotEmpty)
                                        ?.joinToString("\n")
                                } finally {
                                    isRefreshing = false
                                }
                            }
                        },
                        enabled = !isRefreshing,
                    ) {
                        Text(if (isRefreshing) "Refreshing feeds…" else "Refresh feeds")
                    }
                }
                refreshMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (articles.isEmpty() && sources.isNotEmpty() && !isRefreshing) {
                    Text("No articles loaded. Refresh feeds to try again.")
                }
                articles.forEach { article ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(article.title, style = MaterialTheme.typography.titleMedium)
                        Text(article.sourceTitle, style = MaterialTheme.typography.bodySmall)
                        article.publishedAt?.let { publishedAt ->
                            Text(publishedAt, style = MaterialTheme.typography.bodySmall)
                        }
                        article.excerpt?.let { excerpt ->
                            Text(excerpt, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
