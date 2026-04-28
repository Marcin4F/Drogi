package com.example.drogi.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// zmiana motywu
@Composable
fun ThemeToggleIcon(isDarkTheme: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        if (isDarkTheme) {
            Icon(
                imageVector = Icons.Default.LightMode,
                contentDescription = "Jasny motyw",
                tint = Color(0xFFFFD700)
            )
        } else {
            Icon(
                imageVector = Icons.Default.DarkMode,
                contentDescription = "Ciemny motyw",
                tint = Color(0xFF1A237E)
            )
        }
    }
}