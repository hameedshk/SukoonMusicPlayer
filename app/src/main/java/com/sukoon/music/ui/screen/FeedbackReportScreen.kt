package com.sukoon.music.ui.screen

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.theme.accent
import com.sukoon.music.ui.theme.SpacingLarge
import com.sukoon.music.ui.theme.SpacingMedium
import com.sukoon.music.ui.theme.SpacingSmall

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackReportScreen(
    onBackClick: () -> Unit
) {
    val categories = listOf("Lyric", "Ads", "File not found", "File can't play", "Music stops", "App Slow", "Others")
    var selectedCategory by rememberSaveable { mutableStateOf(categories.first()) }
    var details by rememberSaveable { mutableStateOf("") }
    var consentGiven by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val accentTokens = accent()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Tell us the problem") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = SpacingLarge, vertical = SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(SpacingMedium)
        ) {
            Text(
                text = "Select the issue",
                style = MaterialTheme.typography.titleMedium
            )
            categories.chunked(3).forEach { row -> 
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SpacingMedium)
                ) {
                    row.forEachIndexed { index, chip ->
                        val isSelected = chip == selectedCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = chip },
                            label = {
                                Text(
                                    text = chip,
                                    color = if (isSelected) accentTokens.onDark else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = if (isSelected) accentTokens.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        if (index != row.lastIndex) {
                            Spacer(modifier = Modifier.width(SpacingSmall))
                        }
                    }
                }
            }

            Text(
                text = "Details *",
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(
                value = details,
                onValueChange = {
                    if (it.length <= 500) details = it
                },
                placeholder = { Text("Explain what happened, or suggestions.") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                maxLines = 6,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            Text(
                text = "${details.length}/500",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End)
            )

            Text(
                text = "Screenshots or videos (Optional)",
                style = MaterialTheme.typography.titleMedium
            )
            Surface(
                modifier = Modifier
                    .height(80.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                color = MaterialTheme.colorScheme.surfaceVariant,
                onClick = {
                    Toast.makeText(context, "Upload not supported yet", Toast.LENGTH_SHORT).show()
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add screenshot",
                        tint = accentTokens.primary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SpacingSmall)
            ) {
                Checkbox(
                    checked = consentGiven,
                    onCheckedChange = { consentGiven = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = accentTokens.primary
                    )
                )
                Text(
                    text = "To continue, Sukoon Music Player will share your personal data with our customer service provider for support. You can review Terms of Service and Privacy Policy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    Toast.makeText(context, "Thanks! We'll follow up shortly.", Toast.LENGTH_LONG).show()
                    onBackClick()
                },
                enabled = details.isNotBlank() && consentGiven,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentTokens.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    contentColor = Color.White
                )
            ) {
                Text("Submit")
            }
        }
    }
}

@Composable
fun FeedbackReportScreenPreview() {
    SukoonMusicPlayerTheme {
        FeedbackReportScreen(onBackClick = {})
    }
}
