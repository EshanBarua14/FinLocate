package com.example.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = FintechGreen,
    secondary = FintechEmeraldLight,
    tertiary = AccentGold,
    background = SlateDark,
    surface = CardSlate,
    onPrimary = SlateDark,
    onSecondary = SlateDark,
    onBackground = TextLight,
    onSurface = TextLight,
    error = ExpenseRose
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    tertiary = AccentGold,
    background = LightBg,
    surface = LightCard,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = LightText,
    onSurface = LightText,
    error = ExpenseRose
)

object ThemeManager {
    var isDarkThemeState by androidx.compose.runtime.mutableStateOf(true)
    var isUserOverride by androidx.compose.runtime.mutableStateOf(false)

    fun toggleTheme() {
        isUserOverride = true
        isDarkThemeState = !isDarkThemeState
    }

    fun setDarkMode(isDark: Boolean, isManual: Boolean = true) {
        if (isManual) {
            isUserOverride = true
        }
        isDarkThemeState = isDark
    }

    fun resetToSystemTheme(isSystemDark: Boolean) {
        isUserOverride = false
        isDarkThemeState = isSystemDark
    }

    fun updateSystemThemePreference(isSystemDark: Boolean) {
        if (!isUserOverride) {
            isDarkThemeState = isSystemDark
        }
    }
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = ThemeManager.isDarkThemeState,
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  customPrimaryHex: String? = null,
  content: @Composable () -> Unit,
) {
  val customPrimary = customPrimaryHex?.let {
    try {
      Color(android.graphics.Color.parseColor(it))
    } catch (_: Exception) {
      null
    }
  }

  val baseDark = if (customPrimary != null) DarkColorScheme.copy(primary = customPrimary, secondary = customPrimary.copy(alpha = 0.85f)) else DarkColorScheme
  val baseLight = if (customPrimary != null) LightColorScheme.copy(primary = customPrimary, secondary = customPrimary.copy(alpha = 0.85f)) else LightColorScheme

  val targetColorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> baseDark
      else -> baseLight
    }

  // Animating each color of the ColorScheme to achieve a smooth cross-fade transition
  // reminiscent of Framer Motion cross-fade theme transitions.
  val animationSpec = tween<Color>(durationMillis = 600)
  
  val primary = animateColorAsState(targetColorScheme.primary, animationSpec, label = "primary").value
  val onPrimary = animateColorAsState(targetColorScheme.onPrimary, animationSpec, label = "onPrimary").value
  val primaryContainer = animateColorAsState(targetColorScheme.primaryContainer, animationSpec, label = "primaryContainer").value
  val onPrimaryContainer = animateColorAsState(targetColorScheme.onPrimaryContainer, animationSpec, label = "onPrimaryContainer").value
  val inversePrimary = animateColorAsState(targetColorScheme.inversePrimary, animationSpec, label = "inversePrimary").value
  val secondary = animateColorAsState(targetColorScheme.secondary, animationSpec, label = "secondary").value
  val onSecondary = animateColorAsState(targetColorScheme.onSecondary, animationSpec, label = "onSecondary").value
  val secondaryContainer = animateColorAsState(targetColorScheme.secondaryContainer, animationSpec, label = "secondaryContainer").value
  val onSecondaryContainer = animateColorAsState(targetColorScheme.onSecondaryContainer, animationSpec, label = "onSecondaryContainer").value
  val tertiary = animateColorAsState(targetColorScheme.tertiary, animationSpec, label = "tertiary").value
  val onTertiary = animateColorAsState(targetColorScheme.onTertiary, animationSpec, label = "onTertiary").value
  val tertiaryContainer = animateColorAsState(targetColorScheme.tertiaryContainer, animationSpec, label = "tertiaryContainer").value
  val onTertiaryContainer = animateColorAsState(targetColorScheme.onTertiaryContainer, animationSpec, label = "onTertiaryContainer").value
  val background = animateColorAsState(targetColorScheme.background, animationSpec, label = "background").value
  val onBackground = animateColorAsState(targetColorScheme.onBackground, animationSpec, label = "onBackground").value
  val surface = animateColorAsState(targetColorScheme.surface, animationSpec, label = "surface").value
  val onSurface = animateColorAsState(targetColorScheme.onSurface, animationSpec, label = "onSurface").value
  val surfaceVariant = animateColorAsState(targetColorScheme.surfaceVariant, animationSpec, label = "surfaceVariant").value
  val onSurfaceVariant = animateColorAsState(targetColorScheme.onSurfaceVariant, animationSpec, label = "onSurfaceVariant").value
  val surfaceTint = animateColorAsState(targetColorScheme.surfaceTint, animationSpec, label = "surfaceTint").value
  val inverseSurface = animateColorAsState(targetColorScheme.inverseSurface, animationSpec, label = "inverseSurface").value
  val inverseOnSurface = animateColorAsState(targetColorScheme.inverseOnSurface, animationSpec, label = "inverseOnSurface").value
  val error = animateColorAsState(targetColorScheme.error, animationSpec, label = "error").value
  val onError = animateColorAsState(targetColorScheme.onError, animationSpec, label = "onError").value
  val errorContainer = animateColorAsState(targetColorScheme.errorContainer, animationSpec, label = "errorContainer").value
  val onErrorContainer = animateColorAsState(targetColorScheme.onErrorContainer, animationSpec, label = "onErrorContainer").value
  val outline = animateColorAsState(targetColorScheme.outline, animationSpec, label = "outline").value
  val outlineVariant = animateColorAsState(targetColorScheme.outlineVariant, animationSpec, label = "outlineVariant").value
  val scrim = animateColorAsState(targetColorScheme.scrim, animationSpec, label = "scrim").value
  val surfaceBright = animateColorAsState(targetColorScheme.surfaceBright, animationSpec, label = "surfaceBright").value
  val surfaceDim = animateColorAsState(targetColorScheme.surfaceDim, animationSpec, label = "surfaceDim").value
  val surfaceContainer = animateColorAsState(targetColorScheme.surfaceContainer, animationSpec, label = "surfaceContainer").value
  val surfaceContainerHigh = animateColorAsState(targetColorScheme.surfaceContainerHigh, animationSpec, label = "surfaceContainerHigh").value
  val surfaceContainerHighest = animateColorAsState(targetColorScheme.surfaceContainerHighest, animationSpec, label = "surfaceContainerHighest").value
  val surfaceContainerLow = animateColorAsState(targetColorScheme.surfaceContainerLow, animationSpec, label = "surfaceContainerLow").value
  val surfaceContainerLowest = animateColorAsState(targetColorScheme.surfaceContainerLowest, animationSpec, label = "surfaceContainerLowest").value

  val animatedColorScheme = ColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    inversePrimary = inversePrimary,
    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary,
    onTertiary = onTertiary,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    background = background,
    onBackground = onBackground,
    surface = surface,
    onSurface = onSurface,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = onSurfaceVariant,
    surfaceTint = surfaceTint,
    inverseSurface = inverseSurface,
    inverseOnSurface = inverseOnSurface,
    error = error,
    onError = onError,
    errorContainer = errorContainer,
    onErrorContainer = onErrorContainer,
    outline = outline,
    outlineVariant = outlineVariant,
    scrim = scrim,
    surfaceBright = surfaceBright,
    surfaceDim = surfaceDim,
    surfaceContainer = surfaceContainer,
    surfaceContainerHigh = surfaceContainerHigh,
    surfaceContainerHighest = surfaceContainerHighest,
    surfaceContainerLow = surfaceContainerLow,
    surfaceContainerLowest = surfaceContainerLowest
  )

  MaterialTheme(colorScheme = animatedColorScheme, typography = Typography, content = content)
}
