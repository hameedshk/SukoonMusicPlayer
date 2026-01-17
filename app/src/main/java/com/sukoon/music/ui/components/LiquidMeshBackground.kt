package com.sukoon.music.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.sukoon.music.ui.util.AlbumPalette

@Composable
fun LiquidMeshBackground(
    palette: AlbumPalette,
    songId: Long?,
    modifier: Modifier = Modifier
) {
    // TEST: Generate distinct test colors based on song ID
    val testColorHue = ((songId?.hashCode() ?: 0) % 360).toFloat().coerceIn(0f, 360f)
    val testSolidColor = Color.hsv(testColorHue, 1f, 0.6f)

    Box(modifier = Modifier.fillMaxSize()) {
        // Use Canvas instead of background modifier for more direct control
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(testSolidColor)
        }

        // Test: Display color hex value to verify recomposition
        Text(
            text = "BG: ${testSolidColor.value.toString(16)}",
            modifier = Modifier.align(Alignment.Center),
            color = Color.White,
            fontSize = 10.sp
        )
    }
}
