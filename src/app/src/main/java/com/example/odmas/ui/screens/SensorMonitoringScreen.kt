package com.example.odmas.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.odmas.viewmodels.SensorMonitoringViewModel
import com.example.odmas.viewmodels.SensorDataState

@Composable
fun SensorMonitoringScreen(
    modifier: Modifier = Modifier,
    viewModel: SensorMonitoringViewModel = viewModel()
) {
    val sensorState by viewModel.sensorState.collectAsState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Header
        TopBar()
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Sensor data list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // Touch Dynamics Section
                TouchDynamicsCard(sensorState.touchData)
            }
            
            item {
                // Motion Sensors Section
                MotionSensorsCard(sensorState.motionData)
            }
            
            item {
                // Typing Patterns Section
                TypingPatternsCard(sensorState.typingData)
            }
            
            item {
                // App Usage Section
                AppUsageCard(sensorState.appUsageData)
            }
            
            item {
                // Chaquopy Python ML Integration Status
                ChaquopyStatusCard(sensorState.chaquopyStatus)
            }
            
            item {
                // Raw Sensor Data
                RawSensorDataCard(sensorState.rawSensorData)
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
                text = "Sensor Monitoring",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Real-time behavioral data collection",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Icon(
            imageVector = Icons.Default.MonitorHeart,
            contentDescription = "Monitoring",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun TouchDynamicsCard(touchData: TouchData) {
    SensorDataCard(
        title = "Touch Dynamics",
        icon = Icons.Default.TouchApp,
        isActive = touchData.isActive
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DataRow("Dwell Time", "${touchData.dwellTime}ms", touchData.dwellTime > 0)
            DataRow("Flight Time", "${touchData.flightTime}ms", touchData.flightTime > 0)
            DataRow("Pressure", "${touchData.pressure}%", touchData.pressure > 0)
            DataRow("Size", "${touchData.size}px", touchData.size > 0)
            DataRow("Velocity", "${touchData.velocity}px/s", touchData.velocity > 0)
            DataRow("Curvature", "${touchData.curvature}", touchData.curvature > 0)
            DataRow("Touch Count", "${touchData.touchCount}", true)
        }
    }
}

@Composable
private fun MotionSensorsCard(motionData: MotionData) {
    SensorDataCard(
        title = "Motion Sensors",
        icon = Icons.Default.Sensors,
        isActive = motionData.isActive
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DataRow("Acceleration", "${motionData.acceleration}m/s²", motionData.acceleration > 0)
            DataRow("Angular Velocity", "${motionData.angularVelocity}rad/s", motionData.angularVelocity > 0)
            DataRow("Tremor Level", "${motionData.tremorLevel}", motionData.tremorLevel > 0)
            DataRow("Orientation", "${motionData.orientation}°", true)
            DataRow("Motion Count", "${motionData.motionCount}", true)
        }
    }
}

@Composable
private fun TypingPatternsCard(typingData: TypingData) {
    SensorDataCard(
        title = "Typing Patterns",
        icon = Icons.Default.Keyboard,
        isActive = typingData.isActive
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DataRow("Key Press Time", "${typingData.keyPressTime}ms", typingData.keyPressTime > 0)
            DataRow("Key Release Time", "${typingData.keyReleaseTime}ms", typingData.keyReleaseTime > 0)
            DataRow("Inter-key Delay", "${typingData.interKeyDelay}ms", typingData.interKeyDelay > 0)
            DataRow("Typing Speed", "${typingData.typingSpeed}wpm", typingData.typingSpeed > 0)
            DataRow("Error Rate", "${typingData.errorRate}%", true)
            DataRow("Key Count", "${typingData.keyCount}", true)
        }
    }
}

@Composable
private fun AppUsageCard(appUsageData: AppUsageData) {
    SensorDataCard(
        title = "App Usage Patterns",
        icon = Icons.Default.Apps,
        isActive = appUsageData.isActive
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DataRow("Current App", appUsageData.currentApp, appUsageData.currentApp.isNotEmpty())
            DataRow("Session Duration", "${appUsageData.sessionDuration}s", appUsageData.sessionDuration > 0)
            DataRow("App Switch Count", "${appUsageData.appSwitchCount}", true)
            DataRow("Usage Time", "${appUsageData.totalUsageTime}s", appUsageData.totalUsageTime > 0)
        }
    }
}

@Composable
private fun ChaquopyStatusCard(chaquopyStatus: ChaquopyStatus) {
    SensorDataCard(
        title = "Chaquopy Python ML",
        icon = Icons.Default.IntegrationInstructions,
        isActive = chaquopyStatus.isConnected
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DataRow("Connection", if (chaquopyStatus.isConnected) "Connected" else "Disconnected", chaquopyStatus.isConnected)
            DataRow("Model Status", chaquopyStatus.modelStatus, chaquopyStatus.isConnected)
            DataRow("Behavioral Score", "${chaquopyStatus.behavioralScore}%", chaquopyStatus.behavioralScore > 0)
            DataRow("Confidence", "${chaquopyStatus.confidence}%", chaquopyStatus.confidence > 0)
            DataRow("Last Analysis", chaquopyStatus.lastAnalysis, chaquopyStatus.lastAnalysis.isNotEmpty())
        }
    }
}

@Composable
private fun RawSensorDataCard(rawData: RawSensorData) {
    SensorDataCard(
        title = "Raw Sensor Data",
        icon = Icons.Default.DataUsage,
        isActive = rawData.isActive
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DataRow("Accelerometer X", "${rawData.accelX}", rawData.accelX != 0.0f)
            DataRow("Accelerometer Y", "${rawData.accelY}", rawData.accelY != 0.0f)
            DataRow("Accelerometer Z", "${rawData.accelZ}", rawData.accelZ != 0.0f)
            DataRow("Gyroscope X", "${rawData.gyroX}", rawData.gyroX != 0.0f)
            DataRow("Gyroscope Y", "${rawData.gyroY}", rawData.gyroY != 0.0f)
            DataRow("Gyroscope Z", "${rawData.gyroZ}", rawData.gyroZ != 0.0f)
            DataRow("Touch X", "${rawData.touchX}", rawData.touchX > 0)
            DataRow("Touch Y", "${rawData.touchY}", rawData.touchY > 0)
        }
    }
}

@Composable
private fun SensorDataCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            // Always use same background shade; only status dot shows active
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = if (isActive) MaterialTheme.colorScheme.primary else Color.Gray,
                            shape = RoundedCornerShape(6.dp)
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun DataRow(
    label: String,
    value: String,
    hasData: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (hasData) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

// Data classes for sensor information
data class TouchData(
    val isActive: Boolean = false,
    val dwellTime: Long = 0,
    val flightTime: Long = 0,
    val pressure: Float = 0f,
    val size: Float = 0f,
    val velocity: Float = 0f,
    val curvature: Float = 0f,
    val touchCount: Int = 0
)

data class MotionData(
    val isActive: Boolean = false,
    val acceleration: Float = 0f,
    val angularVelocity: Float = 0f,
    val tremorLevel: Float = 0f,
    val orientation: Float = 0f,
    val motionCount: Int = 0
)

data class TypingData(
    val isActive: Boolean = false,
    val keyPressTime: Long = 0,
    val keyReleaseTime: Long = 0,
    val interKeyDelay: Long = 0,
    val typingSpeed: Float = 0f,
    val errorRate: Float = 0f,
    val keyCount: Int = 0
)

data class AppUsageData(
    val isActive: Boolean = false,
    val currentApp: String = "",
    val sessionDuration: Long = 0,
    val appSwitchCount: Int = 0,
    val totalUsageTime: Long = 0
)

data class ChaquopyStatus(
    val isConnected: Boolean = false,
    val modelStatus: String = "Not Loaded",
    val behavioralScore: Float = 0f,
    val confidence: Float = 0f,
    val lastAnalysis: String = ""
)

data class RawSensorData(
    val isActive: Boolean = false,
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val touchX: Float = 0f,
    val touchY: Float = 0f
)
