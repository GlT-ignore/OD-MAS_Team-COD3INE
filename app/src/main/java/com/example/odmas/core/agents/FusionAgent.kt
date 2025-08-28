package com.example.odmas.core.agents

import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Fusion Agent that combines Tier-0 (Mahalanobis) and Tier-1 (Autoencoder) risk scores
 * 
 * Windows: 3s with 50% overlap
 * Tier-0: Mahalanobis d² → p₀ = 1 - CDF_χ²(d², df)
 * Tier-1: AE reconstruction error e → z = (e - μₑ)/σₑ → p₁ = Φ(z) (upper-tail)
 * Weights: w₀ = 0.7 for first 20s, else w₀ = 0.5
 * Risk = 100 * (w₀*p₀ + (1-w₀)*p₁)
 * SessionRisk = EMA(risk, α=0.3)
 */
class FusionAgent {
    
    private var sessionStartTime: Long = 0L
    private var lastTier1RunTime: Long = 0L
    private var sessionRiskEMA: Double = 0.0
    private var isInitialized: Boolean = false
    
    // Tier-1 baseline statistics (from owner data)
    private var baselineMean: Double = 0.0
    private var baselineStd: Double = 1.0
    
    companion object {
        private const val WINDOW_DURATION_MS = 3000L // 3 seconds
        private const val WINDOW_OVERLAP = 0.5 // 50% overlap
        private const val TIER1_THRESHOLD = 15.0 // Run Tier-1 when Tier-0 > 15 (more sensitive)
        private const val TIER1_INTERVAL_MS = 5000L // Run Tier-1 every 5s (more frequent)
        private const val SESSION_WEIGHT_DURATION_MS = 10000L // 10 seconds
        private const val INITIAL_WEIGHT = 0.3  // Favor Tier-1 initially
        private const val NORMAL_WEIGHT = 0.2   // Tier-0 gets low weight due to reliability issues
        private const val EMA_ALPHA = 0.3
        private const val DEGREES_OF_FREEDOM = 10 // Adjust based on feature count
    }
    
    fun initializeSession(): Unit {
        sessionStartTime = System.currentTimeMillis()
        lastTier1RunTime = 0L
        sessionRiskEMA = 0.0
        isInitialized = true
    }
    
    fun setTier1Baseline(mean: Double, std: Double): Unit {
        baselineMean = mean
        baselineStd = std
    }
    
    /**
     * Process Tier-0 risk (Mahalanobis distance)
     * @param mahalanobisDistanceSquared The squared Mahalanobis distance
     * @return Tier-0 probability p₀ = 1 - CDF_χ²(d², df)
     */
    fun processTier0Risk(mahalanobisDistanceSquared: Double): Double {
        // Apply more conservative risk calculation for Tier-0
        val p0 = 1.0 - chiSquareCDF(mahalanobisDistanceSquared, DEGREES_OF_FREEDOM)
        // Dampen high Tier-0 risks since they're often false positives
        val dampened = if (p0 > 0.8) 0.6 + (p0 - 0.8) * 0.5 else p0
        return dampened.coerceIn(0.0, 1.0)
    }
    
    /**
     * Process Tier-1 risk (Autoencoder reconstruction error)
     * @param reconstructionError The AE reconstruction error
     * @return Tier-1 probability p₁ = Φ(z) where z = (e - μₑ)/σₑ
     */
    fun processTier1Risk(reconstructionError: Double): Double {
        val z = (reconstructionError - baselineMean) / baselineStd
        val p1 = 1.0 - normalCDF(z) // Upper-tail anomaly
        return p1.coerceIn(0.0, 1.0)
    }
    
    /**
     * Fuse Tier-0 and Tier-1 risks
     * @param tier0Risk Tier-0 probability
     * @param tier1Risk Tier-1 probability (null if not available)
     * @return Fused risk score (0-100)
     */
    fun fuseRisks(tier0Risk: Double, tier1Risk: Double?): Double {
        val currentTime = System.currentTimeMillis()
        val sessionDuration = currentTime - sessionStartTime
        
        // Determine weight based on session duration
        val w0 = if (sessionDuration < SESSION_WEIGHT_DURATION_MS) INITIAL_WEIGHT else NORMAL_WEIGHT
        
        // If Tier-1 is not available, use only Tier-0
        val p1 = tier1Risk ?: tier0Risk // Fallback to Tier-0 if Tier-1 unavailable
        
        // Calculate fused risk
        val risk = 100.0 * (w0 * tier0Risk + (1.0 - w0) * p1)
        
        // Update session risk EMA
        if (!isInitialized) {
            sessionRiskEMA = risk
            isInitialized = true
        } else {
            sessionRiskEMA = EMA_ALPHA * risk + (1.0 - EMA_ALPHA) * sessionRiskEMA
        }
        
        return sessionRiskEMA
    }
    
    /**
     * Check if Tier-1 should run based on Tier-0 risk and timing
     * @param tier0Risk Current Tier-0 risk
     * @return True if Tier-1 should run
     */
    fun shouldRunTier1(tier0Risk: Double): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastTier1 = currentTime - lastTier1RunTime
        
        return tier0Risk > TIER1_THRESHOLD || timeSinceLastTier1 >= TIER1_INTERVAL_MS
    }
    
    fun markTier1Run(): Unit {
        lastTier1RunTime = System.currentTimeMillis()
    }
    
    fun getSessionRisk(): Double = sessionRiskEMA
    
    // Mathematical utility functions
    
    /**
     * Chi-square CDF approximation
     */
    private fun chiSquareCDF(x: Double, df: Int): Double {
        if (x <= 0) return 0.0
        if (x > 1000) return 1.0
        
        // Approximation using incomplete gamma function
        return incompleteGamma(x / 2.0, df / 2.0)
    }
    
    /**
     * Normal CDF approximation
     */
    private fun normalCDF(x: Double): Double {
        return 0.5 * (1.0 + erf(x / sqrt(2.0)))
    }
    
    /**
     * Error function approximation
     */
    private fun erf(x: Double): Double {
        val a1 = 0.254829592
        val a2 = -0.284496736
        val a3 = 1.421413741
        val a4 = -1.453152027
        val a5 = 1.061405429
        val p = 0.3275911
        
        val sign = if (x >= 0) 1.0 else -1.0
        val absX = kotlin.math.abs(x)
        
        val t = 1.0 / (1.0 + p * absX)
        val y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * exp(-absX * absX)
        
        return sign * y
    }
    
    /**
     * Incomplete gamma function approximation
     */
    private fun incompleteGamma(x: Double, a: Double): Double {
        if (x <= 0 || a <= 0) return 0.0
        
        // Simple approximation for small x
        if (x < a + 1) {
            var sum = 1.0 / a
            var term = 1.0 / a
            for (i in 1..100) {
                term *= x / (a + i)
                sum += term
                if (term < 1e-10) break
            }
            return (x.pow(a) * exp(-x) * sum) / gamma(a)
        } else {
            // Use complement for large x
            return 1.0 - incompleteGammaComplement(x, a)
        }
    }
    
    private fun incompleteGammaComplement(x: Double, a: Double): Double {
        var prev = 1.0
        var current = 1.0 + (1.0 - a) / x
        var n = 1
        
        while (n < 100) {
            val next = current + (n - a) * (current - prev) / x
            if (kotlin.math.abs(next - current) < 1e-10) break
            prev = current
            current = next
            n++
        }
        
        return (x.pow(a - 1) * exp(-x) * current) / gamma(a)
    }
    
    /**
     * Gamma function approximation
     */
    private fun gamma(x: Double): Double {
        // Lanczos approximation
        val g = 7.0
        val p = doubleArrayOf(
            0.99999999999980993, 676.5203681218851, -1259.1392167224028,
            771.32342877765313, -176.61502916214059, 12.507343278686905,
            -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7
        )
        
        if (x < 0.5) {
            return Math.PI / (sin(Math.PI * x) * gamma(1.0 - x))
        }
        
        var z = x - 1.0
        var result = p[0]
        for (i in 1 until p.size) {
            result += p[i] / (z + i)
        }
        val t = z + g + 0.5
        
        return sqrt(2.0 * Math.PI) * t.pow(z + 0.5) * exp(-t) * result
    }
    
    private fun sin(x: Double): Double = kotlin.math.sin(x)
}
