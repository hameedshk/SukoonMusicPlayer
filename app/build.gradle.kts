import java.util.Properties
import java.io.FileInputStream
import org.gradle.api.tasks.compile.JavaCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
}

// Load local.properties for sensitive configuration (API keys)
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.sukoon.music"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sukoon.music"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Supported locales (limits APK size by excluding unused translations from dependencies)
        resourceConfigurations += listOf("en", "hi", "pt-rBR")

        // Gemini AI Configuration (for metadata correction)
        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${localProperties.getProperty("GEMINI_API_KEY", "")}\""
        )
        buildConfigField(
            "Boolean",
            "ENABLE_GEMINI_METADATA_CORRECTION",
            "true"  // Set to false to disable Gemini entirely
        )
        buildConfigField(
            "Boolean",
            "ENABLE_GOOGLE_SEARCH_GROUNDING",
            "false"  // Set to true to enable Google Search (may require API setup)
        )
        buildConfigField(
            "String",
            "GEMINI_MODEL",
            "\"gemini-2.5-flash\""  // Updated: removed '-latest' suffix
        )

        // AdMob test IDs (safe for development and QA)
        // NOTE: Only Banner and Native ads are allowed per Google Play policy for music players
        val admobTestAppId = "ca-app-pub-3940256099942544~3347511713"
        val admobTestBannerAdUnitId = "ca-app-pub-3940256099942544/6300978111"
        val admobTestNativeAdUnitId = "ca-app-pub-3940256099942544/2247696110"

        // Production AdMob IDs from local.properties (user must fill in before release)
        val admobProdAppId = localProperties.getProperty("ADMOB_APP_ID", "").trim()
        val admobProdBannerAdUnitId = localProperties.getProperty("ADMOB_BANNER_ID", "").trim()
        val admobProdNativeAdUnitId = localProperties.getProperty("ADMOB_NATIVE_ID", "").trim()

        // Use production IDs if configured; otherwise use test IDs
        val useTestAds = admobProdAppId.isEmpty() || admobProdAppId.startsWith("ca-app-pub-3940")
        val admobAppId = if (useTestAds) admobTestAppId else admobProdAppId
        val admobBannerAdUnitId = if (useTestAds) admobTestBannerAdUnitId else admobProdBannerAdUnitId
        val admobNativeAdUnitId = if (useTestAds) admobTestNativeAdUnitId else admobProdNativeAdUnitId

        manifestPlaceholders["admobAppId"] = admobAppId
        buildConfigField("String", "ADMOB_APP_ID", "\"$admobAppId\"")
        buildConfigField("String", "ADMOB_BANNER_AD_UNIT_ID", "\"$admobBannerAdUnitId\"")
        buildConfigField("String", "ADMOB_NATIVE_AD_UNIT_ID", "\"$admobNativeAdUnitId\"")

        // ⚠️ WARNING: Fill in production AdMob IDs in local.properties before release! ⚠️
        // Test ads will not generate revenue and may violate Play Store policies
        buildConfigField("Boolean", "USE_TEST_ADS", "$useTestAds")

        // Billing Product ID (read from local.properties, defaults to test ID)
        val premiumProductId = localProperties.getProperty("PREMIUM_PRODUCT_ID", "android.test.purchased")
        buildConfigField("String", "PREMIUM_PRODUCT_ID", "\"$premiumProductId\"")
    }

packaging {
    resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

    signingConfigs {
        val releaseStoreFile = localProperties.getProperty("RELEASE_STORE_FILE", "")
        if (releaseStoreFile.isNotEmpty()) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD", "")
                keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS", "release")
                keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD", "")
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (signingConfigs.getNames().contains("release")) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
        }

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true  // Enable BuildConfig generation for API keys
    }
    lint {
        // Suppress experimental API warnings for Media3 @UnstableApi usage
        // Media3 requires use of experimental APIs that are stable in practice
        disable += "UnsafeOptInUsageError"
    }
}

// KSP (KSP2) may keep intermediate by-round Java outputs under generated/ksp/**/java/byRounds.
// Exclude those intermediates from javac inputs to avoid duplicate generated classes.
tasks.withType<JavaCompile>().configureEach {
    exclude("**/byRounds/**")
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended:1.7.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Media3 (ExoPlayer)
    implementation("androidx.media3:media3-exoplayer:1.5.0")
    implementation("androidx.media3:media3-session:1.5.0")
    implementation("androidx.media3:media3-ui:1.5.0")

    // AndroidX Media (for MediaBrowserService - Android Auto)
    implementation("androidx.media:media:1.7.0")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Retrofit & OkHttp (for LRCLIB)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coil (Image Loading with Palette)
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.palette:palette-ktx:1.0.0")

    // DataStore (for preferences)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // AdMob
    implementation("com.google.android.gms:play-services-ads:24.0.0")
    // User Messaging Platform (GDPR/CCPA consent)
    implementation("com.google.android.ump:user-messaging-platform:3.1.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-config-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Google Play Billing Library (for in-app purchases)
    implementation("com.android.billingclient:billing:7.0.0")

    // Google Play In-App Review API (for ratings and feedback)
    implementation("com.google.android.play:review:2.0.1")
    implementation("com.google.android.play:review-ktx:2.0.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Reorderable (for drag-and-drop)
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")

    // Testing
    testImplementation(libs.junit)
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
