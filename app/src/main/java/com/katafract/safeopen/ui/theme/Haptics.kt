package com.katafract.safeopen.ui.theme

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalView
import com.katafract.safeopen.models.RiskLevel
import kotlinx.coroutines.delay

// Compose's HapticFeedback is limited to LongPress + TextHandleMove on
// older Compose versions. For richer effects (tick, reject, double-tap)
// we drop down to `View.performHapticFeedback` which exposes the full
// Android constants on API 30+.

object SafeOpenHaptics {

    /** Light tick — paste button, copy, share, FAB tap. */
    @Composable
    fun rememberTickProvider(): () -> Unit {
        val view = LocalView.current
        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
        return {
            if (!performView(view, ContextClickConstant)) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    /** Reject — used when scan returns DANGEROUS. Heavy double-pulse. */
    suspend fun fireReject(view: View, fallback: HapticFeedback) {
        if (!performView(view, RejectConstant)) {
            // No native Reject available — emulate with two LongPress pulses.
            fallback.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(110)
            fallback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    /** Confirm — used when scan returns SAFE. Single soft tick. */
    fun fireConfirm(view: View, fallback: HapticFeedback) {
        if (!performView(view, ConfirmConstant)) {
            fallback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    /** Caution — used when scan returns SUSPICIOUS. Single firm tap. */
    fun fireCaution(view: View, fallback: HapticFeedback) {
        if (!performView(view, GestureStartConstant)) {
            fallback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    /**
     * Pick the right haptic for a verdict and play it. Caller passes the
     * current view + Compose haptic so we can fall back gracefully.
     */
    suspend fun fireForRisk(level: RiskLevel, view: View, fallback: HapticFeedback) = when (level) {
        RiskLevel.LOW -> fireConfirm(view, fallback)
        RiskLevel.CAUTION -> fireCaution(view, fallback)
        RiskLevel.HIGH -> fireReject(view, fallback)
        RiskLevel.UNKNOWN -> fireCaution(view, fallback)
    }

    // ── private ──────────────────────────────────────────────────────

    /** `HapticFeedbackConstants.REJECT` is API 30+. Guard at runtime. */
    private val RejectConstant: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.REJECT
        } else -1

    private val ConfirmConstant: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else -1

    private val ContextClickConstant: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            HapticFeedbackConstants.CONTEXT_CLICK
        } else -1

    private val GestureStartConstant: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.GESTURE_START
        } else -1

    private fun performView(view: View, constant: Int): Boolean {
        if (constant == -1) return false
        return view.performHapticFeedback(constant)
    }
}
