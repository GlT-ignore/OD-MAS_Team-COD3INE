package com.example.odmas.core.agents

import android.content.Context
import com.example.odmas.core.Modality
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class Tier1BehaviorAgent(private val context: Context) {

    // Temporary switch to disable motion modality entirely
    private val DISABLE_MOTION: Boolean = true

    data class MotionModel(
        var stationaryMean: Double = 0.0,
        var stationaryStd: Double = 1.0,
        var tremorLevelMean: Double = 0.0,
        var tremorLevelStd: Double = 1.0,
        var samples: Int = 0
    )

    data class TouchModel(
        var dwellMean: Double = 0.0,
        var dwellStd: Double = 1.0,
        var dwellMedian: Double = 0.0,
        var intervalMean: Double = 0.0,
        var intervalStd: Double = 1.0,
        var pressureMean: Double = 0.0,
        var pressureStd: Double = 1.0,
        var sizeMean: Double = 0.0,
        var sizeStd: Double = 1.0,
        var taps: Int = 0
    )

    data class TypingModel(
        var dwellMean: Double = 0.0,
        var dwellStd: Double = 1.0,
        var dwellP25: Double = 0.0,
        var dwellP75: Double = 0.0,
        var flightMean: Double = 0.0,
        var flightStd: Double = 1.0,
        var spaceRhythmMean: Double = 0.0,
        var spaceRhythmStd: Double = 1.0,
        var wpm: Double = 0.0,
        var keyEvents: Int = 0
    )

    private val motionAccelNoG = mutableListOf<Double>()
    private val motionTremorLevels = mutableListOf<Double>()
    private var motionWindows = 0

    private val touchDwells = mutableListOf<Double>()
    private val touchIntervals = mutableListOf<Double>()
    private val touchPressures = mutableListOf<Double>()
    private val touchSizes = mutableListOf<Double>()
    private var lastTouchWindowTimeMs: Long? = null
    private var touchWindows = 0

    private val typingDwellsMs = mutableListOf<Long>()
    private val typingFlightsMs = mutableListOf<Long>()
    private val spaceTimestampsMs = mutableListOf<Long>()
    private var typingFirstTimestampMs: Long? = null
    private var typingLastTimestampMs: Long? = null
    private var typingKeys = 0

    private var motionModel: MotionModel? = null
    private var touchModel: TouchModel? = null
    private var typingModel: TypingModel? = null

    private val MOTION_WINDOWS_NEEDED = 4
    private val TOUCH_TAPS_NEEDED = 30
    private val TYPING_KEYS_NEEDED = 100

    fun initializeModel(): Boolean = true

    fun isCalibrated(): Boolean {
        return (
            (DISABLE_MOTION || motionModel != null) &&
            (touchModel != null) &&
            (typingModel != null)
        )
    }

    fun isAnyModalityReady(): Boolean {
        return (motionModel != null) || (touchModel != null) || (typingModel != null)
    }

    fun getCalibrationProgressPercent(): Int {
        val pt = ((touchWindows.toDouble() / TOUCH_TAPS_NEEDED.toDouble()) * 100.0).coerceIn(0.0, 100.0)
        val pk = ((typingKeys.toDouble() / TYPING_KEYS_NEEDED.toDouble()) * 100.0).coerceIn(0.0, 100.0)
        if (DISABLE_MOTION) {
            val avg2 = (pt + pk) / 2.0
            return avg2.toInt()
        }
        val pm = ((motionWindows.toDouble() / MOTION_WINDOWS_NEEDED.toDouble()) * 100.0).coerceIn(0.0, 100.0)
        val avg3 = (pm + pt + pk) / 3.0
        return avg3.toInt()
    }

    fun getMotionProgress(): Pair<Int, Int> = if (DISABLE_MOTION) 0 to 0 else motionWindows to MOTION_WINDOWS_NEEDED
    fun getTouchProgress(): Pair<Int, Int> = touchWindows to TOUCH_TAPS_NEEDED
    fun getTypingProgress(): Pair<Int, Int> = typingKeys to TYPING_KEYS_NEEDED

    enum class CalibrationStage { MOTION, TOUCH, TYPING, COMPLETE }

    fun getCurrentCalibrationStage(): CalibrationStage {
        if (!DISABLE_MOTION && motionModel == null) return CalibrationStage.MOTION
        if (touchModel == null) return CalibrationStage.TOUCH
        if (typingModel == null) return CalibrationStage.TYPING
        return CalibrationStage.COMPLETE
    }

    fun resetBaseline() {
        motionAccelNoG.clear()
        motionTremorLevels.clear()
        motionWindows = 0
        touchDwells.clear()
        touchIntervals.clear()
        touchPressures.clear()
        touchSizes.clear()
        lastTouchWindowTimeMs = null
        touchWindows = 0
        typingDwellsMs.clear()
        typingFlightsMs.clear()
        spaceTimestampsMs.clear()
        typingFirstTimestampMs = null
        typingLastTimestampMs = null
        typingKeys = 0
        motionModel = null
        touchModel = null
        typingModel = null
    }

    fun addBaselineSample(features: DoubleArray, modality: Modality) {
        submitCalibrationSample(features, modality)
        if (modality == Modality.TYPING) {
            val dwellSec = features.getOrNull(4) ?: 0.2
            val flightSec = features.getOrNull(1) ?: 0.2
            val dwellMs = (dwellSec * 1000.0).toLong().coerceAtLeast(1)
            val flightMs = (flightSec * 1000.0).toLong().coerceAtLeast(1)
            submitTypingTiming(dwellMs, flightMs, false)
        }
    }

    fun trainAllIfNeeded() {
        if (!DISABLE_MOTION && motionModel == null && motionWindows >= MOTION_WINDOWS_NEEDED) motionModel = buildMotionModel()
        if (touchModel == null && touchWindows >= TOUCH_TAPS_NEEDED) touchModel = buildTouchModel()
        if (typingModel == null && typingKeys >= TYPING_KEYS_NEEDED) typingModel = buildTypingModel()
    }

    fun close() {}

    fun submitCalibrationSample(features: DoubleArray, modality: Modality) {
        if (features.size != 10) return
        when (modality) {
            Modality.MOTION -> {
                if (DISABLE_MOTION) return
                motionWindows++
                val accelMagnitude = features[0]
                val tremor = features[7]
                val accelNoG = abs(accelMagnitude - 9.8)
                motionAccelNoG.add(accelNoG)
                motionTremorLevels.add(tremor)
                if (motionModel == null && motionWindows >= MOTION_WINDOWS_NEEDED) motionModel = buildMotionModel()
            }
            Modality.TOUCH -> {
                touchWindows++
                val now = System.currentTimeMillis()
                val dwellSec = features.getOrNull(4) ?: 0.0
                val pressure = features.getOrNull(2) ?: 0.0
                val size = features.getOrNull(3) ?: 0.0
                touchDwells.add(max(0.0, dwellSec))
                touchPressures.add(pressure)
                touchSizes.add(size)
                val last = lastTouchWindowTimeMs
                if (last != null) touchIntervals.add((now - last).toDouble() / 1000.0)
                lastTouchWindowTimeMs = now
                if (touchModel == null && touchWindows >= TOUCH_TAPS_NEEDED) touchModel = buildTouchModel()
            }
            Modality.TYPING -> {}
            else -> {}
        }
    }

    fun submitTypingTiming(dwellMs: Long, flightMs: Long, isSpace: Boolean) {
        if (typingFirstTimestampMs == null) typingFirstTimestampMs = System.currentTimeMillis()
        typingLastTimestampMs = System.currentTimeMillis()
        if (dwellMs > 0) typingDwellsMs.add(dwellMs)
        if (flightMs > 0) typingFlightsMs.add(flightMs)
        typingKeys++
        if (isSpace) spaceTimestampsMs.add(System.currentTimeMillis())
        if (typingModel == null && typingKeys >= TYPING_KEYS_NEEDED) typingModel = buildTypingModel()
    }

    fun computeTier1Probability(features: DoubleArray, modality: Modality): Double? {
        if (!isCalibrated()) return 0.5
        return when (modality) {
            Modality.MOTION -> {
                val m = motionModel ?: return 0.5
                scoreMotion(features, m)
            }
            Modality.TOUCH -> {
                val m = touchModel ?: return 0.5
                scoreTouch(features, m)
            }
            Modality.TYPING -> {
                val m = typingModel ?: return 0.5
                scoreTyping(features, m)
            }
            else -> 0.5
        }
    }

    private fun buildMotionModel(): MotionModel {
        val accelStats = meanStd(motionAccelNoG)
        val tremorStats = meanStd(motionTremorLevels)
        return MotionModel(
            stationaryMean = accelStats.first,
            stationaryStd = max(1e-3, accelStats.second),
            tremorLevelMean = tremorStats.first,
            tremorLevelStd = max(1e-3, tremorStats.second),
            samples = motionWindows
        )
    }

    private fun buildTouchModel(): TouchModel {
        val dwellMS = touchDwells.map { it * 1000.0 }
        val dwellMean = mean(touchDwells)
        val dwellStd = std(touchDwells, dwellMean)
        val dwellMedian = median(dwellMS) / 1000.0
        val intervalsMean = mean(touchIntervals)
        val intervalsStd = std(touchIntervals, intervalsMean)
        val pMean = mean(touchPressures)
        val pStd = std(touchPressures, pMean)
        val sMean = mean(touchSizes)
        val sStd = std(touchSizes, sMean)
        return TouchModel(
            dwellMean = dwellMean,
            dwellStd = max(1e-3, dwellStd),
            dwellMedian = dwellMedian,
            intervalMean = intervalsMean,
            intervalStd = max(1e-3, intervalsStd),
            pressureMean = pMean,
            pressureStd = max(1e-3, pStd),
            sizeMean = sMean,
            sizeStd = max(1e-3, sStd),
            taps = touchWindows
        )
    }

    private fun buildTypingModel(): TypingModel {
        val dwellMean = mean(typingDwellsMs.map { it.toDouble() }) / 1000.0
        val dwellStd = std(typingDwellsMs.map { it.toDouble() }, dwellMean * 1000.0) / 1000.0
        val dwellP25 = percentile(typingDwellsMs, 25) / 1000.0
        val dwellP75 = percentile(typingDwellsMs, 75) / 1000.0
        val flightMean = mean(typingFlightsMs.map { it.toDouble() }) / 1000.0
        val flightStd = std(typingFlightsMs.map { it.toDouble() }, flightMean * 1000.0) / 1000.0
        val spaceIntervalsMs = mutableListOf<Long>()
        for (i in 1 until spaceTimestampsMs.size) spaceIntervalsMs.add(spaceTimestampsMs[i] - spaceTimestampsMs[i - 1])
        val srMean = if (spaceIntervalsMs.isNotEmpty()) mean(spaceIntervalsMs.map { it.toDouble() }) / 1000.0 else 0.0
        val srStd = if (spaceIntervalsMs.size > 1) std(spaceIntervalsMs.map { it.toDouble() }, srMean * 1000.0) / 1000.0 else 1.0
        val first = typingFirstTimestampMs ?: System.currentTimeMillis()
        val last = typingLastTimestampMs ?: first
        val totalMin = max(1e-6, (last - first).toDouble() / 60000.0)
        val words = (spaceTimestampsMs.size + 1).toDouble()
        val wpm = words / totalMin
        return TypingModel(
            dwellMean = dwellMean,
            dwellStd = max(1e-3, dwellStd),
            dwellP25 = dwellP25,
            dwellP75 = dwellP75,
            flightMean = flightMean,
            flightStd = max(1e-3, flightStd),
            spaceRhythmMean = srMean,
            spaceRhythmStd = max(1e-3, srStd),
            wpm = wpm,
            keyEvents = typingKeys
        )
    }

    private fun scoreMotion(features: DoubleArray, model: MotionModel): Double {
        val accelMag = features.getOrNull(0) ?: 0.0
        val tremor = features.getOrNull(7) ?: 0.0
        val accelNoG = abs(accelMag - 9.8)
        val accelDev = abs(accelNoG - model.stationaryMean) / (model.stationaryStd + 1e-3)
        val deviations = mutableListOf<Double>()
        deviations.add(accelDev)
        if (accelNoG < model.stationaryMean + model.stationaryStd) {
            val tremorDev = abs(tremor - model.tremorLevelMean) / (model.tremorLevelStd + 1e-3)
            if (tremorDev > 0.5) deviations.add(2.0)
        }
        val avg = deviations.average()
        val score = sigmoid((avg - 2.0) * 0.5)
        return score.coerceIn(0.0, 1.0)
    }

    private fun scoreTouch(features: DoubleArray, model: TouchModel): Double {
        val dwell = max(0.0, features.getOrNull(4) ?: 0.0)
        val pressure = features.getOrNull(2) ?: 0.0
        val size = features.getOrNull(3) ?: 0.0
        val rhythmVar = features.getOrNull(8)?.let { max(0.0, it) } ?: 0.0
        fun z(v: Double, mu: Double, sd: Double): Double = abs(v - mu) / (sd + 1e-3)
        val zDwell = z(dwell, model.dwellMean, model.dwellStd).coerceIn(0.0, 6.0)
        val zPressure = z(pressure, model.pressureMean, model.pressureStd).coerceIn(0.0, 6.0)
        val zSize = z(size, model.sizeMean, model.sizeStd).coerceIn(0.0, 6.0)
        val pDwell = sigmoid((zDwell - 1.0) * 1.2)
        val pPS = 0.5 * sigmoid((zPressure - 1.5) * 0.8) + 0.5 * sigmoid((zSize - 1.5) * 0.8)
        val intervalStd2 = (model.intervalStd * model.intervalStd).coerceAtLeast(1e-9)
        val hasValidRhythm = model.intervalStd > 0.05 && rhythmVar in 1e-6..(10.0 * intervalStd2)
        val pRhythm = if (hasValidRhythm) {
            val rDev = abs(ln(rhythmVar + 1e-9) - ln(intervalStd2 + 1e-9))
            sigmoid((rDev - 0.8) * 1.0)
        } else null
        val score = if (pRhythm != null) (0.80 * pDwell + 0.15 * pPS + 0.05 * pRhythm) else (0.85 * pDwell + 0.15 * pPS)
        return score.coerceIn(0.0, 1.0)
    }

    private fun scoreTyping(features: DoubleArray, model: TypingModel): Double {
        val dwellSec = features.getOrNull(4) ?: model.dwellMean
        val flightSec = features.getOrNull(1) ?: model.flightMean
        val rhythmVariance = max(0.0, features.getOrNull(8) ?: 0.0)
        val scores = mutableListOf<Double>()
        var dwellDev = 0.0
        if (dwellSec < model.dwellP25) {
            dwellDev = (model.dwellP25 - dwellSec) / (model.dwellStd + 1e-3)
        } else if (dwellSec > model.dwellP75) {
            dwellDev = (dwellSec - model.dwellP75) / (model.dwellStd + 1e-3)
        }
        scores.add(sigmoid(dwellDev * 2.0))
        val flightDev = abs(flightSec - model.flightMean) / (model.flightStd + 1e-3)
        scores.add(sigmoid((flightDev - 1.5) * 2.0))
        if (rhythmVariance > 0.0) {
            val rhythmDev = abs(ln(rhythmVariance + 1.0) - ln(model.spaceRhythmStd * model.spaceRhythmStd + 1.0))
            scores.add(sigmoid(rhythmDev * 3.0))
        }
        val currentWpm = model.wpm
        val speedRatio = if (model.wpm > 0) currentWpm / model.wpm else 1.0
        if (speedRatio < 0.5 || speedRatio > 2.0) scores.add(0.8)
        return scores.average().coerceIn(0.0, 1.0)
    }

    private fun mean(x: List<Double>): Double {
        if (x.isEmpty()) return 0.0
        var s = 0.0
        for (v in x) s += v
        return s / x.size
    }

    private fun std(x: List<Double>, mu: Double): Double {
        if (x.size < 2) return 1.0
        var s = 0.0
        for (v in x) {
            val d = v - mu
            s += d * d
        }
        return sqrt(s / x.size)
    }

    private fun meanStd(x: List<Double>): Pair<Double, Double> {
        val mu = mean(x)
        val sd = std(x, mu)
        return mu to sd
    }

    private fun median(x: List<Double>): Double {
        if (x.isEmpty()) return 0.0
        val a = x.sorted()
        val n = a.size
        return if (n % 2 == 1) a[n / 2] else 0.5 * (a[n / 2 - 1] + a[n / 2])
    }

    private fun percentile(x: List<Long>, p: Int): Double {
        if (x.isEmpty()) return 0.0
        if (x.size == 1) return x[0].toDouble()
        val a = x.sorted()
        val rank = ((p / 100.0) * (a.size - 1))
        val lo = a[max(0, rank.toInt())]
        val hi = a[min(a.size - 1, rank.toInt() + 1)]
        val frac = rank - rank.toInt()
        return lo + (hi - lo) * frac
    }

    private fun sigmoid(z: Double): Double = 1.0 / (1.0 + kotlin.math.exp(-z))
}


