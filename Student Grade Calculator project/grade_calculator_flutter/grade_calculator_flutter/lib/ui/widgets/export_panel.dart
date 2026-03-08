// lib/ui/widgets/export_panel.dart
// ─────────────────────────────────────────────────────────────────────────────
// Format selector + export button + open-file button.
// ─────────────────────────────────────────────────────────────────────────────

import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:open_filex/open_filex.dart';
import 'package:provider/provider.dart';

import '../../core/constants.dart';
import '../../core/theme.dart';
import '../../services/app_state.dart';

class ExportPanel extends StatelessWidget {
  const ExportPanel({super.key});

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();

    return Container(
      padding   : const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color       : AppTheme.cardBg,
        border      : Border.all(color: AppTheme.border),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.file_download_outlined, color: AppTheme.primary, size: 20),
              SizedBox(width: 8),
              Text('Exporter le rapport',
                  style: TextStyle(
                    fontSize: 15, fontWeight: FontWeight.w700,
                    color: AppTheme.textPrimary,
                  )),
            ],
          ),
          const SizedBox(height: 16),

          // Format chips
          Wrap(
            spacing: 8, runSpacing: 8,
            children: AppConstants.exportFormats.map((fmt) {
              final selected = fmt == state.selectedFormat;
              final icon     = _iconFor(fmt);
              final color    = _colorFor(fmt);
              return AnimatedContainer(
                duration: 150.ms,
                child: ChoiceChip(
                  label       : Row(
                    mainAxisSize: MainAxisSize.min,
                    children    : [
                      Icon(icon, size: 15, color: selected ? Colors.white : color),
                      const SizedBox(width: 6),
                      Text(fmt, style: TextStyle(
                        color: selected ? Colors.white : AppTheme.textPrimary,
                        fontWeight: FontWeight.w500,
                      )),
                    ],
                  ),
                  selected     : selected,
                  onSelected   : (_) => state.setFormat(fmt),
                  selectedColor: color,
                  backgroundColor: AppTheme.surfaceVariant,
                  side        : BorderSide(
                    color: selected ? color : AppTheme.border,
                    width: selected ? 0 : 1,
                  ),
                ),
              );
            }).toList(),
          ),

          const SizedBox(height: 16),
          const Divider(color: AppTheme.border, height: 1),
          const SizedBox(height: 16),

          // Export button
          Row(
            children: [
              Expanded(
                child: ElevatedButton.icon(
                  onPressed : state.hasResult
                      ? state.export
                      : null,
                  icon  : state.status == AppStatus.loading
                      ? const SizedBox(
                          width : 18, height: 18,
                          child : CircularProgressIndicator(
                            strokeWidth: 2, color: Colors.white,
                          ),
                        )
                      : const Icon(Icons.download_rounded, size: 18),
                  label : Text('Exporter en ${state.selectedFormat}'),
                  style : ElevatedButton.styleFrom(
                    backgroundColor: _colorFor(state.selectedFormat),
                    padding        : const EdgeInsets.symmetric(vertical: 14),
                  ),
                ),
              ),
              if (state.lastExportPath != null) ...[
                const SizedBox(width: 10),
                OutlinedButton.icon(
                  onPressed : () => OpenFilex.open(state.lastExportPath!),
                  icon  : const Icon(Icons.open_in_new, size: 16),
                  label : const Text('Ouvrir'),
                  style : OutlinedButton.styleFrom(
                    foregroundColor: AppTheme.accent,
                    side : const BorderSide(color: AppTheme.accent),
                    padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 16),
                  ),
                ),
              ],
            ],
          ),

          // Last export path
          if (state.lastExportPath != null)
            Padding(
              padding : const EdgeInsets.only(top: 10),
              child   : Row(
                children: [
                  const Icon(Icons.check_circle_outline, size: 14, color: AppTheme.success),
                  const SizedBox(width: 6),
                  Expanded(
                    child: Text(
                      state.lastExportPath!,
                      style: const TextStyle(
                        fontSize: 11, color: AppTheme.textSecondary,
                      ),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }

  IconData _iconFor(String fmt) => switch (fmt) {
    AppConstants.fmtExcel => Icons.table_chart_outlined,
    AppConstants.fmtPdf   => Icons.picture_as_pdf_outlined,
    AppConstants.fmtXml   => Icons.code,
    AppConstants.fmtWord  => Icons.description_outlined,
    _                     => Icons.file_download_outlined,
  };

  Color _colorFor(String fmt) => switch (fmt) {
    AppConstants.fmtExcel => const Color(0xFF217346),
    AppConstants.fmtPdf   => const Color(0xFFDC3545),
    AppConstants.fmtXml   => const Color(0xFFFF6B35),
    AppConstants.fmtWord  => const Color(0xFF2B5899),
    _                     => AppTheme.primary,
  };
}
