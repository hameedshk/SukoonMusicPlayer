package com.sukoon.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sukoon.music.ui.theme.surfaceLevel2Gradient

/**
 * Reusable AlertDialog wrapper with gradient surface styling.
 *
 * Applies the Level 2 gradient background to match the new design system,
 * ensuring visual consistency across all dialogs in the app.
 *
 * @param onDismissRequest Callback when dialog is dismissed
 * @param title Dialog title composable
 * @param text Optional dialog text/content composable
 * @param confirmButton Action button composable (usually positive action)
 * @param dismissButton Optional action button composable (usually negative action)
 * @param modifier Optional modifier for the dialog container
 */
@Composable
fun GradientAlertDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable (() -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        text = text,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        containerColor = Color.Transparent,
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .surfaceLevel2Gradient()
    )
}
