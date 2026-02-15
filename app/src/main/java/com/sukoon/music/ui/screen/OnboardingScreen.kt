package com.sukoon.music.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.onSizeChanged
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sukoon.music.R
import com.sukoon.music.data.preferences.PreferencesManager
import com.sukoon.music.ui.permissions.rememberAudioPermissionState
import com.sukoon.music.domain.model.AppTheme
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.sukoon.music.ui.theme.*

/**
 * Onboarding Screen - Two-step setup:
 * 1. Grant music library permission
 * 2. Optional: Enter user's display name
 */
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    preferencesManager: PreferencesManager
) {
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }

    // Username validation: 3-10 alphabets only
    val isUsernameValid = username.isEmpty() || (username.length in 3..10 && username.all { it.isLetter() })
    val usernameError = when {
        username.isNotEmpty() && username.length < 3 -> "Minimum 3 characters required"
        username.isNotEmpty() && username.length > 10 -> "Maximum 10 characters allowed"
        username.isNotEmpty() && !username.all { it.isLetter() } -> "Only alphabets allowed"
        else -> ""
    }

    // Convert to CamelCase (first letter uppercase, rest lowercase)
    val displayUsername = username.replaceFirstChar { it.uppercase() }
        .drop(1).lowercase()
        .let { if (username.isNotEmpty()) username.first().uppercase() + it else "" }

    // Permission state
    val permissionState = rememberAudioPermissionState(
        onPermissionGranted = {},
        onPermissionDenied = {}
    )

  val scrollState = rememberScrollState()
var contentHeightPx by remember { mutableStateOf(0) }
val density = LocalDensity.current

BoxWithConstraints(Modifier.fillMaxSize()) {
    val screenHeight = maxHeight
    val contentHeightDp = with(density) { contentHeightPx.toDp() }
    val shouldScroll = contentHeightDp > screenHeight + 4.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
			  .then(
				if (shouldScroll) Modifier.verticalScroll(scrollState)
				else Modifier
					)
            .padding(24.dp)
			.imePadding()
            .onSizeChanged { contentHeightPx = it.height },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // App branding
        Text(
            text = "Sukoon",
            style = MaterialTheme.typography.screenHeader,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Offline Music Player",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Symbol image
        Image(
            painter = painterResource(R.drawable.symbol),
            contentDescription = "Sukoon Music Player",
            modifier = Modifier
                .size(180.dp)
                .padding(bottom = 24.dp),
            contentScale = ContentScale.Fit
        )

        // Metadata tags
        Text(
            text = "Offline • Private • On-device",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp, top = 12.dp)
        )

        // Step 1: Permission
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Text(
                text = "Step 1: Find Your Music",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "To play your offline songs, Sukoon needs permission to access your device library.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
            
            Button(
                onClick = { permissionState.requestPermission() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !permissionState.hasPermission,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (permissionState.hasPermission) "✓ Permission Granted" else "Grant Permission",
                    style = MaterialTheme.typography.buttonText
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Step 2: Username (Optional)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Text(
                text = "Step 2: Personalize Your Library",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "What name should we use for your playlists and greetings?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )

            TextField(
                value = username,
                onValueChange = { newValue ->
                    // Only allow alphabets, limit to 10 characters
                    val filtered = newValue.filter { it.isLetter() }
                    if (filtered.length <= 10) {
                        username = filtered
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                placeholder = {
                    Text(
                        "Your name",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                shape = RoundedCornerShape(12.dp),
                isError = usernameError.isNotEmpty(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
					unfocusedContainerColor = MaterialTheme.colorScheme.surface,
					focusedIndicatorColor = MaterialTheme.colorScheme.outline,
				unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
				errorIndicatorColor = MaterialTheme.colorScheme.error
                )
            )

            // Error message or info text
            if (usernameError.isNotEmpty()) {
                Text(
                    text = usernameError,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                if (username.isNotEmpty()) {
                    Text(
                        text = "Display name: $displayUsername",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Your name is only stored locally on the device",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Start Listening Button
        Button(
            onClick = {
                scope.launch {
                    try {
                        // Mark onboarding as complete first (waits for flow emission)
                        preferencesManager.setOnboardingCompleted()
                        // Save username in CamelCase if provided (waits for flow emission)
                        if (username.isNotBlank()) {
                            preferencesManager.setUsername(displayUsername)
                        }
                        // Both setOnboardingCompleted() and setUsername() now wait for flow emission
                        // so navigation will happen only after DataStore propagation is complete
                        onOnboardingComplete()
                    } catch (e: Exception) {
                        // Log error but still navigate
                        android.util.Log.e("OnboardingScreen", "Error saving preferences", e)
                        onOnboardingComplete()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = permissionState.hasPermission && isUsernameValid,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
				disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(
                text = "Start Listening",
                style = MaterialTheme.typography.buttonText
            )
        }

        Text(
			text = "Music access is required to use Sukoon",
			style = MaterialTheme.typography.labelSmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			textAlign = TextAlign.Center,
			modifier = Modifier.padding(top = 12.dp)
			)
        Spacer(modifier = Modifier.height(24.dp))
    }
}
}

@Preview
@Composable
fun OnboardingScreenPreview() {
    SukoonMusicPlayerTheme(theme = AppTheme.DARK) {
        Surface(modifier = Modifier.fillMaxSize()) {
            // Note: This won't work in preview because PreferencesManager requires Context
            // But showing the structure
        }
    }
}