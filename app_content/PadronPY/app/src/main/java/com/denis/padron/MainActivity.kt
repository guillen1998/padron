package com.denis.padron

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.denis.padron.ui.AppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PadronTheme { AppNavigation() }
        }
    }
}

@Composable
private fun PadronTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background   = Color(0xFF080C14),
            surface      = Color(0xFF0F1623),
            primary      = Color(0xFFD52B1E),
            secondary    = Color(0xFF1565C0),
            onPrimary    = Color.White,
            onSecondary  = Color.White,
            onSurface    = Color.White,
            onBackground = Color.White
        ),
        content = content
    )
}
