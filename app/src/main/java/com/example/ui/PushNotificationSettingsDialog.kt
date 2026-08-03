package com.example.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PushNotificationSettingsDialog(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("wealthflow_budget_alerts_prefs", Context.MODE_PRIVATE) }

    // Re-trigger recomposition on state changes
    var refreshTrigger by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Category Budget Push Alerts",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Configure threshold push notification alerts (50%, 75%, 90%) for individual spending categories.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(categories) { cat ->
                        val key50 = "category_${cat.id}_alert_50"
                        val key75 = "category_${cat.id}_alert_75"
                        val key90 = "category_${cat.id}_alert_90"

                        var is50On by remember(cat.id, refreshTrigger) { mutableStateOf(prefs.getBoolean(key50, true)) }
                        var is75On by remember(cat.id, refreshTrigger) { mutableStateOf(prefs.getBoolean(key75, true)) }
                        var is90On by remember(cat.id, refreshTrigger) { mutableStateOf(prefs.getBoolean(key90, true)) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("push_alert_category_${cat.id}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = cat.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 50% threshold toggle
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(
                                            checked = is50On,
                                            onCheckedChange = {
                                                is50On = it
                                                prefs.edit().putBoolean(key50, it).apply()
                                                refreshTrigger++
                                            },
                                            modifier = Modifier.testTag("push_alert_50_cat_${cat.id}")
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("50% Alert", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    // 75% threshold toggle
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(
                                            checked = is75On,
                                            onCheckedChange = {
                                                is75On = it
                                                prefs.edit().putBoolean(key75, it).apply()
                                                refreshTrigger++
                                            },
                                            modifier = Modifier.testTag("push_alert_75_cat_${cat.id}")
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("75% Alert", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    // 90% threshold toggle
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(
                                            checked = is90On,
                                            onCheckedChange = {
                                                is90On = it
                                                prefs.edit().putBoolean(key90, it).apply()
                                                refreshTrigger++
                                            },
                                            modifier = Modifier.testTag("push_alert_90_cat_${cat.id}")
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("90% Alert", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_push_alert_settings_btn")
            ) {
                Text("Save & Close")
            }
        }
    )
}
