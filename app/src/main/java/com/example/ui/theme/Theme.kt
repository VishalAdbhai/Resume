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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = InkNavy,
    onPrimary = Color.White,
    primaryContainer = InkNavyContainer,
    onPrimaryContainer = OnInkNavyContainer,
    secondary = Terracotta,
    onSecondary = Color.White,
    secondaryContainer = TerracottaContainer,
    onSecondaryContainer = OnTerracottaContainer,
    tertiary = ForestSage,
    onTertiary = Color.White,
    tertiaryContainer = ForestSageContainer,
    onTertiaryContainer = OnForestSageContainer,
    background = PaperAlabaster,
    onBackground = CharcoalInk,
    surface = PaperCard,
    onSurface = CharcoalInk,
    surfaceVariant = PaperLinen,
    onSurfaceVariant = SlateMuted,
    outline = PaperHairline,
    outlineVariant = PaperHairline,
    error = CrimsonError,
    onError = Color.White,
    errorContainer = CrimsonErrorContainer,
    onErrorContainer = CrimsonError
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimarySky,
    onPrimary = Color(0xFF0A1929),
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = Color(0xFFD9ECFF),
    secondary = DarkTerracotta,
    onSecondary = Color(0xFF331004),
    secondaryContainer = DarkTerracottaContainer,
    onSecondaryContainer = Color(0xFFFFD8C9),
    tertiary = DarkSage,
    onTertiary = Color(0xFF052519),
    tertiaryContainer = DarkSageContainer,
    onTertiaryContainer = Color(0xFFC5F2DF),
    background = DarkStudioBg,
    onBackground = DarkBoneText,
    surface = DarkStudioSurface,
    onSurface = DarkBoneText,
    surfaceVariant = DarkStudioElevated,
    onSurfaceVariant = DarkSlateText,
    outline = DarkStudioHairline,
    outlineVariant = DarkStudioHairline
)

val ResumeCraftShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ResumeCraftTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
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
        typography = ResumeCraftTypography,
        shapes = ResumeCraftShapes,
        content = content
    )
}
