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
import com.example.unimarket.data.UniMarketDatabase
import com.example.unimarket.data.UniMarketRepository
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class UniMarketApplication : Application() {
    companion object {
        lateinit var repository: UniMarketRepository
            private set
    }

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
            // Si Firebase falla por alguna razón, loggear el error
            e.printStackTrace()
        }

        // --- Room + Repository ---
        try {
            val db = UniMarketDatabase.getInstance(this)
            repository = UniMarketRepository(
                productDao    = db.productDao(),
                wishlistDao   = db.wishlistDao(),
                orderDao      = db.orderDao(),
                imageCacheDao = db.imageCacheDao(),
                pendingOpDao  = db.pendingOpDao()
            )
        } catch (e: Exception) {
            // Loggear cualquier fallo en la creación de la base de datos
            e.printStackTrace()
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
                // Loggear cualquier fallo en el trabajo de sincronización
                e.printStackTrace()
            }
        }
    }
}
