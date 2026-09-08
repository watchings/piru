package app.piru.android

data class DoseEntry(
    val id: String,
    val substance: String,
    val amount: Double,
    val unit: String,
    val route: String,
    val timestamp: Long,
    val notes: String?
)
