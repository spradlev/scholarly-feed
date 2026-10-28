package dev.spradlev.scholarlyfeed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        setContent {
            MaterialTheme {
                ScholarlyFeedScreen()
            }
        }
    }
}

@Composable
private fun ScholarlyFeedScreen() {
    var focusModeEnabled by rememberSaveable { mutableStateOf(false) }
    var showingSources by rememberSaveable { mutableStateOf(false) }
    val sources = remember { mutableStateListOf<FeedSource>() }

    BackHandler(enabled = showingSources) {
        showingSources = false
    }

    if (showingSources) {
        FeedSourceManagementScreen(
            sources = sources,
            onAddSource = { sources.add(it) },
            onRemoveSource = { sources.remove(it) },
            onBack = { showingSources = false },
        )
    } else {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Scholarly Feed",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "Your feed is empty. Add sources to set up your reading list.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "Feeds aren't fetched in this version.",
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
                TextButton(onClick = { showingSources = true }) {
                    Text("Manage sources (${sources.size})")
                }
            }
        }
    }
}
