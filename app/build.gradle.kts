@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.application")
    id("kotlin-android")
    // id("org.jetbrains.kotlin.android")
    // id("org.jetbrains.kotlin.kapt")
    // id("kotlin-kapt")
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
}

kotlin {
    jvmToolchain(17)
}

kapt {
    correctErrorTypes = true
}

android {
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
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        // jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.6"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")
    implementation("androidx.recyclerview:recyclerview:1.3.0")
    implementation("androidx.preference:preference-ktx:1.2.0")

    // Nextcloud:
    // implementation("commons-httpclient:commons-httpclient:20020423@jar")
    implementation("commons-httpclient:commons-httpclient:20020423")
    implementation("com.github.nextcloud:android-library:2.14.0")

    // Compose:
    implementation("androidx.compose.runtime:runtime:1.4.3")
    implementation("androidx.compose.ui:ui:1.4.3")
    implementation("androidx.compose.foundation:foundation:1.4.3")
    implementation("androidx.activity:activity-compose:1.7.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.4.3")
    implementation("androidx.compose.material3:material3:1.0.1")
    implementation("androidx.navigation:navigation-compose:2.5.3")
    implementation("androidx.compose.material:material-icons-extended:1.4.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.1")

    // Room:
    implementation("androidx.room:room-runtime:2.5.1")
    implementation("androidx.room:room-ktx:2.5.1")
    kapt("androidx.room:room-compiler:2.5.1")
    // annotationProcessor("androidx.room:room-compiler:2.5.1")

    // Hilt:
    implementation("com.google.dagger:hilt-android:2.46")
    implementation("androidx.hilt:hilt-navigation-compose:1.0.0")
    // kapt("com.google.dagger:hilt-compiler:2.46")
    annotationProcessor("com.google.dagger:hilt-compiler:2.46")

    // testImplementation("junit:junit:4.13.2")
    // androidTestImplementation("androidx.test.ext:junit:1.1.5")
    // androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    // androidTestImplementation(platform("androidx.compose:compose-bom:2023.05.00"))
    // androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.4.3")
    // debugImplementation("androidx.compose.ui:ui-tooling:1.4.3")
    // debugImplementation("androidx.compose.ui:ui-test-manifest:1.4.3")
}