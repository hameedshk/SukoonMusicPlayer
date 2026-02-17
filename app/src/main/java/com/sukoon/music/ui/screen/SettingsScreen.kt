package com.sukoon.music.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.sukoon.music.domain.model.AppTheme
import com.sukoon.music.domain.model.AudioQuality
import com.sukoon.music.domain.model.AccentProfile
import com.sukoon.music.data.premium.PremiumManager
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.components.GradientAlertDialog
import com.sukoon.music.ui.components.PremiumBanner
import com.sukoon.music.ui.components.SettingsGroupCard
import com.sukoon.music.ui.components.SettingsGroupRow
import com.sukoon.music.ui.components.SettingsRowModel
import com.sukoon.music.ui.components.ValuePlacement
import com.sukoon.music.ui.screen.settings.components.PremiumDialog
import com.sukoon.music.ui.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.flowOf
import com.sukoon.music.ui.theme.*
import com.sukoon.music.ui.analytics.AnalyticsEntryPoint
import dagger.hilt.android.EntryPointAccessors

/**
 * Settings Screen with vertical sectioned list layout.
 *
 * Features:
 * - Premium banner at top (dismissible)
 * - Vertical sections for organized settings
 * - All existing functionality preserved
 * - Improved UX vs tabs (single scroll surface, better Premium visibility)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onNavigateToPlaylists: () -> Unit = {},
    onNavigateToSongs: () -> Unit = {},
    onNavigateToEqualizer: () -> Unit = {},
    onNavigateToExcludedFolders: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToFeedbackReport: () -> Unit = {},
    onNavigateToRestorePlaylist: () -> Unit = {},
    premiumManager: PremiumManager? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()
    val isLoadingStats by viewModel.isLoadingStats.collectAsStateWithLifecycle()
    val isClearingCache by viewModel.isClearingCache.collectAsStateWithLifecycle()
    val isClearingData by viewModel.isClearingData.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val premiumBannerDismissed by viewModel.premiumBannerDismissed.collectAsStateWithLifecycle()
    val billingState by viewModel.billingState.collectAsStateWithLifecycle()
    val isPremium by remember {
        if (premiumManager != null) {
            premiumManager.isPremiumUser
        } else {
            flowOf(false)
        }
    }.collectAsStateWithLifecycle(false)

    val context = LocalContext.current
    val appContext = context.applicationContext
    val analyticsTracker = remember(appContext) {
        runCatching {
            EntryPointAccessors.fromApplication(
                appContext,
                AnalyticsEntryPoint::class.java
            ).analyticsTracker()
        }.getOrNull()
    }
    val coroutineScope = rememberCoroutineScope()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showAccentDialog by remember { mutableStateOf(false) }
    var showAudioQualityDialog by remember { mutableStateOf(false) }
    var showCrossfadeDialog by remember { mutableStateOf(false) }
    var showBufferDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearDatabaseDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showMinDurationDialog by remember { mutableStateOf(false) }
    var showRescanDialog by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Permission result handled
    }

    val accentTokens = accent()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
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
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = SpacingLarge),
            verticalArrangement = Arrangement.spacedBy(SpacingMedium)
        ) {
            item {
                PremiumBanner(
                    isPremium = isPremium,
                    isDismissed = premiumBannerDismissed,
                    onDismiss = { viewModel.dismissPremiumBanner() },
                    onClick = { showPremiumDialog = true },
                    modifier = Modifier.padding(horizontal = SpacingLarge)
                )
            }
            // item {
            //     SettingsGroupCard(
            //         modifier = Modifier.padding(horizontal = SpacingLarge),
            //         rows = listOf(
            //             SettingsRowModel(
            //                 icon = Icons.Default.CloudUpload,
            //                 title = "Backup & restore",
            //                 value = "Log in",
            //                 onClick = { showComingSoonToast(context, "Backup & restore coming soon") }
            //             ),
            //             SettingsRowModel(
            //                 icon = Icons.Default.LibraryMusic,
            //                 title = "Playlists and songs",
            //                 onClick = onNavigateToPlaylists
            //             ),
            //             SettingsRowModel(
            //                 icon = Icons.Default.PlayArrow,
            //                 title = "Playtime",
            //                 onClick = { showComingSoonToast(context, "Playtime coming soon") }
            //             )
            //         )
            //     )
            // }
            item {
                SettingsGroupCard(
                    modifier = Modifier.padding(horizontal = SpacingLarge),
                    rows = listOf(
                        SettingsRowModel(
                            icon = Icons.Default.Palette,
                            title = "Theme",
                            value = getThemeDescription(userPreferences.theme),
                            onClick = { showThemeDialog = true }
                        ),                     
                        SettingsRowModel(
                            icon = Icons.Default.ColorLens,
                            title = "Accent Color",
                            value = userPreferences.accentProfile.label,
                            onClick = { showAccentDialog = true }
                        ),
                        SettingsRowModel(
                            icon = Icons.Default.Timer,
                            title = "Sleep timer",
                            value = getSleepTimerLabel(userPreferences.sleepTimerTargetTimeMs),
                            onClick = { showComingSoonToast(context, "Sleep timer coming soon") }
                        ),
                        SettingsRowModel(
                            icon = Icons.Default.WorkspacePremium,
                            title = "Remove ads",
                            value = if (isPremium) "Premium active" else "48% OFF",
                            valueColor = accentTokens.active,
                            onClick = {
                                analyticsTracker?.logEvent(
                                    name = "remove_ads_tap",
                                    params = mapOf("source" to "settings")
                                )
                                showPremiumDialog = true
                            }
                        )
                    )
                )
            }
            item {
                SettingsGroupCard(
                    modifier = Modifier.padding(horizontal = SpacingLarge),
                    rows = listOf(
                        // SettingsRowModel(
                        //     icon = Icons.Default.VisibilityOff,
                        //     title = "Hidden files",
                        //     value = if (userPreferences.showAllAudioFiles) "Visible" else "Hidden",
                        //     onClick = { showComingSoonToast(context, "Hidden files coming soon") }
                        // ),
                        // SettingsRowModel(
                        //     icon = Icons.Default.Delete,
                        //     title = "Recently deleted",
                        //     value = "0 files",
                        //     onClick = onNavigateToRestorePlaylist
                        // ),
                        // SettingsRowModel(
                        //     icon = Icons.Default.PlayCircle,
                        //     title = "Playback settings",
                        //     onClick = { showComingSoonToast(context, "Playback settings coming soon") }
                        // ),
                        SettingsRowModel(
                            icon = Icons.Default.Notifications,
                            title = "Notification settings",
                            onClick = { showComingSoonToast(context, "Notification settings coming soon") }
                        ),
                        SettingsRowModel(
                            icon = Icons.Default.Language,
                            title = "Language",
                            value = "English",
                            onClick = { showComingSoonToast(context, "Language support coming soon") }
                        )
                    )
                )
            }
            item {
                SettingsGroupCard(
                    modifier = Modifier.padding(horizontal = SpacingLarge),
                    rows = listOf(
                        SettingsRowModel(
                            icon = Icons.Default.Email,
                            title = "Feedback",
                            value = "Share your feedback on Google Play",
                            valuePlacement = ValuePlacement.Below,
                            valueColor = accentTokens.active,
                            onClick = {
                                analyticsTracker?.logEvent("feedback_tap", mapOf("source" to "settings"))
                                onNavigateToFeedbackReport()
                            }
                        ),
                        SettingsRowModel(
                            icon = Icons.Default.Info,
                            title = "About",
                            onClick = onNavigateToAbout
                        )
                    )
                )
            }
            item {
                SettingsGroupCard(
                    modifier = Modifier.padding(horizontal = SpacingLarge),
                    rows = listOf(
                        SettingsRowModel(
                            icon = Icons.Default.VisibilityOff,
                            title = "Private Session",
                            // value = "Dont save your listening history",
                            valuePlacement = ValuePlacement.Below,
                            onClick = { viewModel.togglePrivateSession() },
                            trailingContent = {
                                Switch(
                                    checked = userPreferences.isPrivateSessionEnabled,
                                    onCheckedChange = { viewModel.togglePrivateSession() }
                                )
                            }
                        ),
                        SettingsRowModel(
                            icon = Icons.Default.Notifications,
                            title = "Show Notification Controls",
                            value = "Display playback controls in notification",
                            valuePlacement = ValuePlacement.Below,
                            onClick = { viewModel.setShowNotificationControls(!userPreferences.showNotificationControls) },
                            trailingContent = {
                                Switch(
                                    checked = userPreferences.showNotificationControls,
                                    onCheckedChange = { viewModel.setShowNotificationControls(it) }
                                )
                            }
                        )
                    )
                )
            }
            item {
                SettingsGroupCard(
                    modifier = Modifier.padding(horizontal = SpacingLarge),
                    rows = listOf(
                        SettingsRowModel(
                            icon = Icons.Default.HighQuality,
                            title = "Audio Quality",
                            value = getAudioQualityDescription(userPreferences.audioQuality),
                            onClick = { showAudioQualityDialog = true }
                        ),
                        SettingsRowModel(
                            icon = Icons.Default.Storage,
                            title = "Audio Buffer",
                            value = "${userPreferences.audioBufferMs}ms",
                            onClick = { showBufferDialog = true }
                        ),
                        SettingsRowModel(
                            icon = Icons.Default.Speed,
                            title = "Gapless Playback",
                            value = "Seamless transitions between tracks",
                            valuePlacement = ValuePlacement.Below,
                            onClick = { viewModel.toggleGaplessPlayback() },
                            trailingContent = {
                                Switch(
                                    checked = userPreferences.gaplessPlaybackEnabled,
                                    onCheckedChange = { viewModel.toggleGaplessPlayback() }
                                )
                            }
                        ),
                        SettingsRowModel(
                            icon = Icons.Default.AutoAwesome,
                            title = "Crossfade Duration",
                            value = getCrossfadeLabel(userPreferences.crossfadeDurationMs),
                            onClick = { showCrossfadeDialog = true }
                        ),
                        SettingsRowModel(
                            icon = Icons.Default.Settings,
                            title = "Audio Normalization",
                            value = "Normalize volume across tracks",
                            valuePlacement = ValuePlacement.Below,
                            onClick = { viewModel.toggleAudioNormalization() },
                            trailingContent = {
                                Switch(
                                    checked = userPreferences.audioNormalizationEnabled,
                                    onCheckedChange = { viewModel.toggleAudioNormalization() }
                                )
                            }
                        ),
                        SettingsRowModel(
                            icon = Icons.Default.Equalizer,
                            title = "Equalizer",
                            value = "5-band EQ with bass boost",
                            onClick = onNavigateToEqualizer
                        )
                    )
                )
            }
            item {
                SettingsGroupCard(
                    modifier = Modifier.padding(horizontal = SpacingLarge),
                    rows = listOf(
                        SettingsRowModel(
                            icon = Icons.Default.PermMedia,
                            title = "Audio Permission",
                            value = "Granted",
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
                                }
                            }
                        ),
                        SettingsRowModel(
                            icon = Icons.Default.Search,
                            title = "Scan on Startup",
                            value = "Automatically scan for new music",
                            onClick = { viewModel.toggleScanOnStartup() },
                            trailingContent = {
                                Switch(
                                    checked = userPreferences.scanOnStartup,
                                    onCheckedChange = { viewModel.toggleScanOnStartup() }
                                )
                            }
                        ),
                        SettingsRowModel(
                            icon = Icons.Default.Timer,
                            title = "Minimum Audio Duration",
                            value = "${userPreferences.minimumAudioDuration}s",
                            onClick = { showMinDurationDialog = true }
                        ),
                        SettingsRowModel(
                            icon = Icons.Default.AudioFile,
                            title = "Show All Audio Files",
                            value = "Show hidden files and short clips",
                            valuePlacement = ValuePlacement.Below,
                            onClick = { viewModel.toggleShowAllAudioFiles() },
                            trailingContent = {
                                Switch(
                                    checked = userPreferences.showAllAudioFiles,
                                    onCheckedChange = { viewModel.toggleShowAllAudioFiles() }
                                )
                            }
                        ),
                        SettingsRowModel(
                            icon = Icons.Default.FolderOff,
                            title = "Excluded Folders",
                            value = " folders to exclude from scan",
                            onClick = onNavigateToExcludedFolders
                        ),
                        SettingsRowModel(
                            icon = Icons.Default.Refresh,
                            title = "Rescan Library",
                            value = " Search for new music files",
                            onClick = {
                                viewModel.resetScanState()
                                showRescanDialog = true
                            },
                            showLoading = isScanning
                        )
                    )
                )
            }
            item {
                SettingsGroupCard(
                    modifier = Modifier.padding(horizontal = SpacingLarge),
                    rows = listOf(
                        SettingsRowModel(
                            icon = Icons.Default.Info,
                            title = "Version",
                            value = viewModel.getAppVersion()
                        ),
                        SettingsRowModel(
                            icon = Icons.Default.Star,
                            title = "Rate Us",
                            value = "Share your feedback on Google Play",
                            onClick = {
                                analyticsTracker?.logEvent("rate_us_item_tap", mapOf("source" to "settings"))
                                val activity = context as? ComponentActivity
                                if (activity != null) {
                                    viewModel.triggerInAppReview(activity)
                                }
                            }
                        )                      
                    )
                )
            }
        }
        // Dialogs
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

        if (showAccentDialog) {
            AccentSelectionDialog(
                currentProfile = userPreferences.accentProfile,
                onDismiss = { showAccentDialog = false },
                onProfileSelect = { profile ->
                    viewModel.setAccentProfile(profile)
                    showAccentDialog = false
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
            RescanDialog(
                isScanning = isScanning,
                scanState = scanState,
                onConfirm = {
                    viewModel.rescanLibrary()
                },
                onDismiss = { finalState ->
                    if (!isScanning) {
                        showRescanDialog = false
                        // Show toast when dialog closes with success
                        if (finalState is com.sukoon.music.domain.model.ScanState.Success) {
                            Toast.makeText(
                                context,
                                "✓ Loaded ${finalState.totalSongs} songs",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            )
        }

        if (showPremiumDialog) {
            PremiumDialog(
                billingState = billingState,
                priceText = "$4.99 USD",
                onDismiss = {
                    showPremiumDialog = false
                    viewModel.resetBillingState()
                },
                onPurchase = {
                    analyticsTracker?.logEvent(
                        name = "premium_purchase_tap",
                        params = mapOf("source" to "settings")
                    )
                    val activity = context as? ComponentActivity
                    if (activity != null && premiumManager != null) {
                        coroutineScope.launch {
                            premiumManager.purchasePremium(activity)
                        }
                    }
                },
                onRestore = {
                    analyticsTracker?.logEvent(
                        name = "premium_restore_tap",
                        params = emptyMap()
                    )
                    viewModel.restorePurchases()
                }
            )

            // Auto-dismiss success dialog after 2 seconds
            if (billingState is com.sukoon.music.data.billing.BillingState.Success) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    showPremiumDialog = false
                    viewModel.resetBillingState()
                }
            }
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

    GradientAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Minimum Audio Duration") },
        text = {
            Column {
                Text("Exclude files shorter than: $duration seconds")
                Spacer(modifier = Modifier.height(16.dp))
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
        }
    )
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.sectionHeader,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
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
                    style = MaterialTheme.typography.listItemTitle,
                    color = if (isDestructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    }
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.listItemSubtitle,
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
                    style = MaterialTheme.typography.listItemTitle,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.listItemSubtitle,
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


private fun showComingSoonToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

private fun getSleepTimerLabel(targetMs: Long): String =
    if (targetMs <= System.currentTimeMillis()) {
        "Off"
    } else {
        "On"
    }

private fun getCrossfadeLabel(durationMs: Int): String =
    if (durationMs <= 0) "Off" else "${durationMs}ms"

@Composable
private fun ThemeSelectionDialog(
    currentTheme: AppTheme,
    onDismiss: () -> Unit,
    onThemeSelect: (AppTheme) -> Unit
) {
    GradientAlertDialog(
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
                        Column {
                            Text(
                                text = when (theme) {
                                    AppTheme.LIGHT -> "Light"
                                    AppTheme.DARK -> "Dark"
                                    AppTheme.AMOLED -> "AMOLED Black"
                                    AppTheme.SYSTEM -> "System Default"
                                }
                            )
                            if (theme == AppTheme.AMOLED) {
                                Text(
                                    text = "True black for OLED battery savings",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
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
    GradientAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.CenterHorizontally),
                    tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
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
        }
    )
}

@Composable
private fun AudioQualityDialog(
    currentQuality: AudioQuality,
    onDismiss: () -> Unit,
    onQualitySelect: (AudioQuality) -> Unit
) {
    GradientAlertDialog(
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
        }
    )
}

@Composable
private fun CrossfadeDialog(
    currentDuration: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var duration by remember { mutableStateOf(currentDuration) }

    GradientAlertDialog(
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
        }
    )
}

@Composable
private fun BufferDialog(
    currentBuffer: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var buffer by remember { mutableStateOf(currentBuffer) }

    GradientAlertDialog(
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
        }
    )
}

private fun getThemeDescription(theme: AppTheme): String = when (theme) {
    AppTheme.LIGHT -> "Light Mode"
    AppTheme.DARK -> "Dark Mode"
    AppTheme.AMOLED -> "AMOLED"
    AppTheme.SYSTEM -> "System Default"
}

private fun getAudioQualityDescription(quality: AudioQuality): String = when (quality) {
    AudioQuality.LOW -> "Low (128 kbps)"
    AudioQuality.MEDIUM -> "Medium (192 kbps)"
    AudioQuality.HIGH -> "High (320 kbps)"
    AudioQuality.LOSSLESS -> "Lossless (Original)"
}

@Composable
private fun AccentSelectionDialog(
    currentProfile: AccentProfile,
    onDismiss: () -> Unit,
    onProfileSelect: (AccentProfile) -> Unit
) {
    GradientAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Accent Color") },
        text = {
            Column {
                AccentProfile.ALL.forEach { profile ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = profile.id == currentProfile.id,
                            onClick = { onProfileSelect(profile) }
                        )
                        Spacer(Modifier.width(12.dp))

                        // Color preview + label
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Color swatches showing all accent tokens
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(
                                            color = profile.accentPrimary,
                                            shape = RoundedCornerShape(2.dp)
                                        )
                                )
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(
                                            color = profile.accentActive,
                                            shape = RoundedCornerShape(2.dp)
                                        )
                                )
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(
                                            color = profile.accentOnDark,
                                            shape = RoundedCornerShape(2.dp)
                                        )
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = profile.label,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun RescanDialog(
    isScanning: Boolean,
    scanState: com.sukoon.music.domain.model.ScanState,
    onConfirm: () -> Unit,
    onDismiss: (finalState: com.sukoon.music.domain.model.ScanState) -> Unit
) {
    GradientAlertDialog(
        onDismissRequest = {
            // Only dismiss if not scanning
            if (!isScanning) {
                onDismiss(scanState)
            }
        },
        title = { Text("Rescan Library?") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                when {
                    isScanning -> {
                        // Scanning in progress
                        Text("Scanning your device for music files...")
                        Spacer(modifier = Modifier.height(16.dp))
                        when (scanState) {
                            is com.sukoon.music.domain.model.ScanState.Scanning -> {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Found ${scanState.scannedCount} file(s)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (!scanState.message.isNullOrEmpty()) {
                                    Text(
                                        text = scanState.message ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                            else -> {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    scanState is com.sukoon.music.domain.model.ScanState.Success -> {
                        // Scan completed successfully
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "✓ Found ${scanState.totalSongs} songs",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your library has been updated successfully",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    scanState is com.sukoon.music.domain.model.ScanState.Error -> {
                        // Scan failed
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "✗ Scan Failed",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = scanState.error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    else -> {
                        // Initial state - pre-scan
                        Text("This will scan your device for new music files. It may take a moment.")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        // Terminal states (Success/Error) → close dialog
                        scanState is com.sukoon.music.domain.model.ScanState.Success ||
                        scanState is com.sukoon.music.domain.model.ScanState.Error -> {
                            onDismiss(scanState)
                        }
                        // Idle or any other state → start scan if not already scanning
                        !isScanning -> {
                            onConfirm()
                        }
                        // If scanning, button is disabled, this shouldn't be reached
                    }
                },
                enabled = !isScanning ||
                         scanState is com.sukoon.music.domain.model.ScanState.Success ||
                         scanState is com.sukoon.music.domain.model.ScanState.Error
            ) {
                Text(
                    when {
                        isScanning && scanState is com.sukoon.music.domain.model.ScanState.Scanning -> "Scanning..."
                        scanState is com.sukoon.music.domain.model.ScanState.Success -> "Done"
                        scanState is com.sukoon.music.domain.model.ScanState.Error -> "Done"
                        else -> "Start Scan"
                    }
                )
            }
        },
        dismissButton = {
            if (!isScanning) {
                TextButton(onClick = { onDismiss(scanState) }) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun PremiumDialog(
    priceText: String,
    onDismiss: () -> Unit,
    onPurchase: () -> Unit,
    onRestore: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(0.92f),

        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = "Premium",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(46.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Go Premium",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Enjoy pure, uninterrupted music",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        },

        text = {
            Column(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .heightIn(min = 360.dp, max = 540.dp)
                    .verticalScroll(rememberScrollState())
            ) {

                Spacer(modifier = Modifier.height(10.dp))

                PremiumBenefit("Ad-free listening")
                PremiumBenefit("No interruptions during playback")
                PremiumBenefit("Cleaner, faster experience")
                PremiumBenefit("Works fully offline")
                PremiumBenefit("Support Sukoon development")

                Spacer(modifier = Modifier.height(18.dp))

                // Price Highlight Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    tonalElevation = 3.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = priceText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Lifetime access • One-time purchase",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onRestore,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Restore Purchase")
                }

                Text(
                    text = "No subscription • No recurring charges • Secured by Google Play",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },

        confirmButton = {
            Button(
                onClick = onPurchase,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Unlock Premium",
                    fontWeight = FontWeight.Bold
                )
            }
        },

        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Maybe Later")
            }
        }
    )
}

@Composable
private fun PremiumBenefit(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun SettingsScreenPreview() {
    SukoonMusicPlayerTheme(theme = AppTheme.DARK) {
        SettingsScreen(onBackClick = {})
    }
}
