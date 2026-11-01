package dev.spradlev.scholarlyfeed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.sqrt

@Composable
internal fun Modifier.pinchToZoom(onScaleChange: (Float) -> Unit): Modifier {
    val latestOnScaleChange by rememberUpdatedState(onScaleChange)
    return pointerInput(Unit) {
        awaitEachGesture {
            var previousSpan = 0f
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val pressed = event.changes.filter { it.pressed }
                if (pressed.size >= 2) {
                    val centroid = Offset(
                        x = pressed.map { it.position.x }.average().toFloat(),
                        y = pressed.map { it.position.y }.average().toFloat(),
                    )
                    val span = pressed.map { pointer ->
                        val dx = pointer.position.x - centroid.x
                        val dy = pointer.position.y - centroid.y
                        sqrt(dx * dx + dy * dy)
                    }.average().toFloat()

                    if (previousSpan > 0f && span > 0f) {
                        latestOnScaleChange(span / previousSpan)
                    }
                    previousSpan = span
                    pressed.forEach { it.consume() }
                } else {
                    previousSpan = 0f
                }
            } while (event.changes.any { it.pressed })
        }
    }
}
