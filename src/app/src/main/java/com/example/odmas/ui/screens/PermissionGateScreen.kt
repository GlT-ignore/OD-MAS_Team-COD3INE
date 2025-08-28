package com.example.odmas.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.odmas.utils.PermissionHelper

/**
 * Permission gate screen that blocks app startup until all permissions are granted
 */
@Composable
fun PermissionGateScreen(
    onAllPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    var missingPermissions by remember { mutableStateOf(PermissionHelper.getMissingPermissions(context)) }
    
    // Check permissions periodically when screen is resumed
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000) // Check every second
            val currentMissing = PermissionHelper.getMissingPermissions(context)
            if (currentMissing != missingPermissions) {
                missingPermissions = currentMissing
                Log.d("PermissionGateScreen", "Updated missing permissions: $currentMissing")
                
                if (currentMissing.isEmpty()) {
                    Log.d("PermissionGateScreen", "All permissions granted! Proceeding to main app...")
                    onAllPermissionsGranted()
                    break
                }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        // Header
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "OD-MAS Security Setup",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Grant permissions to enable behavioral biometrics",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Progress indicator
        if (missingPermissions.isNotEmpty()) {
            val totalPermissions = 5 // Runtime + special permissions
            val grantedPermissions = totalPermissions - missingPermissions.size
            val progress = grantedPermissions.toFloat() / totalPermissions
            
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
                    Text(
                        text = "Setup Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "$grantedPermissions of $totalPermissions permissions granted",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Missing permissions list
        if (missingPermissions.isNotEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(missingPermissions) { permission ->
                    PermissionCard(
                        permission = permission,
                        onGrantClick = {
                            handlePermissionRequest(context as Activity, permission)
                        }
                    )
                }
            }
        } else {
            // All permissions granted
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "All Permissions Granted!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Starting OD-MAS security system...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PermissionCard(
    permission: String,
    onGrantClick: () -> Unit
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = getPermissionIcon(permission),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(24.dp)
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = PermissionHelper.getPermissionName(permission),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = PermissionHelper.getPermissionDescription(permission),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onGrantClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Grant Permission")
            }
        }
    }
}

private fun getPermissionIcon(permission: String): ImageVector {
    return when (permission) {
        "Usage Stats Access" -> Icons.Default.Analytics
        "Display over other apps" -> Icons.Default.OpenInNew
        "Accessibility Service" -> Icons.Default.TouchApp
        "Physical Activity Recognition" -> Icons.Default.DirectionsRun
        "Body Sensors" -> Icons.Default.Sensors
        else -> Icons.Default.Security
    }
}

private fun handlePermissionRequest(activity: Activity, permission: String) {
    Log.d("PermissionGateScreen", "Requesting permission: $permission")
    
    try {
        when (permission) {
            "Usage Stats Access" -> {
                PermissionHelper.requestUsageStatsPermission(activity)
            }
            "Display over other apps" -> {
                PermissionHelper.requestSystemAlertWindowPermission(activity)
            }
            "Accessibility Service" -> {
                PermissionHelper.requestAccessibilityServicePermission(activity)
            }
            else -> {
                // Fallback: open app settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:${activity.packageName}")
                activity.startActivity(intent)
            }
        }
    } catch (e: Exception) {
        Log.e("PermissionGateScreen", "Error requesting permission $permission: ${e.message}")
        // Fallback: open app settings
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:${activity.packageName}")
        activity.startActivity(intent)
    }
}
