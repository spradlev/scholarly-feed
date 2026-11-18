// SPDX-License-Identifier: AGPL-3.0-or-later

package dev.spradlev.scholarlyfeed

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReadableTypographyTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun smallTextRolesAreRaisedAboveMaterialDefaults() {
        assertEquals(14.sp, ReadableTypography.bodySmall.fontSize)
        assertEquals(13.sp, ReadableTypography.labelSmall.fontSize)
    }

    @Test
    fun selectedAppTextSizeScalesTheWholeMaterialTypeScale() {
        assertEquals(21.sp, readableTypography(AppTextSize.LARGEST.scale).bodySmall.fontSize)
        assertEquals(
            Typography().headlineSmall.fontSize * AppTextSize.LARGEST.scale,
            readableTypography(AppTextSize.LARGEST.scale).headlineSmall.fontSize,
        )
    }

    @Test
    fun bodyTextContinuesToScaleWithAndroidFontSizeSetting() {
        var measuredHeight = 0
        var scaledMinimumHeight = 0

        composeRule.setContent {
            val currentDensity = LocalDensity.current
            val scaledDensity = Density(currentDensity.density, fontScale = 1.5f)
            val fontSize = ReadableTypography.bodySmall.fontSize.value
            scaledMinimumHeight =
                (fontSize * scaledDensity.density * scaledDensity.fontScale).roundToInt()
            CompositionLocalProvider(LocalDensity provides scaledDensity) {
                MaterialTheme(typography = ReadableTypography) {
                    Text(
                        text = "Readable text",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            measuredHeight = coordinates.size.height
                        },
                    )
                }
            }
        }

        composeRule.waitForIdle()
        assertTrue(measuredHeight >= scaledMinimumHeight)
    }
}
