package com.piecejob.core.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val CustomerColorScheme = lightColorScheme(
    primary = CustomerPrimary,
    secondary = CustomerSecondary,
    tertiary = CustomerAccent,
    background = White,
    surface = White,
    onPrimary = White,
    onSecondary = White,
    onTertiary = White,
    error = ErrorRed,
    onError = White
)

private val ProviderColorScheme = lightColorScheme(
    primary = ProviderPrimary,
    secondary = ProviderSecondary,
    tertiary = ProviderAccent,
    background = White,
    surface = White,
    onPrimary = White,
    onSecondary = White,
    onTertiary = White,
    error = ErrorRed,
    onError = White
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
