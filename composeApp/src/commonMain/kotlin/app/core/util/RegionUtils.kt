package app.core.util

private val normalizedRedundantTokens = listOf(
    "kreisfreie stadt",
    "kreisangehoerige stadt",
    "landkreis",
    "stadtkreis",
    "staedteregion",
    "staedte region",
    "regionalverband",
    "gemeinde",
    "stadt",
    "kreis",
)

fun isRedundantMunicipality(districtName: String, municipalityName: String): Boolean {
    val normalizedDistrict = normalizeRegionName(districtName)
    val normalizedMunicipality = normalizeRegionName(municipalityName)
    return normalizedDistrict.isNotEmpty() && normalizedDistrict == normalizedMunicipality
}

fun normalizeRegionName(name: String): String {
    var normalized = name
        .trim()
        .lowercase()
        .replace("ä", "ae")
        .replace("ö", "oe")
        .replace("ü", "ue")
        .replace("ß", "ss")
        .replace(Regex("[\\p{Punct}]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    var changed = true
    while (changed) {
        changed = false
        for (token in normalizedRedundantTokens) {
            if (normalized == token) {
                normalized = ""
                changed = true
                break
            }
            if (normalized.startsWith("$token ")) {
                normalized = normalized.removePrefix("$token ").trim()
                changed = true
                break
            }
            if (normalized.endsWith(" $token")) {
                normalized = normalized.removeSuffix(" $token").trim()
                changed = true
                break
            }
        }
    }

    return normalized
}
