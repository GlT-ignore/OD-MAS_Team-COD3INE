package com.example.odmas.core.agents

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Policy Agent that manages escalation and de-escalation based on session risk
 * 
 * Policy:
 * - Escalate when sessionRisk > 75 for 5 consecutive windows OR > 85 once
 * - De-escalate when < 60 for 10 consecutive windows
 * - Trust credits: 3 total, decrement on 60-75 "yellow" windows, restore 1 every 30s under 60
 * - On escalate: show BottomSheet → BiometricPrompt
 */
class PolicyAgent {
    
    private var consecutiveHighRiskWindows: Int = 0
    private var consecutiveLowRiskWindows: Int = 0
    private var trustCredits: Int = MAX_TRUST_CREDITS
    private var lastTrustCreditRestoreTime: Long = 0L
    private var isEscalated: Boolean = false
    
    private val _policyState = MutableStateFlow(PolicyState())
    val policyState: StateFlow<PolicyState> = _policyState.asStateFlow()
    
    companion object {
        private const val HIGH_RISK_THRESHOLD = 75.0
        private const val CRITICAL_RISK_THRESHOLD = 85.0
        private const val LOW_RISK_THRESHOLD = 60.0
        private const val YELLOW_RISK_MIN = 60.0
        private const val YELLOW_RISK_MAX = 75.0
        
        private const val CONSECUTIVE_HIGH_RISK_LIMIT = 5
        private const val CONSECUTIVE_LOW_RISK_LIMIT = 10
        private const val MAX_TRUST_CREDITS = 3
        private const val TRUST_CREDIT_RESTORE_INTERVAL_MS = 30000L // 30 seconds
    }
    
    /**
     * Process session risk and determine if escalation is needed
     * @param sessionRisk Current session risk (0-100)
     * @return PolicyAction indicating what action to take
     */
    fun processSessionRisk(sessionRisk: Double): PolicyAction {
        val currentTime = System.currentTimeMillis()
        
        android.util.Log.d("PolicyAgent", "=== PROCESSING SESSION RISK ===")
        android.util.Log.d("PolicyAgent", "Input sessionRisk: $sessionRisk")
        android.util.Log.d("PolicyAgent", "Current state - consecutiveHigh: $consecutiveHighRiskWindows, consecutiveLow: $consecutiveLowRiskWindows, trustCredits: $trustCredits, isEscalated: $isEscalated")
        
        // Update trust credits
        updateTrustCredits(sessionRisk, currentTime)
        android.util.Log.d("PolicyAgent", "After trust update - trustCredits: $trustCredits")
        
        // Determine escalation status
        val shouldEscalate = determineEscalation(sessionRisk)
        android.util.Log.d("PolicyAgent", "Escalation decision: shouldEscalate=$shouldEscalate")
        
        // Update consecutive counters
        updateConsecutiveCounters(sessionRisk)
        android.util.Log.d("PolicyAgent", "After counter update - consecutiveHigh: $consecutiveHighRiskWindows, consecutiveLow: $consecutiveLowRiskWindows")
        
        // Update policy state
        val newState = PolicyState(
            sessionRisk = sessionRisk,
            isEscalated = shouldEscalate,
            trustCredits = trustCredits,
            consecutiveHighRisk = consecutiveHighRiskWindows,
            consecutiveLowRisk = consecutiveLowRiskWindows,
            riskLevel = getRiskLevel(sessionRisk)
        )
        
        _policyState.value = newState
        
        val action = when {
            shouldEscalate && !isEscalated -> {
                isEscalated = true
                android.util.Log.w("PolicyAgent", "🚨 ESCALATING! Risk=$sessionRisk, ConsecutiveHigh=$consecutiveHighRiskWindows")
                PolicyAction.Escalate
            }
            !shouldEscalate && isEscalated -> {
                isEscalated = false
                android.util.Log.i("PolicyAgent", "✅ DE-ESCALATING! Risk=$sessionRisk, ConsecutiveLow=$consecutiveLowRiskWindows")
                PolicyAction.DeEscalate
            }
            else -> {
                android.util.Log.d("PolicyAgent", "📊 MONITORING - Risk=$sessionRisk, Level=${getRiskLevel(sessionRisk)}")
                PolicyAction.Monitor
            }
        }
        
        android.util.Log.d("PolicyAgent", "Final action: $action")
        android.util.Log.d("PolicyAgent", "=== END POLICY PROCESSING ===")
        return action
    }
    
    /**
     * Handle successful biometric verification
     */
    fun onBiometricSuccess(): Unit {
        isEscalated = false
        consecutiveHighRiskWindows = 0
        consecutiveLowRiskWindows = 0
        trustCredits = MAX_TRUST_CREDITS
        lastTrustCreditRestoreTime = System.currentTimeMillis()
        
        _policyState.value = _policyState.value.copy(
            sessionRisk = 0.0,
            isEscalated = false,
            trustCredits = trustCredits,
            consecutiveHighRisk = 0,
            consecutiveLowRisk = 0,
            riskLevel = RiskLevel.LOW
        )
    }
    
    /**
     * Handle biometric failure or cancellation
     */
    fun onBiometricFailure(): Unit {
        // Keep escalated state, don't reset counters
        // This allows for retry attempts
    }
    
    /**
     * Reset policy state (for new session)
     */
    fun reset(): Unit {
        consecutiveHighRiskWindows = 0
        consecutiveLowRiskWindows = 0
        trustCredits = MAX_TRUST_CREDITS
        lastTrustCreditRestoreTime = System.currentTimeMillis()
        isEscalated = false
        
        _policyState.value = PolicyState()
    }
    
    private fun determineEscalation(sessionRisk: Double): Boolean {
        // Critical risk: escalate immediately
        if (sessionRisk > CRITICAL_RISK_THRESHOLD) {
            return true
        }
        
        // High risk: escalate after consecutive windows
        if (sessionRisk > HIGH_RISK_THRESHOLD) {
            return consecutiveHighRiskWindows >= CONSECUTIVE_HIGH_RISK_LIMIT
        }
        
        return false
    }
    
    private fun updateConsecutiveCounters(sessionRisk: Double): Unit {
        when {
            sessionRisk > HIGH_RISK_THRESHOLD -> {
                consecutiveHighRiskWindows++
                consecutiveLowRiskWindows = 0
            }
            sessionRisk < LOW_RISK_THRESHOLD -> {
                consecutiveLowRiskWindows++
                consecutiveHighRiskWindows = 0
            }
            else -> {
                // Yellow zone: reset low risk counter, keep high risk counter
                consecutiveLowRiskWindows = 0
            }
        }
    }
    
    private fun updateTrustCredits(sessionRisk: Double, currentTime: Long): Unit {
        // Decrement trust credits in yellow zone
        if (sessionRisk in YELLOW_RISK_MIN..YELLOW_RISK_MAX && trustCredits > 0) {
            trustCredits--
        }
        
        // Restore trust credits when risk is low
        if (sessionRisk < LOW_RISK_THRESHOLD && 
            currentTime - lastTrustCreditRestoreTime >= TRUST_CREDIT_RESTORE_INTERVAL_MS &&
            trustCredits < MAX_TRUST_CREDITS) {
            trustCredits++
            lastTrustCreditRestoreTime = currentTime
        }
    }
    
    private fun getRiskLevel(sessionRisk: Double): RiskLevel {
        return when {
            sessionRisk >= CRITICAL_RISK_THRESHOLD -> RiskLevel.CRITICAL
            sessionRisk >= HIGH_RISK_THRESHOLD -> RiskLevel.HIGH
            sessionRisk >= YELLOW_RISK_MIN -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }
    
    fun getCurrentState(): PolicyState = _policyState.value
}

/**
 * Policy state data class
 */
data class PolicyState(
    val sessionRisk: Double = 0.0,
    val isEscalated: Boolean = false,
    val trustCredits: Int = 3,
    val consecutiveHighRisk: Int = 0,
    val consecutiveLowRisk: Int = 0,
    val riskLevel: RiskLevel = RiskLevel.LOW
)

/**
 * Risk levels
 */
enum class RiskLevel {
    LOW,      // 0-59: Green
    MEDIUM,   // 60-74: Yellow  
    HIGH,     // 75-84: Orange
    CRITICAL  // 85-100: Red
}

/**
 * Policy actions
 */
enum class PolicyAction {
    Monitor,     // Continue monitoring
    Escalate,    // Show biometric prompt
    DeEscalate   // Hide biometric prompt
}
