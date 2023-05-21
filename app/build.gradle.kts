@file:Suppress("UnstableApiUsage")

import java.io.FileInputStream
import java.util.Properties

val keystoreProperties = Properties()
keystoreProperties.load(FileInputStream(rootProject.file("keystore.properties")))

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
    id("kotlin-parcelize")
}

kotlin {
    jvmToolchain(8)
}

android {
    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }
    namespace = "us.huseli.retain"
    compileSdk = 33

    defaultConfig {
        applicationId = "us.huseli.retain"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
        vectorDrawables.useSupportLibrary = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isDebuggable = true
            isRenderscriptDebuggable = true
            applicationIdSuffix = ".debug"
        }
        release {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        // https://developer.android.com/jetpack/androidx/releases/compose-kotlin
        kotlinCompilerExtensionVersion = "1.4.4"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:1.8.10-1.0.9")
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")
    implementation("androidx.recyclerview:recyclerview:1.3.0")
    implementation("androidx.preference:preference-ktx:1.2.0")
    implementation("androidx.activity:activity-compose:1.7.1")
    implementation("androidx.navigation:navigation-compose:2.5.3")
    // For PickVisualMedia contract
    implementation("androidx.activity:activity-ktx:1.7.1")

    // Lifecycle:
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.1")

    // Nextcloud:
    implementation("commons-httpclient:commons-httpclient:3.1@jar")
    implementation("com.github.nextcloud:android-library:2.14.0") {
        exclude(group = "org.ogce", module = "xpp3")
    }

    // Compose:
    implementation(platform("androidx.compose:compose-bom:2023.05.01"))
    implementation("androidx.compose.ui:ui:1.4.3")
    implementation("androidx.compose.ui:ui-graphics:1.4.3")
    implementation("androidx.compose.ui:ui-tooling-preview:1.4.3")
    implementation("androidx.compose.material3:material3:1.1.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.05.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.4.3")
    debugImplementation("androidx.compose.ui:ui-tooling:1.4.3")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.4.3")
    implementation("androidx.compose.material:material:1.4.3")
    implementation("androidx.compose.material3:material3:1.1.0")
    implementation("androidx.compose.material:material-icons-extended:1.4.3")

    // Room:
    implementation("androidx.room:room-runtime:2.5.1")
    ksp("androidx.room:room-compiler:2.5.1")
    implementation("androidx.room:room-ktx:2.5.1")

    // Hilt:
    implementation("com.google.dagger:hilt-android:2.46.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.0.0")
    kapt("com.google.dagger:hilt-compiler:2.46.1")

    // Gson:
    implementation("com.google.code.gson:gson:2.10.1")

    // Reorder:
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")

    // To change status bar colour etc:
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.27.0")

    // testImplementation("junit:junit:4.13.2")
    // androidTestImplementation("androidx.test.ext:junit:1.1.5")
    // androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    // androidTestImplementation(platform("androidx.compose:compose-bom:2023.05.00"))
    // androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.4.3")
    // debugImplementation("androidx.compose.ui:ui-tooling:1.4.3")
    // debugImplementation("androidx.compose.ui:ui-test-manifest:1.4.3")
}