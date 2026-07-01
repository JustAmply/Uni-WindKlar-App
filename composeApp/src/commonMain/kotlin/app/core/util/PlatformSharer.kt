package app.core.util

import androidx.compose.runtime.Composable

interface PlatformSharer {
    fun shareText(text: String, title: String)
}

@Composable
expect fun rememberPlatformSharer(): PlatformSharer
