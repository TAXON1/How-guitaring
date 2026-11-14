plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.gitara"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.gitara"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    // ЭТО САМОЕ ГЛАВНОЕ — ПЕРЕХОДИМ НА JAVA 17
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.6.10"  // последняя стабильная на 2025
    }
}

dependencies {
    // === ВАЖНЫЕ ДЛЯ РАБОТЫ НАСТРОЕК ===
    // JSON Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // DataStore (Preferences) - для сохранения настроек
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Lifecycle (для сбора Flow в Compose)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // GSON (если вдруг где-то используется старый код, но мы перешли на kotlinx)
    implementation("com.google.code.gson:gson:2.10.1")

    // === COMPOSE ===
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Activity & Navigation
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Icons Extended (для всех красивых иконок)
    implementation("androidx.compose.material:material-icons-extended")

    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Debugging
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}