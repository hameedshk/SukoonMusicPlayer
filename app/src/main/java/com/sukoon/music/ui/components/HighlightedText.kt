package com.sukoon.music.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun HighlightedText(
    text: String,
    query: String,
    style: TextStyle,
    color: Color,
    highlightColor: Color = MaterialTheme.colorScheme.primary
) {
    if (query.isBlank()) {
        Text(text = text, style = style, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        return
    }

    val spanStyles = remember(text, query) {
        val spans = mutableListOf<AnnotatedString.Range<SpanStyle>>()
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        var startIndex = 0
        
        while (startIndex < text.length) {
            val index = lowerText.indexOf(lowerQuery, startIndex)
            if (index == -1) break
            
            spans.add(
                AnnotatedString.Range(
                    SpanStyle(
                        color = highlightColor,
                        fontWeight = FontWeight.Bold
                    ),
                    start = index,
                    end = index + query.length
                )
            )
            startIndex = index + query.length
        }
        spans
    }

    val annotatedString = AnnotatedString(
        text = text,
        spanStyles = spanStyles
    )

    Text(
        text = annotatedString,
        style = style,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
