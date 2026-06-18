package app.feature.profile

data class ProfileUiState(
    val aboutTitle: String = "Über WindKlar",
    val aboutText: String = "WindKlar ist eine Bürgerplattform für Transparenz in der Windenergie. Diese Anwendung wurde im Rahmen eines universitären Projektseminars entwickelt und bereitet öffentlich verfügbare Daten verständlich auf.",
    val version: String = "Version 1.0.0 (MVP)",
    val attribution: String = "Marktstammdatenregister (MaStR)",
    val limitations: List<String> = emptyList(),
)

