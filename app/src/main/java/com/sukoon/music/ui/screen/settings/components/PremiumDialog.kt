package com.sukoon.music.ui.screen.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
            AlertDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.common_continue))
                    }
                },
                title = { Text(stringResource(R.string.label_premium_activated)) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF4CAF50)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.settings_premium_thank_you_desc),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.premium_ads_removed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }

        is BillingState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.common_ok))
                    }
                },
                title = { Text(stringResource(R.string.label_purchase_failed)) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = billingState.msg,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            )
        }

        is BillingState.Loading -> {
            AlertDialog(
                onDismissRequest = { },
                confirmButton = { },
                title = { Text(stringResource(R.string.label_processing_purchase)) },
                text = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            )
        }

        is BillingState.Idle -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                    Button(
                          onClick = onPurchase,
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large
                    ) {
                        Text(stringResource(R.string.label_get_premium))
                    }
                },
                dismissButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = onRestore) {
                            Text(stringResource(R.string.common_restore))
                        }
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.common_cancel))
                        }
                    }
                },
                title = { 
Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = Color(0xFFFFC107)   // Gold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.label_sukoon_premium),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
}
                },
                text = {
                    Column(Modifier.fillMaxWidth()) {
                        PremiumFeatureItem(stringResource(R.string.settings_premium_feature_1), Icons.Default.Block)
                        PremiumFeatureItem(stringResource(R.string.settings_premium_feature_4), Icons.Default.Favorite)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = priceText,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Text(
                            text = stringResource(R.string.premium_one_time_payment_lifetime),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun PremiumFeatureItem(text: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color(0xFF4CAF50)
        )
        Spacer(Modifier.width(12.dp))
        Text(text = text)
    }
}
