package com.katafract.safeopen.ui

import android.content.Intent
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katafract.safeopen.models.InspectionResult
import com.katafract.safeopen.models.RiskLevel

@Composable
fun ResultScreen(
    result: InspectionResult,
    onNavigateBack: () -> Unit,
    onReInspect: (InspectionResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Back")
            }

            Text(
                "Inspection Result",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )
        }

        // Risk indicator circle
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.CenterHorizontally)
                .background(
                    color = when (result.riskLevel) {
                        RiskLevel.LOW -> Color(0xFF10B981) // Emerald green
                        RiskLevel.CAUTION -> Color(0xFFF59E0B) // Amber
                        RiskLevel.HIGH -> Color(0xFFEF4444) // Red
                        RiskLevel.UNKNOWN -> Color(0xFF6B7280) // Gray
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (result.riskLevel) {
                    RiskLevel.LOW -> Icons.Default.Done
                    RiskLevel.HIGH -> Icons.Default.Close
                    else -> Icons.Default.Warning
                },
                contentDescription = result.riskLevel.displayTitle,
                tint = Color.White,
                modifier = Modifier.size(60.dp)
            )
        }

        // Risk level title
        Text(
            result.riskLevel.displayTitle,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 20.dp)
        )

        // Payload type
        Text(
            result.payload.type.toString(),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 20.dp)
        )

        // Title and summary
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                result.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                result.summary,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
                lineHeight = 1.5.sp
            )
        }

        // Risk factors / threat signals
        if (result.riskFactors.isNotEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "Threat Signals",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        result.riskFactors.forEach { factor ->
                            AssistChip(
                                onClick = { },
                                label = { Text(factor, fontSize = 11.sp) },
                                modifier = Modifier.padding(vertical = 4.dp),
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = when {
                                        result.riskLevel == RiskLevel.HIGH -> Color(0xFFEF4444).copy(alpha = 0.1f)
                                        result.riskLevel == RiskLevel.CAUTION -> Color(0xFFF59E0B).copy(alpha = 0.1f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }

        // Original value
        if (result.payload.rawValue != result.finalUrl) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "Original Input",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        result.payload.rawValue,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text("", modifier = Modifier.padding(bottom = 16.dp))
            }
        }

        // Redirect hops
        if (result.redirectHops.isNotEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "Redirect Chain (${result.redirectHops.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                result.redirectHops.forEach { hop ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(8.dp)
                            .padding(bottom = 4.dp)
                    ) {
                        Text(
                            hop,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text("", modifier = Modifier.padding(bottom = 16.dp))
            }
        }

        // Final URL
        if (result.finalUrl != null && result.payload.type.toString().contains("URL")) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "Final URL",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        result.finalUrl,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text("", modifier = Modifier.padding(bottom = 16.dp))
            }
        }

        // Primary actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            // Block button
            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFEF4444)
                )
            ) {
                Icon(Icons.Default.Close, contentDescription = "Block", modifier = Modifier.padding(end = 4.dp))
                Text("Block")
            }

            // Open button (conditional)
            if (result.canOpenSafely && result.finalUrl != null) {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.finalUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    )
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = "Open", modifier = Modifier.padding(end = 4.dp))
                    Text("Open")
                }
            }
        }

        // Secondary actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = android.content.ClipData.newPlainText("url", result.finalUrl ?: result.payload.rawValue)
                    clipboard.setPrimaryClip(clip)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.padding(end = 4.dp))
                Text("Copy")
            }

            OutlinedButton(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, result.finalUrl ?: result.payload.rawValue)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share"))
                },
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.padding(end = 4.dp))
                Text("Share")
            }
        }

        // Re-inspect button
        OutlinedButton(
            onClick = { onReInspect(result) },
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp)
        ) {
            Text("Scan Another")
        }

        Box(modifier = Modifier.padding(bottom = 32.dp))
    }
}
