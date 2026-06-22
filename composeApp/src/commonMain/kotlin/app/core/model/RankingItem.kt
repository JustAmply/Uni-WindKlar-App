package app.core.model

data class RankingItem(
    val id: String,
    val rank: Int,
    val name: String,
    val subtitle: String,
    val valueLabel: String,
    val progress: Float,
    val details: List<RankingDetailLine>,
)

data class RankingDetailLine(
    val label: String,
    val value: String,
)
