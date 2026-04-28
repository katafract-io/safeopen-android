package com.katafract.safeopen.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.katafract.safeopen.models.InspectionResult
import com.katafract.safeopen.models.RiskLevel
import com.katafract.safeopen.ui.theme.RiskPalette
import com.katafract.safeopen.ui.theme.SafeOpenHaptics
import com.katafract.safeopen.ui.theme.riskPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    result: InspectionResult,
    onNavigateBack: () -> Unit,
    onReInspect: (InspectionResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val palette = riskPalette(result.riskLevel)
    val tick = SafeOpenHaptics.rememberTickProvider()

    // Fire the verdict haptic exactly once per result render. DANGEROUS
    // gets a heavy double-pulse — tactile warning even before the user
    // looks at the screen.
    LaunchedEffect(result.id) {
        SafeOpenHaptics.fireForRisk(result.riskLevel, view, haptic)
    }

    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(result.id) { revealed = true }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Inspection Result",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        tick()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                windowInsets = TopAppBarDefaults.windowInsets,
            )
        },
    ) { padding ->
        AnimatedVisibility(
            visible = revealed,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 8 }),
        ) {
            ResultContent(
                result = result,
                palette = palette,
                padding = padding,
                scrollState = scrollState,
                onCopy = {
                    tick()
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = android.content.ClipData.newPlainText(
                        "url",
                        result.finalUrl ?: result.payload.rawValue,
                    )
                    clipboard.setPrimaryClip(clip)
                },
                onShare = {
                    tick()
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            result.finalUrl ?: result.payload.rawValue,
                        )
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share"))
                },
                onOpen = {
                    tick()
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.finalUrl))
                    context.startActivity(intent)
                },
                onBlock = {
                    tick()
                    onNavigateBack()
                },
                onReInspect = {
                    tick()
                    onReInspect(result)
                },
            )
        }
    }
}

@Composable
private fun ResultContent(
    result: InspectionResult,
    palette: RiskPalette,
    padding: PaddingValues,
    scrollState: androidx.compose.foundation.ScrollState,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit,
    onBlock: () -> Unit,
    onReInspect: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(scrollState),
    ) {
        // ── Verdict hero ──────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.soft)
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(palette.color, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = verdictIcon(result.riskLevel),
                    contentDescription = result.riskLevel.displayTitle,
                    tint = palette.onColor,
                    modifier = Modifier.size(48.dp),
                )
            }

            Text(
                text = result.riskLevel.displayTitle,
                style = if (result.riskLevel == RiskLevel.HIGH) {
                    MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                    )
                } else {
                    MaterialTheme.typography.headlineLarge
                },
                color = palette.onSoft,
                modifier = Modifier.padding(top = 16.dp),
            )

            Text(
                text = result.payload.type.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = palette.onSoft.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        // ── Title + summary ───────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                result.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                result.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ── Threat signals ────────────────────────────────────────────
        if (result.riskFactors.isNotEmpty()) {
            SectionHeader("Threat Signals")
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                result.riskFactors.forEach { factor ->
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                factor,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = palette.soft,
                            labelColor = palette.onSoft,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── Original input ────────────────────────────────────────────
        if (result.payload.rawValue != result.finalUrl) {
            SectionHeader("Original Input")
            UrlPanel(
                value = result.payload.rawValue,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
        }

        // ── Redirect chain ────────────────────────────────────────────
        if (result.redirectHops.isNotEmpty()) {
            SectionHeader("Redirect Chain (${result.redirectHops.size})")
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                result.redirectHops.forEach { hop ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(6.dp),
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Text(
                            hop,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── Final URL ─────────────────────────────────────────────────
        if (result.finalUrl != null && result.payload.type.toString().contains("URL")) {
            SectionHeader("Final URL")
            UrlPanel(
                value = result.finalUrl!!,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
        }

        // ── Primary actions ───────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onBlock,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(end = 4.dp),
                )
                Text("Block", style = MaterialTheme.typography.labelLarge)
            }

            if (result.canOpenSafely && result.finalUrl != null) {
                Button(
                    onClick = onOpen,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.color,
                        contentColor = palette.onColor,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInBrowser,
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 4.dp),
                    )
                    Text("Open", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // ── Secondary actions ─────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onCopy,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(end = 4.dp),
                )
                Text("Copy", style = MaterialTheme.typography.labelLarge)
            }
            OutlinedButton(
                onClick = onShare,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(end = 4.dp),
                )
                Text("Share", style = MaterialTheme.typography.labelLarge)
            }
        }

        OutlinedButton(
            onClick = onReInspect,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .height(44.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .padding(end = 4.dp),
            )
            Text("Scan another", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
    )
}

@Composable
private fun UrlPanel(value: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(10.dp),
            )
            .padding(12.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun verdictIcon(level: RiskLevel): ImageVector = when (level) {
    RiskLevel.LOW -> Icons.Default.Check
    RiskLevel.CAUTION -> Icons.Default.WarningAmber
    RiskLevel.HIGH -> Icons.Default.Shield
    RiskLevel.UNKNOWN -> Icons.Default.GppMaybe
}
