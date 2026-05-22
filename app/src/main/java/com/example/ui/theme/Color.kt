package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ── Primary: Electric Indigo ───────────────────────────────────────────────────
val Indigo10  = Color(0xFF0A0D40)
val Indigo20  = Color(0xFF1E2580)
val Indigo40  = Color(0xFF4955D5)  // --accent light  oklch(0.55 0.18 268)
val Indigo60  = Color(0xFF7478E0)
val Indigo80  = Color(0xFF8E92E8)  // --accent dark   oklch(0.74 0.16 268)
val Indigo90  = Color(0xFFD0D2F8)
val Indigo95  = Color(0xFFEEEFFC)

// ── Secondary: Saffron/Warm ────────────────────────────────────────────────────
val Violet20  = Color(0xFF5C3E00)
val Violet40  = Color(0xFFC08B0C)  // --warm          oklch(0.72 0.14 65)
val Violet80  = Color(0xFFD4A83A)
val Violet90  = Color(0xFFFFF3D6)

// ── Tertiary: Leaf/Green ───────────────────────────────────────────────────────
val Teal10    = Color(0xFF0D3020)
val Teal40    = Color(0xFF4A8053)  // --leaf          oklch(0.62 0.14 150)
val Teal80    = Color(0xFF72B990)
val Teal90    = Color(0xFFD4EDE0)

// ── Semantic ───────────────────────────────────────────────────────────────────
val SuccessGreen = Color(0xFF4A8053)
val WarningAmber = Color(0xFFC08B0C)
val ErrorRed     = Color(0xFFD14B3D)

// ── Light surfaces (warm paper) ────────────────────────────────────────────────
val Paper      = Color(0xFFF9F8F4)  // --paper        background
val Paper2     = Color(0xFFF4F2EC)  // --paper-2      surface
val Paper3     = Color(0xFFECEAE3)  // --paper-3      surfaceVariant
val Paper4     = Color(0xFFE3DFD7)  // --paper-4      outlineVariant
val LineLight  = Color(0xFFDDDBD4)  // --line         outline

// ── Dark surfaces (deep blue-slate) ───────────────────────────────────────────
val PaperDark  = Color(0xFF171626)  // --paper dark
val Paper2Dark = Color(0xFF1D1C2D)  // --paper-2 dark
val Paper3Dark = Color(0xFF232236)  // --paper-3 dark
val Paper4Dark = Color(0xFF2A2940)  // --paper-4 dark
val LineDark   = Color(0xFF2D2C40)  // --line dark

// ── Backward-compat aliases (referenced throughout codebase) ──────────────────
// neutral surfaces
val Neutral6   = Color(0xFF100F1E)
val Neutral10  = PaperDark
val Neutral14  = Paper2Dark
val Neutral18  = Paper3Dark
val Neutral22  = Paper4Dark
val Neutral87  = LineLight
val Neutral93  = Paper4
val Neutral96  = Paper3
val Neutral98  = Paper2
val Neutral99  = Paper
// old brand aliases
val ScholarBlue80    = Indigo80
val ScholarBlueMid80 = Violet80
val ScholarAccent80  = Teal80
val ScholarBlue40    = Indigo40
val ScholarBlueMid40 = Violet40
val ScholarAccent40  = Teal40
val Surface0Dark     = PaperDark
val Surface1Dark     = Paper2Dark
val Surface2Dark     = Paper3Dark
val Surface3Dark     = Paper4Dark
val Surface0Light    = Paper
val Surface1Light    = Paper2
val Surface2Light    = Paper3
val Surface3Light    = Paper4
val Purple80         = Indigo80
val PurpleGrey80     = Violet80
val Pink80           = Teal80
val Purple40         = Indigo40
val PurpleGrey40     = Violet40
val Pink40           = Teal40
