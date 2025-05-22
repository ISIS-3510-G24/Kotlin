package com.example.unimarket.ui.views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.unimarket.utils.ConnectivityObserver
import kotlinx.coroutines.flow.collectLatest

@Composable
fun TopOfflineBar(
    message: String = "You are offline - Showing cached data",
) {
    val context = LocalContext.current
    val observer = remember { ConnectivityObserver(context) }
    var isOnline by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(observer) {
        observer.isOnline.collectLatest { online ->
            isOnline = online
        }
    }

    if (!isOnline) {
        OfflineBanner(message = message)
    }
}
