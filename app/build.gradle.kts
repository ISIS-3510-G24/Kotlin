plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics") version "3.0.3"
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.example.unimarket"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.unimarket"
        minSdk = 24
        targetSdk = 35
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
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.firebase.vertexai)
    implementation(libs.firebase.crashlytics.buildtools)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil)
    implementation(libs.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.perf)
    implementation(libs.firebase.inappmessaging.display)
    implementation (libs.accompanist.flowlayout)
    implementation (libs.zxing.android.embedded)
    //implementation(libs.androidx.datastore.preferences)
    implementation (libs.androidx.biometric)
    implementation (libs.gson)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    implementation (libs.androidx.work.runtime.ktx)
    implementation (libs.hilt.android)
    kapt           (libs.hilt.compiler)
    implementation (libs.androidx.hilt.work)
    kapt           (libs.androidx.hilt.hilt.compiler)
    implementation("androidx.datastore:datastore-preferences:1.1.4")
    implementation (libs.androidx.hilt.navigation.compose)
    implementation (libs.androidx.material)



}