package com.example.unimarket.ui.views

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.unimarket.R
import com.example.unimarket.data.PreferencesManager
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.launch

// Data class for onboarding pages
data class OnboardingPage(
    val imageRes: Int, val title: String, val description: String
)

@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit, onSkip: () -> Unit
) {
    // List of pages (slides) for the onboarding
    val pages = listOf(
        OnboardingPage(
            imageRes = R.drawable.onboarding1,
            title = "Welcome to UniMarket",
            description = "Post your items with a few steps, set your price, and connect with buyers instantly. Buy and sell the best school supplies with your app!\nWhat are you waiting for!"
        ), OnboardingPage(
            imageRes = R.drawable.onboarding2,
            title = "Buy and sell school supplies with ease",
            description = "Find the supplies ypu need or sell what you no longer use. Whether you're looking for textbooks, calculators, or art materials, we've got you covered!."
        ), OnboardingPage(
            imageRes = R.drawable.onboarding3,
            title = "Filter by class & major",
            description = "No more endless scrolling--just filter by course or major to find exactly what you need in seconds. Search by subject or specific class, discover items recommended for your major"
        ), OnboardingPage(
            imageRes = R.drawable.onboarding4,
            title = "Turn your unused supplies into cash",
            description = "Post your items with a few taps, set your price, and connect with buyers instantly. Quick & easy listing process."
        )
    )

    var currentPage by remember { mutableStateOf(0) }
    val currentData = pages[currentPage]

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Skip Intro button (top-right)
        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = {
                    // Mark onboarding as completed and skip
                    coroutineScope.launch {
                        PreferencesManager.setOnboardingCompleted(context, true)
                    }
                    onSkip()
                }) {
                Text(text = "Skip Intro")
            }
        }

        // Main content for the current slide
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Display onboarding image
            Image(
                painter = painterResource(id = currentData.imageRes),
                contentDescription = "Onboarding Image",
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = currentData.title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = currentData.description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Dots indicator for pages
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
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                )
            }
        }

        // Next Button
        Button(
            onClick = {
                if (currentPage == pages.lastIndex) {
                    // Mark onboarding as completed on last page
                    FirebaseAnalytics.getInstance(context)
                        .logEvent("onboarding_complete", Bundle())
                    onFinishOnboarding()
                } else {
                    currentPage++
                }
            }, modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (currentPage == pages.lastIndex) "Next" else "Next")
        }
    }
}