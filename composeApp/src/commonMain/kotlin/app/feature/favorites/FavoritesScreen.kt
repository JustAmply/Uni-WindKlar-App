package app.feature.favorites

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import windklar.composeapp.generated.resources.Res
import windklar.composeapp.generated.resources.favorite_windpark_alpen
import windklar.composeapp.generated.resources.favorite_windpark_nordsee
import windklar.composeapp.generated.resources.favorite_windpark_ostsee
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import androidx.compose.runtime.LaunchedEffect
import app.core.ui.theme.WindklarTheme

private val ScreenBackground @Composable get() = WindklarTheme.colors.screenBackground
private val PrimaryGreen @Composable get() = WindklarTheme.colors.primaryGreen
private val HeaderEndGreen @Composable get() = WindklarTheme.colors.headerEndGreen
private val DarkGreen @Composable get() = WindklarTheme.colors.darkGreen
private val MutedGreen @Composable get() = WindklarTheme.colors.mutedGreen
private val PaleGreen @Composable get() = WindklarTheme.colors.paleGreen
private val HeartRed = Color(0xFFE53935)

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onParkSelected: (parkId: String) -> Unit,
    onRegionSelected: (type: String, id: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState = viewModel.uiState

    LaunchedEffect(viewModel) {
        viewModel.loadData()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .verticalScroll(rememberScrollState()),
    ) {
        FavoritesHeader()

        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Favorites Section
            Text(
                text = "Meine Favoriten",
                color = DarkGreen,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
            )

            if (uiState.parks.isEmpty() && uiState.regions.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = WindklarTheme.colors.cardBackground,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = "Keine Favoriten gespeichert. Tippe auf der Karte auf einen Windpark oder eine Region und füge sie zu deinen Favoriten hinzu!",
                        color = MutedGreen,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                if (uiState.parks.isNotEmpty()) {
                    Text(
                        text = "Windparks",
                        color = DarkGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    uiState.parks.forEach { park ->
                        FavoriteParkCard(
                            park = park,
                            onClick = { onParkSelected(park.id) },
                        )
                    }
                }

                if (uiState.regions.isNotEmpty()) {
                    Text(
                        text = "Regionen",
                        color = DarkGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    uiState.regions.forEach { region ->
                        FavoriteRegionCard(
                            region = region,
                            onClick = { onRegionSelected(region.type, region.id) },
                        )
                    }
                }
            }

            // Recently Viewed Section
            if (uiState.recents.isNotEmpty()) {
                Text(
                    text = "Zuletzt angesehen",
                    color = DarkGreen,
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )

                uiState.recents.forEach { park ->
                    FavoriteParkCard(
                        park = park,
                        onClick = { onParkSelected(park.id) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}


@Composable
private fun FavoritesHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(PrimaryGreen, HeaderEndGreen),
                    start = Offset.Zero,
                    end = Offset(900f, 900f),
                ),
            )
            .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Favoriten",
            color = Color.White,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun FavoriteParkCard(
    park: FavoriteParkUiModel,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = WindklarTheme.colors.cardBackground,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FavoriteThumbnail(thumbnail = park.thumbnail, isFavorite = park.isFavorite)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = park.name,
                    color = Color(0xFF1A3A1A),
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = MutedGreen,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = park.distance,
                        color = MutedGreen,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FavoriteMetricChip(
                        text = park.production,
                        icon = Icons.Outlined.Bolt,
                    )
                    FavoriteMetricChip(
                        text = park.co2Reduction,
                        icon = Icons.Outlined.Eco,
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteThumbnail(
    thumbnail: FavoriteParkThumbnail,
    isFavorite: Boolean,
) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(20.dp)),
    ) {
        Image(
            painter = painterResource(thumbnail.drawableResource()),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.9f),
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isFavorite) "Favorit" else "Nicht Favorit",
                    tint = if (isFavorite) HeartRed else Color.Gray,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun FavoriteMetricChip(
    text: String,
    icon: ImageVector,
) {
    Surface(
        shape = CircleShape,
        color = PaleGreen,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = text,
                color = DarkGreen,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun FavoriteRegionCard(
    region: FavoriteRegionUiModel,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = WindklarTheme.colors.cardBackground,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FavoriteThumbnail(thumbnail = region.thumbnail, isFavorite = region.isFavorite)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = region.name,
                    color = Color(0xFF1A3A1A),
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = MutedGreen,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = region.typeLabel,
                        color = MutedGreen,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FavoriteMetricChip(
                        text = region.production,
                        icon = Icons.Outlined.Bolt,
                    )
                    FavoriteMetricChip(
                        text = region.co2Reduction,
                        icon = Icons.Outlined.Eco,
                    )
                }
            }
        }
    }
}

private fun FavoriteParkThumbnail.drawableResource(): DrawableResource = when (this) {
    FavoriteParkThumbnail.Nordsee -> Res.drawable.favorite_windpark_nordsee
    FavoriteParkThumbnail.Ostsee -> Res.drawable.favorite_windpark_ostsee
    FavoriteParkThumbnail.Alpen -> Res.drawable.favorite_windpark_alpen
}
