@file:Suppress("UnstableApiUsage")

import java.io.FileInputStream
import java.util.Properties

val keystoreProperties = Properties()
val secretsProperties = Properties()
val targetSdk = 34

keystoreProperties.load(FileInputStream(rootProject.file("keystore.properties")))
secretsProperties.load(FileInputStream(rootProject.file("secrets.properties")))

plugins {
    id("com.android.application")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

kotlin {
    jvmToolchain(17)
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
    compileSdk = targetSdk

    defaultConfig {
        // val dropboxAppKey = secretsProperties["dropboxAppKey"] as String

        applicationId = "us.huseli.retain"
        minSdk = 26
        targetSdk = targetSdk
        versionCode = 1
        versionName = "1.0.0-beta.1"
        vectorDrawables.useSupportLibrary = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // buildConfigField("String", "dropboxAppKey", "\"${dropboxAppKey}\"")
        // manifestPlaceholders["dropboxAppKey"] = dropboxAppKey
        setProperty("archivesBaseName", "retain_$versionName")
    }

    buildTypes {
        debug {
            val dropboxAppKey = secretsProperties["dropboxAppKeyDebug"] as String

            isDebuggable = true
            isRenderscriptDebuggable = true
            applicationIdSuffix = ".debug"
            buildConfigField("String", "dropboxAppKey", "\"${dropboxAppKey}\"")
            manifestPlaceholders["dropboxAppKey"] = dropboxAppKey
        }
        release {
            val dropboxAppKey = secretsProperties["dropboxAppKey"] as String

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("String", "dropboxAppKey", "\"${dropboxAppKey}\"")
            manifestPlaceholders["dropboxAppKey"] = dropboxAppKey
        }
    }

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
        // https://developer.android.com/jetpack/androidx/releases/compose-kotlin
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

val lifecycleVersion = "2.6.2"
val composeVersion = "1.5.4"
val daggerVersion = "2.48.1"
val roomVersion = "2.6.0"

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:1.9.10-1.0.13")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.navigation:navigation-compose:2.7.4")
    // For PickVisualMedia contract:
    implementation("androidx.activity:activity-ktx:1.8.0")

    // Lifecycle:
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")

    // Nextcloud:
    implementation("commons-httpclient:commons-httpclient:3.1@jar")
    implementation("com.github.nextcloud:android-library:2.14.0") {
        exclude(group = "org.ogce", module = "xpp3")
    }

    // Compose:
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.ui:ui-graphics:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")
    debugImplementation("androidx.compose.ui:ui-tooling:$composeVersion")
    debugImplementation("androidx.compose.ui:ui-test-manifest:$composeVersion")

    // Material:
    implementation("androidx.compose.material:material:$composeVersion")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.material:material-icons-extended:$composeVersion")

    // Room:
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    // Hilt:
    implementation("com.google.dagger:hilt-android:$daggerVersion")
    implementation("androidx.hilt:hilt-navigation-compose:1.0.0")
    kapt("com.google.dagger:hilt-compiler:$daggerVersion")

    // Gson:
    implementation("com.google.code.gson:gson:2.10.1")

    // Reorder:
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")

    // To change status bar colour etc:
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.27.0")

    // HTML parsing:
    implementation("org.jsoup:jsoup:1.16.1")

    // SFTP:
    implementation(group = "com.github.mwiede", name = "jsch", version = "0.2.9")

    // Dropbox:
    implementation("com.dropbox.core:dropbox-core-sdk:5.4.5")

    // Theme:
    implementation("com.github.Eboreg:RetainTheme:2.1.0")
}
