package com.example.unimarket.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.unimarket.data.SyncWorker
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

class ConnectivityObserver(private val context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _rawOnline = MutableStateFlow(checkOnline())

    @OptIn(FlowPreview::class)
    val isOnline: Flow<Boolean> = _rawOnline
        .debounce(300)
        .distinctUntilChanged()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            val valid = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

            val wasOnline = _rawOnline.value
            _rawOnline.value = valid

            if (valid && !wasOnline) {
                val oneTime = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .build()
                WorkManager
                    .getInstance(context)
                    .enqueueUniqueWork(
                        "sync_immediate",
                        ExistingWorkPolicy.KEEP,
                        oneTime
                    )
            }
        }

        override fun onLost(network: Network) {
            _rawOnline.value = false
        }
    }

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    private fun checkOnline(): Boolean {
        val net = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun unregister() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}
