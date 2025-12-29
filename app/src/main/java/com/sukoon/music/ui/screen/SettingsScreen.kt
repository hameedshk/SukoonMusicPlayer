package com.sukoon.music.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.sukoon.music.domain.model.AppTheme
import com.sukoon.music.domain.model.AudioQuality
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.viewmodel.SettingsViewModel

/**
 * Enhanced Settings Screen with functional preferences and storage management.
 *
 * Features:
 * - Private Session toggle
 * - Theme selection (Light/Dark/System)
 * - Scan on startup toggle
 * - Library & Folders management
 * - Storage statistics
 * - Clear cache/database operations
 * - App information
 * - Logout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onNavigateToEqualizer: () -> Unit = {},
    onNavigateToExcludedFolders: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()
    val isClearingCache by viewModel.isClearingCache.collectAsStateWithLifecycle()
    val isClearingData by viewModel.isClearingData.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var showAboutDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAudioQualityDialog by remember { mutableStateOf(false) }
    var showCrossfadeDialog by remember { mutableStateOf(false) }
    var showBufferDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearDatabaseDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showMinDurationDialog by remember { mutableStateOf(false) }
    var showRescanDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Permission result handled
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Privacy Section
            item { SettingsSectionHeader(title = "Privacy") }

            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Lock,
                    title = "Private Session",
                    description = "Don't save your listening history",
                    checked = userPreferences.isPrivateSessionEnabled,
                    onCheckedChange = { viewModel.togglePrivateSession() }
                )
            }

            // Appearance Section
            item { SettingsSectionHeader(title = "Appearance") }

            item {
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = "Theme",
                    description = getThemeDescription(userPreferences.theme),
                    onClick = { showThemeDialog = true }
                )
            }

            // Playback Section
            item { SettingsSectionHeader(title = "Playback") }

            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Notifications,
                    title = "Show Notification Controls",
                    description = "Display playback controls in notification",
                    checked = userPreferences.showNotificationControls,
                    onCheckedChange = { viewModel.toggleNotificationControls() }
                )
            }

            item {
                SettingsSwitchItem(
                    icon = Icons.Default.MusicNote,
                    title = "Gapless Playback",
                    description = "Seamless transitions between tracks",
                    checked = userPreferences.gaplessPlaybackEnabled,
                    onCheckedChange = { viewModel.toggleGaplessPlayback() }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.BlurOn,
                    title = "Crossfade Duration",
                    description = if (userPreferences.crossfadeDurationMs == 0) "Disabled" else "${userPreferences.crossfadeDurationMs}ms",
                    onClick = { showCrossfadeDialog = true }
                )
            }

            item {
                SettingsSwitchItem(
                    icon = Icons.Default.VolumeOff,
                    title = "Pause on Headphones Unplugged",
                    description = "Automatically pause when headphones disconnect",
                    checked = userPreferences.pauseOnAudioNoisy,
                    onCheckedChange = { viewModel.togglePauseOnAudioNoisy() }
                )
            }

            item {
                SettingsSwitchItem(
                    icon = Icons.Default.PlayArrow,
                    title = "Resume on Audio Focus",
                    description = "Auto-resume when regaining audio focus",
                    checked = userPreferences.resumeOnAudioFocus,
                    onCheckedChange = { viewModel.toggleResumeOnAudioFocus() }
                )
            }

            // Audio Quality Section
            item { SettingsSectionHeader(title = "Audio Quality") }

            item {
                SettingsItem(
                    icon = Icons.Default.HighQuality,
                    title = "Audio Quality",
                    description = getAudioQualityDescription(userPreferences.audioQuality),
                    onClick = { showAudioQualityDialog = true }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.GraphicEq,
                    title = "Audio Buffer",
                    description = "${userPreferences.audioBufferMs}ms",
                    onClick = { showBufferDialog = true }
                )
            }

            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Equalizer,
                    title = "Audio Normalization (ReplayGain)",
                    description = "Normalize volume across tracks",
                    checked = userPreferences.audioNormalizationEnabled,
                    onCheckedChange = { viewModel.toggleAudioNormalization() }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.GraphicEq,
                    title = "Equalizer",
                    description = "5-band EQ with bass boost and presets",
                    onClick = onNavigateToEqualizer
                )
            }

            // Library & Folders Section
            item { SettingsSectionHeader(title = "Library & Folders") }

            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Refresh,
                    title = "Scan on Startup",
                    description = "Automatically scan for new music when app opens",
                    checked = userPreferences.scanOnStartup,
                    onCheckedChange = { viewModel.toggleScanOnStartup() }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Timer,
                    title = "Minimum Audio Duration",
                    description = "${userPreferences.minimumAudioDuration} seconds",
                    onClick = { showMinDurationDialog = true }
                )
            }

            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Visibility,
                    title = "Show All Audio Files",
                    description = "Show hidden files and short clips",
                    checked = userPreferences.showAllAudioFiles,
                    onCheckedChange = { viewModel.toggleShowAllAudioFiles() }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.FolderOff,
                    title = "Manage Excluded Folders",
                    description = "${userPreferences.excludedFolderPaths.size} folder(s) excluded",
                    onClick = onNavigateToExcludedFolders
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Search,
                    title = "Rescan Library",
                    description = "Manually search for new music files",
                    onClick = { showRescanDialog = true }
                )
            }

            // Permissions Section
            item { SettingsSectionHeader(title = "Permissions") }

            item {
                SettingsItem(
                    icon = Icons.Default.PermMedia,
                    title = "Audio Permission",
                    description = "Manage app permissions",
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                )
            }

            // Storage Section
            item { SettingsSectionHeader(title = "Storage") }

            item {
                SettingsItem(
                    icon = Icons.Default.Storage,
                    title = "Storage Used",
                    description = storageStats?.totalSizeMB() ?: "Calculating...",
                    onClick = { viewModel.loadStorageStats() }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Delete,
                    title = "Clear Cache",
                    description = "Delete album art cache (${storageStats?.cacheSizeMB() ?: "..."})",
                    onClick = { showClearCacheDialog = true },
                    showLoading = isClearingCache
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.DeleteSweep,
                    title = "Clear Recently Played",
                    description = "Remove recently played history",
                    onClick = { showClearHistoryDialog = true }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.DeleteForever,
                    title = "Clear Database",
                    description = "Delete all song metadata (music files stay)",
                    onClick = { showClearDatabaseDialog = true },
                    isDestructive = true,
                    showLoading = isClearingData
                )
            }

            // App Section
            item { SettingsSectionHeader(title = "App") }

            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "About",
                    description = "Version ${viewModel.getAppVersion()}",
                    onClick = { showAboutDialog = true }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Security,
                    title = "Licenses",
                    description = "Open source licenses",
                    onClick = { /* Navigate to licenses */ }
                )
            }

            // Account Section
            item { SettingsSectionHeader(title = "Account") }

            item {
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    title = "Logout",
                    description = "Clear all data and sign out",
                    onClick = { showLogoutDialog = true },
                    isDestructive = true
                )
            }

            // Footer Spacer
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }

        // Dialogs
        if (showAboutDialog) {
            AboutDialog(
                version = viewModel.getAppVersion(),
                packageName = viewModel.getPackageName(),
                onDismiss = { showAboutDialog = false }
            )
        }

        if (showThemeDialog) {
            ThemeSelectionDialog(
                currentTheme = userPreferences.theme,
                onDismiss = { showThemeDialog = false },
                onThemeSelect = { theme ->
                    viewModel.setTheme(theme)
                    showThemeDialog = false
                }
            )
        }

        if (showClearCacheDialog) {
            ConfirmationDialog(
                title = "Clear Cache?",
                message = "This will delete cached album art. Your music files will not be affected.",
                icon = Icons.Default.Warning,
                onDismiss = { showClearCacheDialog = false },
                onConfirm = {
                    viewModel.clearImageCache()
                    showClearCacheDialog = false
                }
            )
        }

        if (showClearDatabaseDialog) {
            ConfirmationDialog(
                title = "Clear Database?",
                message = "This will delete all song metadata, playlists, and lyrics. Your music files will NOT be deleted. You can rescan later.",
                icon = Icons.Default.Warning,
                isDestructive = true,
                onDismiss = { showClearDatabaseDialog = false },
                onConfirm = {
                    viewModel.clearDatabase()
                    showClearDatabaseDialog = false
                }
            )
        }

        if (showClearHistoryDialog) {
            ConfirmationDialog(
                title = "Clear Recently Played?",
                message = "This will remove your recently played history.",
                icon = Icons.Default.History,
                onDismiss = { showClearHistoryDialog = false },
                onConfirm = {
                    viewModel.clearRecentlyPlayed()
                    showClearHistoryDialog = false
                }
            )
        }

        if (showAudioQualityDialog) {
            AudioQualityDialog(
                currentQuality = userPreferences.audioQuality,
                onDismiss = { showAudioQualityDialog = false },
                onQualitySelect = { quality ->
                    viewModel.setAudioQuality(quality)
                    showAudioQualityDialog = false
                }
            )
        }

        if (showCrossfadeDialog) {
            CrossfadeDialog(
                currentDuration = userPreferences.crossfadeDurationMs,
                onDismiss = { showCrossfadeDialog = false },
                onConfirm = { duration ->
                    viewModel.setCrossfadeDuration(duration)
                    showCrossfadeDialog = false
                }
            )
        }

        if (showBufferDialog) {
            BufferDialog(
                currentBuffer = userPreferences.audioBufferMs,
                onDismiss = { showBufferDialog = false },
                onConfirm = { buffer ->
                    viewModel.setAudioBuffer(buffer)
                    showBufferDialog = false
                }
            )
        }

        if (showMinDurationDialog) {
            MinimumDurationDialog(
                currentDuration = userPreferences.minimumAudioDuration,
                onDismiss = { showMinDurationDialog = false },
                onConfirm = { duration ->
                    viewModel.setMinimumAudioDuration(duration)
                    showMinDurationDialog = false
                }
            )
        }

        if (showRescanDialog) {
            ConfirmationDialog(
                title = "Rescan Library?",
                message = "This will scan your device for new music files. It may take a moment.",
                icon = Icons.Default.Search,
                onDismiss = { showRescanDialog = false },
                onConfirm = {
                    viewModel.rescanLibrary()
                    showRescanDialog = false
                }
            )
        }

        if (showLogoutDialog) {
            ConfirmationDialog(
                title = "Logout?",
                message = "This will delete all app data including database, cache, and preferences. Your music files will NOT be deleted.",
                icon = Icons.Default.Warning,
                isDestructive = true,
                onDismiss = { showLogoutDialog = false },
                onConfirm = {
                    viewModel.logout()
                    showLogoutDialog = false
                }
            )
        }
    }
}

@Composable
private fun MinimumDurationDialog(
    currentDuration: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var duration by remember { mutableStateOf(currentDuration) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Minimum Audio Duration") },
        text = {
            Column {
                Text("Exclude files shorter than: $duration seconds")
                Spacer(Modifier.height(16.dp))
                Slider(
                    value = duration.toFloat(),
                    onValueChange = { duration = it.toInt() },
                    valueRange = 0f..120f,
                    steps = 11
                )
                Text(
                    text = "Useful for hiding ringtones, notification sounds, and short voice notes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(duration) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
    showLoading: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isDestructive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isDestructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    }
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            if (showLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun ThemeSelectionDialog(
    currentTheme: AppTheme,
    onDismiss: () -> Unit,
    onThemeSelect: (AppTheme) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Theme") },
        text = {
            Column {
                AppTheme.entries.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = theme == currentTheme,
                            onClick = { onThemeSelect(theme) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when (theme) {
                                AppTheme.LIGHT -> "Light"
                                AppTheme.DARK -> "Dark"
                                AppTheme.SYSTEM -> "System Default"
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    icon: ImageVector,
    isDestructive: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        },
        title = { Text(text = title) },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = if (isDestructive) {
                    ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                } else {
                    ButtonDefaults.textButtonColors()
                }
            ) {
                Text(if (isDestructive) "Delete" else "Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun AboutDialog(
    version: String,
    packageName: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Sukoon Music Player",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Version $version",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "An offline-only local music player with Spotify-like experience.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun AudioQualityDialog(
    currentQuality: AudioQuality,
    onDismiss: () -> Unit,
    onQualitySelect: (AudioQuality) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Audio Quality") },
        text = {
            Column {
                AudioQuality.entries.forEach { quality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = quality == currentQuality,
                            onClick = { onQualitySelect(quality) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = when (quality) {
                                    AudioQuality.LOW -> "Low"
                                    AudioQuality.MEDIUM -> "Medium"
                                    AudioQuality.HIGH -> "High"
                                    AudioQuality.LOSSLESS -> "Lossless"
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = when (quality) {
                                    AudioQuality.LOW -> "128 kbps equivalent"
                                    AudioQuality.MEDIUM -> "192 kbps equivalent"
                                    AudioQuality.HIGH -> "320 kbps equivalent"
                                    AudioQuality.LOSSLESS -> "Original quality"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun CrossfadeDialog(
    currentDuration: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var duration by remember { mutableStateOf(currentDuration) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crossfade Duration") },
        text = {
            Column {
                Text("Duration: ${duration}ms${if (duration == 0) " (Disabled)" else ""}")
                Spacer(Modifier.height(16.dp))
                Slider(
                    value = duration.toFloat(),
                    onValueChange = { duration = it.toInt() },
                    valueRange = 0f..10000f,
                    steps = 19
                )
                Text(
                    text = "Smoothly fade between tracks (0-10 seconds)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(duration) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun BufferDialog(
    currentBuffer: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var buffer by remember { mutableStateOf(currentBuffer) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Audio Buffer Size") },
        text = {
            Column {
                Text("Buffer: ${buffer}ms")
                Spacer(Modifier.height(16.dp))
                Slider(
                    value = buffer.toFloat(),
                    onValueChange = { buffer = it.toInt() },
                    valueRange = 100f..2000f,
                    steps = 18
                )
                Text(
                    text = "Higher values = more stable playback, higher latency",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(buffer) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

private fun getThemeDescription(theme: AppTheme): String = when (theme) {
    AppTheme.LIGHT -> "Light Mode"
    AppTheme.DARK -> "Dark Mode"
    AppTheme.SYSTEM -> "System Default"
}

private fun getAudioQualityDescription(quality: AudioQuality): String = when (quality) {
    AudioQuality.LOW -> "Low (128 kbps)"
    AudioQuality.MEDIUM -> "Medium (192 kbps)"
    AudioQuality.HIGH -> "High (320 kbps)"
    AudioQuality.LOSSLESS -> "Lossless (Original)"
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun SettingsScreenPreview() {
    SukoonMusicPlayerTheme(darkTheme = true) {
        SettingsScreen(onBackClick = {})
    }
}
