package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGold
import com.example.ui.theme.ExpenseRose
import com.example.ui.theme.FintechGreen

@Composable
fun BudgetCategoryProgressBar(
    categoryName: String,
    spent: Double,
    limit: Double,
    currencySymbol: String = "$",
    formatter: ((Double) -> String)? = null,
    isAdaptive: Boolean = false,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null
) {
    val ratio = if (limit > 0) (spent / limit).toFloat() else 0f
    val clampedRatio = ratio.coerceAtMost(1.0f)
    val animatedProgress by animateFloatAsState(targetValue = clampedRatio, label = "budget_progress")

    val (barColor, statusText, statusBg) = when {
        ratio >= 1.0f -> Triple(ExpenseRose, "OVER BUDGET", ExpenseRose.copy(alpha = 0.15f))
        ratio >= 0.85f -> Triple(AccentGold, "NEAR LIMIT", AccentGold.copy(alpha = 0.15f))
        else -> Triple(FintechGreen, "HEALTHY", FintechGreen.copy(alpha = 0.15f))
    }

    val spentStr = formatter?.invoke(spent) ?: "$currencySymbol${String.format("%.2f", spent)}"
    val limitStr = formatter?.invoke(limit) ?: "$currencySymbol${String.format("%.2f", limit)}"
    val remaining = limit - spent
    val remainingStr = if (remaining >= 0) {
        "Remaining: ${formatter?.invoke(remaining) ?: "$currencySymbol${String.format("%.2f", remaining)}"}"
    } else {
        "Over by: ${formatter?.invoke(-remaining) ?: "$currencySymbol${String.format("%.2f", -remaining)}"}"
    }
    val percentageInt = (ratio * 100).toInt()

    val a11yDescription = "Budget category $categoryName: $spentStr spent of $limitStr monthly limit ($percentageInt% consumed). Status: $statusText. $remainingStr."

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = a11yDescription }
            .testTag("budget_progress_bar_$categoryName")
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = categoryName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = statusBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = statusText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = barColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (isAdaptive) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "AI TUNED",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$percentageInt%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = barColor
                    )
                    if (onEdit != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.testTag("edit_budget_btn_$categoryName")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit monthly budget limit for category $categoryName",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar Visual Component
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(6.dp))
                        .background(barColor)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Details Footer Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CONSUMED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = spentStr,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = remainingStr,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (remaining < 0) ExpenseRose else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "MONTHLY LIMIT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = limitStr,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
