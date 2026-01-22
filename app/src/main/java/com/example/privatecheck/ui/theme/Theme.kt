package com.example.privatecheck.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = MorandiGreen, // Keep Green
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = MorandiDarkBackground,
    surface = MorandiDarkBackground,
    onPrimary = MorandiDarkBackground,
    onBackground = MorandiDarkText,
    onSurface = MorandiDarkText
)

private val LightColorScheme = lightColorScheme(
    primary = MorandiGreen,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = MorandiBackground,
    surface = MorandiBackground,
    onPrimary = Color.White,
    onBackground = MorandiText,
    onSurface = MorandiText

    /* Other default colors to override
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun PrivateCheckTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    customPrimary: Color? = null,
    content: @Composable () -> Unit
) {
    var colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    if (customPrimary != null) {
        colorScheme = colorScheme.copy(primary = customPrimary)
    }
    // SideEffect removed; moved to MainActivity for precise timing control

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
