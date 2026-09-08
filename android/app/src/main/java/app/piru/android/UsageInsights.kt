package app.piru.android

data class UsageSummary(
    val doseCount: Int,
    val substanceCount: Int,
    val totalAmount: Double,
    val windowDays: Int = 30
)

object UsageInsights {
    fun summarize(entries: List<DoseEntry>, now: Long = System.currentTimeMillis()): UsageSummary {
        val window = 30L * 24L * 60L * 60L * 1000L
        val recent = entries.filter { now - it.timestamp < window }
        return UsageSummary(
            doseCount = recent.size,
            substanceCount = recent.map { it.substance }.toSet().size,
            totalAmount = recent.sumOf { it.amount }
        )
    }
}
