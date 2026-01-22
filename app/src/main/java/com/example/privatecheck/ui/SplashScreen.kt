package com.example.privatecheck.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.privatecheck.R

// Exact color from ic_check_logo.xml
private val LogoGreen = Color(0xFF4CAF50)

@Composable
fun SplashScreen(
    onAnimationFinished: () -> Unit
) {
    // 1. Initial Scale = 0f (Invisible/Tiny)
    val scale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Animation: Scale Up 0f -> 1f over 700ms
        // This creates a "Zoom In" effect for the icon entrance
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
        
        // Navigation (Main Activity handles the 0.3s Fade Out transition)
        onAnimationFinished()
    }

    // Green Background fills the screen, matching the Icon's own background perfectly
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LogoGreen),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_check_logo),
            contentDescription = "Logo",
            modifier = Modifier
                .size(120.dp)
                .scale(scale.value), // Bind scale animation
            contentScale = ContentScale.Fit
        )
    }
}
