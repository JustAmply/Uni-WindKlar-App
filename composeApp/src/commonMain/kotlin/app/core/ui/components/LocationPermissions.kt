package app.core.ui.components

import androidx.compose.runtime.Composable

@Composable
expect fun rememberLocationPermissionLauncher(onResult: (Boolean) -> Unit): () -> Unit
