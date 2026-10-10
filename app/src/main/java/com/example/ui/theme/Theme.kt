package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TuningDarkColorScheme = darkColorScheme(
  primary = TuningRed,
  onPrimary = Color.White,
  primaryContainer = TuningRedHover,
  onPrimaryContainer = Color.White,
  secondary = TunerAmber,
  onSecondary = Color.Black,
  secondaryContainer = DarkSurfaceElevated,
  onSecondaryContainer = TunerAmber,
  tertiary = TunerCyan,
  onTertiary = Color.Black,
  background = CarbonBlack,
  onBackground = TextPrimary,
  surface = DarkSurface,
  onSurface = TextPrimary,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = TextSecondary,
  outline = BorderStroke,
  outlineVariant = BorderStroke.copy(alpha = 0.5f),
  error = StatusRed,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // For Tuning Sensor, the dark cockpit/tuner styling is always preferred for crisp contrast!
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = TuningDarkColorScheme,
    typography = Typography,
    content = content
  )
}
