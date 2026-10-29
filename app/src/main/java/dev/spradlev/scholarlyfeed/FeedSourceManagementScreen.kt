package dev.spradlev.scholarlyfeed

import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.io.IOException

private data class SourceLevel(
    val number: Int,
    val description: String,
) {
    val label: String
        get() = "Level $number: $description"
}

private val sourceLevels = listOf(
    SourceLevel(1, "Research and scientific reporting"),
    SourceLevel(2, "Academic commentary and analysis"),
    SourceLevel(3, "National and global civic affairs"),
    SourceLevel(4, "Local and regional civic affairs"),
)

private data class SourceCandidate(
    val name: String,
    val level: Int,
    val topics: String,
)

private val sourceCandidates = listOf(
    SourceCandidate("EurekAlert", 1, "science research health"),
    SourceCandidate("Phys.org", 1, "science technology environment"),
    SourceCandidate("ScienceDaily", 1, "science health research"),
    SourceCandidate("Nature", 1, "research science medicine"),
    SourceCandidate("arXiv", 1, "research preprints physics mathematics computing"),
    SourceCandidate("bioRxiv", 1, "research preprints biology"),
    SourceCandidate("PubMed", 1, "biomedical research medicine health"),
    SourceCandidate("The Conversation", 2, "academic commentary analysis"),
    SourceCandidate("Quanta Magazine", 2, "mathematics physics biology science"),
    SourceCandidate("Arts & Letters Daily", 2, "culture commentary essays"),
    SourceCandidate("NPR", 3, "national global civic affairs"),
    SourceCandidate("South Seattle Emerald", 4, "Seattle local civic affairs"),
    SourceCandidate("KUOW", 4, "Seattle Washington local public affairs"),
    SourceCandidate("Washington State Standard", 4, "Washington state civic affairs"),
)

@Composable
internal fun FeedSourceManagementScreen(
    sources: List<FeedSource>,
    onVerifyAndAdd: suspend (FeedSource) -> ParsedFeed,
    onRemoveSource: (FeedSource) -> Unit,
    onBack: () -> Unit,
    onSearchWeb: (String) -> Boolean,
    onImportOpml: suspend (List<OpmlSource>) -> OpmlImportResult,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var feedUrl by rememberSaveable { mutableStateOf("") }
    var selectedLevel by rememberSaveable { mutableIntStateOf(1) }
    var levelMenuExpanded by remember { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    var showingDiscovery by rememberSaveable { mutableStateOf(false) }
    var opmlMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var opmlMessageIsError by rememberSaveable { mutableStateOf(false) }
    var isImportingOpml by remember { mutableStateOf(false) }
    var isAddingStarterSources by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val selectedSourceLevel = sourceLevels.first { it.number == selectedLevel }

    suspend fun importOpmlSources(opmlSources: List<OpmlSource>) {
        val result = onImportOpml(opmlSources)
        opmlMessage = buildString {
            append("Imported ${result.importedCount} source")
            if (result.importedCount != 1) append("s")
            append(".")
            if (result.errors.isNotEmpty()) {
                append(" ")
                append(result.errors.take(3).joinToString(" "))
                if (result.errors.size > 3) append(" And ${result.errors.size - 3} more.")
            }
        }
        opmlMessageIsError = result.importedCount == 0 || result.errors.isNotEmpty()
    }

    val importOpmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isImportingOpml = true
                opmlMessage = null
                try {
                    importOpmlSources(readOpmlDocument(context, uri))
                } catch (exception: IOException) {
                    opmlMessage = exception.message ?: "Could not read the selected OPML file."
                    opmlMessageIsError = true
                } catch (exception: OpmlFormatException) {
                    opmlMessage = exception.message ?: "The selected file is not valid OPML."
                    opmlMessageIsError = true
                } catch (exception: SecurityException) {
                    opmlMessage = exception.message ?: "Permission to read the selected OPML file was denied."
                    opmlMessageIsError = true
                } finally {
                    isImportingOpml = false
                }
            }
        }
    }
    val exportOpmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(OPML_MIME_TYPE),
    ) { uri ->
        if (uri != null) {
            try {
                writeOpmlDocument(context, uri, createOpml(sources))
                opmlMessage = "Exported ${sources.size} source${if (sources.size == 1) "" else "s"}."
                opmlMessageIsError = false
            } catch (exception: IOException) {
                opmlMessage = exception.message ?: "Could not write the OPML file."
                opmlMessageIsError = true
            } catch (exception: OpmlFormatException) {
                opmlMessage = exception.message ?: "Could not create the OPML file."
                opmlMessageIsError = true
            } catch (exception: SecurityException) {
                opmlMessage = exception.message ?: "Permission to write the selected OPML file was denied."
                opmlMessageIsError = true
            }
        }
    }

    if (showingDiscovery) {
        SourceDiscoveryScreen(
            onBack = { showingDiscovery = false },
            onSelect = { candidate ->
                title = candidate.name
                selectedLevel = candidate.level
                feedUrl = ""
                errorMessage = null
                showingDiscovery = false
            },
            onSearchWeb = onSearchWeb,
        )
        return
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextButton(onClick = onBack) {
                Text("Back to feed")
            }
            Text(
                text = "Sources",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Verify an RSS or Atom feed before adding it. Verified sources are saved on this device.",
                style = MaterialTheme.typography.bodyLarge,
            )
            TextButton(
                onClick = { showingDiscovery = true },
                modifier = Modifier.testTag("discoverSourcesButton"),
            ) {
                Text("Discover publishers")
            }
            Text("OPML source lists", style = MaterialTheme.typography.titleMedium)
            Text(
                "Add the starter feeds or import an OPML file. Every feed is verified before it is saved. You can also export your verified sources.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = {
                    scope.launch {
                        isImportingOpml = true
                        isAddingStarterSources = true
                        opmlMessage = null
                        try {
                            importOpmlSources(readStarterOpml(context))
                        } catch (exception: IOException) {
                            opmlMessage = exception.message ?: "Could not read the starter source list."
                            opmlMessageIsError = true
                        } catch (exception: OpmlFormatException) {
                            opmlMessage = exception.message ?: "The starter source list is invalid."
                            opmlMessageIsError = true
                        } finally {
                            isAddingStarterSources = false
                            isImportingOpml = false
                        }
                    }
                },
                enabled = !isImportingOpml,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("addStarterSourcesButton"),
            ) {
                Text(if (isAddingStarterSources) "Adding…" else "Add starter sources")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { importOpmlLauncher.launch(arrayOf("*/*")) },
                    enabled = !isImportingOpml,
                    modifier = Modifier.testTag("importOpmlButton"),
                ) {
                    Text(if (isImportingOpml && !isAddingStarterSources) "Importing…" else "Import OPML")
                }
                TextButton(
                    onClick = { exportOpmlLauncher.launch("scholarly-feed-sources.opml") },
                    enabled = sources.isNotEmpty(),
                    modifier = Modifier.testTag("exportOpmlButton"),
                ) {
                    Text("Export OPML")
                }
            }
            opmlMessage?.let { message ->
                Text(
                    text = message,
                    color = if (opmlMessageIsError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                )
            }
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    errorMessage = null
                },
                label = { Text("Source name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sourceTitleInput"),
            )
            OutlinedTextField(
                value = feedUrl,
                onValueChange = {
                    feedUrl = it
                    errorMessage = null
                },
                label = { Text("Public HTTPS feed URL") },
                supportingText = { Text("The app fetches the feed to verify it and will load it again when refreshed.") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sourceUrlInput"),
            )
            Column {
                Text(
                    text = "Source level",
                    style = MaterialTheme.typography.labelLarge,
                )
                TextButton(
                    onClick = { levelMenuExpanded = true },
                    modifier = Modifier.testTag("sourceLevelSelector"),
                ) {
                    Text(selectedSourceLevel.label)
                }

                DropdownMenu(
                    expanded = levelMenuExpanded,
                    onDismissRequest = { levelMenuExpanded = false },
                ) {
                    sourceLevels.forEach { level ->
                        DropdownMenuItem(
                            text = { Text(level.label) },
                            onClick = {
                                selectedLevel = level.number
                                levelMenuExpanded = false
                            },
                        )
                    }
                }

            }
            Button(
                onClick = {
                    val normalizedTitle = title.trim()
                    val normalizedUrl = feedUrl.trim()
                    when {
                        normalizedTitle.isEmpty() -> {
                            errorMessage = "Enter a source name."
                        }
                        !isHttpsUrl(normalizedUrl) -> {
                            errorMessage = "Enter a valid HTTPS URL with a host."
                        }
                        sources.any { it.feedUrl == normalizedUrl } -> {
                            errorMessage = "This feed URL is already in your list."
                        }
                        else -> {
                            scope.launch {
                                isVerifying = true
                                errorMessage = null
                                try {
                                    val source = FeedSource(
                                        feedUrl = normalizedUrl,
                                        title = normalizedTitle,
                                        feedTitle = "",
                                        level = selectedLevel,
                                        verifiedAtMillis = 0L,
                                    )
                                    val parsedFeed = onVerifyAndAdd(source)
                                    title = ""
                                    feedUrl = ""
                                    selectedLevel = 1
                                    errorMessage = "Verified ${parsedFeed.title} and added it."
                                } catch (exception: FeedFetchException) {
                                    errorMessage = exception.message ?: "Could not verify this feed."
                                } catch (exception: SQLiteConstraintException) {
                                    errorMessage = "This feed URL is already in your list."
                                } finally {
                                    isVerifying = false
                                }
                            }
                        }
                    }
                },
                enabled = !isVerifying && title.isNotBlank() && feedUrl.isNotBlank(),
            ) {
                Text(if (isVerifying) "Verifying feed…" else "Verify and add")
            }
            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                text = "Verified sources",
                style = MaterialTheme.typography.titleMedium,
            )
            if (sources.isEmpty()) {
                Text("No verified sources yet.")
            } else {
                sources.forEach { source ->
                    SourceListItem(
                        source = source,
                        onRemove = { onRemoveSource(source) },
                    )
                }
            }
        }
    }
}

private const val OPML_MIME_TYPE = "text/x-opml"

@Composable
private fun SourceDiscoveryScreen(
    onBack: () -> Unit,
    onSelect: (SourceCandidate) -> Unit,
    onSearchWeb: (String) -> Boolean,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var searchError by rememberSaveable { mutableStateOf(false) }
    val matchingCandidates = remember(query) {
        val searchTerms = query.trim().split(Regex("\\s+")).filter(String::isNotEmpty)
        sourceCandidates.filter { candidate ->
            val searchableText = "${candidate.name} ${candidate.topics} ${
                sourceLevels.first { it.number == candidate.level }.description
            }"
            searchTerms.all { term -> searchableText.contains(term, ignoreCase = true) }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextButton(onClick = onBack) {
                Text("Back to sources")
            }
            Text(
                text = "Discover publishers",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Search this starter directory or search the web for feed links. Directory entries are candidates, not verified feeds.",
                style = MaterialTheme.typography.bodyLarge,
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search publishers or topics") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sourceDiscoverySearch"),
            )
            Button(
                onClick = {
                    searchError = !onSearchWeb(query.trim())
                },
                enabled = query.isNotBlank(),
                modifier = Modifier.testTag("searchWebButton"),
            ) {
                Text("Search the web for RSS feeds")
            }
            if (searchError) {
                Text(
                    text = "No web search app is available. You can still search the built-in directory or enter a feed URL.",
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (matchingCandidates.isEmpty()) {
                Text("No candidates match. Try another publisher or topic, or search the web.")
            } else {
                matchingCandidates.forEach { candidate ->
                    val level = sourceLevels.first { it.number == candidate.level }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(candidate.name, style = MaterialTheme.typography.titleMedium)
                            Text(candidate.topics, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Candidate · ${level.label}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        TextButton(
                            onClick = { onSelect(candidate) },
                            modifier = Modifier.testTag("selectCandidate-${candidate.name}"),
                        ) {
                            Text("Use")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceListItem(
    source: FeedSource,
    onRemove: () -> Unit,
) {
    val sourceLevel = sourceLevels.first { it.number == source.level }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(source.title, style = MaterialTheme.typography.titleSmall)
            Text("Feed: ${source.feedTitle}", style = MaterialTheme.typography.bodyMedium)
            Text(source.feedUrl, style = MaterialTheme.typography.bodyMedium)
            Text(sourceLevel.label, style = MaterialTheme.typography.bodySmall)
            Text(
                "Verified ${java.text.DateFormat.getDateInstance().format(java.util.Date(source.verifiedAtMillis))}",
                style = MaterialTheme.typography.bodySmall,
            )
            source.lastSuccessfulRefreshAtMillis?.let { refreshedAtMillis ->
                Text(
                    "Last refreshed ${
                        java.text.DateFormat.getDateTimeInstance().format(java.util.Date(refreshedAtMillis))
                    }",
                    style = MaterialTheme.typography.bodySmall,
                )
            } ?: Text("Not refreshed yet.", style = MaterialTheme.typography.bodySmall)
        }
        TextButton(
            onClick = onRemove,
            modifier = Modifier.semantics {
                contentDescription = "Remove ${source.title}"
            },
        ) {
            Text("Remove")
        }
    }
}

private fun isHttpsUrl(value: String): Boolean {
    val uri = Uri.parse(value)
    return uri.scheme.equals("https", ignoreCase = true) &&
        !uri.host.isNullOrBlank() &&
        uri.userInfo == null
}
