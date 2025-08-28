package com.example.odmas.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.odmas.core.agents.RiskLevel
import com.example.odmas.ui.components.BiometricPromptSheet
import com.example.odmas.ui.components.RiskDial
import com.example.odmas.ui.components.StatusChip
import com.example.odmas.viewmodels.SecurityViewModel
import com.example.odmas.viewmodels.SecurityUIState
import com.example.odmas.viewmodels.SensorMonitoringViewModel
import com.example.odmas.utils.PermissionHelper
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.TextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: SecurityViewModel = viewModel(),
    onNavigateToSensors: () -> Unit = {},
    sensorMonitoringViewModel: SensorMonitoringViewModel? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val biometricState by viewModel.biometricPromptState.collectAsState()
    var showCalibrationPromptSheet by remember { mutableStateOf(false) }
    
    // Calibration flow state
    var calibrationFlowActive by remember { mutableStateOf(false) }
    var testModeActive by remember { mutableStateOf(false) }
    var cooldownTimeLeft by remember { mutableStateOf(0) }
    
    // Cooldown timer effect
    LaunchedEffect(cooldownTimeLeft) {
        if (cooldownTimeLeft > 0) {
            while (cooldownTimeLeft > 0) {
                kotlinx.coroutines.delay(1000)
                cooldownTimeLeft--
            }
        }
    }
    
    // Make screen scrollable to ensure all controls are reachable on small screens
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val scrollState = rememberScrollState()
        Column(modifier = Modifier.verticalScroll(scrollState)) {
        // Top bar
        TopBar()
        
        // Heads-up permission banner (visible until all granted)
        PermissionBanner()

        Spacer(modifier = Modifier.height(32.dp))
        
        // Risk dial with explanation
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                RiskDial(
                    risk = uiState.securityState.sessionRisk,
                    riskLevel = uiState.securityState.riskLevel,
                    modifier = Modifier.size(200.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Risk explanation
            Text(
                text = "Behavioral Risk Score (0-100)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Text(
                text = when {
                    uiState.securityState.sessionRisk < 30 -> "Normal behavior pattern"
                    uiState.securityState.sessionRisk < 75 -> "Slight deviation detected"
                    else -> "Unusual pattern - verify identity"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            // Spacer left intentionally; controls appear in a row below
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Status chip + Reset baseline side-by-side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusChip(
                riskLevel = uiState.securityState.riskLevel,
                isEscalated = uiState.securityState.isEscalated
            )
            OutlinedButton(onClick = { viewModel.resetSecurity() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset Baseline")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // POC Demo Controls
        DemoCalibrationCard(
            isLearning = uiState.securityState.isLearning,
            calibrationFlowActive = calibrationFlowActive,
            testModeActive = testModeActive,
            cooldownTimeLeft = cooldownTimeLeft,
            stage = uiState.securityState.calibrationStage,
            touchProgress = uiState.securityState.touchCount to uiState.securityState.touchTarget,
            typingProgress = uiState.securityState.typingCount to uiState.securityState.typingTarget,
            onStartCalibration = { 
                calibrationFlowActive = true
                viewModel.startCalibrationFlow()
                showCalibrationPromptSheet = true 
            },
            onStartTest = {
                testModeActive = true
                viewModel.startTestMode()
            },
            onStopTest = {
                testModeActive = false
                viewModel.stopTestMode()
            },
            onResetCalibration = {
                calibrationFlowActive = false
                testModeActive = false
                cooldownTimeLeft = 0
                viewModel.resetSecurity()
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Trust credits
        TrustCreditsCard(
            trustCredits = uiState.securityState.trustCredits,
            consecutiveHighRisk = uiState.securityState.consecutiveHighRisk,
            consecutiveLowRisk = uiState.securityState.consecutiveLowRisk
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Agent status
        AgentStatusCard(
            tier0Ready = uiState.securityState.tier0Ready,
            tier1Ready = uiState.securityState.tier1Ready,
            fusionReady = uiState.isInitialized && uiState.securityState.tier0Ready,
            policyReady = uiState.isInitialized
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Debug info
        DebugInfoCard(
            isInitialized = uiState.isInitialized,
            riskLevel = uiState.securityState.riskLevel,
            sessionRisk = uiState.securityState.sessionRisk,
            trustCredits = uiState.securityState.trustCredits
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Demo controls
        if (uiState.isDemoMode) {
            DemoControls(
                onReset = { viewModel.resetSecurity() },
                onToggleDemo = { viewModel.toggleDemoMode() }
            )
        } else {
            DemoModeToggle(
                onToggle = { viewModel.toggleDemoMode() }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Navigation to sensor monitoring
        NavigationButton(
            onNavigateToSensors = onNavigateToSensors
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Permissions button
        PermissionsButton()
        }
    }
    
    // Biometric prompt
    biometricState?.let { state ->
        BiometricPromptSheet(
            isVisible = state.isVisible,
            reason = state.reason,
            onSuccess = { viewModel.onBiometricSuccess() },
            onFailure = { viewModel.onBiometricFailure() },
            onCancel = { viewModel.onBiometricCancelled() }
        )
    }

    // Guided calibration sheet
    val ctx = LocalContext.current
    CalibrationGuidedSheet(
        isVisible = showCalibrationPromptSheet && calibrationFlowActive,
        stage = uiState.securityState.calibrationStage,
        motion = 0 to 0,
        touch = uiState.securityState.touchCount to uiState.securityState.touchTarget,
        typing = uiState.securityState.typingCount to uiState.securityState.typingTarget,
        onDismiss = { showCalibrationPromptSheet = false },
        onOpenA11y = { PermissionHelper.requestAccessibilityServicePermission(ctx as android.app.Activity) },
        onOpenUsageStats = { PermissionHelper.requestUsageStatsPermission(ctx as android.app.Activity) },
        onOpenTextField = { /* No longer redirects to sensors */ },
        onCalibrationComplete = { 
            calibrationFlowActive = false
            showCalibrationPromptSheet = false
            cooldownTimeLeft = 30
            viewModel.completeCalibrationFlow()
        }
    )
}
@Composable
private fun DemoCalibrationCard(
    isLearning: Boolean,
    calibrationFlowActive: Boolean,
    testModeActive: Boolean,
    cooldownTimeLeft: Int,
    stage: String,
    touchProgress: Pair<Int, Int>,
    typingProgress: Pair<Int, Int>,
    onStartCalibration: () -> Unit,
    onStartTest: () -> Unit,
    onStopTest: () -> Unit,
    onResetCalibration: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            // Match Trust Credits background
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🎯 POC Demo - On-Device Behavioral Security",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            when {
                calibrationFlowActive -> {
                    Text("📝 Calibration in Progress", fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Touch", style = MaterialTheme.typography.labelSmall)
                            Text("${touchProgress.first}/${touchProgress.second}")
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Typing", style = MaterialTheme.typography.labelSmall)
                            Text("${typingProgress.first}/${typingProgress.second}")
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Stage", style = MaterialTheme.typography.labelSmall)
                            Text(stage)
                        }
                    }
                }
                cooldownTimeLeft > 0 -> {
                    Text("⏳ Baseline Creation Complete", fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Cooldown: ${cooldownTimeLeft}s")
                    LinearProgressIndicator(
                        progress = (30 - cooldownTimeLeft) / 30f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                testModeActive -> {
                    Text("🔍 Test Mode - Monitoring Risk", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Interact normally to test behavioral detection", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            onStopTest()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("End Test")
                    }
                }
                else -> {
                    Text("Ready for Demo", fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Follow the calibration flow to establish your behavioral baseline", style = MaterialTheme.typography.bodySmall)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Demo buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onStartCalibration,
                    enabled = !calibrationFlowActive && cooldownTimeLeft == 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Calibrate")
                }
                OutlinedButton(
                    onClick = onStartTest,
                    enabled = !calibrationFlowActive && cooldownTimeLeft == 0 && !isLearning,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onResetCalibration,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset All")
            }
        }
    }
}

// Guided bottom sheet mirroring the modified app’s staged prompts
@Composable
private fun CalibrationGuidedSheet(
    isVisible: Boolean,
    stage: String,
    motion: Pair<Int, Int>,
    touch: Pair<Int, Int>,
    typing: Pair<Int, Int>,
    onDismiss: () -> Unit,
    onOpenA11y: () -> Unit,
    onOpenUsageStats: () -> Unit,
    onOpenTextField: () -> Unit,
    onCalibrationComplete: () -> Unit
) {
    if (!isVisible) return
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth(0.94f)
        ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Calibration Guide", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            when (stage) {
                "TOUCH" -> {
                    Text("Touch calibration must be done outside the app to capture natural behavior.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Progress: ${touch.first}/${touch.second}")
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = "Touch",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "1. Minimize this app",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "2. Swipe, scroll, and tap normally (varied movements)",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "3. Return to this app when done",
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    // Minimize the app to background
                                    val activity = context as? android.app.Activity
                                    activity?.moveTaskToBack(true)
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Minimize App")
                            }
                        }
                    }
                }
                "TYPING" -> {
                    var typingText by remember { mutableStateOf("") }
                    val focusRequester = remember { FocusRequester() }
                    val keyboardController = LocalSoftwareKeyboardController.current
                    
                    // Sample sentences for comprehensive typing calibration
                    val calibrationSentences = listOf(
                        "The quick brown fox jumps over the lazy dog. This sentence contains every letter and common patterns.",
                        "Password123! contains uppercase, lowercase, numbers, and special characters for complete pattern analysis.",
                        "Rhythm and timing in typing reveal unique behavioral patterns including dwell time and flight time measurements.",
                        "Mobile security systems analyze touch pressure, finger size, movement velocity, and acceleration patterns.",
                        "Authentication happens continuously and seamlessly in the background without interrupting user experience."
                    )
                    val currentSentence by remember { mutableStateOf(calibrationSentences.random()) }
                    
                    Text("Type the sentence below to calibrate your typing pattern:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Progress: ${typing.first}/${typing.second}")
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Sample sentence to type
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Type this sentence:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                currentSentence,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Text field for typing calibration
                    OutlinedTextField(
                        value = typingText,
                        onValueChange = { newText -> 
                            // Track character additions for calibration
                            if (newText.length > typingText.length) {
                                val addedChar = newText[typingText.length]
                                // Send to SecurityManager for character counting
                                val securityManager = com.example.odmas.core.SecurityManager.getInstance(context)
                                securityManager.onCharacterTyped(addedChar)
                            }
                            typingText = newText 
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        label = { Text("Type here for calibration") },
                        placeholder = { Text("Type the sentence above...") },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Default
                        ),
                        maxLines = 3,
                        minLines = 2
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            }
                        ) {
                            Text("Show Keyboard")
                        }
                        OutlinedButton(
                            onClick = {
                                typingText = ""
                            }
                        ) {
                            Text("Clear Text")
                        }
                    }
                }
                else -> {
                    // Default to touch calibration instructions when motion is disabled
                    Text("Touch calibration must be done outside the app to capture natural behavior.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Progress: ${touch.first}/${touch.second}")
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = "Touch",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "1. Minimize this app",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "2. Swipe, scroll, and tap normally (varied movements)",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "3. Return to this app when done",
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    // Minimize the app to background
                                    val activity = context as? android.app.Activity
                                    activity?.moveTaskToBack(true)
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Minimize App")
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Note: Accessibility Service must be enabled to capture typing patterns system-wide.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenA11y) { Text("Enable Accessibility") }
                OutlinedButton(onClick = onOpenUsageStats) { Text("Enable Usage Stats") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            // Check if calibration is complete
            val isComplete = (touch.first >= touch.second) && (typing.first >= typing.second)
            
            if (isComplete) {
                Button(
                    onClick = onCalibrationComplete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Complete Calibration")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        }
        }
        }
    }
}


@Composable
private fun TopBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "OD-MAS",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "On-Device Multi-Agent Security",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Privacy",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "On-device · No Cloud",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TrustCreditsCard(
    trustCredits: Int,
    consecutiveHighRisk: Int,
    consecutiveLowRisk: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Trust Credits",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$trustCredits/3",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (trustCredits > 0) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.error
                    )
                }
                
                Column {
                    Text(
                        text = "High Risk",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = consecutiveHighRisk.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (consecutiveHighRisk > 0) MaterialTheme.colorScheme.error 
                               else MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Column {
                    Text(
                        text = "Low Risk",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = consecutiveLowRisk.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (consecutiveLowRisk > 0) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionBanner() {
    val context = LocalContext.current
    val missing = remember { PermissionHelper.getMissingPermissions(context) }
    if (missing.isEmpty()) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Permissions needed",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = missing.joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    try { PermissionHelper.requestAccessibilityServicePermission(context as android.app.Activity) } catch (_: Exception) {}
                }) { Text("Enable Accessibility") }
                OutlinedButton(onClick = {
                    try { PermissionHelper.requestUsageStatsPermission(context as android.app.Activity) } catch (_: Exception) {}
                }) { Text("Enable Usage Stats") }
            }
        }
    }
}

@Composable
private fun AgentStatusCard(
    tier0Ready: Boolean,
    tier1Ready: Boolean,
    fusionReady: Boolean,
    policyReady: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Agent Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // All agents in single row to save screen space
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AgentStatusItem(
                    name = "T0",
                    description = "Stats",
                    isReady = tier0Ready,
                    isCompact = true
                )
                
                AgentStatusItem(
                    name = "T1", 
                    description = "Neural",
                    isReady = tier1Ready,
                    isCompact = true
                )
                
                AgentStatusItem(
                    name = "Fusion",
                    description = "Combine",
                    isReady = fusionReady,
                    isCompact = true
                )
                
                AgentStatusItem(
                    name = "Policy", 
                    description = "Control",
                    isReady = policyReady,
                    isCompact = true
                )
            }
        }
    }
}

@Composable
private fun AgentStatusItem(
    name: String,
    description: String,
    isReady: Boolean,
    isCompact: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(if (isCompact) 28.dp else 40.dp)
                .clip(CircleShape)
                .background(
                    if (isReady) MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.outline
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isReady) Icons.Default.Check else Icons.Default.Info,
                contentDescription = if (isReady) "Ready" else "Initializing",
                tint = if (isReady) MaterialTheme.colorScheme.onPrimary 
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(if (isCompact) 14.dp else 20.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(if (isCompact) 2.dp else 4.dp))
        
        Text(
            text = name,
            style = if (isCompact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun DemoModeToggle(
    onToggle: () -> Unit
) {
    Button(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Enable Demo Mode")
    }
}

@Composable
private fun DemoControls(
    onReset: () -> Unit,
    onToggleDemo: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Removed Reset Baseline here to avoid duplicate control.
        OutlinedButton(
            onClick = onToggleDemo,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Pause,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Disable Demo Mode")
        }
    }
}

@Composable
private fun DebugInfoCard(
    isInitialized: Boolean,
    riskLevel: RiskLevel,
    sessionRisk: Double,
    trustCredits: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🔧 Debug Info",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Initialized",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = if (isInitialized) "✅ Yes" else "❌ No",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                
                Column {
                    Text(
                        text = "Risk Level",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = riskLevel.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                
                Column {
                    Text(
                        text = "Session Risk",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "%.1f".format(sessionRisk),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                
                Column {
                    Text(
                        text = "Credits",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "$trustCredits/3",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationButton(
    onNavigateToSensors: () -> Unit
) {
    OutlinedButton(
        onClick = onNavigateToSensors,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            // Match Trust Credits background tone for button container
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Icon(
            imageVector = Icons.Default.MonitorHeart,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Open Sensor Monitoring")
    }
}

@Composable
private fun PermissionsButton() {
    val context = LocalContext.current
    val missingPermissions = PermissionHelper.getMissingPermissions(context)
    
    Log.d("PermissionsButton", "Missing permissions: $missingPermissions")
    
    if (missingPermissions.isNotEmpty()) {
        OutlinedButton(
            onClick = {
                Log.d("PermissionsButton", "Button clicked, missing: $missingPermissions")
                try {
                    if (missingPermissions.contains("Usage Stats Access")) {
                        Log.d("PermissionsButton", "Opening Usage Stats settings...")
                        PermissionHelper.requestUsageStatsPermission(context as android.app.Activity)
                    } else if (missingPermissions.contains("Display over other apps")) {
                        Log.d("PermissionsButton", "Opening System Alert Window settings...")
                        PermissionHelper.requestSystemAlertWindowPermission(context as android.app.Activity)
                    } else if (missingPermissions.contains("Accessibility Service")) {
                        Log.d("PermissionsButton", "Opening Accessibility settings...")
                        PermissionHelper.requestAccessibilityServicePermission(context as android.app.Activity)
                    }
                } catch (e: Exception) {
                    Log.e("PermissionsButton", "Error opening settings: ${e.message}")
                    // Fallback: open general app settings
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = android.net.Uri.parse("package:${context.packageName}")
                    context.startActivity(intent)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Grant Special Permissions (${missingPermissions.size})")
        }
    }
}
