package app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.core.ui.theme.WindklarTheme
import app.core.ui.components.WindklarHeader
import app.core.util.rememberPlatformSharer
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ScreenBackground @Composable get() = WindklarTheme.colors.screenBackground
private val PrimaryGreen @Composable get() = WindklarTheme.colors.primaryGreen
private val HeaderEndGreen @Composable get() = WindklarTheme.colors.headerEndGreen
private val DarkGreen @Composable get() = WindklarTheme.colors.darkGreen
private val MutedGreen @Composable get() = WindklarTheme.colors.mutedGreen
private val PaleGreen @Composable get() = WindklarTheme.colors.paleGreen

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onReplayOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState = viewModel.uiState
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    val platformSharer = rememberPlatformSharer()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .verticalScroll(rememberScrollState()),
    ) {
        ProfileHeader()

        Column(
            modifier = Modifier
                .offset(y = (-32).dp)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            AboutCard(uiState = uiState)

            DataSourceCard(uiState = uiState)

            InfoSettingsCard(
                onPrivacyClick = { showPrivacyDialog = true },
                onClearHistoryClick = { showClearHistoryDialog = true },
                onReplayOnboardingClick = onReplayOnboarding,
                onExportDataHintsClick = {
                    viewModel.exportDataHints { csvContent ->
                        platformSharer.shareText(csvContent, "WindKlar Datenhinweise")
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Lokale Speicherung & Datenschutz", color = DarkGreen, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "WindKlar speichert keine personenbezogenen Konten. Ihr Standort wird nur temporär zur Zentrierung der Karte verwendet und im MVP nicht dauerhaft gespeichert. Favoriten, Verlauf und Datenhinweise bleiben lokal auf Ihrem Gerät.",
                    color = DarkGreen,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyDialog = false }
                ) {
                    Text("Schließen")
                }
            }
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Verlauf löschen", color = DarkGreen, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Möchten Sie den Verlauf der zuletzt angesehenen Windparks wirklich löschen? Dies kann nicht rückgängig gemacht werden.",
                    color = DarkGreen,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearRecentWindParks {
                            showClearHistoryDialog = false
                        }
                    }
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showClearHistoryDialog = false }
                ) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun ProfileHeader() {
    WindklarHeader(
        title = "Info & Einstellungen",
        bottomPadding = 48.dp,
        extraContent = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Air,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
        }
    )
}

@Composable
private fun InfoSettingsCard(
    onPrivacyClick: () -> Unit,
    onClearHistoryClick: () -> Unit,
    onReplayOnboardingClick: () -> Unit,
    onExportDataHintsClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = WindklarTheme.colors.cardBackground,
        shadowElevation = 10.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "App & Datenschutz",
                modifier = Modifier.padding(16.dp),
                color = DarkGreen,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Medium,
            )

            SettingsRowDivider()

            SettingsActionRow(
                icon = Icons.Outlined.Language,
                label = "Sprache",
                trailingText = "Deutsch",
            )

            SettingsRowDivider()

            SettingsActionRow(
                icon = Icons.Outlined.Security,
                label = "Lokale Speicherung & Datenschutz",
                onClick = onPrivacyClick
            )

            SettingsRowDivider()

            SettingsActionRow(
                icon = Icons.Outlined.Share,
                label = "Datenhinweise exportieren",
                onClick = onExportDataHintsClick
            )

            SettingsRowDivider()

            SettingsActionRow(
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                label = "Einführung erneut anzeigen",
                onClick = onReplayOnboardingClick
            )

            SettingsRowDivider()

            SettingsActionRow(
                icon = Icons.Outlined.DeleteSweep,
                label = "Verlauf löschen",
                onClick = onClearHistoryClick
            )
        }
    }
}

@Composable
private fun DataSourceCard(
    uiState: ProfileUiState,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = WindklarTheme.colors.cardBackground,
        shadowElevation = 10.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Datenquelle & Datenqualität",
                    color = DarkGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = "Datenherkunft:",
                color = DarkGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = uiState.attribution,
                color = MutedGreen,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            if (uiState.limitations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = WindklarTheme.colors.errorRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Wichtige Einschränkungen:",
                        color = WindklarTheme.colors.errorRed,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    uiState.limitations.forEach { limitation ->
                        Text(
                            text = "• $limitation",
                            color = MutedGreen,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    label: String,
    onClick: (() -> Unit)? = null,
    trailingText: String? = null,
) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 64.dp)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(horizontal = 16.dp, vertical = 12.dp)

    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(PaleGreen, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(18.dp),
                )
            }

            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = DarkGreen,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Normal,
            )
        }

        if (trailingText != null) {
            Text(
                text = trailingText,
                modifier = Modifier.padding(start = 12.dp),
                color = MutedGreen,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    label: String,
    supportingText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(PaleGreen, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.size(18.dp),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                color = DarkGreen,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Normal,
            )
            Text(
                text = supportingText,
                color = MutedGreen,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun SettingsRowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(PaleGreen),
    )
}

@Composable
private fun AboutCard(
    uiState: ProfileUiState,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(PaleGreen, WindklarTheme.colors.contactCardEndGreen),
                    ),
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(20.dp),
        ) {
            Text(
                text = uiState.aboutTitle,
                color = DarkGreen,
                fontSize = 18.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.Medium,
            )

            Text(
                text = uiState.aboutText,
                modifier = Modifier.padding(top = 8.dp),
                color = MutedGreen,
                fontSize = 14.sp,
                lineHeight = 23.sp,
            )

            Text(
                text = uiState.version,
                modifier = Modifier.padding(top = 16.dp),
                color = MutedGreen,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
    }
}
