// 📄 app/src/main/java/com/perceptnote/ui/theme/Color.kt
package com.perceptnote.ui.theme

import androidx.compose.ui.graphics.Color

// === Palette PerceptNote — Thème principal ===
// Inspiré d'un bleu profond + accent indigo pour évoquer l'intelligence et la clarté

// Primaires
val Primary80 = Color(0xFF4A90D9)       // Bleu vif — éléments actifs
val Primary40 = Color(0xFF1A5FAD)       // Bleu foncé — texte et icônes
val PrimaryContainer80 = Color(0xFFD4E8FF)
val PrimaryContainer40 = Color(0xFF003870)

// Secondaires (indigo/violet)
val Secondary80 = Color(0xFF7C6EAD)
val Secondary40 = Color(0xFF4B3E7A)
val SecondaryContainer80 = Color(0xFFE6DFFF)
val SecondaryContainer40 = Color(0xFF322460)

// Tertiaires (cyan pour les capteurs actifs)
val Tertiary80 = Color(0xFF3DBCB8)
val Tertiary40 = Color(0xFF006E6B)

// Surfaces
val Surface = Color(0xFFF5F9FF)
val SurfaceVariant = Color(0xFFE0EAFF)
val Background = Color(0xFFF8FAFF)

// Couleurs fonctionnelles
val RecordingRed = Color(0xFFE53935)    // Indicateur enregistrement
val RecordingRedAlpha = Color(0x33E53935)
val OcrGreen = Color(0xFF43A047)        // OCR actif
val GpsOrange = Color(0xFFFF8F00)       // GPS actif
val AccelPurple = Color(0xFF8E24AA)     // Accéléromètre

// Dark theme
val PrimaryDark = Color(0xFF94C8FF)
val BackgroundDark = Color(0xFF0D1B2A)
val SurfaceDark = Color(0xFF1A2E42)

// EchoesClass — Mode dyslexie
val DyslexiaBackground = Color(0xFFFFFACD)  // Jaune très pâle
val DyslexiaSurface = Color(0xFFFFF8B3)
val DyslexiaText = Color(0xFF1A1A1A)
