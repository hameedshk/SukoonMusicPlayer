package com.sukoon.music.ui.screen.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sukoon.music.R
import com.sukoon.music.domain.model.AccentProfile
import kotlinx.coroutines.delay
import kotlin.math.sqrt

/**
 * Accent color selector with batched updates.
 *
 * Features:
 * - Color swatch selection
 * - Selection indicator
 * - Batched updates (300ms debounce) to prevent recomposition storms
 * - TalkBack support
 */
@Composable
fun AccentColorSelector(
    currentProfile: AccentProfile,
    onProfileSelected: (AccentProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingProfile by remember { mutableStateOf<AccentProfile?>(null) }

    LaunchedEffect(pendingProfile) {
        if (pendingProfile != null) {
            delay(300) // Debounce 300ms
            onProfileSelected(pendingProfile!!)
            pendingProfile = null
        }
    }

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        item {
            ColorSwatch(
                color = AccentProfile.Teal.accentPrimary,
                label = stringResource(R.string.settings_accent_teal),
                isSelected = currentProfile == AccentProfile.Teal,
                onSelect = { pendingProfile = AccentProfile.Teal }
            )
        }

        item {
            ColorSwatch(
                color = AccentProfile.SteelBlue.accentPrimary,
                label = stringResource(R.string.settings_accent_steel_blue),
                isSelected = currentProfile == AccentProfile.SteelBlue,
                onSelect = { pendingProfile = AccentProfile.SteelBlue }
            )
        }

        item {
            ColorSwatch(
                color = AccentProfile.SoftCyan.accentPrimary,
                label = stringResource(R.string.settings_accent_soft_cyan),
                isSelected = currentProfile == AccentProfile.SoftCyan,
                onSelect = { pendingProfile = AccentProfile.SoftCyan }
            )
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    label: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(color = color, shape = MaterialTheme.shapes.small)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                shape = MaterialTheme.shapes.small
            )
            .clickable(onClick = onSelect),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = if (isColorLight(color)) Color.Black else Color.White
            )
        }
    }
}

private fun isColorLight(color: Color): Boolean {
    val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    return luminance > 0.5f
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun AccentColorSelectorPreview() {
    AccentColorSelector(
        currentProfile = AccentProfile.Teal,
        onProfileSelected = {}
    )
}
