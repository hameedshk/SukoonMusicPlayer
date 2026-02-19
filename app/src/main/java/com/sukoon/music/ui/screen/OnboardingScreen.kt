package com.sukoon.music.ui.screen

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.analytics.FirebaseAnalytics
import com.sukoon.music.R
import com.sukoon.music.data.preferences.PreferencesManager
import com.sukoon.music.domain.model.AppTheme
import com.sukoon.music.ui.analytics.AnalyticsEntryPoint
import com.sukoon.music.ui.permissions.AudioPermissionState
import com.sukoon.music.ui.permissions.rememberAudioPermissionState
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.theme.buttonText
import com.sukoon.music.ui.theme.screenHeader
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

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
    var isSaving by remember { mutableStateOf(false) }
    var playEntranceAnimation by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    val appContext = LocalContext.current.applicationContext
    val analyticsTracker = remember(appContext) {
        runCatching {
            EntryPointAccessors.fromApplication(
                appContext,
                AnalyticsEntryPoint::class.java
            ).analyticsTracker()
        }.getOrNull()
    }

    val isUsernameValid = username.isEmpty() || (username.length in 3..10 && username.all { it.isLetter() })
    val usernameError = when {
        username.isNotEmpty() && username.length < 3 -> "Minimum 3 characters required"
        username.isNotEmpty() && username.length > 10 -> "Maximum 10 characters allowed"
        username.isNotEmpty() && !username.all { it.isLetter() } -> "Only alphabets allowed"
        else -> ""
    }

    val displayUsername = username.lowercase().replaceFirstChar { it.uppercase() }

    val permissionState = rememberAudioPermissionState(
        onPermissionGranted = {},
        onPermissionDenied = {}
    )

    val onUsernameChange: (String) -> Unit = { newValue ->
        val filtered = newValue.filter { it.isLetter() }
        if (filtered.length <= 10) {
            username = filtered
        }
    }

    fun onStartClick() {
        if (isSaving) return
        keyboardController?.hide()
        scope.launch {
            isSaving = true
            try {
                preferencesManager.setOnboardingCompleted()
                if (username.isNotBlank()) {
                    preferencesManager.setUsername(displayUsername)
                }
                analyticsTracker?.logEvent(
                    name = FirebaseAnalytics.Event.TUTORIAL_COMPLETE,
                    params = mapOf("has_username" to username.isNotBlank())
                )
                onOnboardingComplete()
            } catch (e: Exception) {
                Log.e("OnboardingScreen", "Error saving preferences", e)
                onOnboardingComplete()
            } finally {
                isSaving = false
            }
        }
    }

    LaunchedEffect(Unit) {
        playEntranceAnimation = true
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val shouldScroll = maxHeight < 760.dp
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .imePadding()

        if (shouldScroll) {
            Column(
                modifier = contentModifier.verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OnboardingHeader(visible = playEntranceAnimation)
                Spacer(modifier = Modifier.height(22.dp))
                PermissionStepCard(
                    visible = playEntranceAnimation,
                    permissionState = permissionState
                )
                Spacer(modifier = Modifier.height(14.dp))
                UsernameStepCard(
                    visible = playEntranceAnimation,
                    username = username,
                    onUsernameChange = onUsernameChange,
                    usernameError = usernameError,
                    displayUsername = displayUsername
                )
                Spacer(modifier = Modifier.height(24.dp))
                OnboardingFooter(
                    visible = playEntranceAnimation,
                    hasPermission = permissionState.hasPermission,
                    isPermanentlyDenied = permissionState.isPermanentlyDenied,
                    isUsernameValid = isUsernameValid,
                    isSaving = isSaving,
                    onStartClick = ::onStartClick
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        } else {
            Column(
                modifier = contentModifier,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    OnboardingHeader(visible = playEntranceAnimation)
                    Spacer(modifier = Modifier.height(24.dp))
                    PermissionStepCard(
                        visible = playEntranceAnimation,
                        permissionState = permissionState
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    UsernameStepCard(
                        visible = playEntranceAnimation,
                        username = username,
                        onUsernameChange = onUsernameChange,
                        usernameError = usernameError,
                        displayUsername = displayUsername
                    )
                }
                OnboardingFooter(
                    visible = playEntranceAnimation,
                    hasPermission = permissionState.hasPermission,
                    isPermanentlyDenied = permissionState.isPermanentlyDenied,
                    isUsernameValid = isUsernameValid,
                    isSaving = isSaving,
                    onStartClick = ::onStartClick
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun OnboardingHeader(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 320)) +
            slideInVertically(
                animationSpec = tween(durationMillis = 360),
                initialOffsetY = { it / 4 }
            )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(186.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.symbol),
                    contentDescription = null,
                    modifier = Modifier.size(132.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Text(
                text = "Sukoon Music",
                style = MaterialTheme.typography.screenHeader,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 14.dp)
            )
            Text(
                text = "Offline Music Player",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = "Private offline music, on your device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = "Offline | Private | On-device",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun PermissionStepCard(
    visible: Boolean,
    permissionState: AudioPermissionState
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 320, delayMillis = 70)) +
            slideInVertically(
                animationSpec = tween(durationMillis = 360, delayMillis = 70),
                initialOffsetY = { it / 5 }
            )
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Allow Music Access",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    StepBadge(text = "Required", isRequired = true)
                }

                Text(
                    text = "Sukoon needs permission to scan and play songs stored on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 14.dp)
                )

                Button(
                    onClick = permissionState.requestPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !permissionState.hasPermission,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = when {
                            permissionState.hasPermission -> "Access Granted"
                            permissionState.isPermanentlyDenied -> "Open Settings"
                            else -> "Grant Access"
                        },
                        style = MaterialTheme.typography.buttonText
                    )
                }

                if (permissionState.isPermanentlyDenied && !permissionState.hasPermission) {
                    Text(
                        text = "Permission is blocked for this app. Open Settings and allow music access.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun UsernameStepCard(
    visible: Boolean,
    username: String,
    onUsernameChange: (String) -> Unit,
    usernameError: String,
    displayUsername: String
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 320, delayMillis = 130)) +
            slideInVertically(
                animationSpec = tween(durationMillis = 360, delayMillis = 130),
                initialOffsetY = { it / 6 }
            )
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Your Name",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    StepBadge(text = "Optional", isRequired = false)
                }

                Text(
                    text = "Used for greetings and playlists. Leave it empty if you want to skip.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 14.dp)
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "Your name",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    shape = RoundedCornerShape(14.dp),
                    isError = usernameError.isNotEmpty()
                )

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
                            text = "Stored only on this device",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingFooter(
    visible: Boolean,
    hasPermission: Boolean,
    isPermanentlyDenied: Boolean = false,
    isUsernameValid: Boolean,
    isSaving: Boolean,
    onStartClick: () -> Unit
) {
    val canContinue = hasPermission && isUsernameValid && !isSaving

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 320, delayMillis = 190)) +
            slideInVertically(
                animationSpec = tween(durationMillis = 360, delayMillis = 190),
                initialOffsetY = { it / 7 }
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onStartClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = canContinue,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = if (isSaving) "Starting..." else "Start Listening",
                    style = MaterialTheme.typography.buttonText
                )
            }

            if (!hasPermission) {
                Text(
                    text = if (isPermanentlyDenied) {
                        "Music access is blocked. Use \"Open Settings\" above to continue."
                    } else {
                        "Music access is required to continue."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun StepBadge(
    text: String,
    isRequired: Boolean
) {
    val containerColor = if (isRequired) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = if (isRequired) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        shape = RoundedCornerShape(30.dp),
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Preview
@Composable
fun OnboardingScreenPreview() {
    SukoonMusicPlayerTheme(theme = AppTheme.DARK) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                OnboardingHeader(visible = true)
                Spacer(modifier = Modifier.height(20.dp))
                PermissionStepCard(
                    visible = true,
                    permissionState = AudioPermissionState(
                        hasPermission = false,
                        requestPermission = {}
                    )
                )
                Spacer(modifier = Modifier.height(14.dp))
                UsernameStepCard(
                    visible = true,
                    username = "",
                    onUsernameChange = {},
                    usernameError = "",
                    displayUsername = ""
                )
                Spacer(modifier = Modifier.height(20.dp))
                OnboardingFooter(
                    visible = true,
                    hasPermission = false,
                    isUsernameValid = true,
                    isSaving = false,
                    onStartClick = {}
                )
            }
        }
    }
}
