package com.sukoon.music.ui.permissions

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.sukoon.music.ui.theme.*

/**
 * Permission handler for audio file access.
 *
 * Handles version-specific permissions:
 * - Android 13+ (API 33+): READ_MEDIA_AUDIO
 * - Android 12- (API 32-): READ_EXTERNAL_STORAGE
 *
 * Shows rationale dialogs and handles denied states.
 */
@Composable
fun rememberAudioPermissionState(
    onPermissionGranted: () -> Unit = {},
    onPermissionDenied: () -> Unit = {}
): AudioPermissionState {
    val context = LocalContext.current
    var showRationale by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var permissionRequested by remember { mutableStateOf(false) }

    // Determine which permission to request based on Android version
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    // Check if permission is already granted
    val isPermissionGranted = remember(permissionRequested) {
        ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PermissionChecker.PERMISSION_GRANTED
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionRequested = true
        if (isGranted) {
            onPermissionGranted()
        } else {
            // Permission denied - show settings dialog
            showSettingsDialog = true
            onPermissionDenied()
        }
    }

    // Rationale dialog
    if (showRationale) {
        PermissionRationaleDialog(
            onDismiss = { showRationale = false },
            onConfirm = {
                showRationale = false
                permissionLauncher.launch(permission)
            }
        )
    }

    // Settings redirect dialog
    if (showSettingsDialog) {
        PermissionDeniedDialog(
            onDismiss = { showSettingsDialog = false },
            onOpenSettings = {
                showSettingsDialog = false
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )
    }

    return remember(isPermissionGranted) {
        AudioPermissionState(
            hasPermission = isPermissionGranted,
            requestPermission = {
                if (!isPermissionGranted) {
                    // Show rationale first
                    showRationale = true
                }
            }
        )
    }
}

/**
 * State holder for audio permission.
 */
data class AudioPermissionState(
    val hasPermission: Boolean,
    val requestPermission: () -> Unit
)

@Composable
private fun PermissionRationaleDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permission Required") },
        text = {
            Text(
                "Sukoon Music needs access to your audio files to scan and play your local music library. " +
                        "Without this permission, the app cannot discover or play songs stored on your device."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Grant Permission")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now")
            }
        }
    )
}

@Composable
private fun PermissionDeniedDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permission Denied") },
        text = {
            Text(
                "Audio file access was denied. To use Sukoon Music, please enable the permission in Settings.\n\n" +
                        "Settings → Apps → Sukoon Music → Permissions → Files and media"
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
