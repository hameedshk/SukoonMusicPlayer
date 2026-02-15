package com.sukoon.music.ui.screen

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import com.sukoon.music.R
import com.sukoon.music.data.preferences.PreferencesManager
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    preferencesManager: PreferencesManager
) {
    val context = LocalContext.current
    val activity = context as Activity
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    /* ================= PERMISSION SYSTEM ================= */

    fun permission(): String =
        if (Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

    fun checkPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, permission()) ==
                PackageManager.PERMISSION_GRANTED

    var hasPermission by remember { mutableStateOf(checkPermission()) }
    var permanentlyDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val deniedForever =
            !granted && !ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                permission()
            )
        hasPermission = granted
        permanentlyDenied = deniedForever
    }

    fun requestPermission() {
        permissionLauncher.launch(permission())
    }

    /* ---- Auto detect when returning from Settings ---- */

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = checkPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    /* ================= USERNAME VALIDATION ================= */

    val trimmedName = username.trim()

    val isUsernameValid =
        trimmedName.isEmpty() ||
                (trimmedName.length in 2..20 &&
                        trimmedName.all { it.isLetter() || it.isWhitespace() })

    val usernameError = when {
        trimmedName.isNotEmpty() && trimmedName.length < 2 ->
            "Name must be at least 2 characters"
        trimmedName.length > 20 ->
            "Name cannot exceed 20 characters"
        trimmedName.any { !(it.isLetter() || it.isWhitespace()) } ->
            "Only letters and spaces allowed"
        else -> ""
    }

    val displayUsername =
        trimmedName.split(" ").joinToString(" ") {
            it.replaceFirstChar { c -> c.uppercase() }
        }

    /* ================= NO-SCROLL WHEN NOT NEEDED ================= */

    val scrollState = rememberScrollState()
    var contentHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        val contentHeightDp = with(density) { contentHeightPx.toDp() }
        val shouldScroll = contentHeightDp > screenHeight

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
                .then(
                    if (shouldScroll) Modifier.verticalScroll(scrollState)
                    else Modifier
                )
                .onSizeChanged { contentHeightPx = it.height },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(12.dp))

            /* ================= BRAND ================= */

            Text(
                "Sukoon",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                "Your calm, offline music space",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Image(
               painter = painterResource(R.drawable.symbol),
                contentDescription = "Sukoon logo",
                modifier = Modifier.size(170.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(20.dp))

            /* ================= PERMISSION ================= */

            Text("Bring your music into Sukoon(calmness)", style = MaterialTheme.typography.titleMedium)

            Text(
                "Allow Sukoon to scan your device and show your offline songs.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
            )

            Button(
                onClick = { requestPermission() },
                enabled = !hasPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    when {
                        hasPermission -> "✓ Music Access Granted"
                        permanentlyDenied -> "Permission Blocked"
                        else -> "Allow Music Access"
                    }
                )
            }

            if (permanentlyDenied) {
                TextButton(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    }
                ) {
                    Text("Open Settings to enable permission")
                }
            }

            Spacer(Modifier.height(32.dp))

            /* ================= USERNAME ================= */

            Text("Personalize (Optional)", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = usernameError.isNotEmpty(),
                placeholder = { Text("Your name") },

                colors = TextFieldDefaults.outlinedTextFieldColors(
        focusedBorderColor = MaterialTheme.colorScheme.outline,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
        errorBorderColor = MaterialTheme.colorScheme.error,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface
    )
            )

            if (usernameError.isNotEmpty()) {
                Text(
                    usernameError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 6.dp)
                )
            } else if (displayUsername.isNotEmpty()) {
                Text(
                    "Display name: $displayUsername",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
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

            /* ================= START BUTTON ================= */

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
                enabled = hasPermission && isUsernameValid && !isSaving,
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
}