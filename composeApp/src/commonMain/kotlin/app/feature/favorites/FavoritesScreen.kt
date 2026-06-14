package app.feature.favorites

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Favorite
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
import meinwindpark.composeapp.generated.resources.Res
import meinwindpark.composeapp.generated.resources.favorite_windpark_alpen
import meinwindpark.composeapp.generated.resources.favorite_windpark_nordsee
import meinwindpark.composeapp.generated.resources.favorite_windpark_ostsee
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val ScreenBackground = Color(0xFFF8FAF7)
private val PrimaryGreen = Color(0xFF2D5A2D)
private val HeaderEndGreen = Color(0xFF43A047)
private val DarkGreen = Color(0xFF1A3A1A)
private val MutedGreen = Color(0xFF5A7A5A)
private val PaleGreen = Color(0xFFE8F5E9)
private val HeartRed = Color(0xFFE53935)

@Composable
fun FavoritesScreen(
    onBackClick: () -> Unit,
    onParkSelected: (parkId: String) -> Unit,
    modifier: Modifier = Modifier,
    uiState: FavoritesUiState = FavoritesUiState(),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .verticalScroll(rememberScrollState()),
    ) {
        FavoritesHeader(onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .offset(y = (-16).dp)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            uiState.parks.forEach { park ->
                FavoriteParkCard(
                    park = park,
                    onClick = { onParkSelected(park.id) },
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun FavoritesHeader(
    onBackClick: () -> Unit,
) {
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
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                .clickable(onClick = onBackClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Zurueck",
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }

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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            FavoriteThumbnail(thumbnail = park.thumbnail)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = park.name,
                    color = DarkGreen,
                    fontSize = 18.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = MutedGreen,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = park.distance,
                        color = MutedGreen,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

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
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Favorit",
                    tint = HeartRed,
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

private fun FavoriteParkThumbnail.drawableResource(): DrawableResource = when (this) {
    FavoriteParkThumbnail.Nordsee -> Res.drawable.favorite_windpark_nordsee
    FavoriteParkThumbnail.Ostsee -> Res.drawable.favorite_windpark_ostsee
    FavoriteParkThumbnail.Alpen -> Res.drawable.favorite_windpark_alpen
}
