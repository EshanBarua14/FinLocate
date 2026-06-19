package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGold
import com.example.ui.theme.FintechGreen

@Composable
fun AiInsightsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val aiReport by viewModel.aiReport.collectAsState()
    val isLoading by viewModel.aiInsightsLoading.collectAsState()
    val activeConfig by viewModel.activeCountryConfig.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("ai_insights_scroll_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. PRIVACY TRUST CARD ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "On-Device Security",
                        tint = FintechGreen,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PRIVACY-FIRST SECURE AUDIT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = FintechGreen
                        )
                        Text(
                            text = "Analyses are encrypted client-side. Financial models process on-device. Backed by safe direct OAuth configurations.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // --- 2. EVALUATOR TRIGGER CARD ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().testTag("ai_trigger_card")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Smart AI",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "COMPILE SMART PORTFOLIO AUDIT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Instruct Google Gemini 3.5 Flash to consolidate budgets, flag spending anomalies, and align deductions under ${activeConfig.country} structures.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        lineHeight = 16.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (isLoading) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Evaluating transactions ledger in real-time...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Button(
                            onClick = { viewModel.triggerGeminiEvaluation() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("trigger_ai_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("PRODUCE REAL-TIME AI AUDIT (GEMINI)", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        // --- 3. AUDIT RECONCILIATION REPORT DISPLAY ---
        item {
            AnimatedContent(
                targetState = aiReport,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "ai_report_anim"
            ) { report ->
                if (report != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_result_report_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = "Analysis Results",
                                    tint = AccentGold,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "WEALTH ADVISORY TRANSCRIPT",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = 1.2.sp
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                            // Simplified Custom Markdown Renderer for clear layouts
                            RenderMarkdownStructuredText(markdown = report)
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Awaiting trigger",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Awaiting audit command. Choose your target country settings and submit for advice.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RenderMarkdownStructuredText(markdown: String) {
    val lines = markdown.split("\n")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        lines.forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("###") -> {
                    val text = trimmed.substringAfter("###").trim()
                    Text(
                        text = text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
                trimmed.startsWith("####") -> {
                    val text = trimmed.substringAfter("####").trim()
                    Text(
                        text = text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                trimmed.startsWith("*") || trimmed.startsWith("-") -> {
                    val text = trimmed.substring(1).trim()
                    // Detect sub-bold sections (e.g., **Core Net**)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        RenderInlineFormatting(text = text, isListItem = true)
                    }
                }
                trimmed.isNotEmpty() -> {
                    RenderInlineFormatting(text = trimmed, isListItem = false)
                }
            }
        }
    }
}

@Composable
fun RenderInlineFormatting(text: String, isListItem: Boolean) {
    // Basic bold parsing e.g. "You **spent too much** on food"
    if (text.contains("**")) {
        val parts = text.split("**")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            parts.forEachIndexed { idx, part ->
                val isBold = idx % 2 != 0
                Text(
                    text = part,
                    fontSize = if (isListItem) 12.sp else 13.sp,
                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                    color = if (isBold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    } else {
        Text(
            text = text,
            fontSize = if (isListItem) 12.sp else 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            fontWeight = FontWeight.Normal,
            lineHeight = 17.sp
        )
    }
}
