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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ScreenBackground = Color(0xFFF8FAF7)
private val PrimaryGreen = Color(0xFF2D5A2D)
private val HeaderEndGreen = Color(0xFF43A047)
private val DarkGreen = Color(0xFF1A3A1A)
private val MutedGreen = Color(0xFF5A7A5A)
private val PaleGreen = Color(0xFFE8F5E9)

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState = viewModel.uiState
    var showPrivacyDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .verticalScroll(rememberScrollState()),
    ) {
        ProfileHeader(onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .offset(y = (-32).dp)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            InfoSettingsCard(
                onPrivacyClick = { showPrivacyDialog = true }
            )

            DataSourceCard(uiState = uiState)

            AboutCard(uiState = uiState)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Datenschutz", color = DarkGreen, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "WindKlar speichert keine personenbezogenen Daten. Ihr Standort wird nur temporär zur Zentrierung der Karte auf Ihrem Gerät verwendet und niemals übertragen oder serverseitig gespeichert. Favoriten und Verlauf werden ausschließlich lokal auf Ihrem Gerät gesichert.",
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
}

@Composable
private fun ProfileHeader(
    onBackClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(PrimaryGreen, HeaderEndGreen),
                    start = Offset.Zero,
                    end = Offset(900f, 900f),
                ),
            )
            .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 48.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Zurück",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }

            Text(
                text = "Info & Einstellungen",
                color = Color.White,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

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
                    imageVector = Icons.Outlined.PersonOutline,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
    }
}

@Composable
private fun InfoSettingsCard(
    onPrivacyClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 10.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "App-Einstellungen",
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
                onClick = {}
            )

            SettingsRowDivider()

            SettingsActionRow(
                icon = Icons.Outlined.Security,
                label = "Datenschutzerklärung",
                onClick = onPrivacyClick
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
        color = Color.White,
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
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Wichtige Einschränkungen:",
                        color = Color(0xFFD32F2F),
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
    onClick: () -> Unit,
    trailingText: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
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
                color = DarkGreen,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Normal,
            )
        }

        if (trailingText != null) {
            Text(
                text = trailingText,
                color = MutedGreen,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
            )
        }
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
                        colors = listOf(PaleGreen, Color(0xFFC8E6C9)),
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
