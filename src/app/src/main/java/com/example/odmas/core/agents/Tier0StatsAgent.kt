package com.example.odmas.core.agents

import com.example.odmas.core.Modality
import kotlin.math.sqrt
import kotlin.math.max
import kotlin.math.min

/**
 * Tier-0 Stats Agent (multi‑modality):
 * - Maintains per‑modality rolling buffers (Touch, Motion, Typing), each 10‑D
 * - Builds a per‑modality baseline (mean/covariance) during learning
 * - Computes per‑modality Mahalanobis d^2 on current 3s window (last windowSize samples)
 * - Combines available modality d^2 into one Tier‑0 d^2 (logical combining) via max()
 */
class Tier0StatsAgent {

    private val touch = ModBuffers()
    private val typing = ModBuffers()

    private var lastModalitySeen: Modality? = null

    private val windowSize = 10  // Smaller window for more recent behavior
    private val overlapSize = 5
    private val minBaselineSamples = 25  // Reduce to work with calibration data
    private val featureCount = 10

    fun addFeatures(features: DoubleArray, modality: Modality) {
        require(features.size == featureCount) { "Expected $featureCount features, got ${features.size}" }
        val mb = mod(modality)
        val clean = sanitize(features)
        var currentSize: Int
        synchronized(mb.lock) {
            mb.buffer.add(clean)
            if (mb.buffer.size > windowSize * 4) mb.buffer.removeAt(0)  // Keep more history
            currentSize = mb.buffer.size
        }
        lastModalitySeen = modality
        if (!mb.isBaselineEstablished && currentSize >= minBaselineSamples) establishBaselineFor(modality)
    }

    fun computeMahalanobisDistance(): Double? {
        val d2Values = mutableListOf<Double>()
        perModalityList().forEach { modality ->
            computeMahalanobisDistance(modality)?.let { d2Values.add(it) }
        }
        if (d2Values.isEmpty()) return null
        // Use average instead of max to be less aggressive
        val avgD2 = d2Values.average()
        // Normalize the distance to be more reasonable
        return (avgD2 * 0.1).coerceIn(0.0, 2.0)  // Scale down for more reasonable risk
    }

    private fun computeMahalanobisDistance(modality: Modality): Double? {
        val mb = mod(modality)
        if (!mb.isBaselineEstablished) return null
        val window = getWindowSnapshot(modality) ?: return null
        val baselineMean: DoubleArray
        val covariance: Array<DoubleArray>
        synchronized(mb.lock) {
            baselineMean = mb.meanVector?.copyOf() ?: return null
            covariance = mb.covarianceMatrix?.map { it.copyOf() }?.toTypedArray() ?: return null
        }
        val windowMean = computeMean(window)
        val diff = DoubleArray(featureCount) { i -> windowMean[i] - baselineMean[i] }
        return computeMahalanobisSquared(diff, covariance)
    }

    fun getCurrentWindowFeatures(): DoubleArray? {
        val preferred = lastModalitySeen
        if (preferred != null) {
            val w = getWindowMeanFor(preferred)
            if (w != null) return w
        }
        for (m in listOf(Modality.TOUCH, Modality.TYPING)) {
            val w = getWindowMeanFor(m)
            if (w != null) return w
        }
        return null
    }

    private fun getWindowMeanFor(modality: Modality): DoubleArray? {
        val window = getWindowSnapshot(modality) ?: return null
        return computeMean(window)
    }

    fun isBaselineReady(): Boolean {
        return touch.isBaselineEstablished || typing.isBaselineEstablished
    }

    fun resetBaseline() {
        touch.reset()
        typing.reset()
        lastModalitySeen = null
    }

    fun getRunningStats(): Pair<DoubleArray, DoubleArray>? {
        val preferred = lastModalitySeen
        if (preferred != null) {
            val s = runningStatsOf(preferred)
            if (s != null) return s
        }
        for (m in listOf(Modality.TOUCH, Modality.TYPING)) {
            val s = runningStatsOf(m)
            if (s != null) return s
        }
        return null
    }

    fun getRunningStatsFor(modality: Modality): Pair<DoubleArray, DoubleArray>? {
        return runningStatsOf(modality)
    }

    fun getWindowFeaturesFor(modality: Modality): DoubleArray? {
        val window = getWindowSnapshot(modality) ?: return null
        return computeMean(window)
    }

    fun getWindowStatsFor(modality: Modality): Pair<DoubleArray, DoubleArray>? {
        val window = getWindowSnapshot(modality) ?: return null
        val mean = DoubleArray(featureCount) { 0.0 }
        var count = 0
        for (fv in window) {
            try {
                if (fv.size >= featureCount) {
                    for (i in 0 until featureCount) mean[i] += fv[i]
                    count++
                }
            } catch (_: Throwable) {}
        }
        if (count == 0) return null
        for (i in 0 until featureCount) mean[i] /= count.toDouble()
        val variance = DoubleArray(featureCount) { 0.0 }
        var count2 = 0
        for (fv in window) {
            try {
                if (fv.size >= featureCount) {
                    for (i in 0 until featureCount) {
                        val d = fv[i] - mean[i]
                        variance[i] += d * d
                    }
                    count2++
                }
            } catch (_: Throwable) {}
        }
        val denom = if (count2 > 0) count2.toDouble() else 1.0
        for (i in 0 until featureCount) variance[i] /= denom
        val std = DoubleArray(featureCount) { i -> sqrt(variance[i]) }
        return mean to std
    }

    private fun runningStatsOf(modality: Modality): Pair<DoubleArray, DoubleArray>? {
        val snapshot = getFullSnapshot(modality)
        if (snapshot.size < 2) return null
        val mean = DoubleArray(featureCount) { 0.0 }
        for (v in snapshot) {
            for (i in 0 until featureCount) mean[i] += v[i]
        }
        for (i in 0 until featureCount) mean[i] /= snapshot.size.toDouble()
        val variance = DoubleArray(featureCount) { 0.0 }
        for (v in snapshot) {
            for (i in 0 until featureCount) {
                val d = v[i] - mean[i]
                variance[i] += d * d
            }
        }
        for (i in 0 until featureCount) variance[i] /= max(1.0, snapshot.size.toDouble())
        val std = DoubleArray(featureCount) { i -> sqrt(variance[i]) }
        return mean to std
    }

    private fun establishBaselineFor(modality: Modality) {
        val mb = mod(modality)
        val snapshot = getFullSnapshot(modality)
        if (snapshot.size < minBaselineSamples) return
        val mean = computeMean(snapshot)
        val cov = computeCovarianceMatrix(snapshot, mean)
        synchronized(mb.lock) {
            mb.meanVector = mean
            mb.covarianceMatrix = cov
            mb.isBaselineEstablished = true
        }
    }

    private fun computeMean(features: List<DoubleArray>): DoubleArray {
        val mean = DoubleArray(featureCount) { 0.0 }
        var count = 0
        for (fv in features) {
            try {
                for (i in 0 until featureCount) mean[i] += fv[i]
                count++
            } catch (_: Throwable) {}
        }
        val denom = if (count > 0) count.toDouble() else 1.0
        for (i in 0 until featureCount) mean[i] /= denom
        return mean
    }

    private fun computeCovarianceMatrix(features: List<DoubleArray>, mean: DoubleArray): Array<DoubleArray> {
        val covariance = Array(featureCount) { DoubleArray(featureCount) { 0.0 } }
        var nGood = 0
        for (fv in features) {
            try {
                for (i in 0 until featureCount) {
                    val di = fv[i] - mean[i]
                    for (j in 0 until featureCount) {
                        val dj = fv[j] - mean[j]
                        covariance[i][j] += di * dj
                    }
                }
                nGood++
            } catch (_: Throwable) {}
        }
        val denom = if (nGood > 0) nGood.toDouble() else 1.0
        for (i in 0 until featureCount) for (j in 0 until featureCount) covariance[i][j] /= denom
        for (i in 0 until featureCount) covariance[i][i] += if (nGood > 0) 1e-6 else 1e-3
        return covariance
    }

    private fun computeMahalanobisSquared(diff: DoubleArray, covariance: Array<DoubleArray>): Double {
        val L = cholesky(covariance)
        val y = solveLowerTriangular(L, diff)
        var sum = 0.0
        for (i in y.indices) sum += y[i] * y[i]
        return sum
    }

    private fun cholesky(matrix: Array<DoubleArray>): Array<DoubleArray> {
        val n = matrix.size
        val L = Array(n) { DoubleArray(n) { 0.0 } }
        for (i in 0 until n) {
            for (j in 0..i) {
                var sum = 0.0
                if (j == i) {
                    for (k in 0 until j) sum += L[j][k] * L[j][k]
                    L[j][j] = sqrt(max(matrix[j][j] - sum, 1e-12))
                } else {
                    for (k in 0 until j) sum += L[i][k] * L[j][k]
                    L[i][j] = (matrix[i][j] - sum) / L[j][j]
                }
            }
        }
        return L
    }

    private fun solveLowerTriangular(L: Array<DoubleArray>, b: DoubleArray): DoubleArray {
        val n = L.size
        val y = DoubleArray(n) { 0.0 }
        for (i in 0 until n) {
            var s = 0.0
            for (j in 0 until i) s += L[i][j] * y[j]
            y[i] = (b[i] - s) / L[i][i]
        }
        return y
    }

    private fun sanitize(arr: DoubleArray): DoubleArray {
        val out = DoubleArray(featureCount) { 0.0 }
        for (i in 0 until featureCount) {
            val v = arr[i]
            out[i] = if (v.isNaN() || v == Double.POSITIVE_INFINITY || v == Double.NEGATIVE_INFINITY) 0.0 else v
        }
        return out
    }

    private fun perModalityList(): List<Modality> = listOf(Modality.TOUCH, Modality.TYPING)

    private fun buffersOf(modality: Modality): MutableList<DoubleArray> = when (modality) {
        Modality.TOUCH -> touch.buffer
        Modality.TYPING -> typing.buffer
        else -> touch.buffer
    }

    private fun mod(modality: Modality): ModBuffers = when (modality) {
        Modality.TOUCH -> touch
        Modality.TYPING -> typing
        else -> touch
    }

    private fun getWindowSnapshot(modality: Modality): List<DoubleArray>? {
        val mb = mod(modality)
        synchronized(mb.lock) {
            if (mb.buffer.isEmpty()) return null
            val n = min(windowSize, mb.buffer.size)
            return mb.buffer.takeLast(n).map { it.copyOf() }
        }
    }

    private fun getFullSnapshot(modality: Modality): List<DoubleArray> {
        val mb = mod(modality)
        synchronized(mb.lock) {
            return mb.buffer.map { it.copyOf() }
        }
    }

    private class ModBuffers {
        val lock: Any = Any()
        val buffer: MutableList<DoubleArray> = mutableListOf()
        var meanVector: DoubleArray? = null
        var covarianceMatrix: Array<DoubleArray>? = null
        var isBaselineEstablished: Boolean = false
        fun reset() {
            synchronized(lock) {
                buffer.clear()
                meanVector = null
                covarianceMatrix = null
                isBaselineEstablished = false
            }
        }
    }
}
