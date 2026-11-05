plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.myvpn"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.myvpn"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "2.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
}

dependencies {
    implementation("com.wireguard.android:tunnel:1.0.20230706")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}
