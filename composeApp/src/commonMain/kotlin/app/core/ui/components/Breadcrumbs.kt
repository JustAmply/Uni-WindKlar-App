package app.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BreadcrumbSegment(
    val name: String,
    val onClick: (() -> Unit)? = null
)

@Composable
fun Breadcrumbs(
    segments: List<BreadcrumbSegment>,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.White
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically
    ) {
        segments.forEachIndexed { index, segment ->
            val isClickable = segment.onClick != null
            val isLast = index == segments.size - 1
            
            Text(
                text = segment.name,
                color = if (isClickable) contentColor else contentColor.copy(alpha = 0.65f),
                fontSize = 13.sp,
                fontWeight = if (isLast) FontWeight.Bold else if (isClickable) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (isClickable) {
                    Modifier
                        .clickable { segment.onClick() }
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                } else {
                    Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
                }
            )

            if (!isLast) {
                Text(
                    text = " > ",
                    color = contentColor.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}
