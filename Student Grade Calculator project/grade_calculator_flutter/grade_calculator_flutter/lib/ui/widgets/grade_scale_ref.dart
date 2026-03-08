// lib/ui/widgets/grade_scale_ref.dart
// ─────────────────────────────────────────────────────────────────────────────
// Read-only reference table showing all grading bands.
// ─────────────────────────────────────────────────────────────────────────────

import 'package:flutter/material.dart';
import '../../core/theme.dart';
import '../../models/grade_scale.dart';

class GradeScaleRef extends StatelessWidget {
  const GradeScaleRef({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding   : const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color       : AppTheme.cardBg,
        border      : Border.all(color: AppTheme.border),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.grading, size: 16, color: AppTheme.primary),
              SizedBox(width: 8),
              Text('Barème de notation',
                  style: TextStyle(
                    fontSize: 13, fontWeight: FontWeight.w700,
                    color   : AppTheme.textPrimary,
                  )),
            ],
          ),
          const SizedBox(height: 12),
          ...GradeScale.bands.map(_bandRow),
        ],
      ),
    );
  }

  Widget _bandRow(GradeBand band) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child  : Row(
        children: [
          SizedBox(
            width: 30,
            child: Text(band.letter,
                style: TextStyle(
                  fontSize: 12, fontWeight: FontWeight.w800,
                  color: band.color,
                )),
          ),
          Expanded(
            child: ClipRRect(
              borderRadius: BorderRadius.circular(4),
              child: LinearProgressIndicator(
                value            : band.maxInclusive / 100,
                minHeight        : 8,
                backgroundColor  : AppTheme.border,
                valueColor       : AlwaysStoppedAnimation(band.color.withOpacity(0.5)),
              ),
            ),
          ),
          const SizedBox(width: 10),
          Text(
            '${band.minInclusive.toInt()}–${band.maxInclusive.toInt()}',
            style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary),
          ),
        ],
      ),
    );
  }
}
