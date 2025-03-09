plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
//    id("com.google.devtools.ksp") version "2.0.21-1.0.27"
//    id("com.google.devtools.ksp") version "1.9.22-1.0.17"
//    id("com.google.devtools.ksp")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.dagger.hilt.android") version "2.50"
//    id("kotlin-kapt")
    id("org.jetbrains.kotlin.kapt")
    id("kotlin-parcelize")
}


android {
    namespace = "com.healthtech.doccareplus"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.healthtech.doccareplus"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Thêm cấu hình Room Database ở đây
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas",
                    "room.incremental" to "true"
                )
            }
        }

//        buildConfigField(
//            "String",
//            "EMAIL_SENDER",
//            "\"${project.findProperty("EMAIL_SENDER") ?: ""}\""
//        )
//        buildConfigField(
//            "String",
//            "EMAIL_PASSWORD",
//            "\"${project.findProperty("EMAIL_PASSWORD") ?: ""}\""
//        )

//        buildConfigField(
//            "String",
//            "CLOUDINARY_API_KEY",
//            "\"${project.properties["CLOUDINARY_API_KEY"]}\""
//        )
//        buildConfigField(
//            "String",
//            "CLOUDINARY_API_SECRET",
//            "\"${project.properties["CLOUDINARY_API_SECRET"]}\""
//        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources {
            excludes.add("META-INF/NOTICE.md")
            excludes.add("META-INF/LICENSE.md")
            excludes.add("META-INF/DEPENDENCIES")
            excludes.add("META-INF/*.properties")
            excludes.add("META-INF/AL2.0")
            excludes.add("META-INF/LGPL2.1")
        }
    }

//    ksp {
//        arg("room.schemaLocation", "$projectDir/schemas")
//        arg("room.incremental", "true")
//    }

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
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
    buildFeatures {
        android.buildFeatures.buildConfig = true
    }
//    configurations.all {
//        exclude(group = "com.android.support", module = "support-compat")
//    }
}

dependencies {
//    implementation("com.cloudinary:kotlin-url-gen:1.7.0")
    implementation("com.cloudinary:cloudinary-android:2.3.1")

    implementation("com.github.ZEGOCLOUD:zego_inapp_chat_uikit_android:+")

    implementation(libs.dexter)

    // Hỗ trợ media
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)


    // Timber
    implementation(libs.timber)

    implementation(libs.volley)

//    // WebRTC dependencies
//    implementation(libs.google.webrtc)

    // lottie
    implementation(libs.lottie)

    implementation(libs.androidx.viewpager2)
    implementation(libs.material.v1110)
    implementation(libs.circleindicator)

    implementation(libs.circleimageview)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.database)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.messaging)


    implementation(libs.play.services.safetynet)
    implementation(libs.play.services.auth)

    // Architecture Components
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.activity.ktx)
//    implementation(libs.androidx.lifecycle.viewmodel.ktx.v270)
//    implementation(libs.androidx.lifecycle.livedata.ktx.v270)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.legacy.support.v4)
    //noinspection KaptUsageInsteadOfKsp
    kapt(libs.androidx.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.coroutines.core)


    // https://mvnrepository.com/artifact/com.github.bumptech.glide/glide
    implementation(libs.glide)

    // Gson
    implementation(libs.gson)


    // RecyclerView
    implementation(libs.androidx.recyclerview)
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(kotlin("script-runtime"))
}