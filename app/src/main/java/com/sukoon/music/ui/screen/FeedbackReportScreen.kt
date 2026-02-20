package com.sukoon.music.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sukoon.music.R
import com.sukoon.music.domain.model.FeedbackAttachment
import com.sukoon.music.domain.model.FeedbackCategory
import com.sukoon.music.domain.model.FeedbackResult
import com.sukoon.music.ui.theme.SpacingLarge
import com.sukoon.music.ui.theme.SpacingMedium
import com.sukoon.music.ui.theme.SpacingSmall
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.theme.accent
import com.sukoon.music.util.AppUrls
import com.sukoon.music.ui.viewmodel.FeedbackViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackReportScreen(
    onBackClick: () -> Unit,
    viewModel: FeedbackViewModel = hiltViewModel()
) {
    val categories = FeedbackCategory.values().map { it.displayName }
    var selectedCategory by rememberSaveable { mutableStateOf(categories.first()) }
    var details by rememberSaveable { mutableStateOf("") }
    var consentGiven by rememberSaveable { mutableStateOf(false) }
    var showErrorDialog by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    var attachment by remember { mutableStateOf<FeedbackAttachment?>(null) }
    val accentTokens = accent()
    val submitState by viewModel.submitState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val isLoading = submitState is FeedbackResult.Loading

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        attachment = buildFeedbackAttachment(context, uri)
    }

    LaunchedEffect(submitState) {
        when (submitState) {
            is FeedbackResult.Success -> {
                onBackClick()
                viewModel.resetSubmitState()
            }
            is FeedbackResult.Error -> {
                errorMessage = (submitState as FeedbackResult.Error).message
                showErrorDialog = true
            }
            else -> Unit
        }
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text(stringResource(R.string.feedback_error_title)) },
            text = {
                Text(
                    text = if (errorMessage.isBlank()) {
                        stringResource(R.string.feedback_error_message)
                    } else {
                        errorMessage
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showErrorDialog = false
                        viewModel.resetSubmitState()
                    }
                ) {
                    Text(stringResource(R.string.feedback_error_retry))
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.label_tell_us_the_problem)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 2.dp,
                shadowElevation = 6.dp
            ) {
                Button(
                    onClick = {
                        val category = FeedbackCategory.values().find { it.displayName == selectedCategory }
                        category?.let { viewModel.submitFeedback(it, details, consentGiven, attachment) }
                    },
                    enabled = details.isNotBlank() && consentGiven && !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SpacingLarge, vertical = SpacingMedium),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentTokens.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        contentColor = Color.White
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.common_submit))
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SpacingLarge, vertical = SpacingMedium)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(SpacingMedium)
        ) {
            SectionCard(title = stringResource(R.string.feedback_select_issue)) {
                FlowRow(
                    modifier = Modifier.wrapContentWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { chip ->
                        val isSelected = chip == selectedCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = chip },
                            label = {
                                Text(
                                    text = chip,
                                    color = if (isSelected) accentTokens.onDark else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            modifier = Modifier.height(36.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = if (isSelected) {
                                    accentTokens.primary.copy(alpha = 0.16f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        )
                    }
                }
            }

            SectionCard(title = stringResource(R.string.feedback_details_required)) {
                OutlinedTextField(
                    value = details,
                    onValueChange = { if (it.length <= 500) details = it },
                    placeholder = { Text(stringResource(R.string.label_explain_what_happened)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    maxLines = 7,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                Text(
                    text = stringResource(R.string.feedback_details_counter, details.length),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }

            SectionCard(
                title = stringResource(R.string.feedback_attachments_optional)
            ) {
                if (attachment == null) {
                    OutlinedButton(
                        onClick = {
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = stringResource(R.string.feedback_add_screenshot)
                        )
                        Spacer(modifier = Modifier.size(SpacingSmall))
                        Text(stringResource(R.string.feedback_add_screenshot))
                    }
                    Text(
                        text = stringResource(R.string.feedback_attachment_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = SpacingSmall)
                    )
                } else {
                    AttachmentPreviewCard(
                        attachment = attachment!!,
                        isLoading = isLoading,
                        onReplace = {
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onRemove = { attachment = null }
                    )
                }
            }

            ConsentSection(
                consentGiven = consentGiven,
                onConsentChanged = { consentGiven = it },
                accentColor = accentTokens.primary,
                onTermsClick = { openUrlSafely(context, AppUrls.SUKOON_PRIVACY_POLICY) },
                onPrivacyClick = { openUrlSafely(context, AppUrls.SUKOON_PRIVACY_POLICY) }
            )

            Spacer(modifier = Modifier.height(SpacingLarge))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(SpacingSmall)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

@Composable
private fun AttachmentPreviewCard(
    attachment: FeedbackAttachment,
    isLoading: Boolean,
    onReplace: () -> Unit,
    onRemove: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(SpacingMedium),
            horizontalArrangement = Arrangement.spacedBy(SpacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = Uri.parse(attachment.uri),
                contentDescription = stringResource(R.string.feedback_attachment_selected),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = attachment.fileName ?: stringResource(R.string.feedback_attachment_selected),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val sizeText = attachment.sizeBytes?.let { Formatter.formatShortFileSize(context, it) }
                if (!sizeText.isNullOrBlank()) {
                    Text(
                        text = sizeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onReplace, enabled = !isLoading) {
                        Text(stringResource(R.string.feedback_replace_screenshot))
                    }
                    TextButton(onClick = onRemove, enabled = !isLoading) {
                        Text(stringResource(R.string.feedback_remove_screenshot))
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsentSection(
    consentGiven: Boolean,
    onConsentChanged: (Boolean) -> Unit,
    accentColor: Color,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpacingSmall)
        ) {
            Checkbox(
                checked = consentGiven,
                onCheckedChange = onConsentChanged,
                colors = CheckboxDefaults.colors(checkedColor = accentColor)
            )
            val termsLabel = stringResource(R.string.terms_of_service)
            val privacyLabel = stringResource(R.string.privacy_policy)
            val consentText = stringResource(R.string.feedback_consent_text, termsLabel, privacyLabel)
            val annotatedString = buildAnnotatedString {
                append(consentText)
                val termsStart = consentText.indexOf(termsLabel)
                if (termsStart >= 0) {
                    val termsEnd = termsStart + termsLabel.length
                    addStringAnnotation(tag = "TERMS", annotation = "", start = termsStart, end = termsEnd)
                    addStyle(
                        style = SpanStyle(color = accentColor, textDecoration = TextDecoration.Underline),
                        start = termsStart,
                        end = termsEnd
                    )
                }
                val privacyStart = consentText.indexOf(privacyLabel)
                if (privacyStart >= 0) {
                    val privacyEnd = privacyStart + privacyLabel.length
                    addStringAnnotation(tag = "PRIVACY", annotation = "", start = privacyStart, end = privacyEnd)
                    addStyle(
                        style = SpanStyle(color = accentColor, textDecoration = TextDecoration.Underline),
                        start = privacyStart,
                        end = privacyEnd
                    )
                }
            }
            ClickableText(
                text = annotatedString,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                onClick = { offset ->
                    annotatedString.getStringAnnotations(tag = "TERMS", start = offset, end = offset)
                        .firstOrNull()
                        ?.let { onTermsClick() }
                    annotatedString.getStringAnnotations(tag = "PRIVACY", start = offset, end = offset)
                        .firstOrNull()
                        ?.let { onPrivacyClick() }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun openUrlSafely(context: Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}

private fun buildFeedbackAttachment(context: Context, uri: Uri): FeedbackAttachment {
    val resolver = context.contentResolver
    val mimeType = resolver.getType(uri)
    var fileName: String? = null
    var sizeBytes: Long? = null

    runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) fileName = cursor.getString(nameIndex)
                if (sizeIndex >= 0) sizeBytes = cursor.getLong(sizeIndex)
            }
        }
    }

    return FeedbackAttachment(
        uri = uri.toString(),
        mimeType = mimeType,
        fileName = fileName,
        sizeBytes = sizeBytes
    )
}

@Composable
fun FeedbackReportScreenPreview() {
    SukoonMusicPlayerTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            FeedbackReportScreen(onBackClick = {})
        }
    }
}
