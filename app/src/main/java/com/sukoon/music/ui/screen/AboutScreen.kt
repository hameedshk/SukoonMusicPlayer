package com.sukoon.music.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.hilt.navigation.compose.hiltViewModel
import com.sukoon.music.R
import com.sukoon.music.domain.model.AppTheme
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.viewmodel.SettingsViewModel
import com.sukoon.music.ui.theme.*
import com.sukoon.music.util.AppUrls
import com.sukoon.music.ui.components.SettingsGroupCard
import com.sukoon.music.ui.components.SettingsRowModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.gradientBackground(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.about_screen_title),
                        color = MaterialTheme.colorScheme.background
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_back),
                            tint = MaterialTheme.colorScheme.onSurface
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
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(
                start = SpacingLarge,
                end = SpacingLarge,
                top = SpacingXLarge,
                bottom = SpacingSmall
            ),
            verticalArrangement = Arrangement.spacedBy(SpacingMedium)
        ) {
            // App Logo & Version Header (Non-clickable)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = SpacingXXLarge),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_icon_cat),
                        contentDescription = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.about_app_icon_content_description),
                        modifier = Modifier
                            .size(120.dp)
                            .padding(bottom = SpacingLarge),
                        contentScale = ContentScale.Fit
                    )

                    Text(
                        text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = SpacingSmall)
                    )

                    Text(
                        text = androidx.compose.ui.res.stringResource(
                            com.sukoon.music.R.string.about_version_format,
                            viewModel.getAppVersion()
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = SpacingXLarge)
                    )
                }
            }

            // App Description
            item {
                Text(
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.about_app_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = SpacingXXLarge)
                )
            }

            // Actions Section
            item {
                SettingsGroupCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = SpacingLarge),
                    rows = listOf(
                        SettingsRowModel(
                            icon = Icons.Default.ThumbUp,
                            title = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.rate_us),
                            onClick = {
                                // Open Play Store rating
                                val playStoreUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    data = android.net.Uri.parse(playStoreUrl)
                                }
                                context.startActivity(intent)
                            }
                        ),
                        SettingsRowModel(
                            icon = Icons.Default.Share,
                            title = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.share_app),
                            onClick = {
                                // Share app
                                val shareText = context.getString(
                                    com.sukoon.music.R.string.about_share_app_message,
                                    context.packageName
                                )
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(
                                    android.content.Intent.createChooser(
                                        intent,
                                        context.getString(com.sukoon.music.R.string.about_share_chooser_title)
                                    )
                                )
                            }
                        )
                    )
                )
            }

            // Legal Section
            item {
                SettingsGroupCard(
                    modifier = Modifier.fillMaxWidth(),
                    rows = listOf(
                        SettingsRowModel(
                            icon = Icons.Default.Security,
                            title = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.privacy_policy),
                            onClick = {
                                // Open privacy policy using global base URL
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    data = android.net.Uri.parse("${AppUrls.BASE}apps/sukoon-music/privacy-policy")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Silently handle if no browser available
                                }
                            }
                        ),
                        SettingsRowModel(
                            icon = Icons.Default.Description,
                            title = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.terms_of_service),
                            onClick = {
                                // Open terms of service using global base URL
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    data = android.net.Uri.parse("${AppUrls.BASE}apps/sukoon-music/terms-conditions")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Silently handle if no browser available
                                }
                            }
                        ),
                        SettingsRowModel(
                            icon = Icons.Default.Code,
                            title = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.open_source_licenses),
                            onClick = {
                                // Open licenses info
                                // This could be expanded to show in-app license info
                            }
                        )
                    )
                )
            }

            // Copyright
            item {
                val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                Text(
                    text = androidx.compose.ui.res.stringResource(
                        com.sukoon.music.R.string.about_copyright_format,
                        currentYear
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = SpacingXLarge)
                )
            }
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun AboutScreenPreview() {
    SukoonMusicPlayerTheme(theme = AppTheme.DARK) {
        AboutScreen(onBackClick = {})
    }
}

