package com.example.worshipstudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.platform.LocalContext
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
                    val bgColors = when (appTheme) {
                        AppTheme.NIGHTFALL -> listOf(Color(0xFF0D1311), Color(0xFF070B09))
                        AppTheme.DAWN_MIST -> listOf(Color(0xFFF5FAF7), Color(0xFFEAF2ED))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(bgColors))
                    )

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
