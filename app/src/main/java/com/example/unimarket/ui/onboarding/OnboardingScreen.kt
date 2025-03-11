package com.example.unimarket.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.unimarket.R

// Composable function for the Onboarding screen
@Composable
fun OnboardingScreen(
    onNavigateToLogin: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {
    // When the state indicates navigation, trigger the callback
    if (viewModel.navigateToLogin) {
        LaunchedEffect(key1 = Unit) {
            onNavigateToLogin()
        }
    }
    // UI layout using a Column
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Display the onboarding image
        Image(
            painter = painterResource(id = R.drawable.sample_image), // Replace with your actual image resource
            contentDescription = "Onboarding Image",
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(8.dp)) // Apply rounded corners
        )
        Spacer(modifier = Modifier.height(24.dp))
        // Title text
        Text(
            text = "Welcome to UniMarket!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Description text
        Text(
            text = "Find & offer academic materials quickly and easily.",
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        // "Get Started" button
        Button(
            onClick = { viewModel.onGetStartedClicked() },
            modifier = Modifier.height(48.dp)
        ) {
            Text(text = "Get Started")
        }
    }
}
