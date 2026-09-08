package app.piru.android

import kotlin.math.exp
import kotlin.math.ln

object PkModel {
    fun remainingFraction(minutes: Double, halfLifeMinutes: Double, timeToPeakMinutes: Double = 60.0): Double {
        if (minutes <= 0) return 1.0
        if (halfLifeMinutes <= 0 || timeToPeakMinutes <= 0) return 0.0
        val ke = ln(2.0) / halfLifeMinutes
        var ka = 4.0 * ke
        repeat(50) {
            if (ka <= ke) return 0.0
            val delta = ka - ke
            val f = ln(ka / ke) / delta - timeToPeakMinutes
            val derivative = (delta / ka - ln(ka / ke)) / (delta * delta)
            if (kotlin.math.abs(derivative) >= 1e-15) {
                ka = maxOf(ke * 1.01, ka - f / derivative)
            }
        }
        if (kotlin.math.abs(ka - ke) < 1e-10) return (1 + ke * minutes) * exp(-ke * minutes)
        return maxOf(0.0, (ka * exp(-ke * minutes) - ke * exp(-ka * minutes)) / (ka - ke))
    }
}
