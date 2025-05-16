package com.example.unimarket

import android.app.Application
import android.os.Build
import android.os.Bundle
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.unimarket.data.PreferencesManager
import com.example.unimarket.data.SyncWorker
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class UniMarketApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // --- Firebase Analytics & Crashlytics ---
        try {
            FirebaseAnalytics.getInstance(this)
                .logEvent("app_open", Bundle())
            FirebaseCrashlytics.getInstance()
                .setCustomKey("os_version", Build.VERSION.RELEASE)
            FirebaseCrashlytics.getInstance()
                .setCustomKey("device", "${Build.MANUFACTURER} ${Build.MODEL}")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
        }

        // --- Coroutines + WorkManager ---
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val interval = PreferencesManager
                    .syncInterval(this@UniMarketApplication)
                    .first()
                    .toLong()
                val wifiOnly = PreferencesManager
                    .syncOnWifiOnly(this@UniMarketApplication)
                    .first()

                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(
                        if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
                    )
                    .build()

                val syncWork = PeriodicWorkRequestBuilder<SyncWorker>(
                    interval,
                    TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(this@UniMarketApplication)
                    .enqueueUniquePeriodicWork(
                        "sync_ops",
                        ExistingPeriodicWorkPolicy.KEEP,
                        syncWork
                    )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
