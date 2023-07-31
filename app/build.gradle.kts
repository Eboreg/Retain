@file:Suppress("UnstableApiUsage")

import java.io.FileInputStream
import java.util.Properties

val keystoreProperties = Properties()
val secretsProperties = Properties()

keystoreProperties.load(FileInputStream(rootProject.file("keystore.properties")))
secretsProperties.load(FileInputStream(rootProject.file("secrets.properties")))

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
    id("kotlin-parcelize")
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
    compileSdk = 33

    defaultConfig {
        // val dropboxAppKey = secretsProperties["dropboxAppKey"] as String

        applicationId = "us.huseli.retain"
        minSdk = 26
        targetSdk = 33
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
    implementation("androidx.recyclerview:recyclerview:1.3.1")
    implementation("androidx.preference:preference-ktx:1.2.0")
    implementation("androidx.activity:activity-compose:1.7.2")
    implementation("androidx.navigation:navigation-compose:2.6.0")
    // For PickVisualMedia contract:
    implementation("androidx.activity:activity-ktx:1.7.2")

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
    implementation("androidx.compose.ui:ui:1.4.3")
    implementation("androidx.compose.ui:ui-graphics:1.4.3")
    implementation("androidx.compose.ui:ui-tooling-preview:1.4.3")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.4.3")
    debugImplementation("androidx.compose.ui:ui-tooling:1.4.3")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.4.3")

    // Material:
    implementation("androidx.compose.material:material:1.4.3")
    implementation("androidx.compose.material3:material3:1.1.1")
    implementation("androidx.compose.material:material-icons-extended:1.4.3")

    // Room:
    implementation("androidx.room:room-runtime:2.5.2")
    ksp("androidx.room:room-compiler:2.5.2")
    implementation("androidx.room:room-ktx:2.5.2")

    // Hilt:
    implementation("com.google.dagger:hilt-android:2.46.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.0.0")
    kapt("com.google.dagger:hilt-compiler:2.46.1")

    // Gson:
    implementation("com.google.code.gson:gson:2.10.1")

    // Reorder:
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")
    // implementation("com.github.Eboreg:ComposeReorderable:main-b0729bddae-1")

    // To change status bar colour etc:
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.27.0")

    // HTML parsing:
    implementation("org.jsoup:jsoup:1.16.1")

    // SFTP:
    implementation(group = "com.github.mwiede", name = "jsch", version = "0.2.9")

    // Dropbox:
    implementation("com.dropbox.core:dropbox-core-sdk:5.4.5")

    // Theme:
    implementation("com.github.Eboreg:RetainTheme:1.1.3")

    // testImplementation("junit:junit:4.13.2")
    // androidTestImplementation("androidx.test.ext:junit:1.1.5")
    // androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
