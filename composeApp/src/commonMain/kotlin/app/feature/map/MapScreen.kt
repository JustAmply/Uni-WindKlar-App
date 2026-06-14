package app.feature.map

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import windklar.composeapp.generated.resources.Res
import windklar.composeapp.generated.resources.start_background
import org.jetbrains.compose.resources.painterResource

@Composable
fun MapScreen(
    onParkSelected: (parkId: String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val isActiveSelected = true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAF7)),
    ) {
        MapBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SearchBar(
                query = query,
                onQueryChange = { query = it },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(text = "Aktiv", selected = isActiveSelected)
                StatusChip(text = "Geplant", selected = !isActiveSelected)
                StatusChip(text = "Im Bau", selected = !isActiveSelected)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp, bottom = 230.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MapActionButton(
                icon = Icons.Outlined.Add,
                contentDescription = "Windanlage melden",
                containerColor = Color(0xFF2D5A2D),
                contentColor = Color.White,
            )
            MapActionButton(
                icon = Icons.Outlined.NearMe,
                contentDescription = "Standort zentrieren",
                containerColor = Color.White,
                contentColor = Color(0xFF2D5A2D),
            )
        }

        ParkPreviewSheet(
            modifier = Modifier.align(Alignment.BottomCenter),
            onDetailsClick = { onParkSelected("demo-park") },
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 10.dp,
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = {
                Text(
                    text = "Windpark oder Ort suchen...",
                    color = Color(0xFF5A7A5A),
                    fontSize = 16.sp,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Suche",
                    tint = Color(0xFF5A7A5A),
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun StatusChip(
    text: String,
    selected: Boolean,
) {
    Surface(
        shape = CircleShape,
        color = if (selected) Color(0xFF2D5A2D) else Color.White,
        shadowElevation = 4.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            color = if (selected) Color.White else Color(0xFF2D5A2D),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun MapActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        modifier = Modifier
            .size(56.dp)
            .clickable { },
        shape = CircleShape,
        color = containerColor,
        shadowElevation = 10.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun MapBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(color = Color(0xFFE8F5E9), alpha = 0.35f)
        drawArc(
            color = Color(0xFFCFE2CF),
            startAngle = 20f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(size.width * 0.18f, size.height * 0.08f),
            size = Size(size.width * 0.64f, size.height * 0.16f),
            alpha = 0.45f,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f),
        )
        drawCircle(color = Color(0xFFBDD7BD), radius = 12f, center = Offset(size.width * 0.30f, size.height * 0.21f))
        drawCircle(color = Color(0xFFB4C8B4), radius = 12f, center = Offset(size.width * 0.75f, size.height * 0.16f))
        drawCircle(color = Color(0xFFAFCEAF), radius = 12f, center = Offset(size.width * 0.50f, size.height * 0.41f))
        drawCircle(color = Color(0xFFC4D9AB), radius = 12f, center = Offset(size.width * 0.40f, size.height * 0.53f))
        drawCircle(color = Color(0xFFACCCAC), radius = 12f, center = Offset(size.width * 0.70f, size.height * 0.29f))
    }
}

@Composable
private fun ParkPreviewSheet(
    modifier: Modifier = Modifier,
    onDetailsClick: () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = Color.White,
        shadowElevation = 24.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
                    .clip(RoundedCornerShape(16.dp)),
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(Res.drawable.start_background),
                    contentDescription = "Windpark Vorschau",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = CircleShape,
                    color = Color(0xFF43A047),
                ) {
                    Text(
                        text = "Aktiv",
                        color = Color.White,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Windpark Nordsee",
                    color = Color(0xFF1A3A1A),
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Medium,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF5A7A5A),
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "12 km entfernt",
                        color = Color(0xFF5A7A5A),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Jahresproduktion",
                    value = "42 GWh",
                    icon = Icons.Outlined.Bolt,
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "CO2 Einsparung",
                    value = "25.000 t",
                    icon = Icons.Outlined.Eco,
                )
            }

            Button(
                onClick = onDetailsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2D5A2D),
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = "Details anzeigen",
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFE8F5E9),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF2D5A2D),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = label,
                    color = Color(0xFF2D5A2D),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
            Text(
                text = value,
                color = Color(0xFF1A3A1A),
                fontSize = 18.sp,
                lineHeight = 28.sp,
            )
        }
    }
}
