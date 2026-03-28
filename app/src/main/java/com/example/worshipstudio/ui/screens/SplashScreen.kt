package com.example.worshipstudio.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.worshipstudio.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onComplete: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2500L)
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0825))
    ) {
        Image(
            painter          = painterResource(R.drawable.splash_screen),
            contentDescription = null,
            modifier         = Modifier.fillMaxSize(),
            contentScale     = ContentScale.Crop
        )
    }
}
