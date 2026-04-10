package com.katafract.safeopen.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Back")
            }

            Text(
                "Inspection Result",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopCenter)
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
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Risk factors
        if (result.riskFactors.isNotEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "Risk Factors",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                result.riskFactors.forEach { factor ->
                    Text(
                        "• $factor",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Text("", modifier = Modifier.padding(bottom = 16.dp))
            }
        }

        // Redirect hops
        if (result.redirectHops.isNotEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "Redirect Chain",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                result.redirectHops.forEach { hop ->
                    Text(
                        hop,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
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

                Text(
                    result.finalUrl,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }
        }

        // Action buttons
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
            if (result.canOpenSafely) {
                Button(
                    onClick = {
                        if (result.finalUrl != null) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.finalUrl))
                            context.startActivity(intent)
                        }
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
                onClick = { onReInspect(result) },
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
            ) {
                Text("Re-Inspect")
            }

            OutlinedButton(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, result.finalUrl ?: result.payload.normalizedValue)
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

        Box(modifier = Modifier.padding(bottom = 32.dp))
    }
}

@Composable
private fun <T> Row.height(height: androidx.compose.ui.unit.Dp): Modifier {
    return this.then(Modifier.size(height = height))
}
