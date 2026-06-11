package com.wergogo.travelnote.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    error = Error
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),   // inputs, chips
    medium = RoundedCornerShape(16.dp),  // cards
    large = RoundedCornerShape(28.dp)    // FAB, bottom sheet top
)

@Composable
fun TravelnoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
