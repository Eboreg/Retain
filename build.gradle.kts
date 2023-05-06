buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:8.0.1")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.46")
        // classpath("org.jetbrains.kotlin:kotlin-gradle-plugin")
        // classpath("com.google.dagger:hilt-android-gradle-plugin")
    }
}

plugins {
    id("com.android.application") version "8.0.0" apply false
    id("com.android.library") version "8.0.0" apply false
    id("org.jetbrains.kotlin.android") version "1.8.20" apply false
    id("org.jetbrains.kotlin.kapt") version "1.8.20" apply false
    id("com.google.dagger.hilt.android") version "2.46" apply false
}