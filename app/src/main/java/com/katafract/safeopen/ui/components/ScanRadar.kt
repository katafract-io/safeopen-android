package com.katafract.safeopen.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.katafract.safeopen.ui.theme.KataGold

/**
 * SafeOpen's branded scan-in-progress indicator. Replaces stock
 * CircularProgressIndicator — three concentric rings with a sweeping
 * gold radar arc, mirroring the iOS InspectingSealView ring treatment.
 */
@Composable
fun ScanRadar(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    accent: Color = KataGold,
) {
    val transition = rememberInfiniteTransition(label = "scan-radar")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val center = Offset(w / 2f, w / 2f)
            val outer = (w / 2f) * 0.96f
            val mid = outer * 0.66f
            val inner = outer * 0.36f

            drawCircle(
                color = color.copy(alpha = 0.18f),
                radius = outer, center = center,
                style = Stroke(width = 1f),
            )
            drawCircle(
                color = color.copy(alpha = 0.14f),
                radius = mid, center = center,
                style = Stroke(width = 1f),
            )
            drawCircle(
                color = color.copy(alpha = 0.10f),
                radius = inner, center = center,
                style = Stroke(width = 1f),
            )

            drawCircle(
                color = accent.copy(alpha = 0.85f * pulse),
                radius = inner * 0.22f,
                center = center,
            )

            val gradient = Brush.sweepGradient(
                0f to Color.Transparent,
                0.65f to Color.Transparent,
                0.85f to accent.copy(alpha = 0.05f),
                0.97f to accent.copy(alpha = 0.55f),
                1f to accent.copy(alpha = 0.85f),
                center = center,
            )
            rotate(degrees = sweep, pivot = center) {
                drawArc(
                    brush = gradient,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(center.x - outer, center.y - outer),
                    size = Size(outer * 2, outer * 2),
                    style = Stroke(width = 2f),
                )
            }
        }
    }
}
