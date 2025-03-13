// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
//        classpath(libs.hilt.android.gradle.plugin)
        classpath(libs.google.services)
        classpath(libs.androidx.navigation.safe.args.gradle.plugin)
        classpath(libs.gradle)
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.google.gms.google.services) apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.27" apply false
//    id("com.google.devtools.ksp") version "2.1.0-1.0.28" apply false
    id("org.jetbrains.kotlin.kapt") version "2.0.0" apply false
//    id("com.google.dagger.hilt.android") version "2.50" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
}

//allprojects {
//    repositories {
//        google()
//        mavenCentral()
//    }
//}