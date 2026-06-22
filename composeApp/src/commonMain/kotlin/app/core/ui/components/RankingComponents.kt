package app.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.core.model.RankingItem

private val PrimaryGreen = Color(0xFF2D5A2D)
private val SoftGreen = Color(0xFFE8F5E9)
private val DarkText = Color(0xFF17261A)
private val MutedText = Color(0xFF647568)
private val TrackGreen = Color(0xFFDDEBDD)

@Composable
fun RankingList(
    values: List<RankingItem>,
    onDetailsClick: (String) -> Unit,
    onActionClick: ((String) -> Unit)? = null,
) {
    if (values.isEmpty()) {
        EmptyText(text = "Keine Ranglisteneinträge verfügbar.")
        return
    }

    var expandedItemId by remember(values) { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        values.forEach { item ->
            RankingItemRow(
                item = item,
                isExpanded = expandedItemId == item.id,
                onToggleExpand = {
                    expandedItemId = if (expandedItemId == item.id) null else item.id
                },
                onDetailsClick = onDetailsClick,
                onActionClick = onActionClick,
            )
        }
    }
}

@Composable
fun RankingItemRow(
    item: RankingItem,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDetailsClick: (String) -> Unit,
    onActionClick: ((String) -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggleExpand)
            .animateContentSize()
            .padding(vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "${item.rank}",
                color = PrimaryGreen,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(22.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name.ifBlank { "Unbekannter Name" },
                            color = DarkText,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = item.subtitle,
                            color = MutedText,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = item.valueLabel,
                        color = MutedText,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                ProgressTrack(progress = item.progress)
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier.padding(start = 32.dp, top = 10.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HorizontalDivider(color = TrackGreen)
                item.details.forEach { line ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = line.label,
                            color = MutedText,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        )
                        Text(
                            text = line.value,
                            color = DarkText,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onDetailsClick(item.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = SoftGreen, contentColor = PrimaryGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Details",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            maxLines = 1,
                        )
                    }
                    if (onActionClick != null) {
                        Button(
                            onClick = { onActionClick(item.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Vergleichen",
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressTrack(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(TrackGreen),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(999.dp))
                .background(PrimaryGreen),
        )
    }
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text = text,
        color = MutedText,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )
}
