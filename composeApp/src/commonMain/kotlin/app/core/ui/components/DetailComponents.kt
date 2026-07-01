package app.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.core.model.SnapshotAssumption
import app.core.ui.theme.WindklarTheme
import app.core.util.formatGermanNumber

data class ImpactMetric(
    val label: String,
    val value: String,
    val isMissing: Boolean,
    val note: String?,
    val icon: ImageVector
)

@Composable
fun CitizenImpactDashboard(
    title: String = "Lokaler Nutzen & Klimawirkung",
    metrics: List<ImpactMetric>
) {
    var showDetails by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = WindklarTheme.colors.cardBackground,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                color = WindklarTheme.colors.darkGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            metrics.forEach { metric ->
                ImpactMetricRow(
                    icon = metric.icon,
                    label = metric.label,
                    value = metric.value,
                    isMissing = metric.isMissing,
                    note = metric.note,
                    showNote = showDetails
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDetails = !showDetails }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showDetails) "Details ausblenden" else "Details einblenden",
                    color = WindklarTheme.colors.primaryGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (showDetails) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = WindklarTheme.colors.primaryGreen,
                    modifier = Modifier.size(20.dp).padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ImpactMetricRow(
    icon: ImageVector,
    label: String,
    value: String,
    isMissing: Boolean,
    note: String?,
    showNote: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(WindklarTheme.colors.paleGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = WindklarTheme.colors.primaryGreen,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = WindklarTheme.colors.darkGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                color = if (isMissing) WindklarTheme.colors.mutedGreen else WindklarTheme.colors.primaryGreen,
                fontSize = 18.sp,
                fontWeight = if (isMissing) FontWeight.Normal else FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp)
            )
            if (!note.isNullOrBlank()) {
                AnimatedVisibility(visible = showNote) {
                    Text(
                        text = note,
                        color = WindklarTheme.colors.mutedGreen,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DataStatusFooter(
    dataQuality: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (dataQuality != null) {
            val quality = qualityColors(dataQuality)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Qualität der Stammdaten:",
                    color = WindklarTheme.colors.mutedGreen,
                    fontSize = 11.sp
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = quality.container
                ) {
                    Text(
                        text = formatDataQuality(dataQuality),
                        color = quality.content,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Text(
            text = "Berechnete Nutzen- und Klimawerte beruhen auf standardisierten Durchschnittsannahmen (keine Live-Daten) und können regional abweichen.",
            color = WindklarTheme.colors.mutedGreen,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
