# OD-MAS Installation and Deployment Guide
## Complete Setup and Deployment Instructions

### Prerequisites

#### System Requirements

**Development Environment**:
- **Operating System**: Windows 10+, macOS 10.15+, or Ubuntu 18.04+
- **RAM**: 8GB minimum, 16GB recommended
- **Storage**: 10GB free space
- **Processor**: Multi-core processor (Intel i5/AMD Ryzen 5 or better)

**Target Device Requirements**:
- **Android Version**: 7.0+ (API Level 24+)
- **RAM**: 4GB minimum, 6GB recommended
- **Storage**: 100MB free space
- **Sensors**: Touch screen with pressure sensitivity
- **Biometrics**: Fingerprint sensor or face recognition

#### Required Software

**Development Tools**:
- **Android Studio**: Arctic Fox or later (2023.1+)
- **Java Development Kit (JDK)**: Version 11 or 17
- **Android SDK**: API Level 24+
- **Git**: Version 2.40.0+
- **Python**: Version 3.8+ (for Chaquopy integration)

**Dependencies**:
- **Gradle**: Version 8.0+
- **Kotlin**: Version 1.9.0+
- **Chaquopy**: Version 15.0.1

### Development Environment Setup

#### Step 1: Install Android Studio

**Download and Install**:
1. Visit [Android Developer Portal](https://developer.android.com/studio)
2. Download Android Studio for your operating system
3. Run the installer and follow the setup wizard
4. Install Android SDK during setup

**Configure Android SDK**:
```bash
# Open Android Studio
# Go to Tools > SDK Manager
# Install the following:
# - Android SDK Platform 34
# - Android SDK Build-Tools 34.0.0
# - Android SDK Platform-Tools
# - Android Emulator
# - Android SDK Tools
```

#### Step 2: Install JDK

**Download JDK 17**:
```bash
# For Windows: Download from Oracle or use OpenJDK
# For macOS: brew install openjdk@17
# For Ubuntu: sudo apt install openjdk-17-jdk

# Set JAVA_HOME environment variable
export JAVA_HOME=/path/to/jdk-17
export PATH=$JAVA_HOME/bin:$PATH
```

#### Step 3: Clone Repository

**Clone OD-MAS Repository**:
```bash
git clone https://github.com/GlT-ignore/OD-MAS_Team-COD3INE.git
cd OD-MAS_Team-COD3INE
```

#### Step 4: Configure Gradle

**Update gradle.properties**:
```properties
# Gradle configuration
org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=1024m -XX:+HeapDumpOnOutOfMemoryError
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.configureondemand=true

# Android configuration
android.useAndroidX=true
android.enableJetifier=true
android.nonTransitiveRClass=true

# Kotlin configuration
kotlin.code.style=official
kotlin.incremental=true
kotlin.incremental.useClasspathSnapshot=true
```

#### Step 5: Configure Chaquopy

**Add Chaquopy Plugin**:
```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
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

### Building the Application

#### Step 1: Sync Project

**Sync Gradle Files**:
```bash
# In Android Studio: File > Sync Project with Gradle Files
# Or via command line:
./gradlew clean
./gradlew build
```

#### Step 2: Build Debug APK

**Generate Debug APK**:
```bash
./gradlew assembleDebug
```

**APK Location**:
```
app/build/outputs/apk/debug/app-debug.apk
```

#### Step 3: Build Release APK

**Generate Release APK**:
```bash
./gradlew assembleRelease
```

**APK Location**:
```
app/build/outputs/apk/release/app-release.apk
```

### Installation on Device

#### Step 1: Enable Developer Options

**On Android Device**:
1. Go to **Settings** > **About phone**
2. Tap **Build number** 7 times
3. Go back to **Settings** > **Developer options**
4. Enable **USB debugging**

#### Step 2: Install APK

**Via ADB (Command Line)**:
```bash
# Connect device via USB
adb devices

# Install APK
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Via Android Studio**:
1. Connect device via USB
2. Enable USB debugging
3. Click **Run** button in Android Studio
4. Select your device and click **OK**

**Via File Manager**:
1. Copy APK to device
2. Open file manager on device
3. Navigate to APK file
4. Tap to install (enable "Install from unknown sources" if prompted)

### Permission Setup

#### Required Permissions

**AndroidManifest.xml Permissions**:
```xml
<uses-permission android:name="android.permission.USE_BIOMETRIC" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" />
```

#### Runtime Permission Setup

**Grant Permissions**:
1. **Biometric Authentication**: Set up fingerprint/face recognition in device settings
2. **Accessibility Service**: Go to Settings > Accessibility > OD-MAS > Enable
3. **Usage Access**: Go to Settings > Apps > Special app access > Usage access > OD-MAS > Allow
4. **Notification Access**: Allow notifications when prompted
5. **Display Over Apps**: Allow when prompted for biometric prompts

### Configuration

#### App Configuration

**Initial Setup**:
1. Launch OD-MAS app
2. Grant required permissions when prompted
3. Complete initial calibration process
4. Configure security settings

**Calibration Process**:
1. **Touch Calibration**: Interact normally with device for 30+ touch samples
2. **Typing Calibration**: Type 100 characters using provided sentences
3. **Baseline Creation**: Wait for automatic model training
4. **Test Mode**: Start behavioral monitoring

#### Security Configuration

**Risk Thresholds**:
- **Critical Risk**: 85-100% (immediate biometric prompt)
- **High Risk**: 75-84% (biometric prompt after 5 consecutive windows)
- **Medium Risk**: 60-74% (trust credit depletion)
- **Low Risk**: 0-59% (normal operation)

**Trust Credits**:
- **Total Credits**: 3
- **Depletion**: At 60-75% risk levels
- **Restoration**: 1 credit every 30 seconds when risk < 60%

### Deployment Options

#### Development Deployment

**Debug Build**:
```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Debug Features**:
- Detailed logging enabled
- Performance monitoring
- Debug UI elements
- Development tools

#### Production Deployment

**Release Build**:
```bash
# Build release APK
./gradlew assembleRelease

# Sign APK (if needed)
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore my-release-key.keystore app-release-unsigned.apk alias_name
```

**Production Features**:
- Code obfuscation enabled
- Performance optimized
- Release logging
- Production configuration

#### Enterprise Deployment

**Samsung Knox Integration**:
1. **Knox Platform**: Integrate with Samsung Knox SDK
2. **Enterprise Policy**: Configure enterprise security policies
3. **Device Management**: Deploy via Samsung Knox Admin
4. **Compliance**: Ensure enterprise security compliance

**MDM Integration**:
1. **Mobile Device Management**: Deploy via MDM solution
2. **Policy Enforcement**: Configure security policies
3. **Remote Management**: Enable remote configuration
4. **Monitoring**: Integrate with enterprise monitoring

### Testing Deployment

#### Unit Testing

**Run Unit Tests**:
```bash
./gradlew test
```

**Test Coverage**:
```bash
./gradlew jacocoTestReport
```

#### Integration Testing

**Run Integration Tests**:
```bash
./gradlew connectedAndroidTest
```

**Test on Multiple Devices**:
```bash
# Test on specific device
adb -s <device-id> shell am instrument -w com.example.odmas.test/androidx.test.runner.AndroidJUnitRunner

# Test on all connected devices
./gradlew connectedAndroidTest
```

#### Performance Testing

**Benchmark Testing**:
```bash
./gradlew benchmark
```

**Performance Monitoring**:
1. **CPU Usage**: Monitor processing overhead
2. **Memory Usage**: Track memory consumption
3. **Battery Impact**: Measure battery usage
4. **Response Time**: Test real-time performance

### Troubleshooting

#### Common Issues

**Build Issues**:
```bash
# Clean and rebuild
./gradlew clean
./gradlew build

# Invalidate caches in Android Studio
File > Invalidate Caches and Restart
```

**Permission Issues**:
1. **Accessibility Service**: Ensure service is enabled in device settings
2. **Usage Access**: Grant usage access permission
3. **Biometric**: Set up biometric authentication in device settings
4. **Notifications**: Allow notification permissions

**Performance Issues**:
1. **Memory Usage**: Monitor memory consumption in Android Studio
2. **Battery Drain**: Check background processing settings
3. **Slow Response**: Verify device meets minimum requirements
4. **Crashes**: Check logcat for error messages

#### Debug Information

**Enable Debug Logging**:
```kotlin
// In SecurityManager.kt
private val logger = Logger.getLogger("OD-MAS")
logger.level = Level.ALL
```

**Collect Logs**:
```bash
# Collect device logs
adb logcat -s OD-MAS

# Save logs to file
adb logcat -s OD-MAS > odmas_logs.txt
```

**Performance Profiling**:
1. **Android Studio Profiler**: Use built-in profiling tools
2. **Systrace**: System-level performance analysis
3. **Memory Profiler**: Track memory allocation
4. **CPU Profiler**: Monitor processing time

### Security Considerations

#### Privacy Protection

**Data Handling**:
- **Local Storage**: All data stored locally on device
- **Encryption**: AES-256 encryption for sensitive data
- **No Cloud**: Zero cloud connectivity
- **User Control**: Complete data deletion capability

**Permission Management**:
- **Minimal Permissions**: Only essential permissions requested
- **Runtime Permissions**: Proper runtime permission handling
- **Permission Explanation**: Clear explanation of permission usage
- **User Consent**: Explicit user consent for data collection

#### Security Best Practices

**Code Security**:
- **Code Obfuscation**: ProGuard/R8 enabled for release builds
- **API Protection**: Secure API key management
- **Input Validation**: Proper input sanitization
- **Error Handling**: Secure error handling without information leakage

**Runtime Security**:
- **Root Detection**: Detect and handle rooted devices
- **Emulator Detection**: Detect and handle emulator environments
- **Debug Protection**: Prevent debugger attachment in release builds
- **Tamper Detection**: Detect application tampering

### Maintenance and Updates

#### Version Management

**Version Control**:
```kotlin
// app/build.gradle.kts
android {
    defaultConfig {
        versionCode = 1
        versionName = "1.0.0"
    }
}
```

**Update Strategy**:
1. **Incremental Updates**: Regular minor version updates
2. **Feature Updates**: Major version updates with new features
3. **Security Updates**: Critical security patches
4. **Compatibility Updates**: Android version compatibility updates

#### Backup and Recovery

**Data Backup**:
- **Baseline Backup**: Export behavioral baseline data
- **Configuration Backup**: Backup app configuration
- **Settings Backup**: Backup user preferences
- **Recovery Process**: Restore from backup if needed

**Update Process**:
1. **Backup Current Data**: Export current baseline and settings
2. **Install Update**: Install new version
3. **Restore Data**: Import backup data
4. **Verify Functionality**: Test updated application

### Support and Documentation

#### User Support

**Documentation**:
- **User Manual**: Complete user guide
- **FAQ**: Frequently asked questions
- **Troubleshooting Guide**: Common issues and solutions
- **Video Tutorials**: Step-by-step video guides

**Support Channels**:
- **Email Support**: Technical support via email
- **Community Forum**: User community discussions
- **GitHub Issues**: Bug reports and feature requests
- **Documentation**: Comprehensive documentation

#### Developer Support

**API Documentation**:
- **KDoc**: Kotlin documentation
- **Code Examples**: Implementation examples
- **Integration Guide**: Third-party integration
- **Best Practices**: Development best practices

**Community Resources**:
- **GitHub Repository**: Source code and issues
- **Wiki**: Project documentation
- **Discussions**: Community discussions
- **Contributing Guide**: How to contribute

---

*This installation and deployment guide provides comprehensive instructions for setting up, building, installing, and maintaining the OD-MAS behavioral biometrics application.*
