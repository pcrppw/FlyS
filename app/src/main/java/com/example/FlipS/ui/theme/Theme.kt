package com.example.FlipS.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Define your colors here (Material 3 uses a different color naming convention)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val CharColor = Color(0xFF82E0AA)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF1C1B1F), // Example dark background
    surface = Color(0xFF1C1B1F), // Example dark surface
    onPrimary = Color.White, // Example text color on primary
    onSecondary = Color.White, // Example text color on secondary
    onTertiary = Color.White, // Example text color on tertiary
    onBackground = Color.White, // Example text color on background
    onSurface = Color.White, // Example text color on surface
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFFFBFE), // Example light background
    surface = Color(0xFFFFFBFE), // Example light surface
    onPrimary = Color.White, // Example text color on primary
    onSecondary = Color.White, // Example text color on secondary
    onTertiary = Color.White, // Example text color on tertiary
    onBackground = Color.Black, // Example text color on background
    onSurface = Color.Black, // Example text color on surface
)

@Composable
fun FlipSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}