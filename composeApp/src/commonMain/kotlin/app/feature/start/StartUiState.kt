package app.feature.start

data class StartUiState(
    val appName: String = "WindKlar",
    val subtitle: String = "Transparente Windenergie für Deutschland",
    val highlights: List<String> = listOf(
        "Entdecken Sie Windparks in Ihrer Nähe",
        "Verstehen Sie erneuerbare Energie",
        "Schützen Sie unsere Umwelt",
    ),
    val ctaLabel: String = "Jetzt entdecken",
)
