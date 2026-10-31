package dev.spradlev.scholarlyfeed

import android.content.Context
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
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

internal data class AppPreferences(
    val textSize: AppTextSize = AppTextSize.STANDARD,
    val colorScheme: AppColorScheme = AppColorScheme.SYSTEM,
) {
    companion object {
        private const val PREFERENCES_FILE = "app_accessibility_preferences"
        private const val TEXT_SIZE_KEY = "text_size"
        private const val COLOR_SCHEME_KEY = "color_scheme"

        fun load(context: Context): AppPreferences {
            val preferences = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)
            return AppPreferences(
                textSize = AppTextSize.entries.firstOrNull {
                    it.name == preferences.getString(TEXT_SIZE_KEY, null)
                } ?: AppTextSize.STANDARD,
                colorScheme = AppColorScheme.entries.firstOrNull {
                    it.name == preferences.getString(COLOR_SCHEME_KEY, null)
                } ?: AppColorScheme.SYSTEM,
            )
        }

        fun save(context: Context, value: AppPreferences) {
            context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)
                .edit()
                .putString(TEXT_SIZE_KEY, value.textSize.name)
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
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (dark) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }
}

internal fun scaleTextUnit(value: TextUnit, scale: Float): TextUnit =
    if (value.isSpecified) value * scale else value
