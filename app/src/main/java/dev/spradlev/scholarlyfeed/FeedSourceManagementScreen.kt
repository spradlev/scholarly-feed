package dev.spradlev.scholarlyfeed

import android.net.Uri
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

internal data class FeedSource(
    val title: String,
    val feedUrl: String,
    val level: Int,
)

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

@Composable
internal fun FeedSourceManagementScreen(
    sources: List<FeedSource>,
    onAddSource: (FeedSource) -> Unit,
    onRemoveSource: (FeedSource) -> Unit,
    onBack: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var feedUrl by rememberSaveable { mutableStateOf("") }
    var selectedLevel by rememberSaveable { mutableIntStateOf(1) }
    var levelMenuExpanded by remember { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedSourceLevel = sourceLevels.first { it.number == selectedLevel }

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
                text = "Add and organize feed sources. They are kept only while the app is open and aren't verified or fetched yet.",
                style = MaterialTheme.typography.bodyLarge,
            )
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
                label = { Text("HTTPS feed URL") },
                supportingText = { Text("The URL won't be checked or opened yet.") },
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
                        !isHttpsUrl(normalizedUrl) -> {
                            errorMessage = "Enter a valid HTTPS URL with a host."
                        }
                        sources.any { it.feedUrl == normalizedUrl } -> {
                            errorMessage = "This feed URL is already in your list."
                        }
                        else -> {
                            onAddSource(
                                FeedSource(
                                    title = normalizedTitle,
                                    feedUrl = normalizedUrl,
                                    level = selectedLevel,
                                ),
                            )
                            title = ""
                            feedUrl = ""
                            selectedLevel = 1
                            errorMessage = null
                        }
                    }
                },
                enabled = title.isNotBlank() && feedUrl.isNotBlank(),
            ) {
                Text("Add source")
            }
            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                text = "Sources added this session",
                style = MaterialTheme.typography.titleMedium,
            )
            if (sources.isEmpty()) {
                Text("No sources added yet.")
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
            Text(source.feedUrl, style = MaterialTheme.typography.bodyMedium)
            Text(sourceLevel.label, style = MaterialTheme.typography.bodySmall)
            Text("Not verified", style = MaterialTheme.typography.bodySmall)
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
    return uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
}
