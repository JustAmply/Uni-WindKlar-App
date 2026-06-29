package app.feature.start

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.data.repository.AppSettingsRepository
import kotlinx.coroutines.launch

class StartViewModel(private val repository: AppSettingsRepository) : ViewModel() {
    var uiState: StartUiState by mutableStateOf(StartUiState())
        private set

    init {
        checkOnboardingStatus()
    }

    private fun checkOnboardingStatus() {
        viewModelScope.launch {
            try {
                val completed = repository.isOnboardingCompleted()
                uiState = uiState.copy(isOnboardingCompleted = completed)
            } catch (e: Throwable) {
                // If it fails to read (database error), default to showing onboarding
                uiState = uiState.copy(isOnboardingCompleted = false)
            }
        }
    }

    fun completeOnboarding(onCompleted: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.setOnboardingCompleted(true)
            } catch (e: Throwable) {
                // Ignore DB write errors for MVP UX flow
            }
            uiState = uiState.copy(isOnboardingCompleted = true)
            onCompleted()
        }
    }
}
