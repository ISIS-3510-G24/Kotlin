package com.example.unimarket.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PersonalizationScreen(
    onFinishPersonalization: () -> Unit
) {
    // Example list of interests (puedes personalizar esta lista)
    val interests = listOf(
        "Sell Items",
        "Turbo Delivery",
        "Buying Major Specific Materials",
        "Buying Class Specific Materials",
        "Extra Curricular Supplies",
        "School Supplies Exchange",
        "Advanced Browsing",
        "Everything",
    )

    // State for selected interests
    val selectedInterests = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Personalize your experience",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Choose your interests.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // List of interests with checkboxes
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(interests) { interest ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = interest)
                    Checkbox(
                        checked = selectedInterests.contains(interest),
                        onCheckedChange = { checked ->
                            if (checked) {
                                selectedInterests.add(interest)
                            } else {
                                selectedInterests.remove(interest)
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Next Button to finish personalization
        Button(
            onClick = {
                // Optionally save preferences here
                onFinishPersonalization()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Next")
        }
    }
}