package dev.spradlev.scholarlyfeed

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun AppBottomBar(
    focusModeEnabled: Boolean,
    settingsSelected: Boolean,
    onSettingsClick: () -> Unit,
    onFocusModeClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 64.dp)
                .padding(horizontal = 8.dp),
        ) {
            Spacer(Modifier.weight(1f))
            NavigationBarItem(
                selected = settingsSelected,
                onClick = onSettingsClick,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null,
                    )
                },
                label = { Text("Settings") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("bottomSettingsButton"),
            )
            NavigationBarItem(
                selected = focusModeEnabled,
                onClick = onFocusModeClick,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                    )
                },
                label = { Text("Focus") },
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        stateDescription = if (focusModeEnabled) "On" else "Off"
                    }
                    .testTag("bottomFocusModeButton"),
            )
        }
    }
}
