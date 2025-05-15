package com.example.unimarket.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unimarket.utils.ConnectivityObserver

@Composable
fun TopOfflineBar() {
    val context = LocalContext.current
    val observer = remember { ConnectivityObserver(context) }
    val isOnline by observer.isOnline.collectAsState()

    if (!isOnline) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFEB3B))
                .height(32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = "Offline",
                tint = Color.Black,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "You are offline - Showing cached data",
                color = Color.Black,
                modifier = Modifier.padding(start = 4.dp),
                fontSize = 14.sp
            )
        }
    }
}
