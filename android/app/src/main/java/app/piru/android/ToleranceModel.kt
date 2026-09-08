package app.piru.android

import kotlin.math.exp

object ToleranceModel {
    fun modulation(daysSinceDose: Double, recoveryHalfLifeDays: Double = 7.0): Double {
        if (daysSinceDose <= 0) return 1.0
        if (recoveryHalfLifeDays <= 0) return 0.0
        return (1.0 - exp(-daysSinceDose * kotlin.math.ln(2.0) / recoveryHalfLifeDays))
            .coerceIn(0.0, 1.0)
    }
}
