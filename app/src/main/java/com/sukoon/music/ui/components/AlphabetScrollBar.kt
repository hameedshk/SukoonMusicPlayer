package com.sukoon.music.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukoon.music.ui.theme.*

/**
 * A vertical scroll bar displaying letters A-Z for quick navigation.
 * Used for fast scrolling in long lists like Genres, Artists, and Albums.
 */
@Composable
fun AlphabetScrollBar(
    onLetterClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    activeLetter: String? = null
) {
    val alphabet = ('A'..'Z').map { it.toString() } + "#"

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        alphabet.forEach { letter ->
            val isActive = letter == activeLetter
            
            Text(
                text = letter,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onLetterClick(letter) }
                    )
                    .padding(vertical = 1.dp, horizontal = 8.dp),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (isActive) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            )
        }
    }
}
