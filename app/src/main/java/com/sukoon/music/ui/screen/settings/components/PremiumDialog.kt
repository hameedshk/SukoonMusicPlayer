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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
                        Text("Continue")
                    }
                },
                title = { Text("Premium Activated!") },
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
                            text = "Thank you for supporting Sukoon Music Player!",
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Ads have been removed.",
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
                        Text("OK")
                    }
                },
                title = { Text("Purchase Failed") },
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
                            text = billingState.message,
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
                title = { Text("Processing Purchase...") },
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
                    Button(onClick = onPurchase) {
                        Text("Get Premium")
                    }
                },
                dismissButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = onRestore) {
                            Text("Restore")
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                    }
                },
                title = { Text("Sukoon Premium") },
                text = {
                    Column(Modifier.fillMaxWidth()) {
                        PremiumFeatureItem("Ad-Free Experience", Icons.Default.Block)
                        PremiumFeatureItem("Support Development", Icons.Default.Favorite)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = priceText,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Text(
                            text = "One-time payment • Lifetime access",
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
