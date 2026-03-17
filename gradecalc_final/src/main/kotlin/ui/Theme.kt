// src/main/kotlin/ui/Theme.kt
package com.gradecalc.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

object AppColors {
    val Primary        = Color(0xFF4F6AF5)
    val Accent         = Color(0xFF00D4B4)
    val Danger         = Color(0xFFEF5350)
    val Warning        = Color(0xFFFFAB40)
    val Success        = Color(0xFF66BB6A)
    val Surface        = Color(0xFF1A1D2E)
    val SurfaceVariant = Color(0xFF242742)
    val CardBg         = Color(0xFF2A2D45)
    val Border         = Color(0xFF3A3D5C)
    val TextPrimary    = Color(0xFFF0F2FF)
    val TextSecondary  = Color(0xFF9B9FC4)

    private val gradeMap = mapOf(
        "A"  to Color(0xFF66BB6A),
        "B+" to Color(0xFF26C6DA),
        "B"  to Color(0xFF42A5F5),
        "C+" to Color(0xFFAB47BC),
        "C"  to Color(0xFF7E57C2),
        "D+" to Color(0xFFFFCA28),
        "D"  to Color(0xFFFFA726),
        "F"  to Color(0xFFEF5350)
    )

    fun forGrade(letter: String): Color  = gradeMap[letter] ?: TextSecondary
    fun forScore(score: Double): Color   = when {
        score >= 80 -> Success
        score >= 50 -> Accent
        score >= 40 -> Warning
        else        -> Danger
    }
    fun forFormat(label: String): Color  = when {
        label.contains("xlsx", true) -> Color(0xFF217346)
        label.contains("pdf",  true) -> Color(0xFFDC3545)
        label.contains("xml",  true) -> Color(0xFFFF6B35)
        label.contains("docx", true) -> Color(0xFF2B5899)
        else                         -> Primary
    }

    val colorScheme = darkColorScheme(
        primary   = Primary,
        secondary = Accent,
        surface   = Surface,
        error     = Danger,
        onSurface = TextPrimary
    )
}
