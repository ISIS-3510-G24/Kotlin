pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    plugins {
        // Android + Kotlin + Compose
        id("com.android.application")         version "8.9.2"    apply false
        id("org.jetbrains.kotlin.android")    version "2.0.0"    apply false
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false

        // Hilt & Kapt
        id("dagger.hilt.android.plugin")      version "2.44"     apply false
        id("org.jetbrains.kotlin.kapt")       version "2.0.0"    apply false

        // Google services & Crashlytics
        id("com.google.gms.google-services")  version "4.4.2"    apply false
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Unimarket"
include(":app")
