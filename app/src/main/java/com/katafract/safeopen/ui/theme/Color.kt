package com.katafract.safeopen.ui.theme

import androidx.compose.ui.graphics.Color

// ── SafeOpen brand palette ──────────────────────────────────────────────
// Semantic verdict colors are the heart of the product (Safe / Suspicious /
// Dangerous). Chrome borrows the Katafract dark+gold accent used by
// SafeOpen-iOS for loading + splash treatments.

// Verdict — Safe
internal val SafeGreen500   = Color(0xFF10B981) // emerald
internal val SafeGreen600   = Color(0xFF059669)
internal val SafeGreen300   = Color(0xFF34D399)
internal val SafeGreenSoft  = Color(0xFFD1FAE5)
internal val SafeGreenDark  = Color(0xFF064E3B)

// Verdict — Suspicious / Caution
internal val CautionAmber500 = Color(0xFFF59E0B)
internal val CautionAmber600 = Color(0xFFD97706)
internal val CautionAmberSoft = Color(0xFFFEF3C7)
internal val CautionAmberDark = Color(0xFF78350F)

// Verdict — Dangerous
internal val DangerRed500 = Color(0xFFEF4444)
internal val DangerRed600 = Color(0xFFDC2626)
internal val DangerRed300 = Color(0xFFF87171)
internal val DangerRedSoft = Color(0xFFFEE2E2)
internal val DangerRedDark = Color(0xFF7F1D1D)

// Neutrals
internal val Neutral50  = Color(0xFFFAFAFA)
internal val Neutral100 = Color(0xFFF3F4F6)
internal val Neutral200 = Color(0xFFE5E7EB)
internal val Neutral300 = Color(0xFFD1D5DB)
internal val Neutral400 = Color(0xFF9CA3AF)
internal val Neutral500 = Color(0xFF6B7280)
internal val Neutral700 = Color(0xFF374151)
internal val Neutral800 = Color(0xFF1F2937)
internal val Neutral900 = Color(0xFF111827)
internal val NeutralInk = Color(0xFF0F172A)

// Katafract chrome accent — used sparingly (splash, loading, brand chip).
// Mirrors SafeOpen-iOS kataGold + kataMidnight.
internal val KataGold     = Color(0xFFFCC42A)
internal val KataGoldSoft = Color(0xFFFFE599)
internal val KataMidnight = Color(0xFF0F172A)
internal val KataSapphire = Color(0xFF1E3A8A)
