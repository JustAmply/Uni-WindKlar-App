package app.feature.search

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import app.core.ui.components.ScreenPlaceholder

@Composable
fun SearchScreen(
    onParkSelected: (parkId: String) -> Unit,
) {
    ScreenPlaceholder(
        title = "Windpark suchen",
        body = "Hier entstehen Suche, Suchhistorie und direkte Einstiege zu Windpark-Details.",
        action = {
            Button(onClick = { onParkSelected("demo-park") }) {
                Text("Letzten Windpark öffnen")
            }
        },
    )
}
