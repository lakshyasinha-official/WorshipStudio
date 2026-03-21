package com.example.worshipstudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.worshipstudio.navigation.AppNavigation
import com.example.worshipstudio.ui.theme.WorshipStudioTheme
import com.example.worshipstudio.utils.AppTheme
import com.example.worshipstudio.utils.ThemeStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            var appTheme by remember { mutableStateOf(ThemeStore.load(context)) }

            WorshipStudioTheme(appTheme = appTheme) {
                Box(modifier = Modifier.fillMaxSize()) {

                    // ── Background layer — changes per theme ──────────────────
                    when (appTheme) {
                        AppTheme.NIGHTFALL -> {
                            Image(
                                painter            = painterResource(R.drawable.app_bg),
                                contentDescription = null,
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier.fillMaxSize()
                            )
                        }
                        AppTheme.DAWN_MIST -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFFFDF6EE),
                                                Color(0xFFFAE8D8),
                                                Color(0xFFF2D9E8),
                                                Color(0xFFE8D5F0)
                                            )
                                        )
                                    )
                            )
                        }
                        AppTheme.HOLY_LIGHT -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFFFAFCFF),
                                                Color(0xFFE8F3FF),
                                                Color(0xFFD6EAFF),
                                                Color(0xFFE9F5F0)
                                            )
                                        )
                                    )
                            )
                        }
                        AppTheme.SANCTUARY -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFFFDFBF7),
                                                Color(0xFFEEF5EE),
                                                Color(0xFFE4EEF7),
                                                Color(0xFFEDE8F6)
                                            )
                                        )
                                    )
                            )
                        }
                    }

                    // ── App content renders on top ─────────────────────────────
                    AppNavigation(
                        currentTheme  = appTheme,
                        onThemeChange = { selected ->
                            ThemeStore.save(context, selected)
                            appTheme = selected
                        }
                    )
                }
            }
        }
    }
}
