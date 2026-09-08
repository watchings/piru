package app.piru.android

import android.content.Context
import org.json.JSONArray

data class SubstanceSummary(
    val name: String,
    val displayName: String,
    val category: String
)

class SubstanceCatalog(context: Context) {
    private val substances: List<SubstanceSummary>

    init {
        val json = context.assets.open("catalog/substances.json")
            .bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        substances = (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            SubstanceSummary(
                name = item.optString("name"),
                displayName = item.optString("display_name"),
                category = item.optString("category")
            )
        }
    }

    fun search(query: String, limit: Int = 50): List<SubstanceSummary> {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return substances.take(limit)
        return substances.asSequence()
            .filter { it.name.lowercase().contains(needle) ||
                it.displayName.lowercase().contains(needle) }
            .sortedBy { it.name }
            .take(limit)
            .toList()
    }
}
