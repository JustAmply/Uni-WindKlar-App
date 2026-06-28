package app.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.window.Dialog
import app.core.model.RankingItem
import app.core.ui.theme.WindklarTheme

private const val DefaultTopRankingItemCount = 5
private const val DefaultFullRankingVisibleItemLimit = 100

@Composable
fun TopRankingList(
    values: List<RankingItem>,
    onDetailsClick: (String) -> Unit,
    onShowFullListClick: () -> Unit,
    modifier: Modifier = Modifier,
    onActionClick: ((String) -> Unit)? = null,
    visibleItemCount: Int = DefaultTopRankingItemCount,
    showFullListLabel: String = "Gesamte Liste anzeigen",
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RankingList(
            values = values.take(visibleItemCount),
            onDetailsClick = onDetailsClick,
            onActionClick = onActionClick,
        )
        if (values.isNotEmpty()) {
            Button(
                onClick = onShowFullListClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WindklarTheme.colors.paleGreen,
                    contentColor = WindklarTheme.colors.primaryGreen,
                ),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    text = showFullListLabel,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

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
                color = WindklarTheme.colors.primaryGreen,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.widthIn(min = 30.dp),
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
                            color = WindklarTheme.colors.darkText,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = item.subtitle,
                            color = WindklarTheme.colors.mutedText,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = item.valueLabel,
                        color = WindklarTheme.colors.mutedText,
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
                HorizontalDivider(color = WindklarTheme.colors.trackGreen)
                item.details.forEach { line ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = line.label,
                            color = WindklarTheme.colors.mutedText,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        )
                        Text(
                            text = line.value,
                            color = WindklarTheme.colors.darkText,
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
                        colors = ButtonDefaults.buttonColors(containerColor = WindklarTheme.colors.paleGreen, contentColor = WindklarTheme.colors.primaryGreen),
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
                            colors = ButtonDefaults.buttonColors(containerColor = WindklarTheme.colors.primaryGreen, contentColor = Color.White),
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
fun FullRankingDialog(
    title: String,
    rankingItems: List<RankingItem>,
    onDismiss: () -> Unit,
    onDetailsClick: (String) -> Unit,
    onActionClick: ((String) -> Unit)? = null,
    visibleItemLimit: Int = DefaultFullRankingVisibleItemLimit,
) {
    var query by remember { mutableStateOf("") }
    var pageIndex by remember(rankingItems) { mutableStateOf(0) }
    val filteredItems = remember(query, rankingItems) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) {
            rankingItems
        } else {
            rankingItems.filter { item ->
                item.name.contains(normalizedQuery, ignoreCase = true) ||
                    item.subtitle.contains(normalizedQuery, ignoreCase = true)
            }
        }
    }
    val pageSize = visibleItemLimit.coerceAtLeast(1)
    val pageCount = ((filteredItems.size + pageSize - 1) / pageSize).coerceAtLeast(1)
    LaunchedEffect(query) {
        pageIndex = 0
    }
    LaunchedEffect(pageCount) {
        pageIndex = pageIndex.coerceIn(0, pageCount - 1)
    }
    val visibleItems = remember(filteredItems, pageIndex, pageSize) {
        filteredItems
            .drop(pageIndex * pageSize)
            .take(pageSize)
    }
    val firstVisibleNumber = if (visibleItems.isEmpty()) 0 else pageIndex * pageSize + 1
    val lastVisibleNumber = pageIndex * pageSize + visibleItems.size
    var expandedItemId by remember(visibleItems) { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(pageIndex, query) {
        listState.scrollToItem(0)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp),
            shape = RoundedCornerShape(12.dp),
            color = WindklarTheme.colors.cardBackground,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        color = WindklarTheme.colors.darkGreen,
                        fontSize = 16.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onDismiss) {
                        Text(text = "Schließen")
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = WindklarTheme.colors.mutedGreen,
                        )
                    },
                    label = { Text("Suchen") },
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(visibleItems, key = { it.id }) { item ->
                        RankingItemRow(
                            item = item,
                            isExpanded = expandedItemId == item.id,
                            onToggleExpand = {
                                expandedItemId = if (expandedItemId == item.id) null else item.id
                            },
                            onActionClick = onActionClick,
                            onDetailsClick = onDetailsClick,
                        )
                        HorizontalDivider(color = WindklarTheme.colors.trackGreen.copy(alpha = 0.5f))
                    }
                    if (visibleItems.isEmpty()) {
                        item {
                            EmptyText(text = "Keine Treffer für diese Suche.")
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = { pageIndex = (pageIndex - 1).coerceAtLeast(0) },
                        enabled = pageIndex > 0,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "Zurück",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            maxLines = 1,
                        )
                    }
                    Text(
                        text = if (filteredItems.isEmpty()) {
                            "0 Einträge"
                        } else {
                            "$firstVisibleNumber-$lastVisibleNumber von ${filteredItems.size}"
                        },
                        color = WindklarTheme.colors.mutedText,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    TextButton(
                        onClick = { pageIndex = (pageIndex + 1).coerceAtMost(pageCount - 1) },
                        enabled = pageIndex < pageCount - 1,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "Weiter",
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

@Composable
private fun ProgressTrack(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(WindklarTheme.colors.trackGreen),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(999.dp))
                .background(WindklarTheme.colors.primaryGreen),
        )
    }
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text = text,
        color = WindklarTheme.colors.mutedText,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )
}
