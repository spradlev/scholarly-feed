package dev.spradlev.scholarlyfeed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal val AppArticleShape = CutCornerShape(14.dp)
internal val AppNavigationShape = CutCornerShape(16.dp)

@Composable
internal fun AppArticleSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = AppArticleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        content = content,
    )
}

@Composable
internal fun AppToolbarButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(percent = 50),
    ) {
        Text(text)
    }
}
