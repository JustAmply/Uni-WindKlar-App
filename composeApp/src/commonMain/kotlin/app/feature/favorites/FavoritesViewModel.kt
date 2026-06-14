package app.feature.favorites

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class FavoritesViewModel : ViewModel() {
    var uiState: FavoritesUiState by mutableStateOf(FavoritesUiState())
        private set
}
