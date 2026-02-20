package com.sukoon.music.ui.screen.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sukoon.music.R
import com.sukoon.music.domain.model.AppTheme
import kotlinx.coroutines.delay

/**
 * Theme preview card selector with 3 theme options.
 *
 * Features:
 * - Visual preview cards for Light/Dark/AMOLED
 * - Selection indicator
 * - Debounced updates to prevent recomposition storms
 */
@Composable
fun ThemePreviewCard(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingTheme by remember { mutableStateOf<AppTheme?>(null) }

    LaunchedEffect(pendingTheme) {
        if (pendingTheme != null) {
            delay(300) // Debounce 300ms
            onThemeSelected(pendingTheme!!)
            pendingTheme = null
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                ThemePreviewItem(
                    theme = AppTheme.LIGHT,
                    label = stringResource(R.string.settings_theme_light),
                    isSelected = currentTheme == AppTheme.LIGHT,
                    backgroundColor = Color.White,
                    textColor = Color.Black,
                    onSelect = { pendingTheme = AppTheme.LIGHT }
                )
            }

            item {
                ThemePreviewItem(
                    theme = AppTheme.DARK,
                    label = stringResource(R.string.settings_theme_dark),
                    isSelected = currentTheme == AppTheme.DARK,
                    backgroundColor = Color(0xFF121212),
                    textColor = Color.White,
                    onSelect = { pendingTheme = AppTheme.DARK }
                )
            }

            item {
                ThemePreviewItem(
                    theme = AppTheme.AMOLED,
                    label = stringResource(R.string.settings_theme_amoled),
                    isSelected = currentTheme == AppTheme.AMOLED,
                    backgroundColor = Color.Black,
                    textColor = Color.White,
                    onSelect = { pendingTheme = AppTheme.AMOLED }
                )
            }
        }
    }
}

@Composable
private fun ThemePreviewItem(
    theme: AppTheme,
    label: String,
    isSelected: Boolean,
    backgroundColor: Color,
    textColor: Color,
    onSelect: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onSelect)
            .width(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    color = backgroundColor,
                    shape = MaterialTheme.shapes.medium
                )
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.medium
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.settings_theme_preview_sample),
                color = textColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ThemePreviewCardPreview() {
    ThemePreviewCard(
        currentTheme = AppTheme.DARK,
        onThemeSelected = {}
    )
}
