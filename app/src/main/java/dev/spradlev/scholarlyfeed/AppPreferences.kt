// SPDX-License-Identifier: AGPL-3.0-or-later

package dev.spradlev.scholarlyfeed

import android.content.Context
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import android.os.Build

internal enum class AppTextSize(val label: String, val scale: Float) {
    STANDARD("Standard (100%)", 1f),
    LARGE("Large (115%)", 1.15f),
    LARGER("Larger (130%)", 1.3f),
    LARGEST("Largest (150%)", 1.5f),
}

internal enum class AppColorScheme(val label: String) {
    SYSTEM("System default"),
    LIGHT("Light"),
    DARK("Dark"),
}

internal const val MIN_READING_SCALE = 1f
internal const val MAX_READING_SCALE = 2f

internal data class AppPreferences(
    val textSize: AppTextSize = AppTextSize.STANDARD,
    val readingScale: Float = MIN_READING_SCALE,
    val colorScheme: AppColorScheme = AppColorScheme.SYSTEM,
) {
    companion object {
        private const val PREFERENCES_FILE = "app_accessibility_preferences"
        private const val TEXT_SIZE_KEY = "text_size"
        private const val READING_SCALE_KEY = "reading_scale"
        private const val COLOR_SCHEME_KEY = "color_scheme"

        fun load(context: Context): AppPreferences {
            val preferences = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)
            return AppPreferences(
                textSize = AppTextSize.entries.firstOrNull {
                    it.name == preferences.getString(TEXT_SIZE_KEY, null)
                } ?: AppTextSize.STANDARD,
                readingScale = preferences.getFloat(READING_SCALE_KEY, MIN_READING_SCALE)
                    .coerceIn(MIN_READING_SCALE, MAX_READING_SCALE),
                colorScheme = AppColorScheme.entries.firstOrNull {
                    it.name == preferences.getString(COLOR_SCHEME_KEY, null)
                } ?: AppColorScheme.SYSTEM,
            )
        }

        fun save(context: Context, value: AppPreferences) {
            context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)
                .edit()
                .putString(TEXT_SIZE_KEY, value.textSize.name)
                .putFloat(
                    READING_SCALE_KEY,
                    value.readingScale.coerceIn(MIN_READING_SCALE, MAX_READING_SCALE),
                )
                .putString(COLOR_SCHEME_KEY, value.colorScheme.name)
                .apply()
        }
    }
}

@Composable
internal fun appColorScheme(preference: AppColorScheme) = run {
    val dark = when (preference) {
        AppColorScheme.SYSTEM -> isSystemInDarkTheme()
        AppColorScheme.LIGHT -> false
        AppColorScheme.DARK -> true
    }
    val context = LocalContext.current
    val baseColorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (dark) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }
    val brandPrimary = if (dark) Color(0xFFA5D5CA) else Color(0xFF285F58)
    val onBrandPrimary = if (dark) Color(0xFF073732) else Color.White
    val brandContainer = if (dark) Color(0xFF194C45) else Color(0xFFD4E8E3)
    val onBrandContainer = if (dark) Color(0xFFC1E8DE) else Color(0xFF123C38)

    baseColorScheme.copy(
        primary = brandPrimary,
        onPrimary = onBrandPrimary,
        primaryContainer = brandContainer,
        onPrimaryContainer = onBrandContainer,
        secondary = if (dark) Color(0xFFB5CCC5) else Color(0xFF526965),
        onSecondary = if (dark) Color(0xFF203631) else Color.White,
        secondaryContainer = if (dark) Color(0xFF374C46) else Color(0xFFD9E7E1),
        onSecondaryContainer = if (dark) Color(0xFFD1E6DE) else Color(0xFF263B36),
        tertiary = if (dark) Color(0xFFD1C2A8) else Color(0xFF665A43),
        onTertiary = if (dark) Color(0xFF382E1A) else Color.White,
    )
}

internal fun scaleTextUnit(value: TextUnit, scale: Float): TextUnit =
    if (value.isSpecified) value * scale else value
