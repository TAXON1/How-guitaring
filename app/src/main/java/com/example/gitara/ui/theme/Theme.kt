package com.example.gitara.ui.theme

import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val PurplePrimary = Color(0xFF6A1071)
private val PinkAccent = Color(0xFFCB1FD7)

// Новый фон для светлой темы (чуть сероватый), чтобы белые карточки были видны
private val LightBackground = Color(0xFFF2F0F4)

private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    secondary = PinkAccent,
    tertiary = PinkAccent,

    // 🔥 ВАЖНОЕ ИЗМЕНЕНИЕ:
    background = LightBackground, // Фон всего экрана теперь не чисто белый
    surface = Color.White,        // А вот карточки остаются белыми (теперь они видны!)

    surfaceVariant = Color(0xFFE7E0EC), // Чуть темнее для плашек
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    outline = Color(0xFF79747E)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFE68FFF),
    tertiary = PinkAccent,

    background = Color(0xFF0D0D1A),
    surface = Color(0xFF1A1826),
    surfaceVariant = Color(0xFF2D2739),

    onPrimary = Color(0xFF2D1B3A),
    onSecondary = Color(0xFF2D1B3A),
    onBackground = Color(0xFFE6E0E9),
    onSurface = Color(0xFFE6E0E9),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99)
)

@Composable
fun GitaraTheme(
    darkTheme: Boolean,
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
        typography = Typography(),
        content = content
    )
}
