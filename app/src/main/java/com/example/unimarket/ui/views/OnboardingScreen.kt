package com.example.unimarket.ui.views

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.unimarket.R
import com.example.unimarket.data.PreferencesManager
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Data class for onboarding pages
data class OnboardingPage(
    val imageRes: Int, val title: String, val description: String
)

@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit,
    onSkip: () -> Unit
) {
    // Define the pages
    val pages = listOf(
        OnboardingPage(
            imageRes = R.drawable.onboarding1,
            title = "Welcome to UniMarket",
            description = "Post your items with a few steps… What are you waiting for!"
        ),
        OnboardingPage(
            imageRes = R.drawable.onboarding2,
            title = "Buy and sell with ease",
            description = "Find the supplies you need or sell what you no longer use."
        ),
        OnboardingPage(
            imageRes = R.drawable.onboarding3,
            title = "Filter by class & major",
            description = "Filter by course or major to find exactly what you need."
        ),
        OnboardingPage(
            imageRes = R.drawable.onboarding4,
            title = "Turn unused supplies into cash",
            description = "Quick & easy listing process."
        )
    )

    // Current page status
    var currentPage by remember { mutableStateOf(0) }
    val currentData = pages[currentPage]

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // State to store the preloaded ImageBitmaps
    val bitmaps = remember { mutableStateListOf<ImageBitmap>() }

    // Preload in the background when mounting the Composable
    LaunchedEffect(Unit) {
        // One time IO
        withContext(Dispatchers.IO) {
            pages.forEach { page ->
                val bmp = BitmapFactory
                    .decodeResource(context.resources, page.imageRes)
                    .asImageBitmap()
                bitmaps.add(bmp)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // --- Skip button ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = {
                coroutineScope.launch {
                    PreferencesManager.setOnboardingCompleted(context, true)
                }
                onSkip()
            }) {
                Text(text = "Skip Intro")
            }
        }

        // --- Main content ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // The bitmap if it is already preloaded, if not, a placeholder
            val bitmap: ImageBitmap? = bitmaps.getOrNull(currentPage)
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Onboarding Image",
                    modifier = Modifier
                        .height(200.dp)
                        .fillMaxWidth()
                )
            } else {
                // Placeholder o loader
                Box(
                    modifier = Modifier
                        .height(200.dp)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = currentData.title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = currentData.description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // --- Dots indicator ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            pages.forEachIndexed { index, _ ->
                val isSelected = index == currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                )
            }
        }

        // --- Next / Finish button ---
        Button(
            onClick = {
                if (currentPage == pages.lastIndex) {
                    // Log event and finish
                    FirebaseAnalytics.getInstance(context)
                        .logEvent("onboarding_complete", Bundle())
                    onFinishOnboarding()
                } else {
                    currentPage++
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (currentPage == pages.lastIndex) "Finish" else "Next")
        }
    }
}
