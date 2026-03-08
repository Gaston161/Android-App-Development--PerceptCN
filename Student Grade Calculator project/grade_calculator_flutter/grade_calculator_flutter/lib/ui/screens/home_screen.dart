// lib/ui/screens/home_screen.dart
// ─────────────────────────────────────────────────────────────────────────────
// Main application screen — three-column responsive desktop layout.
// ─────────────────────────────────────────────────────────────────────────────

import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:provider/provider.dart';

import '../../core/theme.dart';
import '../../services/app_state.dart';
import '../widgets/drop_zone.dart';
import '../widgets/export_panel.dart';
import '../widgets/file_list.dart';
import '../widgets/grade_scale_ref.dart';
import '../widgets/stats_panel.dart';
import '../widgets/student_table.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.surface,
      body: Row(
        children: [
          // ── Left sidebar ──
          _Sidebar(),
          // ── Main content ──
          Expanded(child: _MainContent()),
        ],
      ),
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
// Sidebar
// ══════════════════════════════════════════════════════════════════════════════

class _Sidebar extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();

    return Container(
      width     : 300,
      height    : double.infinity,
      decoration: const BoxDecoration(
        color : AppTheme.surfaceVariant,
        border: Border(right: BorderSide(color: AppTheme.border)),
      ),
      child: SingleChildScrollView(
        padding : const EdgeInsets.all(20),
        child   : Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Logo / app name
            _Logo(),
            const SizedBox(height: 24),

            // Drop zone
            const DropZone(),
            const SizedBox(height: 16),

            // Loaded files
            const FileList(),
            const SizedBox(height: 16),

            // Reset button (visible when files loaded)
            if (state.hasResult) ...[
              OutlinedButton.icon(
                onPressed : state.reset,
                icon  : const Icon(Icons.restart_alt, size: 16),
                label : const Text('Réinitialiser'),
                style : OutlinedButton.styleFrom(
                  foregroundColor: AppTheme.danger,
                  side : const BorderSide(color: AppTheme.danger),
                  minimumSize: const Size.fromHeight(44),
                ),
              ),
              const SizedBox(height: 20),
            ],

            // Export panel
            const ExportPanel(),
            const SizedBox(height: 20),

            // Grade scale reference
            const GradeScaleRef(),
            const SizedBox(height: 20),

            // Error log
            if (state.errorLog.isNotEmpty) _ErrorLog(errors: state.errorLog),
          ],
        ),
      ),
    );
  }
}

class _Logo extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Container(
          width     : 40, height: 40,
          decoration: BoxDecoration(
            gradient    : const LinearGradient(
              colors: [AppTheme.primary, AppTheme.accent],
              begin : Alignment.topLeft,
              end   : Alignment.bottomRight,
            ),
            borderRadius: BorderRadius.circular(12),
          ),
          child: const Icon(Icons.school, color: Colors.white, size: 22),
        ),
        const SizedBox(width: 12),
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: const [
            Text('GradeCalc Pro',
                style: TextStyle(
                  fontSize: 16, fontWeight: FontWeight.w800,
                  color: AppTheme.textPrimary,
                )),
            Text('Student Grade Calculator',
                style: TextStyle(fontSize: 10, color: AppTheme.textSecondary)),
          ],
        ),
      ],
    );
  }
}

class _ErrorLog extends StatelessWidget {
  final List<String> errors;
  const _ErrorLog({required this.errors});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding   : const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color       : AppTheme.danger.withOpacity(0.1),
        border      : Border.all(color: AppTheme.danger.withOpacity(0.4)),
        borderRadius: BorderRadius.circular(10),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.warning_amber, size: 14, color: AppTheme.danger),
              SizedBox(width: 6),
              Text('Avertissements',
                  style: TextStyle(fontSize: 12, fontWeight: FontWeight.w700,
                      color: AppTheme.danger)),
            ],
          ),
          const SizedBox(height: 8),
          ...errors.map((e) => Padding(
            padding : const EdgeInsets.only(bottom: 4),
            child   : Text(e,
                style: const TextStyle(fontSize: 11, color: AppTheme.danger)),
          )),
        ],
      ),
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
// Main Content
// ══════════════════════════════════════════════════════════════════════════════

class _MainContent extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();

    return Column(
      children: [
        // Top bar
        _TopBar(),
        // Body
        Expanded(
          child: state.hasResult
              ? _ResultsView()
              : _EmptyState(),
        ),
      ],
    );
  }
}

class _TopBar extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();

    return Container(
      height   : 64,
      padding  : const EdgeInsets.symmetric(horizontal: 24),
      decoration: const BoxDecoration(
        color : AppTheme.surfaceVariant,
        border: Border(bottom: BorderSide(color: AppTheme.border)),
      ),
      child: Row(
        children: [
          // Status
          Expanded(
            child: AnimatedSwitcher(
              duration: 300.ms,
              child: state.statusMessage.isNotEmpty
                  ? Row(
                      key     : ValueKey(state.statusMessage),
                      children: [
                        if (state.status == AppStatus.loading)
                          const SizedBox(
                            width: 16, height: 16,
                            child: CircularProgressIndicator(
                              strokeWidth: 2, color: AppTheme.primary,
                            ),
                          ),
                        if (state.status == AppStatus.done)
                          const Icon(Icons.check_circle_outline,
                              size: 16, color: AppTheme.success),
                        if (state.status == AppStatus.error)
                          const Icon(Icons.error_outline,
                              size: 16, color: AppTheme.danger),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(state.statusMessage,
                              overflow: TextOverflow.ellipsis,
                              style   : TextStyle(
                                fontSize: 13,
                                color   : state.status == AppStatus.error
                                    ? AppTheme.danger
                                    : AppTheme.textSecondary,
                              )),
                        ),
                      ],
                    )
                  : const SizedBox.shrink(),
            ),
          ),
          // Version tag
          Container(
            padding   : const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
            decoration: BoxDecoration(
              color       : AppTheme.primary.withOpacity(0.15),
              borderRadius: BorderRadius.circular(20),
            ),
            child: const Text('v1.0.0',
                style: TextStyle(fontSize: 11, color: AppTheme.primary)),
          ),
        ],
      ),
    );
  }
}

class _EmptyState extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width     : 120, height: 120,
            decoration: BoxDecoration(
              gradient    : LinearGradient(
                colors: [AppTheme.primary.withOpacity(0.2), AppTheme.accent.withOpacity(0.1)],
                begin : Alignment.topLeft,
                end   : Alignment.bottomRight,
              ),
              borderRadius: BorderRadius.circular(30),
            ),
            child: const Icon(Icons.upload_file, size: 56, color: AppTheme.primary),
          ).animate().fadeIn(duration: 600.ms).scaleXY(begin: 0.8, end: 1),
          const SizedBox(height: 24),
          const Text('Bienvenue dans GradeCalc Pro',
              style: TextStyle(
                fontSize: 22, fontWeight: FontWeight.w800,
                color: AppTheme.textPrimary,
              )).animate().fadeIn(delay: 200.ms),
          const SizedBox(height: 10),
          const Text(
            'Glissez vos fichiers Excel dans la zone de gauche\n'
            'ou cliquez sur la zone pour sélectionner vos fichiers.',
            textAlign: TextAlign.center,
            style    : TextStyle(fontSize: 14, color: AppTheme.textSecondary, height: 1.6),
          ).animate().fadeIn(delay: 400.ms),
          const SizedBox(height: 32),
          _FormatHint().animate().fadeIn(delay: 600.ms),
        ],
      ),
    );
  }
}

class _FormatHint extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final cols = [
      ('Nom',       'Colonne A', AppTheme.primary),
      ('Matricule', 'Colonne B', AppTheme.accent),
      ('Note',      'Colonne C', AppTheme.warning),
    ];
    return Container(
      padding   : const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color       : AppTheme.surfaceVariant.withOpacity(0.6),
        border      : Border.all(color: AppTheme.border),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        children: [
          const Text('Format attendu du fichier Excel',
              style: TextStyle(fontSize: 13, fontWeight: FontWeight.w700,
                  color: AppTheme.textPrimary)),
          const SizedBox(height: 14),
          Row(
            mainAxisSize: MainAxisSize.min,
            children    : cols.map((c) => Padding(
              padding: const EdgeInsets.symmetric(horizontal: 8),
              child  : Column(
                children: [
                  Text(c.$1,
                      style: TextStyle(
                        fontSize: 12, fontWeight: FontWeight.w700, color: c.$3,
                      )),
                  const SizedBox(height: 4),
                  Text(c.$2,
                      style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary)),
                ],
              ),
            )).toList(),
          ),
        ],
      ),
    );
  }
}

class _ResultsView extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Padding(
      padding : const EdgeInsets.all(24),
      child   : Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Stats
          const StatsPanel(),
          const SizedBox(height: 24),
          const Divider(color: AppTheme.border),
          const SizedBox(height: 16),
          // Table header
          const Row(
            children: [
              Icon(Icons.table_rows_outlined, color: AppTheme.primary, size: 18),
              SizedBox(width: 8),
              Text('Liste des étudiants',
                  style: TextStyle(
                    fontSize: 15, fontWeight: FontWeight.w700,
                    color   : AppTheme.textPrimary,
                  )),
            ],
          ),
          const SizedBox(height: 12),
          // Table
          const Expanded(child: StudentTable()),
        ],
      ),
    );
  }
}
