package com.piecejob.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val CustomerColorScheme = lightColorScheme(
    primary = EarthyRed,
    secondary = DeepMauve,
    tertiary = Cream,
    background = White,
    surface = White,
    onPrimary = White,
    onSecondary = White,
    onTertiary = EarthyRed
)

private val ProviderColorScheme = lightColorScheme(
    primary = ForestGreen,
    secondary = OliveDrab,
    tertiary = CadetGray,
    background = White,
    surface = White,
    onPrimary = White,
    onSecondary = White,
    onTertiary = ForestGreen
)

@Composable
fun PieceJobTheme(
    isProvider: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isProvider) ProviderColorScheme else CustomerColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
