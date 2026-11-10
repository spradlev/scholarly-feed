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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.contentDescription
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = Room.databaseBuilder(
            applicationContext,
            FeedDatabase::class.java,
            "scholarly-feed.db",
        )
            .addMigrations(
                FeedDatabase.MIGRATION_1_2,
                FeedDatabase.MIGRATION_2_3,
                FeedDatabase.MIGRATION_3_4,
                FeedDatabase.MIGRATION_4_5,
            )
            .build()
        val fetcher = SafeFeedFetcher()
        setContent {
            ScholarlyFeedScreen(database, fetcher)
        }
    }
}

internal fun createWebFeedSearchIntent(query: String): Intent =
    Intent(Intent.ACTION_WEB_SEARCH).apply {
        putExtra(SearchManager.QUERY, "$query RSS feed")
    }

@Composable
internal fun ScholarlyFeedScreen(
    database: FeedDatabase,
    fetcher: FeedFetcher,
    publisherPageLoader: PublisherPageLoader? = null,
) {
    val context = LocalContext.current
    val publisherPageLoader = remember(publisherPageLoader) {
        publisherPageLoader ?: SafePublisherPageLoader()
    }
    var appPreferences by remember(context) {
        mutableStateOf(AppPreferences.load(context))
    }

    MaterialTheme(
        colorScheme = appColorScheme(appPreferences.colorScheme),
        typography = readableTypography(appPreferences.textSize.scale),
    ) {
        ScholarlyFeedContent(
            database = database,
            fetcher = fetcher,
            appPreferences = appPreferences,
            publisherPageLoader = publisherPageLoader,
            onAppPreferencesChange = { updated ->
                AppPreferences.save(context, updated)
                appPreferences = updated
            },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ScholarlyFeedContent(
    database: FeedDatabase,
    fetcher: FeedFetcher,
    appPreferences: AppPreferences,
    publisherPageLoader: PublisherPageLoader,
    onAppPreferencesChange: (AppPreferences) -> Unit,
) {
    val context = LocalContext.current
    var focusModeEnabled by rememberSaveable { mutableStateOf(false) }
    var showingSources by rememberSaveable { mutableStateOf(false) }
    var showingSettings by rememberSaveable { mutableStateOf(false) }
    var showingDriveSettings by rememberSaveable { mutableStateOf(false) }
    var sourcesReturnToSettings by rememberSaveable { mutableStateOf(false) }
    var selectedArticle by remember { mutableStateOf<FeedArticle?>(null) }
    val feedListState = rememberLazyListState()
    var scrollFeedToTopOnHome by remember { mutableStateOf(false) }
    var isFeedSearchVisible by rememberSaveable { mutableStateOf(false) }
    var feedSearchQuery by rememberSaveable { mutableStateOf("") }
    val repository = remember(database, fetcher) { FeedRepository(database, fetcher) }
    val sources by repository.observeSources().collectAsState(initial = emptyList())
    val articles by repository.observeArticles().collectAsState(initial = emptyList())
    val visibleArticles = remember(focusModeEnabled, sources, articles) {
        if (focusModeEnabled) {
            val sourceLevels = sources.associate { source -> source.feedUrl to source.level }
            articles.filter { article -> sourceLevels[article.sourceUrl] == 1 }
        } else {
            articles
        }
    }
    val searchQuery = feedSearchQuery.trim()
    val filteredArticles = remember(visibleArticles, searchQuery) {
        if (searchQuery.isEmpty()) {
            visibleArticles
        } else {
            visibleArticles.filter { article ->
                article.title.contains(searchQuery, ignoreCase = true) ||
                    article.sourceTitle.contains(searchQuery, ignoreCase = true) ||
                    article.excerpt?.let(::cleanArticleText)
                        ?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshMessage by remember { mutableStateOf<String?>(null) }
    val isHomeScreen = !showingSources && !showingSettings && selectedArticle == null
    val infoBarTitle = when {
        isHomeScreen -> "Latest articles"
        selectedArticle != null -> "Article"
        else -> "Settings"
    }

    val refreshFeeds: () -> Unit = {
        if (sources.isNotEmpty() && !isRefreshing) {
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
        }
    }

    BackHandler(
        enabled = showingSources || showingSettings || showingDriveSettings ||
            selectedArticle != null || isFeedSearchVisible,
    ) {
        if (showingSources) {
            showingSources = false
            showingDriveSettings = false
            showingSettings = sourcesReturnToSettings
            sourcesReturnToSettings = false
        } else if (showingSettings) {
            showingSettings = false
        } else if (showingDriveSettings) {
            showingDriveSettings = false
            showingSettings = true
        } else if (selectedArticle != null) {
            selectedArticle = null
        } else if (isFeedSearchVisible) {
            isFeedSearchVisible = false
            feedSearchQuery = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        key(infoBarTitle) {
            AppInfoBar(
                pageTitle = infoBarTitle,
                isFloating = isHomeScreen || selectedArticle != null,
                focusModeEnabled = isHomeScreen && focusModeEnabled,
                onBack = if (selectedArticle != null) {
                    { selectedArticle = null }
                } else {
                    null
                },
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            when {
                showingSources -> FeedSourceManagementScreen(
                    sources = sources,
                    onVerifyAndAdd = { source ->
                        repository.verifyAndAdd(source)
                    },
                    onRemoveSource = { source ->
                        scope.launch { repository.removeSource(source) }
                    },
                    onBack = {
                        showingSources = false
                        showingDriveSettings = false
                        showingSettings = sourcesReturnToSettings
                        sourcesReturnToSettings = false
                    },
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
                    onUpdateSourceMetadata = { source ->
                        repository.updateSourceMetadata(source)
                    },
                    driveSettingsOnly = showingDriveSettings,
                    backToSettings = sourcesReturnToSettings,
                )
                showingSettings -> AppSettingsScreen(
                    preferences = appPreferences,
                    onPreferencesChange = onAppPreferencesChange,
                    onManageSources = {
                        sourcesReturnToSettings = true
                        showingDriveSettings = false
                        showingSettings = false
                        showingSources = true
                    },
                    onGoogleDriveSync = {
                        sourcesReturnToSettings = true
                        showingDriveSettings = true
                        showingSettings = false
                        showingSources = true
                    },
                    onBack = { showingSettings = false },
                )
                selectedArticle != null -> key(
                    selectedArticle!!.sourceUrl,
                    selectedArticle!!.id,
                ) {
                    FeedArticleDetailScreen(
                        article = selectedArticle!!,
                        onOpenOriginalArticle = { link ->
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                                true
                            } catch (_: ActivityNotFoundException) {
                                false
                            }
                        },
                        onVote = { userVote ->
                            val saved = repository.updateArticleVote(selectedArticle!!, userVote)
                            if (saved) selectedArticle = selectedArticle?.copy(userVote = userVote)
                            saved
                        },
                        publisherPageLoader = publisherPageLoader,
                        relatedArticles = recommendSimilarArticles(
                            selectedArticle!!,
                            visibleArticles,
                        ),
                        onOpenRelatedArticle = { related ->
                            selectedArticle = related
                        },
                        readingScale = appPreferences.readingScale,
                        onReadingScaleChange = { scale ->
                            onAppPreferencesChange(
                                appPreferences.copy(
                                    readingScale = scale.coerceIn(
                                        MIN_READING_SCALE,
                                        MAX_READING_SCALE,
                                    ),
                                ),
                            )
                        },
                    )
                }
                else -> {
                    LaunchedEffect(scrollFeedToTopOnHome) {
                        if (scrollFeedToTopOnHome) {
                            feedListState.animateScrollToItem(0)
                            scrollFeedToTopOnHome = false
                        }
                    }
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.fillMaxSize()) {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = refreshFeeds,
                    modifier = Modifier.fillMaxSize(),
                ) {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = READABLE_CONTENT_MAX_WIDTH)
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .align(Alignment.TopCenter)
                        .keyboardFocusTraversal(feedListState)
                        .padding(24.dp)
                        .testTag("feedArticleList"),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    state = feedListState,
                ) {
                    if (articles.isEmpty() && sources.isEmpty()) {
                        item {
                            Text(
                                text = "Your feed is empty. Add sources to set up your reading list.",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                    if (isFeedSearchVisible) {
                        item {
                            OutlinedTextField(
                                value = feedSearchQuery,
                                onValueChange = { feedSearchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("feedSearchField"),
                                label = { Text("Search loaded articles") },
                                singleLine = true,
                            )
                        }
                    }
                    refreshMessage?.let { message ->
                        item {
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    if (articles.isEmpty() && sources.isNotEmpty() && !isRefreshing) {
                        item { Text("No articles loaded. Refresh feeds to try again.") }
                    }
                    if (
                        focusModeEnabled &&
                        articles.isNotEmpty() &&
                        visibleArticles.isEmpty()
                    ) {
                        item { Text("No Level 1 articles yet. Turn off Focus Mode to see all sources.") }
                    }
                    if (visibleArticles.isNotEmpty() && filteredArticles.isEmpty()) {
                        item { Text("No articles match \"$searchQuery\".") }
                    }
                    items(
                        items = filteredArticles,
                        key = { article -> "${article.sourceUrl}|${article.id}" },
                    ) { article ->
                        FeedArticleListItem(article = article) {
                            selectedArticle = article
                        }
                    }
                }
                }
                }
            }
        }
        }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            AppBottomBar(
                focusModeEnabled = focusModeEnabled,
                settingsSelected = showingSettings || (showingSources && sourcesReturnToSettings),
                homeSelected = !showingSources && !showingSettings && selectedArticle == null,
                isFeedSearchVisible = isFeedSearchVisible,
                isRefreshing = isRefreshing,
                canRefresh = sources.isNotEmpty(),
                onSettingsClick = {
                    showingSources = false
                    showingDriveSettings = false
                    showingSettings = true
                    sourcesReturnToSettings = false
                },
                onHomeClick = {
                    scrollFeedToTopOnHome = true
                    showingSources = false
                    showingDriveSettings = false
                    showingSettings = false
                    sourcesReturnToSettings = false
                    selectedArticle = null
                },
                onFocusModeClick = { focusModeEnabled = !focusModeEnabled },
                onFeedSearchClick = {
                    isFeedSearchVisible = !isFeedSearchVisible
                    if (!isFeedSearchVisible) feedSearchQuery = ""
                },
                onRefreshClick = refreshFeeds,
            )
        }
    }
}

@Composable
internal fun FeedArticleListItem(
    article: FeedArticle,
    testTag: String? = null,
    onClick: () -> Unit,
) {
    AppArticleSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = "Open article: ${article.title}"
            }
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(vertical = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.testTag("feedArticleTitle"),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = article.sourceTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .testTag("feedArticleSource"),
                )
                formatArticleDate(article.publishedAtMillis)?.let { date ->
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("feedArticleDate"),
                    )
                }
            }
            article.excerpt?.let(::cleanArticleText)?.takeIf(String::isNotEmpty)?.let { excerpt ->
                Text(
                    text = excerpt,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 5,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("feedArticleSummary"),
                )
            }
        }
    }
}
