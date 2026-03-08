// lib/ui/widgets/file_list.dart
// ─────────────────────────────────────────────────────────────────────────────
// Side-panel showing loaded files with remove action.
// ─────────────────────────────────────────────────────────────────────────────

import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:provider/provider.dart';

import '../../core/theme.dart';
import '../../services/app_state.dart';

class FileList extends StatelessWidget {
  const FileList({super.key});

  @override
  Widget build(BuildContext context) {
    final files = context.select<AppState, List<String>>((s) => s.loadedFiles);
    if (files.isEmpty) return const SizedBox.shrink();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 6),
          child  : Row(
            children: [
              const Icon(Icons.folder_open, size: 16, color: AppTheme.textSecondary),
              const SizedBox(width: 8),
              Text('Fichiers chargés (${files.length})',
                  style: const TextStyle(
                    fontSize: 13, fontWeight: FontWeight.w600,
                    color   : AppTheme.textSecondary,
                  )),
            ],
          ),
        ),
        ...files.asMap().entries.map((e) => _fileTile(context, e.value, e.key)),
      ],
    );
  }

  Widget _fileTile(BuildContext ctx, String path, int idx) {
    final name = path.split(Platform.pathSeparator).last;
    return Container(
      margin   : const EdgeInsets.only(bottom: 6),
      padding  : const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      decoration: BoxDecoration(
        color       : AppTheme.surfaceVariant,
        border      : Border.all(color: AppTheme.border),
        borderRadius: BorderRadius.circular(10),
      ),
      child: Row(
        children: [
          const Icon(Icons.table_view, size: 18, color: AppTheme.accent),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(name,
                    maxLines : 1,
                    overflow : TextOverflow.ellipsis,
                    style    : const TextStyle(
                      fontSize: 13, fontWeight: FontWeight.w500,
                      color   : AppTheme.textPrimary,
                    )),
                Text(path,
                    maxLines : 1,
                    overflow : TextOverflow.ellipsis,
                    style    : const TextStyle(
                      fontSize: 10, color: AppTheme.textSecondary,
                    )),
              ],
            ),
          ),
          IconButton(
            icon     : const Icon(Icons.close, size: 16),
            color    : AppTheme.textSecondary,
            tooltip  : 'Retirer ce fichier',
            onPressed: () => ctx.read<AppState>().removeFile(path),
          ),
        ],
      ),
    ).animate(delay: Duration(milliseconds: idx * 50))
     .fadeIn(duration: 300.ms).slideX(begin: -0.05, end: 0);
  }
}
