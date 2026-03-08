// lib/ui/widgets/drop_zone.dart
// ─────────────────────────────────────────────────────────────────────────────
// Drag-and-drop + click-to-browse file upload zone.
// ─────────────────────────────────────────────────────────────────────────────

import 'package:flutter/material.dart';
import 'package:desktop_drop/desktop_drop.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:provider/provider.dart';

import '../../core/theme.dart';
import '../../core/constants.dart';
import '../../services/app_state.dart';

class DropZone extends StatefulWidget {
  const DropZone({super.key});

  @override
  State<DropZone> createState() => _DropZoneState();
}

class _DropZoneState extends State<DropZone> {
  bool _hovering = false;

  @override
  Widget build(BuildContext context) {
    return DropTarget(
      onDragEntered : (_) => setState(() => _hovering = true),
      onDragExited  : (_) => setState(() => _hovering = false),
      onDragDone    : (details) {
        setState(() => _hovering = false);
        final paths = details.files
            .where((f) => AppConstants.allowedExtensions
                .any((ext) => f.path.toLowerCase().endsWith('.$ext')))
            .map((f) => f.path)
            .toList();
        if (paths.isNotEmpty) {
          context.read<AppState>().loadFiles(paths);
        } else {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Seuls les fichiers .xlsx/.xls sont acceptés.')),
          );
        }
      },
      child: AnimatedContainer(
        duration    : const Duration(milliseconds: 200),
        height      : 200,
        decoration  : BoxDecoration(
          color        : _hovering
              ? AppTheme.primary.withOpacity(0.12)
              : AppTheme.surfaceVariant.withOpacity(0.6),
          border       : Border.all(
            color: _hovering ? AppTheme.primary : AppTheme.border,
            width: _hovering ? 2 : 1.5,
          ),
          borderRadius : BorderRadius.circular(20),
        ),
        child: InkWell(
          onTap        : _pickFiles,
          borderRadius : BorderRadius.circular(20),
          child        : Center(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(
                  _hovering ? Icons.file_download : Icons.cloud_upload_outlined,
                  size : 48,
                  color: _hovering ? AppTheme.primary : AppTheme.textSecondary,
                ).animate(target: _hovering ? 1 : 0)
                 .scaleXY(end: 1.15, duration: 200.ms),
                const SizedBox(height: 14),
                Text(
                  _hovering
                      ? 'Déposez ici !'
                      : 'Glissez vos fichiers Excel ici',
                  style: TextStyle(
                    fontSize   : 16,
                    fontWeight : FontWeight.w600,
                    color      : _hovering ? AppTheme.primary : AppTheme.textPrimary,
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  'ou cliquez pour parcourir  •  .xlsx / .xls  •  multi-sélection',
                  style: const TextStyle(
                    fontSize: 12, color: AppTheme.textSecondary,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Future<void> _pickFiles() async {
    final result = await FilePicker.platform.pickFiles(
      type             : FileType.custom,
      allowedExtensions: AppConstants.allowedExtensions,
      allowMultiple    : true,
    );
    if (result != null && result.files.isNotEmpty) {
      final paths = result.files.map((f) => f.path!).toList();
      if (mounted) context.read<AppState>().loadFiles(paths);
    }
  }
}
