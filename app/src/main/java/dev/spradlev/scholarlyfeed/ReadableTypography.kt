package dev.spradlev.scholarlyfeed

import androidx.compose.material3.Typography
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val ReadableTypography = readableTypography(1f)

internal fun readableTypography(scale: Float) = Typography().let { defaults ->
    defaults.copy(
        displayLarge = defaults.displayLarge.copy(
            fontSize = scaleTextUnit(defaults.displayLarge.fontSize, scale),
            lineHeight = scaleTextUnit(defaults.displayLarge.lineHeight, scale),
        ),
        displayMedium = defaults.displayMedium.copy(
            fontSize = scaleTextUnit(defaults.displayMedium.fontSize, scale),
            lineHeight = scaleTextUnit(defaults.displayMedium.lineHeight, scale),
        ),
        displaySmall = defaults.displaySmall.copy(
            fontSize = scaleTextUnit(defaults.displaySmall.fontSize, scale),
            lineHeight = scaleTextUnit(defaults.displaySmall.lineHeight, scale),
        ),
        headlineLarge = defaults.headlineLarge.copy(
            fontSize = scaleTextUnit(defaults.headlineLarge.fontSize, scale),
            lineHeight = scaleTextUnit(defaults.headlineLarge.lineHeight, scale),
        ),
        headlineMedium = defaults.headlineMedium.copy(
            fontSize = scaleTextUnit(defaults.headlineMedium.fontSize, scale),
            lineHeight = scaleTextUnit(defaults.headlineMedium.lineHeight, scale),
        ),
        headlineSmall = defaults.headlineSmall.copy(
            fontSize = scaleTextUnit(defaults.headlineSmall.fontSize, scale),
            lineHeight = scaleTextUnit(defaults.headlineSmall.lineHeight, scale),
        ),
        titleLarge = defaults.titleLarge.copy(
            fontSize = scaleTextUnit(defaults.titleLarge.fontSize, scale),
            lineHeight = scaleTextUnit(defaults.titleLarge.lineHeight, scale),
        ),
        titleMedium = defaults.titleMedium.copy(
            fontSize = scaleTextUnit(defaults.titleMedium.fontSize, scale),
            lineHeight = scaleTextUnit(defaults.titleMedium.lineHeight, scale),
        ),
        titleSmall = defaults.titleSmall.copy(
            fontSize = scaleTextUnit(defaults.titleSmall.fontSize, scale),
            lineHeight = scaleTextUnit(defaults.titleSmall.lineHeight, scale),
        ),
        bodyLarge = defaults.bodyLarge.copy(
            fontSize = scaleTextUnit(defaults.bodyLarge.fontSize, scale),
            lineHeight = scaleTextUnit(defaults.bodyLarge.lineHeight, scale),
        ),
        bodyMedium = defaults.bodyMedium.copy(
            fontSize = scaleTextUnit(defaults.bodyMedium.fontSize, scale),
            lineHeight = scaleTextUnit(defaults.bodyMedium.lineHeight, scale),
        ),
        bodySmall = defaults.bodySmall.copy(
            fontSize = scaleTextUnit(14.sp, scale),
            lineHeight = scaleTextUnit(20.sp, scale),
        ),
        labelLarge = defaults.labelLarge.copy(
            fontSize = scaleTextUnit(defaults.labelLarge.fontSize, scale),
            lineHeight = scaleTextUnit(defaults.labelLarge.lineHeight, scale),
        ),
        labelMedium = defaults.labelMedium.copy(
            fontSize = scaleTextUnit(defaults.labelMedium.fontSize, scale),
            lineHeight = scaleTextUnit(defaults.labelMedium.lineHeight, scale),
        ),
        labelSmall = defaults.labelSmall.copy(
            fontSize = scaleTextUnit(13.sp, scale),
            lineHeight = scaleTextUnit(18.sp, scale),
        ),
    )
}

internal val READABLE_CONTENT_MAX_WIDTH = 600.dp
