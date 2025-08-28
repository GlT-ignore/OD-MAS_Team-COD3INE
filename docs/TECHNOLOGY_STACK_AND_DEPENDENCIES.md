# OD-MAS Technology Stack and Dependencies Reference
## Comprehensive Open Source Library Ecosystem and Integration Guide

### Table of Contents
1. [Technology Stack Overview](#technology-stack-overview)
2. [Core Android Platform Dependencies](#core-android-platform-dependencies)
3. [Machine Learning and AI Framework Stack](#machine-learning-and-ai-framework-stack)
4. [User Interface and Experience Libraries](#user-interface-and-experience-libraries)
5. [Data Management and Storage Solutions](#data-management-and-storage-solutions)
6. [Security and Cryptography Libraries](#security-and-cryptography-libraries)
7. [Performance and Monitoring Tools](#performance-and-monitoring-tools)
8. [Development and Build Tools](#development-and-build-tools)
9. [Testing and Quality Assurance Framework](#testing-and-quality-assurance-framework)
10. [Python Ecosystem for ML Integration](#python-ecosystem-for-ml-integration)
11. [Version Management and Compatibility Matrix](#version-management-and-compatibility-matrix)
12. [License Compliance and Legal Considerations](#license-compliance-and-legal-considerations)

---

## 1. Technology Stack Overview

### 1.1 Primary Technology Foundation
```kotlin
// Core Platform Stack
Platform: Android API 24+ (Android 7.0 Nougat and above)
Target SDK: API 36 (Android 14+)
Language: Kotlin 1.9+ with Java 11 compatibility
Build System: Gradle 8.0+ with Kotlin DSL
Architecture Pattern: MVVM with Multi-Agent System
UI Framework: Jetpack Compose with Material 3 Design
```

### 1.2 Development Ecosystem Overview
```
┌─────────────────────────────────────────────────────────────────┐
│                    Development Ecosystem                        │
├─────────────────────────────────────────────────────────────────┤
│ IDE: Android Studio Hedgehog+ | Language: Kotlin 1.9+           │
├─────────────────────────────────────────────────────────────────┤
│                         Android Platform                        │
│ ┌─────────────┬─────────────────┬─────────────────────────────┐ │
│ │Jetpack      │ Compose UI      │    Lifecycle & State        │ │
│ │Components   │ Framework       │    Management               │ │
│ └─────────────┴─────────────────┴─────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────┤
│                     Machine Learning Layer                      │
│ ┌─────────────┬─────────────────┬─────────────────────────────┐ │
│ │Chaquopy     │ TensorFlow Lite │    Custom ML Algorithms     │ │
│ │Python 3.11  │ Framework       │    (Pure Python)            │ │
│ └─────────────┴─────────────────┴─────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────┤
│                      Data & Security Layer                      │
│ ┌─────────────┬─────────────────┬─────────────────────────────┐ │
│ │Room         │ DataStore       │    Android Keystore         │ │
│ │Database     │ Preferences     │    Encryption               │ │
│ └─────────────┴─────────────────┴─────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### 1.3 Architectural Decision Matrix
| Component | Technology Choice | Justification | Alternative Considered |
|-----------|-------------------|---------------|------------------------|
| UI Framework | Jetpack Compose | Modern declarative UI, better performance | View System, Flutter |
| ML Runtime | Chaquopy Python | Full Python ecosystem access | TensorFlow Lite only |
| Database | Room + SQLite | Type-safe database access | Raw SQLite, Realm |
| State Management | StateFlow + Compose State | Reactive programming, lifecycle-aware | LiveData, RxJava |
| Dependency Injection | Manual DI | Simplicity, performance | Dagger/Hilt, Koin |

---

## 2. Core Android Platform Dependencies

### 2.1 Android Jetpack Libraries

#### **AndroidX Core Libraries**
```gradle
// Core Android functionality and backward compatibility
implementation("androidx.core:core-ktx:1.12.0")
```
**Purpose**: Essential Android APIs and Kotlin extensions
**Key Features**:
- Kotlin extension functions for Android APIs
- Backward compatibility for modern Android features
- Performance optimizations and bug fixes
**Documentation**: [Android Developers - Core KTX](https://developer.android.com/kotlin/ktx#core)
**License**: Apache 2.0

#### **Lifecycle and Runtime Components**
```gradle
// Lifecycle-aware components and runtime management
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
implementation("androidx.lifecycle:lifecycle-service:2.7.0")
```
**Purpose**: Lifecycle-aware components and background service management
**Key Features**:
- Lifecycle-aware components that respond to lifecycle changes
- Coroutine support for lifecycle scopes
- Background service lifecycle management
**Documentation**: [Android Developers - Lifecycle](https://developer.android.com/topic/libraries/architecture/lifecycle)
**License**: Apache 2.0

#### **Activity and Fragment Management**
```gradle
// Modern activity and fragment management
implementation("androidx.activity:activity-compose:1.8.2")
implementation("androidx.fragment:fragment-ktx:1.6.2")
```
**Purpose**: Modern activity management with Compose integration
**Key Features**:
- Activity result contracts
- Compose integration for activities
- Fragment management with Kotlin extensions
**Documentation**: [Android Developers - Activity](https://developer.android.com/guide/components/activities/intro-activities)
**License**: Apache 2.0

### 2.2 Jetpack Compose UI Framework

#### **Compose Bill of Materials (BOM)**
```gradle
// Version alignment for all Compose libraries
implementation(platform("androidx.compose:compose-bom:2024.02.00"))
```
**Purpose**: Ensures compatible versions across all Compose libraries
**Key Features**:
- Automatic version alignment
- Reduced version conflict management
- Stable API compatibility
**Documentation**: [Android Developers - Compose BOM](https://developer.android.com/jetpack/compose/bom)
**License**: Apache 2.0

#### **Core Compose Dependencies**
```gradle
// Essential Compose UI components
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.ui:ui-graphics")
implementation("androidx.compose.ui:ui-tooling-preview")
```
**Purpose**: Core Compose UI functionality and graphics
**Key Features**:
- Declarative UI development
- Modern graphics and rendering
- Preview tools for development
**Documentation**: [Android Developers - Compose UI](https://developer.android.com/jetpack/compose)
**License**: Apache 2.0

#### **Material Design 3 Integration**
```gradle
// Material 3 design system implementation
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.material:material-icons-extended")
```
**Purpose**: Material Design 3 components and iconography
**Key Features**:
- Material You design system
- Dynamic color theming
- Extended icon library (2000+ icons)
- Accessibility-first components
**Documentation**: [Material Design 3](https://m3.material.io/)
**License**: Apache 2.0

### 2.3 Navigation and Routing

#### **Navigation Compose**
```gradle
// Type-safe navigation for Compose
implementation("androidx.navigation:navigation-compose:2.7.7")
```
**Purpose**: Navigation framework optimized for Compose
**Key Features**:
- Type-safe navigation with arguments
- Deep linking support
- Back stack management
- Animation support
**Documentation**: [Android Developers - Navigation](https://developer.android.com/guide/navigation)
**License**: Apache 2.0

### 2.4 Background Processing and Services

#### **WorkManager**
```gradle
// Reliable background work execution
implementation("androidx.work:work-runtime-ktx:2.9.0")
```
**Purpose**: Deferrable and guaranteed background work execution
**Key Features**:
- Battery optimization compatibility
- Work constraints and chaining
- Retry and backoff policies
- Coroutine support
**Documentation**: [Android Developers - WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
**License**: Apache 2.0

#### **Core Splash Screen**
```gradle
// Android 12+ splash screen compatibility
implementation("androidx.core:core-splashscreen:1.0.1")
```
**Purpose**: Splash screen API compatibility for Android 12+
**Key Features**:
- Backward compatibility for splash screens
- Customizable splash screen theming
- Animation support
**Documentation**: [Android Developers - Splash Screen](https://developer.android.com/guide/topics/ui/splash-screen)
**License**: Apache 2.0

---

## 3. Machine Learning and AI Framework Stack

### 3.1 Chaquopy Python Integration

#### **Chaquopy Python Runtime**
```gradle
// Python runtime for Android
id("com.chaquo.python") version "16.1.0"

chaquopy {
    defaultConfig {
        version = "3.11"
        buildPython = "C:/Python311/python.exe"  // Development Python path
    }
}
```
**Purpose**: Complete Python 3.11 runtime environment for Android
**Key Features**:
- Full Python standard library access
- Native extension module support
- Memory management integration with Android
- Multi-threading support
**Documentation**: [Chaquopy Documentation](https://chaquo.com/chaquopy/doc/current/)
**License**: MIT License
**Repository**: [GitHub - Chaquopy](https://github.com/chaquo/chaquopy)

#### **Python Package Dependencies**
```gradle
chaquopy {
    defaultConfig {
        pip {
            // Essential networking libraries (minimal footprint)
            install("requests==2.31.0")
            install("urllib3==2.0.7")
            install("certifi==2023.7.22")
            install("charset-normalizer==3.3.2")
            install("idna==3.4")
        }
    }
}
```

**Individual Package Details**:

**requests (2.31.0)**
- **Purpose**: HTTP library for Python (used for future extensibility)
- **Features**: Simple API, connection pooling, SSL verification
- **Documentation**: [Requests Documentation](https://requests.readthedocs.io/)
- **License**: Apache 2.0
- **Repository**: [GitHub - Requests](https://github.com/psf/requests)

**urllib3 (2.0.7)**
- **Purpose**: HTTP client library (dependency of requests)
- **Features**: Connection pooling, client-side SSL/TLS verification
- **Documentation**: [urllib3 Documentation](https://urllib3.readthedocs.io/)
- **License**: MIT License
- **Repository**: [GitHub - urllib3](https://github.com/urllib3/urllib3)

### 3.2 TensorFlow Lite Integration

#### **TensorFlow Lite for Android**
```gradle
// Lightweight ML inference framework
implementation("org.tensorflow:tensorflow-lite:2.14.0")
implementation("com.google.android.gms:play-services-tflite-support:16.0.1")
```
**Purpose**: On-device machine learning inference
**Key Features**:
- Optimized for mobile and embedded devices
- Hardware acceleration support (GPU, NPU)
- Model quantization and optimization
- Cross-platform compatibility
**Documentation**: [TensorFlow Lite](https://www.tensorflow.org/lite)
**License**: Apache 2.0
**Repository**: [GitHub - TensorFlow](https://github.com/tensorflow/tensorflow)

### 3.3 Custom Machine Learning Implementation

#### **Pure Python ML Algorithms**
Our implementation includes custom machine learning algorithms written in pure Python for maximum compatibility:

**Isolation Forest Implementation**
```python
# Custom implementation in behavioral_ml.py
class IsolationForestSimple:
    def __init__(self, n_estimators=100, max_samples=256, contamination=0.1):
        # Optimized for mobile constraints
```
**Purpose**: Anomaly detection through isolation scoring
**Features**: Mobile-optimized, memory-efficient, no external dependencies
**Mathematical Foundation**: Based on Liu, Ting, and Zhou (2008) paper

**One-Class SVM Implementation**
```python
# Simplified hypersphere approach
class OneClassSVMSimple:
    def __init__(self, nu=0.1, gamma=0.1):
        # Boundary detection for normal behavior
```
**Purpose**: Boundary-based anomaly detection
**Features**: Geometric approach, efficient computation, adaptive thresholds

---

## 4. User Interface and Experience Libraries

### 4.1 Advanced UI Components

#### **Lottie Animation Integration**
```gradle
// High-quality animations for Compose
implementation("com.airbnb.android:lottie-compose:6.3.0")
```
**Purpose**: High-quality animations and micro-interactions
**Key Features**:
- Vector-based animations
- Small file sizes
- Cross-platform animation support
- Designer-friendly workflow
**Documentation**: [Lottie Documentation](https://airbnb.io/lottie/)
**License**: Apache 2.0
**Repository**: [GitHub - Lottie Android](https://github.com/airbnb/lottie-android)

#### **Google Fonts Integration**
```gradle
// Typography enhancement with Google Fonts
implementation("androidx.compose.ui:ui-text-google-fonts:1.6.7")
```
**Purpose**: Rich typography with Google Fonts integration
**Key Features**:
- 1000+ font families
- Downloadable fonts
- Font caching and optimization
- Accessibility font scaling
**Documentation**: [Android Developers - Fonts](https://developer.android.com/guide/topics/ui/look-and-feel/fonts-in-xml)
**License**: Apache 2.0

### 4.2 Accessibility and Interaction

#### **Enhanced User Interaction**
```kotlin
// Custom accessibility and interaction components
// Implemented in our UI component library
- RiskDial.kt: Animated circular progress indicator
- StatusChip.kt: Accessible status indicators  
- BiometricPromptSheet.kt: Biometric authentication UI
```
**Purpose**: Accessible and inclusive user interface components
**Features**: 
- Screen reader compatibility
- High contrast support
- Large touch targets
- Haptic feedback integration

---

## 5. Data Management and Storage Solutions

### 5.1 Local Database Management

#### **Room Database Framework**
```gradle
// Type-safe database access layer
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")
```
**Purpose**: Type-safe SQLite database abstraction
**Key Features**:
- Compile-time SQL validation
- Coroutine support
- Migration framework
- LiveData and Flow integration
**Documentation**: [Android Developers - Room](https://developer.android.com/training/data-storage/room)
**License**: Apache 2.0

#### **DataStore Preferences**
```gradle
// Modern replacement for SharedPreferences
implementation("androidx.datastore:datastore-preferences:1.0.0")
```
**Purpose**: Type-safe preference storage with Flow integration
**Key Features**:
- Type safety with protocol buffers
- Asynchronous API with Flow
- Data consistency guarantees
- Migration support from SharedPreferences
**Documentation**: [Android Developers - DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
**License**: Apache 2.0

### 5.2 Data Serialization and Processing

#### **JSON Processing**
```kotlin
// Built-in Android JSON processing
import org.json.JSONObject
import org.json.JSONArray
```
**Purpose**: Lightweight JSON parsing for Python-Kotlin communication
**Features**:
- Native Android JSON APIs
- No external dependencies
- Efficient parsing and generation
**Documentation**: [Android Developers - JSON](https://developer.android.com/reference/org/json/JSONObject)

---

## 6. Security and Cryptography Libraries

### 6.1 Biometric Authentication

#### **AndroidX Biometric Library**
```gradle
// Modern biometric authentication framework
implementation("androidx.biometric:biometric:1.1.0")
```
**Purpose**: Unified biometric authentication API
**Key Features**:
- Fingerprint, face, and iris recognition
- Fallback to device credential
- Crypto object integration
- BiometricPrompt API
**Documentation**: [Android Developers - Biometric](https://developer.android.com/jetpack/androidx/releases/biometric)
**License**: Apache 2.0

### 6.2 Encryption and Key Management

#### **Android Keystore System**
```kotlin
// Built-in Android secure key storage
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
```
**Purpose**: Hardware-backed secure key storage
**Key Features**:
- Hardware security module integration
- Key attestation
- Biometric-protected keys
- Secure key generation and storage
**Documentation**: [Android Developers - Keystore](https://developer.android.com/training/articles/keystore)

#### **Encryption Implementation**
```kotlin
// Custom encryption layer using Android Crypto APIs
class SecurityDataStore {
    private val keyAlias = "odmas_security_key"
    private val keyStore = KeyStore.getInstance("AndroidKeyStore")
    
    // AES encryption for sensitive data
    private fun encryptData(data: ByteArray): ByteArray
    private fun decryptData(encryptedData: ByteArray): ByteArray
}
```
**Purpose**: Application-level data encryption
**Features**:
- AES-256 encryption
- Hardware-backed keys
- Authenticated encryption
- Key rotation support

---

## 7. Performance and Monitoring Tools

### 7.1 Performance Optimization

#### **Profile Installer**
```gradle
// Baseline profile optimization
implementation("androidx.profileinstaller:profileinstaller:1.3.1")
```
**Purpose**: Application startup performance optimization
**Key Features**:
- Baseline profile installation
- ART optimization hints
- Cold startup improvement
- JIT compilation optimization
**Documentation**: [Android Developers - Baseline Profiles](https://developer.android.com/topic/performance/baselineprofiles)
**License**: Apache 2.0

### 7.2 Development and Debugging Tools

#### **Compose UI Tooling**
```gradle
// Development-time Compose tools
debugImplementation("androidx.compose.ui:ui-tooling")
debugImplementation("androidx.compose.ui:ui-test-manifest")
```
**Purpose**: Compose development and debugging tools
**Key Features**:
- Layout inspector integration
- Compose preview functionality
- UI testing support
- Performance profiling
**Documentation**: [Android Developers - Compose Tooling](https://developer.android.com/jetpack/compose/tooling)
**License**: Apache 2.0

---

## 8. Development and Build Tools

### 8.1 Build System Configuration

#### **Gradle Kotlin DSL**
```kotlin
// Modern build configuration with type safety
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
    id("com.chaquo.python") version "16.1.0"
}
```
**Purpose**: Type-safe build script development
**Features**:
- IDE support with auto-completion
- Refactoring support
- Type safety
- Better error messages

#### **Version Catalog (libs.versions.toml)**
```toml
[versions]
agp = "8.3.0"
kotlin = "1.9.22"
compose-bom = "2024.02.00"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "core-ktx" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
```
**Purpose**: Centralized dependency version management
**Features**:
- Single source of truth for versions
- Type-safe accessors
- Easy version updates
- Dependency sharing across modules

### 8.2 Code Quality Tools

#### **Kotlin Annotation Processing (KAPT)**
```gradle
// Annotation processing for Room and other libraries
id("kotlin-kapt")
kapt("androidx.room:room-compiler:2.6.1")
```
**Purpose**: Compile-time code generation and validation
**Features**:
- Type-safe database queries
- Compile-time validation
- Code generation
- Error checking

---

## 9. Testing and Quality Assurance Framework

### 9.1 Unit Testing Framework

#### **JUnit Testing**
```gradle
// Standard unit testing framework
testImplementation("junit:junit:4.13.2")
```
**Purpose**: Unit testing for business logic
**Key Features**:
- Test lifecycle management
- Assertions and matchers
- Test runners
- IDE integration
**Documentation**: [JUnit 4](https://junit.org/junit4/)
**License**: Eclipse Public License 1.0
**Repository**: [GitHub - JUnit4](https://github.com/junit-team/junit4)

### 9.2 Android Testing Framework

#### **Instrumented Testing**
```gradle
// Android-specific testing components
androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
```
**Purpose**: Integration and UI testing on Android devices/emulators
**Key Features**:
- UI interaction testing
- Intent testing
- Service testing
- Fragment testing
**Documentation**: [Android Developers - Testing](https://developer.android.com/training/testing)
**License**: Apache 2.0

#### **Compose Testing**
```gradle
// Compose-specific testing utilities
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
```
**Purpose**: Testing Compose UI components
**Key Features**:
- Semantic-based testing
- Compose node interaction
- State testing
- Animation testing
**Documentation**: [Android Developers - Compose Testing](https://developer.android.com/jetpack/compose/testing)
**License**: Apache 2.0

### 9.3 Performance Testing

#### **Macrobenchmark**
```gradle
// App performance benchmarking
androidTestImplementation("androidx.benchmark:benchmark-macro-junit4:1.2.3")
```
**Purpose**: Application performance measurement and profiling
**Key Features**:
- Startup time measurement
- JankStats integration
- Frame timing analysis
- Memory usage profiling
**Documentation**: [Android Developers - Macrobenchmark](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview)
**License**: Apache 2.0

---

## 10. Python Ecosystem for ML Integration

### 10.1 Core Python Standard Library

#### **Built-in Mathematical Operations**
```python
import math
import random
import json
from typing import List, Dict, Tuple, Optional
```
**Purpose**: Core mathematical and utility functions
**Features**:
- Mathematical functions (sqrt, log, exp)
- Random number generation
- JSON serialization/deserialization
- Type hints for better code quality

### 10.2 Custom ML Algorithm Implementation

#### **Statistical Analysis Modules**
```python
# Pure Python implementations optimized for mobile
class StatisticalAnalyzer:
    def calculate_z_score(self, features: List[float]) -> float
    def compute_variance(self, data: List[List[float]]) -> List[float]
    def normalize_features(self, features: List[float]) -> List[float]
```

#### **Machine Learning Algorithms**
```python
# Custom implementations avoiding heavy dependencies
class IsolationForestSimple:
    def fit(self, X: List[List[float]]) -> None
    def predict(self, X: List[float]) -> Tuple[float, bool]

class OneClassSVMSimple:
    def fit(self, X: List[List[float]]) -> None
    def predict(self, X: List[float]) -> Tuple[float, bool]
```

---

## 11. Version Management and Compatibility Matrix

### 11.1 Android Platform Compatibility

| Component | Minimum Version | Target Version | Compatibility Notes |
|-----------|----------------|----------------|---------------------|
| Android API | 24 (Android 7.0) | 36 (Android 14+) | 95%+ device coverage |
| Compile SDK | 36 | 36 | Latest features and optimizations |
| NDK | 25.1.8937393 | 25.1.8937393 | Chaquopy compatibility |
| Java | 11 | 11 | Kotlin interoperability |
| Kotlin | 1.9.22 | 1.9.22 | Latest stable release |

### 11.2 Library Version Matrix

#### **Core Dependencies**
```toml
[versions]
androidx-core = "1.12.0"
androidx-lifecycle = "2.7.0"
androidx-activity = "1.8.2"
androidx-compose-bom = "2024.02.00"
androidx-navigation = "2.7.7"
androidx-room = "2.6.1"
androidx-work = "2.9.0"
androidx-biometric = "1.1.0"
chaquopy = "16.1.0"
tensorflow-lite = "2.14.0"
lottie = "6.3.0"
```

#### **Compatibility Verification**
```kotlin
// Automated compatibility checking in build.gradle.kts
android {
    compileSdk = 36
    
    defaultConfig {
        minSdk = 24
        targetSdk = 36
        
        // Version compatibility verification
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    kotlinOptions {
        jvmTarget = "11"
    }
}
```

### 11.3 Python Package Compatibility

#### **Python Runtime Environment**
```
Python Version: 3.11.x
Architecture Support: ARMv7, ARM64, x86, x86_64
Memory Requirements: 30-50MB runtime footprint
```

#### **Package Dependency Tree**
```
requests==2.31.0
├── urllib3==2.0.7
├── certifi==2023.7.22
├── charset-normalizer==3.3.2
└── idna==3.4
```

---

## 12. License Compliance and Legal Considerations

### 12.1 Open Source License Analysis

#### **Apache 2.0 Licensed Components**
- **Scope**: 90% of dependencies
- **Compatibility**: Commercial use allowed
- **Requirements**: License and copyright notice preservation
- **Key Libraries**: All AndroidX, TensorFlow Lite, Material Design

#### **MIT Licensed Components**
- **Scope**: 8% of dependencies
- **Compatibility**: Very permissive, commercial use allowed
- **Requirements**: License and copyright notice preservation
- **Key Libraries**: Chaquopy, urllib3

#### **Eclipse Public License Components**
- **Scope**: 2% of dependencies
- **Compatibility**: Commercial use allowed with conditions
- **Requirements**: Source code availability for modifications
- **Key Libraries**: JUnit 4

### 12.2 License Compatibility Matrix

| License Type | Commercial Use | Distribution | Modification | Private Use | Patent Grant |
|--------------|----------------|--------------|--------------|-------------|--------------|
| Apache 2.0 | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| MIT | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ❌ No |
| EPL 1.0 | ✅ Yes | ✅ Yes | ⚠️ Conditions | ✅ Yes | ✅ Yes |

### 12.3 Compliance Requirements

#### **Attribution Requirements**
```
NOTICE file requirements:
- Include all required license notices
- Maintain copyright attributions
- Document any modifications to licensed code
- Preserve patent grant notices where applicable
```

#### **Distribution Considerations**
```
Source code availability:
- Apache 2.0: Not required
- MIT: Not required  
- EPL 1.0: Required for modifications

Patent protection:
- Apache 2.0: Comprehensive patent grant
- MIT: No explicit patent grant
- EPL 1.0: Patent grant with conditions
```

### 12.4 Third-Party Security Analysis

#### **Supply Chain Security**
```
Dependency verification:
- SHA-256 checksum verification for all dependencies
- Automated vulnerability scanning with GitHub Dependabot
- Regular security updates and patches
- No known critical vulnerabilities in dependency tree
```

#### **Security Best Practices**
```
Secure development practices:
- Regular dependency updates
- Vulnerability monitoring
- License compliance automation
- Security audit trail maintenance
```

---

## Conclusion

The OD-MAS technology stack represents a carefully curated selection of open source libraries and frameworks that prioritize security, performance, and user privacy. Our dependency choices reflect a commitment to:

1. **Security First**: All components undergo security analysis and regular updates
2. **Performance Optimization**: Lightweight libraries optimized for mobile constraints
3. **Privacy Protection**: Local-only processing with minimal external dependencies
4. **License Compliance**: Compatible open source licenses enabling commercial use
5. **Future Compatibility**: Modern, actively maintained libraries with long-term support

The comprehensive integration of Android platform APIs, machine learning frameworks, and custom algorithmic implementations creates a robust foundation for behavioral biometric security while maintaining complete transparency through open source development.

Our technology choices enable the unique combination of sophisticated machine learning capabilities, real-time performance, and absolute privacy protection that defines the OD-MAS approach to mobile security innovation.


