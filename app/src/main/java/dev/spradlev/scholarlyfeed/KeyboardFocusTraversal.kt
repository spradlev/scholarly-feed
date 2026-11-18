// SPDX-License-Identifier: AGPL-3.0-or-later

package dev.spradlev.scholarlyfeed

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
internal fun Modifier.keyboardFocusTraversal(
    scrollState: ScrollableState? = null,
): Modifier {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val accessibilityManager = remember(context) {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    }
    var touchExplorationEnabled by remember(accessibilityManager) {
        mutableStateOf(accessibilityManager?.isTouchExplorationEnabled == true)
    }

    DisposableEffect(accessibilityManager) {
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { enabled ->
            touchExplorationEnabled = enabled
        }
        accessibilityManager?.addTouchExplorationStateChangeListener(listener)
        onDispose {
            accessibilityManager?.removeTouchExplorationStateChangeListener(listener)
        }
    }

    return this
        .onPreviewKeyEvent { event ->
            if (
                event.type == KeyEventType.KeyDown &&
                event.key == Key.Tab &&
                event.isShiftPressed
            ) {
                focusManager.moveFocus(FocusDirection.Previous)
            } else {
                false
            }
        }
        .onKeyEvent { event ->
            val direction = when (event.key) {
                Key.DirectionDown -> 1f
                Key.DirectionUp -> -1f
                else -> return@onKeyEvent false
            }
            if (
                event.type != KeyEventType.KeyDown ||
                touchExplorationEnabled ||
                scrollState == null
            ) {
                return@onKeyEvent false
            }

            val distance = with(density) { 64.dp.toPx() } * direction
            scope.launch { scrollState.scrollBy(distance) }
            true
        }
        .then(
            if (scrollState != null && !touchExplorationEnabled) {
                Modifier.focusable()
            } else {
                Modifier
            },
        )
}
