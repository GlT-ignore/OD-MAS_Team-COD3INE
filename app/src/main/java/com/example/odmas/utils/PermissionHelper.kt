package com.example.odmas.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Helper class for handling runtime permissions required by the app
 */
object PermissionHelper {
    
    // Permission request codes
    const val REQUEST_CODE_ACTIVITY_RECOGNITION = 1001
    const val REQUEST_CODE_USAGE_STATS = 1002
    const val REQUEST_CODE_SYSTEM_ALERT_WINDOW = 1003
    const val REQUEST_CODE_ACCESSIBILITY_SERVICE = 1004
    
    // Required runtime permissions
    private val RUNTIME_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf(
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.BODY_SENSORS
        )
    } else {
        arrayOf(
            Manifest.permission.BODY_SENSORS
        )
    }
    
    /**
     * Check if all required permissions are granted
     */
    fun hasAllPermissions(context: Context): Boolean {
        return RUNTIME_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Request runtime permissions
     */
    fun requestPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            RUNTIME_PERMISSIONS,
            REQUEST_CODE_ACTIVITY_RECOGNITION
        )
    }
    
    /**
     * Check if usage stats permission is granted
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val packageManager = context.packageManager
                val applicationInfo = packageManager.getApplicationInfo(context.packageName, 0)
                val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
                val mode = appOpsManager.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    applicationInfo.uid,
                    context.packageName
                )
                mode == android.app.AppOpsManager.MODE_ALLOWED
            } catch (e: Exception) {
                false
            }
        } else {
            true
        }
    }
    
    /**
     * Request usage stats permission
     */
    fun requestUsageStatsPermission(activity: Activity) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.data = Uri.parse("package:${activity.packageName}")
        activity.startActivityForResult(intent, REQUEST_CODE_USAGE_STATS)
    }
    
    /**
     * Check if system alert window permission is granted
     */
    fun hasSystemAlertWindowPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }
    
    /**
     * Request system alert window permission
     */
    fun requestSystemAlertWindowPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = Uri.parse("package:${activity.packageName}")
            activity.startActivityForResult(intent, REQUEST_CODE_SYSTEM_ALERT_WINDOW)
        }
    }
    
    /**
     * Check if accessibility service is enabled
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val accessibilityEnabled = try {
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: Settings.SettingNotFoundException) {
            0
        }
        
        if (accessibilityEnabled == 1) {
            val services = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return services?.contains("${context.packageName}/com.example.odmas.core.services.TouchAccessibilityService") == true
        }
        
        return false
    }
    
    /**
     * Request accessibility service permission
     */
    fun requestAccessibilityServicePermission(activity: Activity) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        activity.startActivityForResult(intent, REQUEST_CODE_ACCESSIBILITY_SERVICE)
    }
    
    /**
     * Get list of missing permissions
     */
    fun getMissingPermissions(context: Context): List<String> {
        val missing = mutableListOf<String>()
        
        // Check runtime permissions
        RUNTIME_PERMISSIONS.forEach { permission ->
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                missing.add(permission)
            }
        }
        
        // Check special permissions
        if (!hasUsageStatsPermission(context)) {
            missing.add("Usage Stats Access")
        }
        
        if (!hasSystemAlertWindowPermission(context)) {
            missing.add("Display over other apps")
        }
        
        if (!isAccessibilityServiceEnabled(context)) {
            missing.add("Accessibility Service")
        }
        
        return missing
    }
    
    /**
     * Get user-friendly permission names
     */
    fun getPermissionName(permission: String): String {
        return when (permission) {
            Manifest.permission.ACTIVITY_RECOGNITION -> "Physical Activity Recognition"
            Manifest.permission.BODY_SENSORS -> "Body Sensors"
            "Usage Stats Access" -> "App Usage Statistics"
            "Display over other apps" -> "Display over other apps"
            "Accessibility Service" -> "Accessibility Service for Touch Monitoring"
            else -> permission
        }
    }
    
    /**
     * Get permission description
     */
    fun getPermissionDescription(permission: String): String {
        return when (permission) {
            Manifest.permission.ACTIVITY_RECOGNITION -> "Required to detect device movement patterns for security analysis"
            Manifest.permission.BODY_SENSORS -> "Required to access motion sensors for behavioral biometrics"
            "Usage Stats Access" -> "Required to monitor app usage patterns for security analysis"
            "Display over other apps" -> "Required for system-wide touch monitoring"
            "Accessibility Service" -> "Required to detect touch events across all apps for security monitoring"
            else -> "Required for app functionality"
        }
    }
}
