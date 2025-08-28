package com.example.odmas

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.odmas.ui.theme.ODMASTheme
import com.example.odmas.ui.screens.MainScreen
import com.example.odmas.ui.screens.PermissionGateScreen
import com.example.odmas.ui.screens.SensorMonitoringScreen
import com.example.odmas.ui.screens.SplashScreen
import com.example.odmas.utils.PermissionHelper
import com.example.odmas.viewmodels.SecurityViewModel
import com.example.odmas.viewmodels.SensorMonitoringViewModel

class MainActivity : FragmentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    // Permission request launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            Log.d(TAG, "All runtime permissions granted")
        } else {
            Log.w(TAG, "Some permissions denied: ${permissions.filter { !it.value }.keys}")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            ODMASTheme {
                var showSplash by remember { mutableStateOf(true) }
                var allPermissionsGranted by remember { mutableStateOf(false) }
                
                // Check initial permissions state
                LaunchedEffect(Unit) {
                    val missingPermissions = PermissionHelper.getMissingPermissions(this@MainActivity)
                    allPermissionsGranted = missingPermissions.isEmpty()
                    Log.d(TAG, "Initial permission check - missing: $missingPermissions")
                    
                    // Request runtime permissions first
                    if (!PermissionHelper.hasAllPermissions(this@MainActivity)) {
                        PermissionHelper.requestPermissions(this@MainActivity)
                    }
                }
                
                if (showSplash) {
                    SplashScreen(onSplashComplete = { showSplash = false })
                } else if (allPermissionsGranted) {
                    // Main app with all permissions granted
                    val navController = rememberNavController()
                    val sensorMonitoringViewModel: SensorMonitoringViewModel = viewModel()
                    
                    NavHost(navController = navController, startDestination = "main") {
                        composable("main") {
                            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                                // If launched from overlay asking to verify, trigger biometric
                                val needBiometric = intent?.getBooleanExtra("trigger_biometric", false) == true
                                val vm: SecurityViewModel = viewModel()
                                if (needBiometric) {
                                    LaunchedEffect(Unit) {
                                        com.example.odmas.utils.BiometricAuth.authenticate(
                                            context = this@MainActivity,
                                            title = "Verify it's you",
                                            subtitle = "Behavioral anomaly detected",
                                            confirmationRequired = false,
                                            onSuccess = { vm.onBiometricSuccess() },
                                            onFailure = { vm.onBiometricFailure() },
                                            onCancel = { vm.onBiometricCancelled() }
                                        )
                                    }
                                }
                                MainScreen(
                                    modifier = Modifier.padding(innerPadding),
                                    onNavigateToSensors = {
                                        navController.navigate("sensors")
                                    },
                                    sensorMonitoringViewModel = sensorMonitoringViewModel
                                )
                            }
                        }
                        composable("sensors") {
                            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                                SensorMonitoringScreen(
                                    modifier = Modifier.padding(innerPadding),
                                    viewModel = sensorMonitoringViewModel
                                )
                            }
                        }
                    }
                } else {
                    // Permission gate screen
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        PermissionGateScreen(
                            onAllPermissionsGranted = {
                                allPermissionsGranted = true
                                Log.d(TAG, "All permissions granted - switching to main app")
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.getBooleanExtra("trigger_biometric", false) == true) {
            val sm = com.example.odmas.core.SecurityManager.getInstance(applicationContext)
            com.example.odmas.utils.BiometricAuth.authenticate(
                context = this@MainActivity,
                title = "Verify it's you",
                subtitle = "Behavioral anomaly detected",
                confirmationRequired = false,
                onSuccess = { sm.onBiometricSuccess() },
                onFailure = { sm.onBiometricFailure() },
                onCancel = { sm.onBiometricFailure() }
            )
        }
    }

}