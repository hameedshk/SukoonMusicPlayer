package com.sukoon.music.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Glassmorphic card with semi-transparent background and optional blur effect.
 * - enableBlur=true (default): Full glassmorphism with blur on API 31+, alpha fallback on older APIs
 * - enableBlur=false: Only semi-transparent background with rounded corners, no blur
 *
 * Use cases:
 * - enableBlur=true: Control sections, secondary content cards
 * - enableBlur=false: Album art, images that should remain sharp
 */
@Composable
fun GlassCard(
	modifier: Modifier = Modifier,
	enableBlur: Boolean = true,
	elevation: androidx.compose.ui.unit.Dp = 0.dp,
	content: @Composable () -> Unit
) {
	val baseColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
	val blurMod = if (enableBlur) {
		// Blur enabled: Apply API-aware blur effect
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			// API 31+: Apply blur effect
			modifier.blur(radiusX = 16.dp, radiusY = 16.dp)
		} else {
			// API <31: Use alpha-based transparency fallback
			modifier.alpha(0.95f)
		}
	} else {
		// Blur disabled: Keep original modifier (no blur, no alpha modification)
		modifier
	}

	Surface(
		modifier = blurMod
			.background(baseColor, RoundedCornerShape(16.dp)),
		color = Color.Transparent,
		shape = RoundedCornerShape(16.dp),
		shadowElevation = elevation
	) {
		content()
	}
}

/**
 * Semi-transparent blurred background for album art overlays.
 */
@Composable
fun BlurredBackground(
	modifier: Modifier = Modifier,
	content: @Composable () -> Unit
) {
	val bgColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f)
	val blurMod = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
		modifier.blur(radiusX = 24.dp, radiusY = 24.dp)
	} else {
		modifier.alpha(0.9f)
	}

	Surface(
		modifier = blurMod.background(bgColor),
		color = Color.Transparent
	) {
		content()
	}
}

/**
 * Elevated glass button with rounded corners and shadow depth.
 */
@Composable
fun GlassButton(
	modifier: Modifier = Modifier,
	backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
	onClick: () -> Unit,
	content: @Composable () -> Unit
) {
	Surface(
		modifier = modifier,
		onClick = onClick,
		shape = RoundedCornerShape(12.dp),
		color = backgroundColor,
		shadowElevation = 8.dp
	) {
		content()
	}
}
