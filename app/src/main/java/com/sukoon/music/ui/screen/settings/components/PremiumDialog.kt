package com.sukoon.music.ui.screen.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradient
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sukoon.music.R
import com.sukoon.music.data.billing.BillingState

@Composable
fun PremiumDialog(
    billingState: BillingState,
    priceText: String,
    onDismiss: () -> Unit,
    onPurchase: () -> Unit,
    onRestore: () -> Unit
) {
    when (billingState) {
        is BillingState.Success -> {
            Dialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
            ) {
                SuccessDialog(onDismiss)
            }
        }

        is BillingState.Error -> {
            Dialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
            ) {
                ErrorDialog(billingState.msg, onDismiss)
            }
        }

        is BillingState.Loading -> {
            Dialog(
                onDismissRequest = { },
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                LoadingDialog()
            }
        }

        is BillingState.Idle -> {
            Dialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
            ) {
                PremiumUpsellDialog(priceText, onPurchase, onRestore, onDismiss)
            }
        }
    }
}

@Composable
private fun SuccessDialog(onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .clip(RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF4CAF50)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.label_premium_activated),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_premium_thank_you_desc),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.premium_ads_removed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.common_continue))
            }
        }
    }
}

@Composable
private fun ErrorDialog(errorMsg: String, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .clip(RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.label_purchase_failed),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = errorMsg,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.common_ok))
            }
        }
    }
}

@Composable
private fun LoadingDialog() {
    Surface(
        modifier = Modifier
            .fillMaxWidth(0.6f)
            .clip(RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.label_processing_purchase),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun PremiumUpsellDialog(
    priceText: String,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .border(2.dp, primaryColor, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Crown Icon
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Color(0xFFFFD700)
            )
            Spacer(Modifier.height(8.dp))

            // Title
            Text(
                text = stringResource(R.string.label_sukoon_premium),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            )
            Spacer(Modifier.height(4.dp))

            // Tagline
            Text(
                text = "Music without interruptions. Forever.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            // Comparison Table
            PremiumComparisonTable(primaryColor)
            Spacer(Modifier.height(12.dp))

            // Price
            Text(
                text = priceText,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700),
                fontSize = 40.sp
            )
            Spacer(Modifier.height(4.dp))

            // Price description
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "One-time payment • Lifetime access",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "No subscription • No hidden fees",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))

            // Unlock Premium Button
            Button(
                onClick = onPurchase,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(14.dp),
                        ambientColor = primaryColor.copy(alpha = 0.5f),
                        spotColor = primaryColor.copy(alpha = 0.7f)
                    ),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor
                )
            ) {
                Text(
                    text = stringResource(R.string.label_get_premium),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            Spacer(Modifier.height(4.dp))

            // Restore & Not Now buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(onClick = onRestore) {
                    Text(
                        text = stringResource(R.string.common_restore),
                        color = primaryColor
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "|",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.common_cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Security note
            Text(
                text = "Secure purchase via Google Play • Instant activation",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PremiumComparisonTable(primaryColor: Color = MaterialTheme.colorScheme.primary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, primaryColor, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Free Plan",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = primaryColor.copy(alpha = 0.7f),
                modifier = Modifier.weight(0.5f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "💙 Premium",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                modifier = Modifier.weight(0.5f),
                textAlign = TextAlign.Center
            )
        }

        Divider(thickness = 1.dp, color = primaryColor)

        // Features
        ComparisonRow("Ad-Free Listening", "❌", "✔")
        Divider(thickness = 0.5.dp, color = primaryColor.copy(alpha = 0.3f))
        ComparisonRow("Unlimited Offline Music", "Limited", "✔")
        Divider(thickness = 0.5.dp, color = primaryColor.copy(alpha = 0.3f))
        ComparisonRow("Premium Themes", "❌", "✔")
        Divider(thickness = 0.5.dp, color = primaryColor.copy(alpha = 0.3f))
        ComparisonRow("Early Access", "❌", "✔")
    }
}

@Composable
private fun ComparisonRow(feature: String, freePlan: String, premium: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = feature,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = freePlan,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.5f),
            textAlign = TextAlign.Center,
            color = when {
                freePlan == "✔" -> Color(0xFF4CAF50)
                freePlan == "❌" -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        Text(
            text = premium,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.5f),
            textAlign = TextAlign.Center,
            color = when {
                premium == "✔" -> Color(0xFF4CAF50)
                premium == "❌" -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
