package app.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.core.ui.theme.WindklarTheme

data class FactListItem(
    val label: String,
    val value: String,
)

@Composable
fun FactList(
    items: List<FactListItem>,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        Text(
            text = "Keine Angaben verfügbar.",
            color = WindklarTheme.colors.mutedGreen,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            FactRow(item = item)
        }
    }
}

@Composable
private fun FactRow(item: FactListItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = WindklarTheme.colors.screenBackground,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = item.label,
                color = WindklarTheme.colors.mutedGreen,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.weight(0.9f),
            )
            Text(
                text = item.value,
                color = WindklarTheme.colors.darkGreen,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1.1f),
            )
        }
    }
}
