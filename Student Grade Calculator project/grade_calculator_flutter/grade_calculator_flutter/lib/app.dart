// lib/app.dart
// ─────────────────────────────────────────────────────────────────────────────

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import 'core/theme.dart';
import 'services/app_state.dart';
import 'ui/screens/home_screen.dart';

class GradeCalcApp extends StatelessWidget {
  const GradeCalcApp({super.key});

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider(
      create: (_) => AppState(),
      child : MaterialApp(
        title          : 'GradeCalc Pro',
        theme          : AppTheme.dark,
        debugShowCheckedModeBanner: false,
        home           : const HomeScreen(),
      ),
    );
  }
}
