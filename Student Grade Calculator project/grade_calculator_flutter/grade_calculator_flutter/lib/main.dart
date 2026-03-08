// lib/main.dart
// ─────────────────────────────────────────────────────────────────────────────
// Application entry point.
// Enables desktop window with minimum / preferred size.
// ─────────────────────────────────────────────────────────────────────────────

import 'package:flutter/material.dart';

import 'app.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();

  // On desktop, enforce a minimum window size via platform channel
  // (actual window size config is in the platform-specific runners).
  runApp(const GradeCalcApp());
}
