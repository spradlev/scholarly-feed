// SPDX-License-Identifier: AGPL-3.0-or-later

package dev.spradlev.scholarlyfeed

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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
internal fun AppSettingsScreen(
    preferences: AppPreferences,
    onPreferencesChange: (AppPreferences) -> Unit,
    onManageSources: () -> Unit,
    onGoogleDriveSync: () -> Unit,
    onBack: () -> Unit,
) {
    val scrollState = rememberScrollState()
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
                    .padding(24.dp)
                    .testTag("appSettingsScreen"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppToolbarButton("Back to feed", onBack)
                Text(
                    "Accessibility & display",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Choose a comfortable in-app text size and color scheme. Android's system font-size setting still applies.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                HorizontalDivider()
                Text(
                    "Text size",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                AppArticleSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .selectableGroup(),
                    ) {
                        AppTextSize.entries.forEach { option ->
                            PreferenceRadioRow(
                                label = option.label,
                                selected = preferences.textSize == option,
                                testTag = "fontSizeOption_${option.name}",
                                onClick = {
                                    onPreferencesChange(preferences.copy(textSize = option))
                                },
                            )
                        }
                    }
                }
                Text(
                    "Article reading size",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Pinch article text to zoom, or adjust the size here. Text reflows to fit the screen.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Reading size: ${(preferences.readingScale * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag("readingScaleValue"),
                )
                Slider(
                    value = preferences.readingScale,
                    onValueChange = { scale ->
                        onPreferencesChange(preferences.copy(readingScale = scale))
                    },
                    valueRange = MIN_READING_SCALE..MAX_READING_SCALE,
                    steps = 19,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Article reading size"
                        }
                        .testTag("readingScaleSlider"),
                )
                TextButton(
                    onClick = {
                        onPreferencesChange(
                            preferences.copy(readingScale = MIN_READING_SCALE),
                        )
                    },
                    enabled = preferences.readingScale != MIN_READING_SCALE,
                    modifier = Modifier.testTag("resetReadingScaleButton"),
                ) {
                    Text("Reset reading size")
                }
                Text(
                    "Color scheme",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                AppArticleSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .selectableGroup(),
                    ) {
                        AppColorScheme.entries.forEach { option ->
                            PreferenceRadioRow(
                                label = option.label,
                                selected = preferences.colorScheme == option,
                                testTag = "colorSchemeOption_${option.name}",
                                onClick = {
                                    onPreferencesChange(preferences.copy(colorScheme = option))
                                },
                            )
                        }
                    }
                }
                Text(
                    text = "Aa  Preview text adjusts with your selection.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                AppToolbarButton(
                    text = "Manage sources",
                    onClick = onManageSources,
                    modifier = Modifier.testTag("manageSourcesButton"),
                )
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                Text(
                    "Data & sync",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Source lists can optionally be synced to your private Google Drive app-data area. Articles remain on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                AppToolbarButton(
                    text = "Google Drive sync",
                    onClick = onGoogleDriveSync,
                    modifier = Modifier.testTag("googleDriveSettingsButton"),
                )
            }
        }
    }
}

@Composable
private fun PreferenceRadioRow(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .testTag(testTag)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
