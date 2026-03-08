// lib/core/theme.dart
// ─────────────────────────────────────────────────────────────────────────────
// Central theme definition — dark + light, Material 3.
// ─────────────────────────────────────────────────────────────────────────────

import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class AppTheme {
  AppTheme._();

  // Brand palette
  static const Color primary        = Color(0xFF4F6AF5);
  static const Color primaryDark    = Color(0xFF3A52D4);
  static const Color accent         = Color(0xFF00D4B4);
  static const Color danger         = Color(0xFFFF5252);
  static const Color warning        = Color(0xFFFFAB40);
  static const Color success        = Color(0xFF66BB6A);
  static const Color surface        = Color(0xFF1A1D2E);
  static const Color surfaceVariant = Color(0xFF242742);
  static const Color cardBg         = Color(0xFF2A2D45);
  static const Color border         = Color(0xFF3A3D5C);
  static const Color textPrimary    = Color(0xFFF0F2FF);
  static const Color textSecondary  = Color(0xFF9B9FC4);

  // Grade colours
  static const Map<String, Color> gradeColours = {
    'A'  : Color(0xFF66BB6A),
    'B+' : Color(0xFF26C6DA),
    'B'  : Color(0xFF42A5F5),
    'C+' : Color(0xFFAB47BC),
    'C'  : Color(0xFF7E57C2),
    'D+' : Color(0xFFFFCA28),
    'D'  : Color(0xFFFFA726),
    'F'  : Color(0xFFEF5350),
  };

  static Color gradeColor(String letter) =>
      gradeColours[letter] ?? textSecondary;

  static ThemeData get dark {
    final base = ThemeData.dark(useMaterial3: true);
    return base.copyWith(
      colorScheme: ColorScheme.dark(
        primary       : primary,
        secondary     : accent,
        surface       : surface,
        error         : danger,
        onPrimary     : Colors.white,
        onSurface     : textPrimary,
      ),
      scaffoldBackgroundColor: surface,
      textTheme: GoogleFonts.interTextTheme(base.textTheme).apply(
        bodyColor   : textPrimary,
        displayColor: textPrimary,
      ),
      cardTheme: CardTheme(
        color        : cardBg,
        elevation    : 0,
        shape        : RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side        : const BorderSide(color: border, width: 1),
        ),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: primary,
          foregroundColor: Colors.white,
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          textStyle: GoogleFonts.inter(fontWeight: FontWeight.w600, fontSize: 14),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled          : true,
        fillColor       : surfaceVariant,
        border          : OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide  : const BorderSide(color: border),
        ),
        enabledBorder   : OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide  : const BorderSide(color: border),
        ),
        focusedBorder   : OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide  : const BorderSide(color: primary, width: 2),
        ),
        labelStyle : const TextStyle(color: textSecondary),
        hintStyle  : const TextStyle(color: textSecondary),
      ),
      chipTheme: ChipThemeData(
        backgroundColor : surfaceVariant,
        selectedColor   : primary.withOpacity(0.3),
        labelStyle      : GoogleFonts.inter(fontSize: 13, color: textPrimary),
        side            : const BorderSide(color: border),
        shape           : RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      ),
      dividerTheme: const DividerThemeData(color: border, thickness: 1),
      scrollbarTheme: ScrollbarThemeData(
        thumbColor: WidgetStateProperty.all(primary.withOpacity(0.4)),
      ),
    );
  }
}
