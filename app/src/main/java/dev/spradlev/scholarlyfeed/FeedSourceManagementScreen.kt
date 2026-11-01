package dev.spradlev.scholarlyfeed

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.content.IntentSender
import androidx.activity.result.IntentSenderRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope

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
    val feedUrl: String,
    val level: Int,
    val topics: String,
)

private val sourceCandidates = listOf(
    SourceCandidate(
        "ScienceDaily",
        "https://www.sciencedaily.com/rss/all.xml",
        1,
        "science research health",
    ),
    SourceCandidate(
        "Nature",
        "https://www.nature.com/nature.rss",
        1,
        "research science medicine biology",
    ),
    SourceCandidate("arXiv: Computer Science", "https://rss.arxiv.org/rss/cs", 1, "research preprints computing"),
    SourceCandidate("arXiv: AI", "https://rss.arxiv.org/rss/cs.AI", 1, "research preprints artificial intelligence machine learning"),
    SourceCandidate(
        "bioRxiv",
        "https://connect.biorxiv.org/biorxiv_xml.php?subject=all",
        1,
        "research preprints biology",
    ),
    SourceCandidate(
        "The Conversation: US",
        "https://theconversation.com/us/articles.atom",
        2,
        "academic commentary analysis",
    ),
    SourceCandidate(
        "Quanta Magazine",
        "https://www.quantamagazine.org/feed/",
        2,
        "mathematics physics biology science",
    ),
    SourceCandidate("NPR News", "https://feeds.npr.org/1001/rss.xml", 3, "national global civic affairs"),
    SourceCandidate(
        "Washington State Standard",
        "https://washingtonstatestandard.com/feed/",
        4,
        "Washington state civic affairs",
    ),
)

@Composable
internal fun FeedSourceManagementScreen(
    sources: List<FeedSource>,
    onVerifyAndAdd: suspend (FeedSource) -> ParsedFeed,
    onRemoveSource: (FeedSource) -> Unit,
    onBack: () -> Unit,
    onSearchWeb: (String) -> Boolean,
    onImportOpml: suspend (List<OpmlSource>) -> OpmlImportResult,
    onUpdateSourceMetadata: suspend (FeedSource) -> Unit,
    driveSettingsOnly: Boolean = false,
    backToSettings: Boolean = false,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val driveScrollState = rememberScrollState()
    val sourceScrollState = rememberScrollState()
    val discoveryScrollState = rememberScrollState()
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
    var isDriveConnected by remember(context) {
        mutableStateOf(
            context.getSharedPreferences(DRIVE_PREFERENCES, 0)
                .getBoolean(DRIVE_CONNECTED_KEY, false),
        )
    }
    var isSyncingDrive by remember { mutableStateOf(false) }
    var driveMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDriveSync by remember { mutableStateOf<PendingDriveSync?>(null) }
    var useDriveConflictValues by remember { mutableStateOf(false) }
    val selectedSourceLevel = sourceLevels.first { it.number == selectedLevel }
    val driveStore = remember { GoogleDriveSourceStore() }
    val authorizationClient = remember(context) { Identity.getAuthorizationClient(context) }

    fun setDriveConnected(connected: Boolean) {
        isDriveConnected = connected
        context.getSharedPreferences(DRIVE_PREFERENCES, 0)
            .edit()
            .putBoolean(DRIVE_CONNECTED_KEY, connected)
            .apply()
    }

    suspend fun applyDriveSync(
        accessToken: String,
        remoteFile: DriveSourceFile?,
        remoteSources: List<OpmlSource>,
        useDriveConflictValues: Boolean,
    ) {
        val plan = planSourceSync(sources, remoteSources)
        val importResult = onImportOpml(plan.driveOnly)
        if (importResult.errors.isNotEmpty()) {
            driveMessage = buildString {
                append("Added ${importResult.importedCount} source")
                if (importResult.importedCount != 1) append("s")
                append(" from Drive, but some feeds could not be verified. The Drive copy was not changed.")
                append(" ")
                append(importResult.errors.take(2).joinToString(" "))
                if (importResult.errors.size > 2) append(" And ${importResult.errors.size - 2} more.")
            }
            return
        }

        if (useDriveConflictValues) {
            plan.conflicts.forEach { conflict ->
                val local = sources.first { it.feedUrl == conflict.local.feedUrl }
                if (local.title != conflict.drive.title || local.level != conflict.drive.level) {
                    onUpdateSourceMetadata(
                        local.copy(
                            title = conflict.drive.title,
                            level = conflict.drive.level,
                        ),
                    )
                }
            }
        }

        val mergedSources = mergeSourceLists(sources, remoteSources, useDriveConflictValues)
        val fileId = driveStore.writeSourceFile(accessToken, remoteFile?.id, mergedSources)
        setDriveConnected(true)
        driveMessage = "Synced ${mergedSources.size} source${if (mergedSources.size == 1) "" else "s"} with Google Drive."
    }

    fun continueDriveSync(accessToken: String) {
        scope.launch {
            isSyncingDrive = true
            driveMessage = null
            try {
                val remoteFile = driveStore.readSourceFile(accessToken)
                setDriveConnected(true)
                val remoteSources = remoteFile?.sources.orEmpty()
                val plan = planSourceSync(sources, remoteSources)
                if (plan.conflicts.isNotEmpty()) {
                    useDriveConflictValues = false
                    pendingDriveSync = PendingDriveSync(accessToken, remoteFile, remoteSources)
                } else if (remoteFile == null && sources.isEmpty()) {
                    driveMessage = "Add a source first, then sync it with Google Drive."
                } else {
                    applyDriveSync(
                        accessToken = accessToken,
                        remoteFile = remoteFile,
                        remoteSources = remoteSources,
                        useDriveConflictValues = false,
                    )
                }
            } catch (exception: GoogleDriveSyncException) {
                driveMessage = exception.message ?: "Could not sync sources with Google Drive."
                if (driveMessage?.contains("access was denied or expired") == true) {
                    setDriveConnected(false)
                }
            } catch (exception: OpmlFormatException) {
                driveMessage = exception.message ?: "The Google Drive source list is invalid."
            } catch (exception: IOException) {
                driveMessage = exception.message ?: "Could not sync sources with Google Drive."
            } catch (exception: SQLiteException) {
                driveMessage = exception.message ?: "Could not save verified Google Drive sources on this device."
            } finally {
                if (pendingDriveSync == null) isSyncingDrive = false
            }
        }
    }

    fun handleAuthorizationResult(result: AuthorizationResult) {
        if (!result.grantedScopes.contains(DRIVE_APP_DATA_SCOPE.scopeUri)) {
            isSyncingDrive = false
            driveMessage =
                "Google didn't return Drive app-data access. Check that Drive API is enabled and " +
                    "drive.appdata is allowed in OAuth Data Access for this project. Also verify " +
                    "this app's package and signing certificate are registered. Your sources remain on this device."
            return
        }
        val accessToken = result.accessToken
        if (accessToken.isNullOrBlank()) {
            isSyncingDrive = false
            driveMessage = "Google Drive did not provide an access token. Try again."
            return
        }
        continueDriveSync(accessToken)
    }

    val driveAuthorizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            try {
                handleAuthorizationResult(
                    authorizationClient.getAuthorizationResultFromIntent(result.data!!),
                )
            } catch (exception: ApiException) {
                isSyncingDrive = false
                driveMessage = exception.message ?: "Google Drive authorization failed. Try again."
            }
        } else {
            isSyncingDrive = false
            driveMessage = "Google Drive authorization was canceled. Your sources remain on this device."
        }
    }

    fun requestDriveSync() {
        isSyncingDrive = true
        driveMessage = null
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(DRIVE_APP_DATA_SCOPE))
            .build()
        authorizationClient.authorize(request)
            .addOnSuccessListener { authorizationResult ->
                if (authorizationResult.hasResolution()) {
                    val pendingIntent = authorizationResult.pendingIntent
                    if (pendingIntent == null) {
                        isSyncingDrive = false
                        driveMessage = "Google Drive needs permission, but no authorization prompt was available."
                    } else {
                        try {
                            driveAuthorizationLauncher.launch(
                                IntentSenderRequest.Builder(pendingIntent).build(),
                            )
                        } catch (exception: IntentSender.SendIntentException) {
                            isSyncingDrive = false
                            driveMessage = exception.message ?: "Could not open Google Drive authorization."
                        }
                    }
                } else {
                    handleAuthorizationResult(authorizationResult)
                }
            }
            .addOnFailureListener { exception ->
                isSyncingDrive = false
                driveMessage = exception.message ?: "Could not authorize Google Drive on this device."
            }
    }

    fun disconnectGoogleDrive() {
        isSyncingDrive = true
        val request = RevokeAccessRequest.builder()
            .setScopes(listOf(DRIVE_APP_DATA_SCOPE))
            .build()
        authorizationClient.revokeAccess(request)
            .addOnSuccessListener {
                setDriveConnected(false)
                isSyncingDrive = false
                driveMessage = "Google Drive access disconnected. The private Drive copy was left unchanged."
            }
            .addOnFailureListener { exception ->
                isSyncingDrive = false
                driveMessage = exception.message ?: "Could not disconnect Google Drive access."
            }
    }

    fun resolveDriveSync(pending: PendingDriveSync, useDriveConflictValues: Boolean) {
        scope.launch {
            try {
                applyDriveSync(
                    accessToken = pending.accessToken,
                    remoteFile = pending.remoteFile,
                    remoteSources = pending.remoteSources,
                    useDriveConflictValues = useDriveConflictValues,
                )
            } catch (exception: GoogleDriveSyncException) {
                driveMessage = exception.message ?: "Could not sync sources with Google Drive."
            } catch (exception: OpmlFormatException) {
                driveMessage = exception.message ?: "The Google Drive source list is invalid."
            } catch (exception: IOException) {
                driveMessage = exception.message ?: "Could not sync sources with Google Drive."
            } catch (exception: SQLiteException) {
                driveMessage = exception.message ?: "Could not save Google Drive source details on this device."
            } finally {
                pendingDriveSync = null
                isSyncingDrive = false
            }
        }
    }

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
                } catch (exception: SQLiteException) {
                    opmlMessage = "Couldn't save imported sources. Check available device storage and try again."
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

    if (driveSettingsOnly) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .widthIn(max = READABLE_CONTENT_MAX_WIDTH)
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .align(Alignment.TopCenter)
                        .verticalScroll(driveScrollState)
                        .keyboardFocusTraversal(driveScrollState)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    TextButton(onClick = onBack) {
                        Text("Back to settings")
                    }
                    Text(
                        text = "Google Drive sync",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        "Optional manual sync stores only this source list in the app's private Drive data. Cached articles stay on this device. New feeds from Drive are verified before they are added.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        onClick = ::requestDriveSync,
                        enabled = !isSyncingDrive,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("googleDriveSyncButton"),
                    ) {
                        Text(
                            when {
                                isSyncingDrive -> "Syncing sources…"
                                isDriveConnected -> "Sync sources with Google Drive"
                                else -> "Connect Google Drive and sync"
                            },
                        )
                    }
                    if (isDriveConnected) {
                        TextButton(
                            onClick = ::disconnectGoogleDrive,
                            enabled = !isSyncingDrive,
                            modifier = Modifier.testTag("disconnectGoogleDriveButton"),
                        ) {
                            Text("Disconnect Google Drive")
                        }
                    }
                    driveMessage?.let { message ->
                        Text(
                            text = message,
                            color = if (
                                message.contains("could not", ignoreCase = true) ||
                                message.contains("failed", ignoreCase = true) ||
                                message.contains("not granted", ignoreCase = true) ||
                                message.contains("denied", ignoreCase = true) ||
                                message.contains("invalid", ignoreCase = true) ||
                                message.contains("not available", ignoreCase = true) ||
                                message.contains("check", ignoreCase = true) ||
                                message.contains("canceled", ignoreCase = true)
                            ) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        )
                    }
                }
            }
        }
    }

    if (driveSettingsOnly) return

    if (showingDiscovery) {
        SourceDiscoveryScreen(
            onBack = { showingDiscovery = false },
            onSelect = { candidate ->
                title = candidate.name
                selectedLevel = candidate.level
                feedUrl = candidate.feedUrl
                errorMessage = null
                showingDiscovery = false
            },
            onSearchWeb = onSearchWeb,
            scrollState = discoveryScrollState,
        )
        return
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .widthIn(max = READABLE_CONTENT_MAX_WIDTH)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .align(Alignment.TopCenter)
                    .verticalScroll(sourceScrollState)
                    .keyboardFocusTraversal(sourceScrollState)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("backToFeedButton"),
                ) {
                    Text(if (backToSettings) "Back to settings" else "Back to feed")
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
                            } catch (exception: SQLiteException) {
                                opmlMessage = "Couldn't save starter sources. Check available device storage and try again."
                                opmlMessageIsError = true
                            } finally {
                                isAddingStarterSources = false
                                isImportingOpml = false
                            }
                        }
                    },
                    enabled = !isImportingOpml && !isSyncingDrive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("addStarterSourcesButton"),
                ) {
                    Text(if (isAddingStarterSources) "Adding…" else "Add starter sources")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { importOpmlLauncher.launch(arrayOf("*/*")) },
                        enabled = !isImportingOpml && !isSyncingDrive,
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
                    enabled = !isSyncingDrive,
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
                    enabled = !isSyncingDrive,
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
                        enabled = !isSyncingDrive,
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
                    enabled = !isVerifying && !isSyncingDrive && title.isNotBlank() && feedUrl.isNotBlank(),
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
                            enabled = !isSyncingDrive,
                            onRemove = { onRemoveSource(source) },
                        )
                    }
                }
            }
        }
    }

    pendingDriveSync?.let { pending ->
        val conflicts = planSourceSync(sources, pending.remoteSources).conflicts
        val conflictSummary = conflicts
            .take(3)
            .joinToString("\n") { conflict ->
                "${conflict.local.title} / ${conflict.drive.title}"
            }
        AlertDialog(
            onDismissRequest = {
                pendingDriveSync = null
                isSyncingDrive = false
                driveMessage = "Sync canceled. Neither list was replaced."
            },
            title = { Text("Resolve source-list differences") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        buildString {
                            append(
                                "${conflicts.size} feed URL${if (conflicts.size == 1) " has" else "s have"} different names or levels on this device and Drive.",
                            )
                            if (conflictSummary.isNotEmpty()) {
                                append("\n\n")
                                append(conflictSummary)
                            }
                            if (conflicts.size > 3) {
                                append("\nAnd ${conflicts.size - 3} more.")
                            }
                            append("\n\nOther sources will be merged. Entries removed on one side are retained from the other; deletions are not synced.")
                        },
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = !useDriveConflictValues,
                            onClick = { useDriveConflictValues = false },
                        )
                        TextButton(onClick = { useDriveConflictValues = false }) {
                            Text("Keep this device's details")
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = useDriveConflictValues,
                            onClick = { useDriveConflictValues = true },
                        )
                        TextButton(onClick = { useDriveConflictValues = true }) {
                            Text("Use Drive's details")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { resolveDriveSync(pending, useDriveConflictValues) }) {
                    Text("Apply choice")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingDriveSync = null
                        isSyncingDrive = false
                        driveMessage = "Sync canceled. Neither list was replaced."
                    },
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}

private const val OPML_MIME_TYPE = "text/x-opml"
private const val DRIVE_PREFERENCES = "google_drive_sync"
private const val DRIVE_CONNECTED_KEY = "connected"
private val DRIVE_APP_DATA_SCOPE = Scope("https://www.googleapis.com/auth/drive.appdata")

private data class PendingDriveSync(
    val accessToken: String,
    val remoteFile: DriveSourceFile?,
    val remoteSources: List<OpmlSource>,
)

@Composable
private fun SourceDiscoveryScreen(
    onBack: () -> Unit,
    onSelect: (SourceCandidate) -> Unit,
    onSearchWeb: (String) -> Boolean,
    scrollState: ScrollState,
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
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .widthIn(max = READABLE_CONTENT_MAX_WIDTH)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .align(Alignment.TopCenter)
                    .verticalScroll(scrollState)
                    .keyboardFocusTraversal(scrollState)
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
                                        Text(candidate.feedUrl, style = MaterialTheme.typography.bodySmall)
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
}

@Composable
private fun SourceListItem(
    source: FeedSource,
    enabled: Boolean,
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
            enabled = enabled,
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
