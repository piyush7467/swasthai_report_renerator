package com.piyush.swasthaireportgenerator.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * High-performance, theme-adaptive shimmer modifier for Compose.
 * Automatically adapts between Light, Dark, and AMOLED themes without external dependencies.
 */
fun Modifier.shimmerEffect(
    shape: Shape = RoundedCornerShape(8.dp)
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer_anim")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val shimmerColors = listOf(
        surfaceVariant.copy(alpha = 0.35f),
        surfaceVariant.copy(alpha = 0.85f),
        surfaceVariant.copy(alpha = 0.35f)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 400f, translateAnim - 400f),
        end = Offset(translateAnim, translateAnim)
    )

    this
        .clip(shape)
        .background(brush)
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    Box(
        modifier = modifier.shimmerEffect(shape)
    )
}

/**
 * Skeleton placeholder for Dashboard Statistics cards.
 */
@Composable
fun SkeletonStatCard(
    modifier: Modifier = Modifier,
    height: Dp = 90.dp
) {
    Card(
        modifier = modifier.height(height),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBox(modifier = Modifier.size(width = 65.dp, height = 12.dp))
                SkeletonBox(modifier = Modifier.size(20.dp), shape = CircleShape)
            }
            SkeletonBox(modifier = Modifier.size(width = 45.dp, height = 24.dp))
            SkeletonBox(modifier = Modifier.size(width = 80.dp, height = 10.dp))
        }
    }
}

/**
 * Skeleton placeholder for recent worklist drafts on Dashboard.
 */
@Composable
fun SkeletonWorklistItem(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SkeletonBox(modifier = Modifier.size(42.dp), shape = RoundedCornerShape(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SkeletonBox(modifier = Modifier.size(width = 140.dp, height = 16.dp))
                SkeletonBox(modifier = Modifier.size(width = 90.dp, height = 12.dp))
            }
            SkeletonBox(modifier = Modifier.size(width = 60.dp, height = 22.dp), shape = RoundedCornerShape(12.dp))
        }
    }
}

@Composable
fun SkeletonWorklistList(count: Int = 3) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        repeat(count) {
            SkeletonWorklistItem()
        }
    }
}

/**
 * Skeleton placeholder for Patient cards on Patients list screen.
 */
@Composable
fun SkeletonPatientCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Avatar circle
            SkeletonBox(modifier = Modifier.size(46.dp), shape = CircleShape)

            // Info column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkeletonBox(modifier = Modifier.size(width = 130.dp, height = 18.dp))
                    SkeletonBox(modifier = Modifier.size(width = 50.dp, height = 18.dp), shape = RoundedCornerShape(10.dp))
                }
                SkeletonBox(modifier = Modifier.size(width = 100.dp, height = 13.dp))
                SkeletonBox(modifier = Modifier.size(width = 80.dp, height = 12.dp))
            }
        }
    }
}

@Composable
fun SkeletonPatientList(count: Int = 5) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 120.dp)
    ) {
        repeat(count) {
            SkeletonPatientCard()
        }
    }
}

/**
 * Skeleton placeholder for Report cards on Reports list screen.
 */
@Composable
fun SkeletonReportCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top row: ID and Status pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBox(modifier = Modifier.size(width = 90.dp, height = 14.dp))
                SkeletonBox(modifier = Modifier.size(width = 75.dp, height = 22.dp), shape = RoundedCornerShape(12.dp))
            }

            // Patient Name
            SkeletonBox(modifier = Modifier.size(width = 170.dp, height = 20.dp))

            // Test categories
            SkeletonBox(modifier = Modifier.size(width = 220.dp, height = 14.dp))

            // Bottom row: Date & Action icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBox(modifier = Modifier.size(width = 110.dp, height = 12.dp))
                SkeletonBox(modifier = Modifier.size(24.dp), shape = CircleShape)
            }
        }
    }
}

@Composable
fun SkeletonReportList(count: Int = 5) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 120.dp)
    ) {
        repeat(count) {
            SkeletonReportCard()
        }
    }
}

