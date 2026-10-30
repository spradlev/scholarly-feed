package dev.spradlev.scholarlyfeed

import androidx.compose.material3.Typography
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val ReadableTypography = Typography().let { defaults ->
    defaults.copy(
        bodySmall = defaults.bodySmall.copy(
            fontSize = 14.sp,
            lineHeight = 20.sp,
        ),
        labelSmall = defaults.labelSmall.copy(
            fontSize = 13.sp,
            lineHeight = 18.sp,
        ),
    )
}

internal val READABLE_CONTENT_MAX_WIDTH = 600.dp
