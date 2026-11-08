package dev.spradlev.scholarlyfeed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun AppBottomBar(
    focusModeEnabled: Boolean,
    settingsSelected: Boolean,
    homeSelected: Boolean,
    onSettingsClick: () -> Unit,
    onHomeClick: () -> Unit,
    onFocusModeClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(bottom = 4.dp),
        shape = AppNavigationShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            BottomBarTooltip("Settings") { focusModifier ->
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(48.dp)
                        .backgroundIfSelected(settingsSelected)
                        .then(focusModifier)
                        .semantics { selected = settingsSelected }
                        .testTag("bottomSettingsButton"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.size(20.dp),
                        tint = if (settingsSelected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            BottomBarTooltip("Home") { focusModifier ->
                IconButton(
                    onClick = onHomeClick,
                    modifier = Modifier
                        .size(48.dp)
                        .backgroundIfSelected(homeSelected)
                        .then(focusModifier)
                        .semantics {
                            contentDescription = "Home"
                            selected = homeSelected
                        }
                        .testTag("bottomHomeButton"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (homeSelected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            BottomBarTooltip("Focus mode: ${if (focusModeEnabled) "On" else "Off"}") {
                    focusModifier ->
                IconToggleButton(
                    checked = focusModeEnabled,
                    onCheckedChange = { onFocusModeClick() },
                    modifier = Modifier
                        .size(48.dp)
                        .backgroundIfSelected(focusModeEnabled)
                        .then(focusModifier)
                        .semantics {
                            contentDescription = "Focus mode"
                            selected = focusModeEnabled
                            stateDescription = if (focusModeEnabled) "On" else "Off"
                        }
                        .testTag("bottomFocusModeButton"),
                ) {
                    if (focusModeEnabled) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    } else {
                        OutlineStar(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Modifier.backgroundIfSelected(selected: Boolean): Modifier =
    if (selected) {
        background(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(12.dp),
        )
    } else {
        this
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomBarTooltip(
    text: String,
    content: @Composable (Modifier) -> Unit,
) {
    val state = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()

    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Above,
        ),
        tooltip = { PlainTooltip { Text(text) } },
        state = state,
    ) {
        content(
            Modifier.onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    scope.launch { state.show() }
                } else {
                    state.dismiss()
                }
            },
        )
    }
}

@Composable
private fun OutlineStar(
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val outerRadius = size.minDimension * 0.46f
        val innerRadius = outerRadius * 0.43f
        val path = Path()

        repeat(10) { index ->
            val angle = -PI / 2 + index * PI / 5
            val radius = if (index % 2 == 0) outerRadius else innerRadius
            val x = centerX + cos(angle).toFloat() * radius
            val y = centerY + sin(angle).toFloat() * radius
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 1.8.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}
