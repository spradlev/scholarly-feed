package dev.spradlev.scholarlyfeed

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.room.Room
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = Room.databaseBuilder(
            applicationContext,
            FeedDatabase::class.java,
            "scholarly-feed.db",
        )
            .addMigrations(FeedDatabase.MIGRATION_1_2, FeedDatabase.MIGRATION_2_3)
            .build()
        val fetcher = SafeFeedFetcher()
        setContent {
            MaterialTheme {
                ScholarlyFeedScreen(database, fetcher)
            }
        }
    }
}

internal fun createWebFeedSearchIntent(query: String): Intent =
    Intent(Intent.ACTION_WEB_SEARCH).apply {
        putExtra(SearchManager.QUERY, "$query RSS feed")
    }

@Composable
internal fun ScholarlyFeedScreen(database: FeedDatabase, fetcher: FeedFetcher) {
    val context = LocalContext.current
    var focusModeEnabled by rememberSaveable { mutableStateOf(false) }
    var showingSources by rememberSaveable { mutableStateOf(false) }
    var selectedArticle by remember { mutableStateOf<FeedArticle?>(null) }
    val repository = remember(database, fetcher) { FeedRepository(database, fetcher) }
    val sources by repository.observeSources().collectAsState(initial = emptyList())
    val articles by repository.observeArticles().collectAsState(initial = emptyList())
    val visibleArticles = if (focusModeEnabled) {
        val sourceLevels = sources.associate { source -> source.feedUrl to source.level }
        articles.filter { article -> sourceLevels[article.sourceUrl] == 1 }
    } else {
        articles
    }
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshMessage by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = showingSources || selectedArticle != null) {
        if (showingSources) {
            showingSources = false
        } else {
            selectedArticle = null
        }
    }

    if (showingSources) {
        FeedSourceManagementScreen(
            sources = sources,
            onVerifyAndAdd = { source ->
                repository.verifyAndAdd(source)
            },
            onRemoveSource = { source ->
                scope.launch { repository.removeSource(source) }
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
            onImportOpml = { opmlSources ->
                repository.importOpml(opmlSources)
            },
        )
    } else if (selectedArticle != null) {
        FeedArticleDetailScreen(
            article = selectedArticle!!,
            onBack = { selectedArticle = null },
            onOpenOriginalArticle = { link ->
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
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
                } else if (visibleArticles.isNotEmpty()) {
                    Text("Latest articles", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = "Fetched feed entries and available excerpts are saved on this device.",
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
                if (focusModeEnabled) {
                    Text(
                        text = "Showing Level 1: Research and scientific reporting.",
                        style = MaterialTheme.typography.bodyMedium,
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
                if (
                    focusModeEnabled &&
                    articles.isNotEmpty() &&
                    visibleArticles.isEmpty()
                ) {
                    Text("No Level 1 articles yet. Turn off Focus Mode to see all sources.")
                }
                visibleArticles.forEach { article ->
                    FeedArticleListItem(
                        article = article,
                        onClick = { selectedArticle = article },
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedArticleListItem(
    article: FeedArticle,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = "Open article: ${article.title}"
            }
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(article.title, style = MaterialTheme.typography.titleMedium)
        Text(article.sourceTitle, style = MaterialTheme.typography.bodySmall)
        article.publishedAt?.let { publishedAt ->
            Text("Published $publishedAt", style = MaterialTheme.typography.bodySmall)
        }
        article.excerpt?.let { excerpt ->
            Text(excerpt, style = MaterialTheme.typography.bodyMedium)
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}
