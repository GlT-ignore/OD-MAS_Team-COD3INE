# OD-MAS Implementation and Engineering Guide
## Comprehensive Development and Deployment Guide

### Development Environment Setup

#### Prerequisites

**Required Software**:
- **Android Studio**: Arctic Fox or later (2023.1+)
- **Java Development Kit (JDK)**: Version 11 or 17
- **Android SDK**: API Level 24+ (Android 7.0+)
- **Git**: Version 2.40.0+
- **Python**: Version 3.8+ (for Chaquopy integration)

**System Requirements**:
- **Operating System**: Windows 10+, macOS 10.15+, or Ubuntu 18.04+
- **RAM**: 8GB minimum, 16GB recommended
- **Storage**: 10GB free space for development environment
- **Processor**: Multi-core processor (Intel i5/AMD Ryzen 5 or better)

#### Environment Configuration

**Android Studio Setup**:
```bash
# Download and install Android Studio
# Configure Android SDK
# Install required SDK platforms and tools
sdkmanager "platforms;android-34"
sdkmanager "build-tools;34.0.0"
sdkmanager "platform-tools"
```

**Gradle Configuration**:
```kotlin
// gradle.properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=1024m -XX:+HeapDumpOnOutOfMemoryError
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.configureondemand=true
android.useAndroidX=true
android.enableJetifier=true
kotlin.code.style=official
```

**Chaquopy Setup**:
```kotlin
// app/build.gradle.kts
plugins {
    id("com.chaquo.python")
}

chaquopy {
    defaultConfig {
        version = "3.8"
        pip {
            install("scikit-learn==1.3.0")
            install("numpy==1.24.0")
            install("pandas==2.0.0")
        }
    }
}
```

### Project Structure

#### Directory Organization

```
OD-MAS/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/odmas/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── core/
│   │   │   │   │   ├── SecurityManager.kt
│   │   │   │   │   ├── Modality.kt
│   │   │   │   │   └── agents/
│   │   │   │   │       ├── Tier0StatsAgent.kt
│   │   │   │   │       ├── Tier1AutoencoderAgent.kt
│   │   │   │   │       ├── FusionAgent.kt
│   │   │   │   │       └── PolicyAgent.kt
│   │   │   │   ├── chaquopy/
│   │   │   │   │   └── ChaquopyBehavioralManager.kt
│   │   │   │   ├── services/
│   │   │   │   │   ├── TouchAccessibilityService.kt
│   │   │   │   │   └── ForegroundService.kt
│   │   │   │   └── ui/
│   │   │   │       ├── screens/
│   │   │   │       │   ├── HomeScreen.kt
│   │   │   │       │   ├── CalibrationScreen.kt
│   │   │   │       │   └── SettingsScreen.kt
│   │   │   │       └── components/
│   │   │   │           ├── RiskMeter.kt
│   │   │   │           └── BiometricPrompt.kt
│   │   │   ├── python/
│   │   │   │   └── behavioral_ml.py
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   ├── values/
│   │   │   │   └── drawable/
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   │       └── java/com/example/odmas/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
└── README.md
```

#### Package Structure

**Core Package (`com.example.odmas.core`)**:
```kotlin
package com.example.odmas.core

// Main system orchestrator
class SecurityManager

// Data modality definitions
enum class Modality

// Agent interfaces and implementations
interface Agent
abstract class BaseAgent : Agent
class Tier0StatsAgent : BaseAgent()
class Tier1AutoencoderAgent : BaseAgent()
class FusionAgent : BaseAgent()
class PolicyAgent : BaseAgent()
```

**Chaquopy Package (`com.example.odmas.chaquopy`)**:
```kotlin
package com.example.odmas.chaquopy

// Python ML integration
class ChaquopyBehavioralManager
data class BehavioralAnalysisResult
```

**Services Package (`com.example.odmas.services`)**:
```kotlin
package com.example.odmas.services

// Background services
class TouchAccessibilityService : AccessibilityService()
class ForegroundService : Service()
```

**UI Package (`com.example.odmas.ui`)**:
```kotlin
package com.example.odmas.ui

// Compose UI components
@Composable fun HomeScreen()
@Composable fun CalibrationScreen()
@Composable fun SettingsScreen()
@Composable fun RiskMeter()
@Composable fun BiometricPrompt()
```

### Core Implementation

#### SecurityManager Implementation

**Main Orchestrator**:
```kotlin
class SecurityManager @Inject constructor(
    private val tier0Agent: Tier0StatsAgent,
    private val tier1Agent: Tier1AutoencoderAgent,
    private val fusionAgent: FusionAgent,
    private val policyAgent: PolicyAgent,
    private val chaquopyManager: ChaquopyBehavioralManager,
    private val sensorCollector: TouchSensorCollector
) {
    private val _systemState = MutableStateFlow(SystemState())
    val systemState: StateFlow<SystemState> = _systemState.asStateFlow()
    
    private val agentScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    fun startMonitoring() {
        agentScope.launch {
            _systemState.value = _systemState.value.copy(isMonitoring = true)
            sensorCollector.startCollection()
        }
    }
    
    fun stopMonitoring() {
        agentScope.launch {
            _systemState.value = _systemState.value.copy(isMonitoring = false)
            sensorCollector.stopCollection()
        }
    }
    
    suspend fun processSensorData(data: SensorData) {
        val tier0Result = async(Dispatchers.Default) { tier0Agent.analyze(data) }
        val tier1Result = async(Dispatchers.Default) { tier1Agent.analyze(data) }
        val mlResult = async(Dispatchers.IO) { chaquopyManager.analyzeBehavior(data.toJson()) }
        
        val fusionResult = fusionAgent.calculateFinalRisk(
            tier0Result.await(),
            tier1Result.await(),
            mlResult.await()
        )
        
        val policyDecision = policyAgent.evaluateRisk(fusionResult.riskScore)
        updateSystemState(fusionResult, policyDecision)
    }
    
    private fun updateSystemState(riskScore: RiskScore, decision: PolicyDecision) {
        _systemState.value = _systemState.value.copy(
            currentRisk = riskScore.value,
            lastAnalysis = System.currentTimeMillis()
        )
        
        if (decision.requiresBiometric) {
            showBiometricPrompt(decision.riskLevel)
        }
    }
}
```

#### Tier-0 Agent Implementation

**Statistical Analysis Agent**:
```kotlin
class Tier0StatsAgent @Inject constructor(
    private val baselineStorage: BaselineStorage,
    private val mathUtils: MathUtils
) : BaseAgent() {
    
    private var baseline: Baseline? = null
    private val rollingStats = RollingStatistics()
    
    override suspend fun analyze(data: SensorData): AgentResult {
        val features = extractFeatures(data)
        val baseline = getBaseline()
        
        val mahalanobisDistance = calculateMahalanobisDistance(features, baseline)
        val chiSquareProb = mathUtils.chiSquareCDF(mahalanobisDistance, features.size)
        
        return StatisticalScore(
            probability = chiSquareProb,
            confidence = 0.8,
            processingTime = measureProcessingTime()
        )
    }
    
    private fun calculateMahalanobisDistance(features: DoubleArray, baseline: Baseline): Double {
        val mean = baseline.mean
        val covariance = baseline.covariance
        val diff = features - mean
        val invCov = mathUtils.inverseMatrix(covariance)
        return sqrt(diff.transpose() * invCov * diff)
    }
    
    private fun extractFeatures(data: SensorData): DoubleArray {
        return doubleArrayOf(
            data.pressure,
            data.velocity,
            data.curvature,
            data.dwellTime,
            data.flightTime,
            data.touchArea,
            data.touchDuration,
            data.pressureVariation,
            data.movementPattern,
            data.timingPattern
        )
    }
}
```

#### Tier-1 Agent Implementation

**Autoencoder Neural Network Agent**:
```kotlin
class Tier1AutoencoderAgent @Inject constructor(
    private val baselineStorage: BaselineStorage,
    private val neuralNetwork: Autoencoder
) : BaseAgent() {
    
    private var baseline: AutoencoderBaseline? = null
    
    override suspend fun analyze(data: SensorData): AgentResult {
        val features = normalizeFeatures(extractFeatures(data))
        val reconstruction = neuralNetwork.reconstruct(features)
        val error = calculateReconstructionError(features, reconstruction)
        val zScore = normalizeError(error, getBaseline())
        
        return AutoencoderScore(
            zScore = zScore,
            confidence = 0.9,
            processingTime = measureProcessingTime()
        )
    }
    
    private fun calculateReconstructionError(original: DoubleArray, reconstructed: DoubleArray): Double {
        return original.zip(reconstructed) { o, r -> (o - r) * (o - r) }.sum()
    }
    
    private fun normalizeError(error: Double, baseline: AutoencoderBaseline): Double {
        return (error - baseline.meanError) / baseline.stdError
    }
}

class Autoencoder {
    private val layers = listOf(
        DenseLayer(10, 8, activation = ReLU()),
        DenseLayer(8, 6, activation = ReLU()),
        DenseLayer(6, 8, activation = ReLU()),
        DenseLayer(8, 10, activation = Linear())
    )
    
    fun reconstruct(input: DoubleArray): DoubleArray {
        var current = input
        for (layer in layers) {
            current = layer.forward(current)
        }
        return current
    }
}
```

#### Chaquopy ML Agent Implementation

**Python ML Integration**:
```kotlin
class ChaquopyBehavioralManager @Inject constructor(
    private val pythonModule: PythonModule
) {
    
    suspend fun analyzeBehavior(featuresJson: String): BehavioralAnalysisResult {
        return withContext(Dispatchers.IO) {
            try {
                val result = pythonModule.callAttr("analyze_behavior", featuresJson)
                BehavioralAnalysisResult.fromJson(result.toString())
            } catch (e: Exception) {
                Log.e("ChaquopyML", "Analysis failed", e)
                BehavioralAnalysisResult.fallback()
            }
        }
    }
    
    suspend fun trainBaseline(featuresList: List<String>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val featuresJson = Json.encodeToString(featuresList)
                val result = pythonModule.callAttr("train_baseline", featuresJson)
                result.toString().toBoolean()
            } catch (e: Exception) {
                Log.e("ChaquopyML", "Training failed", e)
                false
            }
        }
    }
    
    suspend fun getModelStatus(): ModelStatus {
        return withContext(Dispatchers.IO) {
            try {
                val result = pythonModule.callAttr("get_model_status")
                ModelStatus.fromJson(result.toString())
            } catch (e: Exception) {
                Log.e("ChaquopyML", "Status check failed", e)
                ModelStatus.unknown()
            }
        }
    }
}
```

**Python ML Implementation**:
```python
# behavioral_ml.py
import json
import numpy as np
from typing import List, Dict, Any

class IsolationForestSimple:
    def __init__(self, n_estimators=50, contamination=0.15):
        self.n_estimators = n_estimators
        self.contamination = contamination
        self.trees = []
    
    def fit(self, X):
        for _ in range(self.n_estimators):
            tree = self._build_tree(X)
            self.trees.append(tree)
    
    def predict(self, X):
        scores = []
        for x in X:
            path_lengths = [self._path_length(x, tree) for tree in self.trees]
            avg_path = np.mean(path_lengths)
            score = 2 ** (-avg_path / self._c_factor(len(X)))
            scores.append(score)
        return np.array(scores)

class OneClassSVMSimple:
    def __init__(self, nu=0.15):
        self.nu = nu
        self.centroid = None
        self.radius = None
    
    def fit(self, X):
        self.centroid = np.mean(X, axis=0)
        distances = [np.linalg.norm(x - self.centroid) for x in X]
        self.radius = np.percentile(distances, (1 - self.nu) * 100)
    
    def predict(self, X):
        distances = [np.linalg.norm(x - self.centroid) for x in X]
        return [1 if d > self.radius else 0 for d in distances]

class BehavioralMLAnalyzer:
    def __init__(self):
        self.isolation_forest = IsolationForestSimple()
        self.one_class_svm = OneClassSVMSimple()
        self.is_trained = False
    
    def train_baseline(self, features_json: str) -> bool:
        try:
            features_list = json.loads(features_json)
            X = np.array(features_list)
            
            self.isolation_forest.fit(X)
            self.one_class_svm.fit(X)
            self.is_trained = True
            
            return True
        except Exception as e:
            print(f"Training failed: {e}")
            return False
    
    def analyze_behavior(self, features_json: str) -> Dict[str, Any]:
        if not self.is_trained:
            return {"risk_score": 0.0, "confidence": 0.0}
        
        try:
            features = json.loads(features_json)
            
            iso_score = self.isolation_forest.predict([features])[0]
            svm_score = self.one_class_svm.predict([features])[0]
            
            ensemble_score = 0.5 * iso_score + 0.5 * svm_score
            confidence = 0.9 if ensemble_score > 0.5 else 0.7
            
            return {
                "risk_score": ensemble_score * 100,
                "confidence": confidence,
                "algorithm_scores": {
                    "isolation_forest": float(iso_score),
                    "one_class_svm": float(svm_score)
                }
            }
        except Exception as e:
            print(f"Analysis failed: {e}")
            return {"risk_score": 0.0, "confidence": 0.0}
    
    def get_model_status(self) -> Dict[str, Any]:
        return {
            "is_trained": self.is_trained,
            "algorithms": ["isolation_forest", "one_class_svm"]
        }
```

#### Fusion Agent Implementation

**Risk Score Combination**:
```kotlin
class FusionAgent @Inject constructor() {
    
    fun calculateFinalRisk(
        tier0Score: StatisticalScore,
        tier1Score: AutoencoderScore,
        chaquopyScore: BehavioralAnalysisResult
    ): FinalRiskScore {
        // Traditional fusion (Tier-0 + Tier-1)
        val fusedRisk = 0.2 * tier0Score.probability + 0.8 * tier1Score.zScore
        
        // Confidence-based blending with Chaquopy ML
        return if (chaquopyScore.confidence > 0.8) {
            val mlRisk = chaquopyScore.riskScore / 100.0
            val blendedRisk = 0.5 * fusedRisk + 0.5 * mlRisk
            FinalRiskScore(
                value = blendedRisk * 100,
                confidence = 0.95,
                fusionMethod = FusionMethod.CONFIDENCE_BLENDED
            )
        } else {
            FinalRiskScore(
                value = fusedRisk * 100,
                confidence = 0.85,
                fusionMethod = FusionMethod.TRADITIONAL
            )
        }
    }
    
    fun calculateConfidence(scores: List<Double>): Double {
        val mean = scores.average()
        val variance = scores.map { (it - mean) * (it - mean) }.average()
        return 1.0 / (1.0 + variance * 10.0)
    }
}
```

#### Policy Agent Implementation

**Security Policy Management**:
```kotlin
class PolicyAgent @Inject constructor(
    private val biometricManager: BiometricManager
) {
    
    private var trustCredits: Int = 3
    private var consecutiveHighRisk: Int = 0
    private var consecutiveLowRisk: Int = 0
    private var lastBiometricTime: Long = 0L
    
    fun evaluateRisk(riskScore: Double): PolicyDecision {
        val decision = when {
            riskScore > 85 -> PolicyDecision.CRITICAL_RISK
            riskScore > 75 && consecutiveHighRisk >= 5 -> PolicyDecision.HIGH_RISK
            riskScore > 60 -> PolicyDecision.MEDIUM_RISK
            else -> PolicyDecision.LOW_RISK
        }
        
        updateConsecutiveCounters(decision)
        updateTrustCredits(decision)
        
        return decision
    }
    
    private fun updateConsecutiveCounters(decision: PolicyDecision) {
        when (decision) {
            PolicyDecision.CRITICAL_RISK, PolicyDecision.HIGH_RISK -> {
                consecutiveHighRisk++
                consecutiveLowRisk = 0
            }
            PolicyDecision.LOW_RISK -> {
                consecutiveLowRisk++
                consecutiveHighRisk = 0
            }
            else -> {
                consecutiveHighRisk = 0
                consecutiveLowRisk = 0
            }
        }
    }
    
    private fun updateTrustCredits(decision: PolicyDecision) {
        when (decision) {
            PolicyDecision.MEDIUM_RISK -> {
                trustCredits = max(0, trustCredits - 1)
            }
            PolicyDecision.LOW_RISK -> {
                if (consecutiveLowRisk >= 10) {
                    trustCredits = min(3, trustCredits + 1)
                }
            }
            else -> { /* No change */ }
        }
    }
    
    fun shouldShowBiometric(decision: PolicyDecision): Boolean {
        val timeSinceLastBiometric = System.currentTimeMillis() - lastBiometricTime
        val minimumInterval = 30000L // 30 seconds
        
        return when (decision) {
            PolicyDecision.CRITICAL_RISK -> true
            PolicyDecision.HIGH_RISK -> trustCredits > 0 && timeSinceLastBiometric > minimumInterval
            else -> false
        }
    }
    
    fun onBiometricSuccess() {
        trustCredits = 3
        consecutiveHighRisk = 0
        lastBiometricTime = System.currentTimeMillis()
    }
}
```

### Data Collection Implementation

#### Touch Sensor Collector

**Real-Time Data Collection**:
```kotlin
class TouchSensorCollector @Inject constructor(
    private val securityManager: SecurityManager
) {
    
    private val featureBuffer = CircularBuffer<TouchFeatures>(100)
    private val processingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var isCollecting = false
    
    fun startCollection() {
        isCollecting = true
        processingScope.launch {
            while (isCollecting) {
                processBufferedFeatures()
                delay(3000) // 3-second processing windows
            }
        }
    }
    
    fun stopCollection() {
        isCollecting = false
    }
    
    fun processTouchEvent(event: TouchEvent) {
        val features = extractFeatures(event)
        featureBuffer.add(features)
    }
    
    private suspend fun processBufferedFeatures() {
        if (featureBuffer.size >= 10) {
            val aggregatedFeatures = aggregateFeatures(featureBuffer)
            securityManager.processSensorData(aggregatedFeatures)
        }
    }
    
    private fun extractFeatures(event: TouchEvent): TouchFeatures {
        return TouchFeatures(
            pressure = event.pressure,
            velocity = calculateVelocity(event),
            curvature = calculateCurvature(event),
            dwellTime = event.dwellTime,
            flightTime = event.flightTime,
            touchArea = event.touchArea,
            touchDuration = event.touchDuration,
            pressureVariation = calculatePressureVariation(event),
            movementPattern = calculateMovementPattern(event),
            timingPattern = calculateTimingPattern(event)
        )
    }
    
    private fun aggregateFeatures(buffer: CircularBuffer<TouchFeatures>): SensorData {
        val features = buffer.toList()
        return SensorData(
            pressure = features.map { it.pressure }.average(),
            velocity = features.map { it.velocity }.average(),
            curvature = features.map { it.curvature }.average(),
            dwellTime = features.map { it.dwellTime }.average(),
            flightTime = features.map { it.flightTime }.average(),
            touchArea = features.map { it.touchArea }.average(),
            touchDuration = features.map { it.touchDuration }.average(),
            pressureVariation = features.map { it.pressureVariation }.average(),
            movementPattern = features.map { it.movementPattern }.average(),
            timingPattern = features.map { it.timingPattern }.average()
        )
    }
}
```

#### Accessibility Service

**Background Touch Monitoring**:
```kotlin
class TouchAccessibilityService : AccessibilityService() {
    
    @Inject
    lateinit var touchCollector: TouchSensorCollector
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                val touchData = extractTouchData(event)
                touchCollector.processTouchEvent(touchData)
            }
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> {
                val touchData = extractTouchData(event)
                touchCollector.processTouchEvent(touchData)
            }
        }
    }
    
    override fun onInterrupt() {
        // Service interrupted, stop collection
    }
    
    private fun extractTouchData(event: AccessibilityEvent): TouchEvent {
        val source = event.source
        return TouchEvent(
            pressure = source?.getPressure() ?: 0.5f,
            dwellTime = calculateDwellTime(event),
            flightTime = calculateFlightTime(event),
            touchArea = source?.getTouchArea() ?: 0.0f,
            touchDuration = event.eventTime - event.recordCount,
            timestamp = event.eventTime
        )
    }
    
    private fun calculateDwellTime(event: AccessibilityEvent): Long {
        // Calculate time between touch down and up
        return event.eventTime - event.recordCount
    }
    
    private fun calculateFlightTime(event: AccessibilityEvent): Long {
        // Calculate time between consecutive touches
        val lastTouchTime = getLastTouchTime()
        val flightTime = event.eventTime - lastTouchTime
        updateLastTouchTime(event.eventTime)
        return flightTime
    }
}
```

### UI Implementation

#### Compose UI Components

**Main Screen**:
```kotlin
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "OD-MAS Security",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        RiskMeter(
            riskLevel = uiState.currentRisk,
            isMonitoring = uiState.isMonitoring
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (uiState.isCalibrated) {
            if (uiState.isMonitoring) {
                Button(
                    onClick = { viewModel.stopMonitoring() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Stop Monitoring")
                }
            } else {
                Button(
                    onClick = { viewModel.startMonitoring() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Start Monitoring")
                }
            }
        } else {
            Button(
                onClick = { viewModel.startCalibration() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Start Calibration")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TrustCreditsDisplay(credits = uiState.trustCredits)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        StatusDisplay(status = uiState.status)
    }
}
```

**Risk Meter Component**:
```kotlin
@Composable
fun RiskMeter(
    riskLevel: Double,
    isMonitoring: Boolean,
    modifier: Modifier = Modifier
) {
    val color = when {
        riskLevel < 60 -> MaterialTheme.colorScheme.primary
        riskLevel < 75 -> MaterialTheme.colorScheme.tertiary
        riskLevel < 85 -> MaterialTheme.colorScheme.error
        else -> Color.Red
    }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Risk Level",
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape
                )
                .border(
                    width = 8.dp,
                    color = color,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${riskLevel.toInt()}%",
                style = MaterialTheme.typography.headlineLarge,
                color = color
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (isMonitoring) "Monitoring Active" else "Monitoring Inactive",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isMonitoring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

**Calibration Screen**:
```kotlin
@Composable
fun CalibrationScreen(
    viewModel: CalibrationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Calibration",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        when (uiState.phase) {
            CalibrationPhase.TOUCH -> TouchCalibrationPhase(
                progress = uiState.touchProgress,
                onMinimizeApp = { viewModel.minimizeApp() }
            )
            CalibrationPhase.TYPING -> TypingCalibrationPhase(
                progress = uiState.typingProgress,
                onTextChange = { viewModel.updateTypingProgress(it) }
            )
            CalibrationPhase.BASELINE -> BaselineCreationPhase(
                progress = uiState.baselineProgress
            )
            CalibrationPhase.COMPLETE -> CalibrationCompletePhase(
                onStartTest = { viewModel.startTest() }
            )
        }
    }
}
```

### Testing Implementation

#### Unit Tests

**Agent Testing**:
```kotlin
@RunWith(MockitoJUnitRunner::class)
class Tier0StatsAgentTest {
    
    @Mock
    private lateinit var baselineStorage: BaselineStorage
    
    @Mock
    private lateinit var mathUtils: MathUtils
    
    private lateinit var agent: Tier0StatsAgent
    
    @Before
    fun setup() {
        agent = Tier0StatsAgent(baselineStorage, mathUtils)
    }
    
    @Test
    fun `analyze should return valid statistical score`() = runTest {
        // Given
        val testData = SensorData(
            pressure = 0.5,
            velocity = 100.0,
            curvature = 0.1,
            dwellTime = 150.0,
            flightTime = 200.0,
            touchArea = 0.3,
            touchDuration = 300.0,
            pressureVariation = 0.05,
            movementPattern = 0.2,
            timingPattern = 0.15
        )
        
        val baseline = Baseline(
            mean = doubleArrayOf(0.5, 100.0, 0.1, 150.0, 200.0, 0.3, 300.0, 0.05, 0.2, 0.15),
            covariance = Array(10) { DoubleArray(10) { if (it == it) 1.0 else 0.0 } }
        )
        
        whenever(baselineStorage.getBaseline()).thenReturn(baseline)
        whenever(mathUtils.chiSquareCDF(any(), any())).thenReturn(0.1)
        
        // When
        val result = agent.analyze(testData)
        
        // Then
        assertThat(result).isInstanceOf(StatisticalScore::class.java)
        assertThat(result.probability).isEqualTo(0.1)
        assertThat(result.confidence).isEqualTo(0.8)
    }
}
```

**Fusion Testing**:
```kotlin
@RunWith(MockitoJUnitRunner::class)
class FusionAgentTest {
    
    private lateinit var agent: FusionAgent
    
    @Before
    fun setup() {
        agent = FusionAgent()
    }
    
    @Test
    fun `calculateFinalRisk should blend scores correctly with high confidence`() {
        // Given
        val tier0Score = StatisticalScore(0.3, 0.8, 10L)
        val tier1Score = AutoencoderScore(0.7, 0.9, 100L)
        val chaquopyScore = BehavioralAnalysisResult(80.0, 0.9, emptyMap())
        
        // When
        val result = agent.calculateFinalRisk(tier0Score, tier1Score, chaquopyScore)
        
        // Then
        val expectedFusedRisk = 0.2 * 0.3 + 0.8 * 0.7
        val expectedBlendedRisk = 0.5 * expectedFusedRisk + 0.5 * 0.8
        assertThat(result.value).isCloseTo(expectedBlendedRisk * 100, within(1.0))
        assertThat(result.confidence).isEqualTo(0.95)
        assertThat(result.fusionMethod).isEqualTo(FusionMethod.CONFIDENCE_BLENDED)
    }
}
```

#### Integration Tests

**End-to-End Testing**:
```kotlin
@RunWith(AndroidJUnit4::class)
class SecurityManagerIntegrationTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun testCompleteCalibrationAndMonitoringFlow() {
        // Given
        val viewModel = HomeViewModel()
        
        // When - Start calibration
        composeTestRule.setContent {
            HomeScreen(viewModel = viewModel)
        }
        
        composeTestRule.onNodeWithText("Start Calibration").performClick()
        
        // Then - Should show calibration screen
        composeTestRule.onNodeWithText("Calibration").assertIsDisplayed()
        
        // When - Complete touch calibration
        composeTestRule.onNodeWithText("Minimize App").performClick()
        // Simulate touch events...
        
        // Then - Should progress to typing phase
        composeTestRule.onNodeWithText("Typing Calibration").assertIsDisplayed()
        
        // When - Complete typing calibration
        // Type required text...
        
        // Then - Should show baseline creation
        composeTestRule.onNodeWithText("Creating Baseline").assertIsDisplayed()
        
        // When - Baseline complete
        // Wait for baseline creation...
        
        // Then - Should show test mode
        composeTestRule.onNodeWithText("Start Test").assertIsDisplayed()
        
        // When - Start monitoring
        composeTestRule.onNodeWithText("Start Test").performClick()
        
        // Then - Should show monitoring active
        composeTestRule.onNodeWithText("Monitoring Active").assertIsDisplayed()
    }
}
```

### Performance Optimization

#### Memory Management

**Object Pooling**:
```kotlin
class FeaturePool {
    private val pool = ObjectPool<TouchFeatures>(100) { TouchFeatures() }
    
    fun acquire(): TouchFeatures = pool.acquire()
    fun release(features: TouchFeatures) = pool.release(features)
}

class AnalysisResultPool {
    private val pool = ObjectPool<AnalysisResult>(50) { AnalysisResult() }
    
    fun acquire(): AnalysisResult = pool.acquire()
    fun release(result: AnalysisResult) = pool.release(result)
}
```

**Lazy Loading**:
```kotlin
class LazyAgentLoader {
    private var tier1Agent: Tier1AutoencoderAgent? = null
    private var chaquopyAgent: ChaquopyBehavioralManager? = null
    
    fun getTier1Agent(): Tier1AutoencoderAgent {
        return tier1Agent ?: Tier1AutoencoderAgent().also { tier1Agent = it }
    }
    
    fun getChaquopyAgent(): ChaquopyBehavioralManager {
        return chaquopyAgent ?: ChaquopyBehavioralManager().also { chaquopyAgent = it }
    }
}
```

#### Caching Strategy

**Analysis Cache**:
```kotlin
class AnalysisCache {
    private val cache = LruCache<String, AnalysisResult>(50)
    
    fun getCachedResult(features: String): AnalysisResult? {
        return cache.get(features.hashCode().toString())
    }
    
    fun cacheResult(features: String, result: AnalysisResult) {
        cache.put(features.hashCode().toString(), result)
    }
}
```

### Build and Deployment

#### Gradle Configuration

**App-level build.gradle.kts**:
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
    id("com.chaquo.python")
}

android {
    namespace = "com.example.odmas"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.example.odmas"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = "1.8"
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    
    // Compose
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-android-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    
    // Biometric
    implementation("androidx.biometric:biometric:1.1.0")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

chaquopy {
    defaultConfig {
        version = "3.8"
        pip {
            install("scikit-learn==1.3.0")
            install("numpy==1.24.0")
            install("pandas==2.0.0")
        }
    }
}
```

#### ProGuard Configuration

**proguard-rules.pro**:
```proguard
# Keep Compose classes
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }

# Keep Hilt classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager { *; }

# Keep Chaquopy classes
-keep class com.chaquo.python.** { *; }

# Keep model classes
-keep class com.example.odmas.core.** { *; }
-keep class com.example.odmas.chaquopy.** { *; }

# Keep Python files
-keep class com.chaquo.python.Python { *; }

# General Android rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
```

### Continuous Integration

#### GitHub Actions Workflow

**.github/workflows/android.yml**:
```yaml
name: Android CI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: gradle
    
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    
    - name: Run tests
      run: ./gradlew test
    
    - name: Build with Gradle
      run: ./gradlew build
    
    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/app-debug.apk
```

---

*This implementation and engineering guide provides comprehensive development instructions, code examples, and best practices for building the OD-MAS behavioral biometrics system.*
