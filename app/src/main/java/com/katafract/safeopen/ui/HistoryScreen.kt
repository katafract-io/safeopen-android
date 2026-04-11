package com.katafract.safeopen.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.katafract.safeopen.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    history: List<InspectionResult>,
    onNavigateBack: () -> Unit,
    onSelectResult: (InspectionResult) -> Unit,
    onClearHistory: () -> Unit,
    viewModel: MainViewModel? = null,
    isPro: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val historyToShow = if (isPro) history else history.take(10)
    val isLimited = !isPro && history.size > 10

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.Close, contentDescription = "Back")
            }

            Text(
                "History",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )

            if (historyToShow.isNotEmpty()) {
                IconButton(onClick = onClearHistory) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear history")
                }
            }
        }

        // Pro upgrade banner
        if (isLimited && viewModel != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Free: Last 10 scans",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Upgrade to Pro for unlimited history",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.launchBilling(context as Activity)
                        },
                        modifier = Modifier
                            .size(80.dp, 32.dp)
                            .padding(start = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Upgrade", fontSize = 11.sp)
                    }
                }
            }
        }

        // History list or empty state
        if (historyToShow.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No history yet.\nStart by scanning a QR code or pasting a link.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                items(historyToShow) { result ->
                    HistoryItem(
                        result = result,
                        onClick = { onSelectResult(result) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                item {
                    Box(modifier = Modifier.padding(bottom = 32.dp))
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(
    result: InspectionResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Risk level indicator
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = when (result.riskLevel) {
                        RiskLevel.LOW -> Color(0xFF10B981)
                        RiskLevel.CAUTION -> Color(0xFFF59E0B)
                        RiskLevel.HIGH -> Color(0xFFEF4444)
                        RiskLevel.UNKNOWN -> Color(0xFF6B7280)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (result.riskLevel) {
                    RiskLevel.LOW -> "✓"
                    RiskLevel.HIGH -> "!"
                    else -> "?"
                },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }

        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = result.payload.rawValue,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "${result.payload.type} • ${formatTime(result.inspectedAt)}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Risk level label
        Text(
            text = result.riskLevel.displayTitle,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = when (result.riskLevel) {
                RiskLevel.LOW -> Color(0xFF10B981)
                RiskLevel.CAUTION -> Color(0xFFF59E0B)
                RiskLevel.HIGH -> Color(0xFFEF4444)
                RiskLevel.UNKNOWN -> Color(0xFF6B7280)
            }
        )
    }
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / (1000 * 60)
    val hours = diff / (1000 * 60 * 60)
    val days = diff / (1000 * 60 * 60 * 24)

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
