package com.sukoon.music.ui.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Permission handler for audio file access.
 *
 * Handles version-specific permissions:
 * - Android 13+ (API 33+): READ_MEDIA_AUDIO
 * - Android 12- (API 32-): READ_EXTERNAL_STORAGE
 *
 * Handles first-time request, rationale, permanent denial, and settings return.
 */
@Composable
fun rememberAudioPermissionState(
    onPermissionGranted: () -> Unit = {},
    onPermissionDenied: () -> Unit = {}
): AudioPermissionState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = remember(context) { context.findActivity() }
    val latestOnPermissionGranted by rememberUpdatedState(onPermissionGranted)
    val latestOnPermissionDenied by rememberUpdatedState(onPermissionDenied)

    var showRationale by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var permissionRequested by rememberSaveable { mutableStateOf(false) }

    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember(permission) {
        mutableStateOf(context.hasPermission(permission))
    }

    fun refreshPermissionState(notifyOnGrant: Boolean) {
        val wasGranted = hasPermission
        val isGrantedNow = context.hasPermission(permission)
        hasPermission = isGrantedNow

        if (notifyOnGrant && isGrantedNow && !wasGranted) {
            latestOnPermissionGranted()
        }
    }

    DisposableEffect(lifecycleOwner, permission) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissionState(notifyOnGrant = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isPermanentlyDenied = !hasPermission &&
            permissionRequested &&
            (activity?.let {
                !ActivityCompat.shouldShowRequestPermissionRationale(it, permission)
            } ?: false)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionRequested = true
        refreshPermissionState(notifyOnGrant = false)

        if (isGranted || hasPermission) {
            latestOnPermissionGranted()
        } else {
            val shouldShowRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, permission)
            } ?: false

            if (!shouldShowRationale && permissionRequested) {
                showSettingsDialog = true
            }
            latestOnPermissionDenied()
        }
    }

    if (showRationale) {
        PermissionRationaleDialog(
            onDismiss = { showRationale = false },
            onConfirm = {
                showRationale = false
                permissionLauncher.launch(permission)
            }
        )
    }

    if (showSettingsDialog) {
        PermissionDeniedDialog(
            onDismiss = { showSettingsDialog = false },
            onOpenSettings = {
                showSettingsDialog = false
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        )
    }

    return remember(hasPermission, isPermanentlyDenied, permission) {
        AudioPermissionState(
            hasPermission = hasPermission,
            requestPermission = {
                if (!hasPermission) {
                    val shouldShowRationale = activity?.let {
                        ActivityCompat.shouldShowRequestPermissionRationale(it, permission)
                    } ?: false

                    when {
                        isPermanentlyDenied -> showSettingsDialog = true
                        shouldShowRationale -> showRationale = true
                        else -> permissionLauncher.launch(permission)
                    }
                }
            },
            isPermanentlyDenied = isPermanentlyDenied
        )
    }
}

/**
 * State holder for audio permission.
 */
data class AudioPermissionState(
    val hasPermission: Boolean,
    val requestPermission: () -> Unit,
    val isPermanentlyDenied: Boolean = false
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
                        "Settings > Apps > Sukoon Music > Permissions > Files and media"
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

private fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        permission
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
