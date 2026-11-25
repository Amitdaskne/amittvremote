package com.amit.amittvremote.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = BlobPurple,
    onPrimary = Color.White,
    background = BgDeep,
    onBackground = TextPrimary,
    surface = BgMid,
    onSurface = TextPrimary
)

@Composable
fun AmitTVRemoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
