package com.example.worshipstudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.worshipstudio.navigation.AppNavigation
import com.example.worshipstudio.ui.theme.WorshipStudioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorshipStudioTheme {
                AppNavigation()
            }
        }
    }
}
