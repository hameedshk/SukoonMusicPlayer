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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.Manifest
import android.app.Activity
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
import com.sukoon.music.R
import com.sukoon.music.data.premium.PremiumManager
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.components.GradientAlertDialog
import com.sukoon.music.ui.components.PremiumBanner
import com.sukoon.music.ui.components.SettingsGroupCard
import com.sukoon.music.ui.components.SettingsGroupRow
import com.sukoon.music.ui.components.SettingsRowModel
import com.sukoon.music.ui.components.ValuePlacement
import com.sukoon.music.ui.theme.switchPadding
import com.sukoon.music.ui.screen.settings.components.PremiumDialog
import com.sukoon.music.ui.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.flowOf
import com.sukoon.music.ui.theme.*
import com.sukoon.music.ui.analytics.AnalyticsEntryPoint
import com.sukoon.music.util.AppLocaleManager
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
    openPremiumDialog: Boolean = false,
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
    val appLanguageTag by viewModel.appLanguageTag.collectAsStateWithLifecycle()
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
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showPremiumDialog by rememberSaveable(openPremiumDialog) { mutableStateOf(openPremiumDialog) }

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
                title = { Text(stringResource(R.string.common_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
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
                            title = stringResource(R.string.settings_screen_theme_title),
                            value = getThemeDescription(context, userPreferences.theme),
                            onClick = { showThemeDialog = true }
                        ),                     
                        SettingsRowModel(
                            icon = Icons.Default.ColorLens,
                            title = stringResource(R.string.settings_screen_accent_color_title),
                            value = userPreferences.accentProfile.label,
                            onClick = { showAccentDialog = true }
                        ),
                        SettingsRowModel(
                            icon = Icons.Default.Timer,
                            title = stringResource(R.string.settings_screen_sleep_timer_title),
                            value = getSleepTimerLabel(context, userPreferences.sleepTimerTargetTimeMs),
                            onClick = { showComingSoonToast(context, context.getString(R.string.settings_screen_sleep_timer_coming_soon)) }
                        ),
                        SettingsRowModel(
                            icon = Icons.Default.WorkspacePremium,
                            title = stringResource(R.string.settings_screen_remove_ads_title),
                            value = if (isPremium) {
                                stringResource(R.string.settings_screen_premium_active)
                            } else {
                                stringResource(R.string.settings_screen_discount_48_off)
                            },
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
                            title = stringResource(R.string.settings_screen_notification_settings_title),
                            onClick = { showComingSoonToast(context, context.getString(R.string.settings_screen_notification_settings_coming_soon)) }
                        ),
                        SettingsRowModel(
                            icon = Icons.Default.Language,
                            title = stringResource(R.string.settings_screen_language_title),
                            value = getLanguageDescription(context, appLanguageTag),
                            onClick = { showLanguageDialog = true }
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
                            title = stringResource(R.string.settings_screen_feedback_title),
                            value = stringResource(R.string.settings_screen_feedback_google_play),
                            valuePlacement = ValuePlacement.Below,
                            valueColor = accentTokens.active,
                            onClick = {
                                analyticsTracker?.logEvent("feedback_tap", mapOf("source" to "settings"))
                                onNavigateToFeedbackReport()
                            }
                        ),
                        SettingsRowModel(
                            icon = Icons.Default.Info,
                            title = stringResource(R.string.settings_screen_about_title),
                            onClick = onNavigateToAbout
                        ),
                           SettingsRowModel(
                            icon = Icons.Default.Star,
                            title = stringResource(R.string.settings_screen_rate_us_title),
                            value = stringResource(R.string.settings_screen_feedback_google_play),
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
            item {
                SettingsGroupCard(
                    modifier = Modifier.padding(horizontal = SpacingLarge),
                    rows = listOf(
                        SettingsRowModel(
                            icon = Icons.Default.VisibilityOff,
                            title = stringResource(R.string.settings_screen_private_session_title),
                            // value = "Dont save your listening history",
                            valuePlacement = ValuePlacement.Below,
                            onClick = { viewModel.togglePrivateSession() },
                            trailingContent = {
                                Switch(
                                    checked = userPreferences.isPrivateSessionEnabled,
                                    onCheckedChange = { viewModel.togglePrivateSession() },            
                                )
                            }
                        ),
                        // SettingsRowModel(
                        //     icon = Icons.Default.Notifications,
                        //     title = stringResource(R.string.settings_screen_show_notification_controls_title),
                        //     value = stringResource(R.string.settings_screen_show_notification_controls_description),
                        //     valuePlacement = ValuePlacement.Below,
                        //     onClick = { viewModel.setShowNotificationControls(!userPreferences.showNotificationControls) },
                        //     trailingContent = {
                        //         Switch(
                        //             checked = userPreferences.showNotificationControls,
                        //             onCheckedChange = { viewModel.setShowNotificationControls(it) },                                    
                        //         )
                        //     }
                        // )
                    )
                )
            }
            item {
                SettingsGroupCard(
                    modifier = Modifier.padding(horizontal = SpacingLarge),
                    rows = listOf(
                        SettingsRowModel(
                            icon = Icons.Default.HighQuality,
                            title = stringResource(R.string.label_audio_quality),
                            value = getAudioQualityDescription(context, userPreferences.audioQuality),
                            onClick = { showAudioQualityDialog = true }
                        ),
                        SettingsRowModel(
                            icon = Icons.Default.Storage,
                            title = stringResource(R.string.settings_screen_audio_buffer_title),
                            value = stringResource(R.string.settings_screen_duration_ms_value, userPreferences.audioBufferMs),
                            onClick = { showBufferDialog = true }
                        ),
                        SettingsRowModel(
                            icon = Icons.Default.Speed,
                            title = stringResource(R.string.settings_screen_gapless_playback_title),
                            value = stringResource(R.string.settings_screen_gapless_playback_description),
                            valuePlacement = ValuePlacement.Below,
                            onClick = { viewModel.toggleGaplessPlayback() },
                            trailingContent = {
                                Switch(
                                    checked = userPreferences.gaplessPlaybackEnabled,
                                    onCheckedChange = { viewModel.toggleGaplessPlayback() },                                    
                                )
                            }
                        ),
                        SettingsRowModel(
                            icon = Icons.Default.AutoAwesome,
                            title = stringResource(R.string.label_crossfade_duration),
                            value = getCrossfadeLabel(context, userPreferences.crossfadeDurationMs),
                            onClick = { showCrossfadeDialog = true }
                        ),
                        // SettingsRowModel(
                        //     icon = Icons.Default.Settings,
                        //     title = stringResource(R.string.settings_screen_audio_normalization_title),
                        //     value = stringResource(R.string.settings_screen_audio_normalization_description),
                        //     valuePlacement = ValuePlacement.Below,
                        //     onClick = { viewModel.toggleAudioNormalization() },
                        //     trailingContent = {
                        //         Switch(
                        //             checked = userPreferences.audioNormalizationEnabled,
                        //             onCheckedChange = { viewModel.toggleAudioNormalization() },                                    
                        //         )
                        //     }
                        // ),
                        SettingsRowModel(
                            icon = Icons.Default.Equalizer,
                            title = stringResource(R.string.settings_screen_equalizer_title),
                            value = stringResource(R.string.settings_screen_equalizer_description),
                            onClick = onNavigateToEqualizer
                        )
                    )
                )
            }
            item {
                SettingsGroupCard(
                    modifier = Modifier.padding(horizontal = SpacingLarge),
                    rows = listOf(
                        // SettingsRowModel(
                        //     icon = Icons.Default.PermMedia,
                        //     title = stringResource(R.string.settings_screen_audio_permission_title),
                        //     value = stringResource(R.string.settings_screen_permission_granted),
                        //     onClick = {
                        //         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        //             permissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
                        //         }
                        //     }
                        // ),
                        SettingsRowModel(
                            icon = Icons.Default.Search,
                            title = stringResource(R.string.settings_screen_scan_on_startup_title),
                            value = stringResource(R.string.settings_screen_scan_on_startup_description),
                            onClick = { viewModel.toggleScanOnStartup() },
                            trailingContent = {
                                Switch(
                                    checked = userPreferences.scanOnStartup,
                                    onCheckedChange = { viewModel.toggleScanOnStartup() },                                    
                                )
                            }
                        ),
                        SettingsRowModel(
                            icon = Icons.Default.Timer,
                            title = stringResource(R.string.label_minimum_audio_duration),
                            value = stringResource(R.string.settings_screen_duration_seconds_value, userPreferences.minimumAudioDuration),
                            onClick = { showMinDurationDialog = true }
                        ),
                        SettingsRowModel(
                            icon = Icons.Default.AudioFile,
                            title = stringResource(R.string.settings_screen_show_all_audio_files_title),
                            value = stringResource(R.string.settings_screen_show_all_audio_files_description),
                            valuePlacement = ValuePlacement.Below,
                            onClick = { viewModel.toggleShowAllAudioFiles() },
                            trailingContent = {
                                Switch(
                                    checked = userPreferences.showAllAudioFiles,
                                    onCheckedChange = { viewModel.toggleShowAllAudioFiles() },                                    
                                )
                            }
                        ),
                        SettingsRowModel(
                            icon = Icons.Default.FolderOff,
                            title = stringResource(R.string.settings_screen_excluded_folders_title),
                            value = stringResource(R.string.settings_screen_excluded_folders_value),
                            onClick = onNavigateToExcludedFolders
                        ),
                        SettingsRowModel(
                            icon = Icons.Default.Refresh,
                            title = stringResource(R.string.settings_screen_rescan_library_title),
                            value = stringResource(R.string.settings_screen_rescan_library_description),
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
                        // SettingsRowModel(
                        //     icon = Icons.Default.Info,
                        //     title = stringResource(R.string.settings_screen_version_title),
                        //     value = viewModel.getAppVersion()
                        // ),
                        SettingsRowModel(
                            icon = Icons.Default.Star,
                            title = stringResource(R.string.settings_screen_rate_us_title),
                            value = stringResource(R.string.settings_screen_feedback_google_play),
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

        if (showLanguageDialog) {
            LanguageSelectionDialog(
                currentLanguageTag = appLanguageTag,
                onDismiss = { showLanguageDialog = false },
                onLanguageSelect = { languageTag ->
                    val selectedTag = AppLocaleManager.normalizeLanguageTag(languageTag)
                    val currentTag = AppLocaleManager.normalizeLanguageTag(appLanguageTag)
                    if (selectedTag != currentTag) {
                        val appliedTag = if (selectedTag == AppLocaleManager.LANGUAGE_SYSTEM) {
                            null
                        } else {
                            selectedTag
                        }
                        // CRITICAL: Save preference, await completion, THEN apply and recreate
                        coroutineScope.launch {
                            viewModel.setAppLanguageTagAndWait(languageTag)  // Suspends until saved
                            AppLocaleManager.applyLanguage(context, appliedTag)
                            (context as? Activity)?.recreate()
                        }
                    } else {
                        // Still save if selection didn't change
                        viewModel.setAppLanguageTag(languageTag)
                    }
                    showLanguageDialog = false
                }
            )
        }

        if (showClearCacheDialog) {
            ConfirmationDialog(
                title = stringResource(R.string.settings_screen_clear_cache_title),
                message = stringResource(R.string.settings_screen_clear_cache_message),
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
                title = stringResource(R.string.settings_screen_clear_database_title),
                message = stringResource(R.string.settings_screen_clear_database_message),
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
                title = stringResource(R.string.settings_screen_clear_recently_played_title),
                message = stringResource(R.string.settings_screen_clear_recently_played_message),
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
                                context.getString(R.string.settings_screen_loaded_songs_toast, finalState.totalSongs),
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
                priceText = stringResource(R.string.settings_screen_premium_price_text),
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
        title = { Text(stringResource(R.string.label_minimum_audio_duration)) },
        text = {
            Column {
                Text(stringResource(R.string.settings_screen_exclude_files_shorter_than_value, duration))
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = duration.toFloat(),
                    onValueChange = { duration = it.toInt() },
                    valueRange = 0f..120f,
                    steps = 11
                )
                Text(
                    text = stringResource(R.string.settings_screen_minimum_duration_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(duration) }) {
                Text(stringResource(R.string.common_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
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

private fun getSleepTimerLabel(context: Context, targetMs: Long): String =
    if (targetMs <= System.currentTimeMillis()) {
        context.getString(R.string.settings_screen_state_off)
    } else {
        context.getString(R.string.settings_screen_state_on)
    }

private fun getCrossfadeLabel(context: Context, durationMs: Int): String =
    if (durationMs <= 0) {
        context.getString(R.string.settings_screen_state_off)
    } else {
        context.getString(R.string.settings_screen_duration_ms_value, durationMs)
    }

private fun getLanguageDescription(context: Context, languageTag: String?): String {
    return when (AppLocaleManager.normalizeLanguageTag(languageTag)) {
        AppLocaleManager.LANGUAGE_SYSTEM -> context.getString(R.string.settings_screen_language_system_default)
        "en" -> context.getString(R.string.settings_screen_language_english)
        "hi-IN" -> context.getString(R.string.settings_screen_language_hindi)
        "pt-BR" -> context.getString(R.string.settings_screen_language_portuguese_brazil)
        else -> context.getString(R.string.settings_screen_language_system_default)
    }
}

@Composable
private fun LanguageSelectionDialog(
    currentLanguageTag: String?,
    onDismiss: () -> Unit,
    onLanguageSelect: (String?) -> Unit
) {
    val selectedTag = AppLocaleManager.normalizeLanguageTag(currentLanguageTag)
    GradientAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_screen_language_select_title)) },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLanguageSelect(null) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedTag == AppLocaleManager.LANGUAGE_SYSTEM,
                        onClick = { onLanguageSelect(null) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.settings_screen_language_system_default))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLanguageSelect("en") }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedTag == "en",
                        onClick = { onLanguageSelect("en") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.settings_screen_language_english))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLanguageSelect("hi-IN") }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedTag == "hi-IN",
                        onClick = { onLanguageSelect("hi-IN") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.settings_screen_language_hindi))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close))
            }
        }
    )
}

@Composable
private fun ThemeSelectionDialog(
    currentTheme: AppTheme,
    onDismiss: () -> Unit,
    onThemeSelect: (AppTheme) -> Unit
) {
    GradientAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.label_choose_theme)) },
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
                                    AppTheme.LIGHT -> stringResource(R.string.settings_screen_theme_light_short)
                                    AppTheme.DARK -> stringResource(R.string.settings_screen_theme_dark_short)
                                    AppTheme.AMOLED -> stringResource(R.string.settings_screen_theme_amoled_black)
                                    AppTheme.SYSTEM -> stringResource(R.string.settings_screen_theme_system_default)
                                }
                            )
                            if (theme == AppTheme.AMOLED) {
                                Text(
                                    text = stringResource(R.string.settings_screen_theme_amoled_hint),
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
                Text(stringResource(R.string.common_close))
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
                Text(
                    if (isDestructive) {
                        stringResource(R.string.common_delete)
                    } else {
                        stringResource(R.string.settings_screen_confirm)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
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
        title = { Text(stringResource(R.string.label_audio_quality)) },
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
                                    AudioQuality.LOW -> stringResource(R.string.settings_screen_audio_quality_low)
                                    AudioQuality.MEDIUM -> stringResource(R.string.settings_screen_audio_quality_medium)
                                    AudioQuality.HIGH -> stringResource(R.string.settings_screen_audio_quality_high)
                                    AudioQuality.LOSSLESS -> stringResource(R.string.settings_screen_audio_quality_lossless)
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = when (quality) {
                                    AudioQuality.LOW -> stringResource(R.string.settings_screen_audio_quality_low_description)
                                    AudioQuality.MEDIUM -> stringResource(R.string.settings_screen_audio_quality_medium_description)
                                    AudioQuality.HIGH -> stringResource(R.string.settings_screen_audio_quality_high_description)
                                    AudioQuality.LOSSLESS -> stringResource(R.string.settings_screen_audio_quality_lossless_description)
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
                Text(stringResource(R.string.common_close))
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
        title = { Text(stringResource(R.string.label_crossfade_duration)) },
        text = {
            Column {
                Text(
                    if (duration == 0) {
                        stringResource(R.string.settings_screen_crossfade_duration_disabled_value, duration)
                    } else {
                        stringResource(R.string.settings_screen_crossfade_duration_value, duration)
                    }
                )
                Spacer(Modifier.height(16.dp))
                Slider(
                    value = duration.toFloat(),
                    onValueChange = { duration = it.toInt() },
                    valueRange = 0f..10000f,
                    steps = 19
                )
                Text(
                    text = stringResource(R.string.settings_screen_crossfade_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(duration) }) {
                Text(stringResource(R.string.common_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
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
        title = { Text(stringResource(R.string.label_audio_buffer_size)) },
        text = {
            Column {
                Text(stringResource(R.string.settings_screen_audio_buffer_value, buffer))
                Spacer(Modifier.height(16.dp))
                Slider(
                    value = buffer.toFloat(),
                    onValueChange = { buffer = it.toInt() },
                    valueRange = 100f..2000f,
                    steps = 18
                )
                Text(
                    text = stringResource(R.string.settings_screen_audio_buffer_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(buffer) }) {
                Text(stringResource(R.string.common_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

private fun getThemeDescription(context: Context, theme: AppTheme): String = when (theme) {
    AppTheme.LIGHT -> context.getString(R.string.settings_screen_theme_light_mode)
    AppTheme.DARK -> context.getString(R.string.settings_screen_theme_dark_mode)
    AppTheme.AMOLED -> context.getString(R.string.settings_screen_theme_amoled)
    AppTheme.SYSTEM -> context.getString(R.string.settings_screen_theme_system_default)
}

private fun getAudioQualityDescription(context: Context, quality: AudioQuality): String = when (quality) {
    AudioQuality.LOW -> context.getString(R.string.settings_screen_audio_quality_low_value)
    AudioQuality.MEDIUM -> context.getString(R.string.settings_screen_audio_quality_medium_value)
    AudioQuality.HIGH -> context.getString(R.string.settings_screen_audio_quality_high_value)
    AudioQuality.LOSSLESS -> context.getString(R.string.settings_screen_audio_quality_lossless_value)
}

@Composable
private fun AccentSelectionDialog(
    currentProfile: AccentProfile,
    onDismiss: () -> Unit,
    onProfileSelect: (AccentProfile) -> Unit
) {
    GradientAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.label_choose_accent_color)) },
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
                Text(stringResource(R.string.common_close))
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
        title = { Text(stringResource(R.string.label_rescan_library_question)) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                when {
                    isScanning -> {
                        // Scanning in progress
                        Text(stringResource(R.string.settings_screen_scanning_device_message))
                        Spacer(modifier = Modifier.height(16.dp))
                        when (scanState) {
                            is com.sukoon.music.domain.model.ScanState.Scanning -> {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.settings_screen_found_files_count, scanState.scannedCount),
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
                            text = stringResource(R.string.settings_screen_found_songs_count, scanState.totalSongs),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.settings_screen_library_updated_successfully),
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
                            text = stringResource(R.string.settings_screen_scan_failed_title),
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
                        Text(stringResource(R.string.settings_screen_rescan_initial_message))
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
                        isScanning && scanState is com.sukoon.music.domain.model.ScanState.Scanning -> stringResource(R.string.settings_screen_scanning_button)
                        scanState is com.sukoon.music.domain.model.ScanState.Success -> stringResource(R.string.settings_screen_done_button)
                        scanState is com.sukoon.music.domain.model.ScanState.Error -> stringResource(R.string.settings_screen_done_button)
                        else -> stringResource(R.string.settings_screen_start_scan_button)
                    }
                )
            }
        },
        dismissButton = {
            if (!isScanning) {
                TextButton(onClick = { onDismiss(scanState) }) {
                    Text(stringResource(R.string.common_cancel))
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
                    contentDescription = stringResource(R.string.settings_screen_premium_content_description),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(46.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.settings_screen_go_premium_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = stringResource(R.string.settings_screen_go_premium_subtitle),
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

                PremiumBenefit(stringResource(R.string.settings_screen_premium_benefit_ad_free_listening))
                PremiumBenefit(stringResource(R.string.settings_screen_premium_benefit_no_interruptions))
                PremiumBenefit(stringResource(R.string.settings_screen_premium_benefit_cleaner_faster))
                PremiumBenefit(stringResource(R.string.settings_screen_premium_benefit_works_offline))
                PremiumBenefit(stringResource(R.string.settings_screen_premium_benefit_support_development))

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
                            text = stringResource(R.string.settings_screen_lifetime_access_one_time_purchase),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onRestore,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(stringResource(R.string.common_restore_purchase))
                }

                Text(
                    text = stringResource(R.string.settings_screen_no_subscription_no_recurring_charges),
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
                    text = stringResource(R.string.settings_screen_unlock_premium),
                    fontWeight = FontWeight.Bold
                )
            }
        },

        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.label_maybe_later))
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
