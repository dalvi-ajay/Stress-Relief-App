package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = MistPrimary,
    onPrimary = MistOnPrimary,
    primaryContainer = MistPrimaryContainer,
    onPrimaryContainer = MistOnPrimaryContainer,
    secondary = SlateMossSecondary,
    onSecondary = SlateMossOnSecondary,
    secondaryContainer = SlateMossSecondaryContainer,
    onSecondaryContainer = SlateMossOnSecondaryContainer,
    tertiary = WarmSandTertiary,
    onTertiary = WarmSandOnTertiary,
    tertiaryContainer = WarmSandTertiaryContainer,
    onTertiaryContainer = WarmSandOnTertiaryContainer,
    background = ForestBackground,
    onBackground = ForestOnBackground,
    surface = ForestSurface,
    onSurface = ForestOnSurface,
    surfaceVariant = ForestSurfaceVariant,
    onSurfaceVariant = ForestOnSurfaceVariant,
    outline = ForestOutline,
    outlineVariant = ForestOutlineVariant,
)

private val LightColorScheme = lightColorScheme(
    primary = SagePrimary,
    onPrimary = SageOnPrimary,
    primaryContainer = SagePrimaryContainer,
    onPrimaryContainer = SageOnPrimaryContainer,
    secondary = MossSecondary,
    onSecondary = MossOnSecondary,
    secondaryContainer = MossSecondaryContainer,
    onSecondaryContainer = MossOnSecondaryContainer,
    tertiary = TerracottaTertiary,
    onTertiary = TerracottaOnTertiary,
    tertiaryContainer = TerracottaTertiaryContainer,
    onTertiaryContainer = TerracottaOnTertiaryContainer,
    background = LinenBackground,
    onBackground = LinenOnBackground,
    surface = LinenSurface,
    onSurface = LinenOnSurface,
    surfaceVariant = LinenSurfaceVariant,
    onSurfaceVariant = LinenOnSurfaceVariant,
    outline = LinenOutline,
    outlineVariant = LinenOutlineVariant,
)

val StillnessShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun StillnessTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = StillnessTypography,
        shapes = StillnessShapes,
        content = content,
    )
}
