package com.sukoon.music.ui.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import com.sukoon.music.R
import com.sukoon.music.data.preferences.PreferencesManager
import com.sukoon.music.ui.permissions.rememberAudioPermissionState
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    preferencesManager: PreferencesManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    val permissionState = rememberAudioPermissionState(
        onPermissionGranted = {},
        onPermissionDenied = {}
    )

    /* ---------------- Username Validation ---------------- */

    val trimmedName = username.trim()

    val isUsernameValid =
        trimmedName.isEmpty() || (trimmedName.length in 2..20 && trimmedName.all { it.isLetter() || it.isWhitespace() })

    val usernameError = when {
        trimmedName.isNotEmpty() && trimmedName.length < 2 -> "Name must be at least 2 characters"
        trimmedName.length > 20 -> "Name cannot exceed 20 characters"
        trimmedName.any { !(it.isLetter() || it.isWhitespace()) } -> "Only letters and spaces allowed"
        else -> ""
    }

    val displayUsername =
        trimmedName.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }

    /* ---------------- UI ---------------- */

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(Modifier.height(12.dp))

        /* ---------- Branding ---------- */

        Text(
            text = "Sukoon",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Your calm, offline music space",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Image(
            painter = painterResource(R.drawable.symbol),
            contentDescription = "Sukoon logo",
            modifier = Modifier.size(170.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(Modifier.height(28.dp))

        /* ---------- Permission Section ---------- */

        Text(
            text = "Access Your Music",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Allow Sukoon to scan your device and instantly show your offline songs.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
            textAlign = TextAlign.Center
        )

        Button(
            onClick = { permissionState.requestPermission() },
            enabled = !permissionState.hasPermission,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                if (permissionState.hasPermission) "✓ Music Access Granted"
                else "Allow Music Access"
            )
        }

        if (!permissionState.hasPermission) {
            TextButton(
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    context.startActivity(intent)
                }
            ) {
                Text("Permission denied? Open Settings")
            }
        }

        Spacer(Modifier.height(32.dp))

        /* ---------- Username Section ---------- */

        Text(
            text = "Personalize (Optional)",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "What name should Sukoon use for greetings and playlists?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
            textAlign = TextAlign.Center
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = usernameError.isNotEmpty(),
            placeholder = { Text("Your name") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { }),
            shape = RoundedCornerShape(12.dp)
        )

        if (usernameError.isNotEmpty()) {
            Text(
                usernameError,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 6.dp)
            )
        } else if (displayUsername.isNotEmpty()) {
            Text(
                "Display name: $displayUsername",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 6.dp)
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "Stored only on your device",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(36.dp))

        /* ---------- Start Button ---------- */

        Button(
            onClick = {
                scope.launch {
                    isSaving = true
                    try {
                        preferencesManager.setOnboardingCompleted()
                        if (displayUsername.isNotBlank()) {
                            preferencesManager.setUsername(displayUsername)
                        }
                    } finally {
                        onOnboardingComplete()
                    }
                }
            },
            enabled = permissionState.hasPermission && isUsernameValid && !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text("Start Listening")
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}