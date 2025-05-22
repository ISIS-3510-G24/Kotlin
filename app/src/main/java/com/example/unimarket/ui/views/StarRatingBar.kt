package com.example.unimarket.ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun StarRatingBar(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    starCount: Int = 5,
    starSize: Dp = 32.dp,
) {
    Row(modifier = Modifier) {
        for (i in 1..starCount) {
            Icon(
                imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = "Star $i",
                modifier = modifier
                    .size(starSize)
                    .clickable { onRatingChange(i) }
            )
        }
    }
}