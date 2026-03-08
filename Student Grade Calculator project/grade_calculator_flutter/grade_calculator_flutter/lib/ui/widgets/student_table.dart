// lib/ui/widgets/student_table.dart
// ─────────────────────────────────────────────────────────────────────────────
// Sortable, searchable data table with grade-coloured chips.
// ─────────────────────────────────────────────────────────────────────────────

import 'package:data_table_2/data_table_2.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/theme.dart';
import '../../models/student.dart';
import '../../services/app_state.dart';
import '../../services/grade_calculator.dart';

class StudentTable extends StatelessWidget {
  const StudentTable({super.key});

  @override
  Widget build(BuildContext context) {
    final state    = context.watch<AppState>();
    final students = state.displayStudents;

    if (students.isEmpty) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.school_outlined, size: 64, color: AppTheme.textSecondary.withOpacity(0.4)),
            const SizedBox(height: 12),
            Text('Aucun étudiant trouvé',
                style: TextStyle(
                  color: AppTheme.textSecondary.withOpacity(0.7), fontSize: 15,
                )),
          ],
        ),
      );
    }

    return Column(
      children: [
        _toolbar(context, state),
        const SizedBox(height: 12),
        Expanded(child: _table(context, students, state)),
      ],
    );
  }

  // ── Toolbar (search + count) ───────────────────────────────────────────────

  Widget _toolbar(BuildContext ctx, AppState state) {
    return Row(
      children: [
        Expanded(
          child: TextField(
            onChanged   : state.setSearchQuery,
            style       : const TextStyle(fontSize: 14, color: AppTheme.textPrimary),
            decoration  : InputDecoration(
              hintText : 'Rechercher par nom, matricule ou mention…',
              prefixIcon: const Icon(Icons.search, size: 20, color: AppTheme.textSecondary),
              suffixIcon: state.searchQuery.isNotEmpty
                  ? IconButton(
                      icon    : const Icon(Icons.clear, size: 18),
                      onPressed: () => state.setSearchQuery(''),
                    )
                  : null,
              contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            ),
          ),
        ),
        const SizedBox(width: 16),
        Container(
          padding   : const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
          decoration: BoxDecoration(
            color       : AppTheme.surfaceVariant,
            borderRadius: BorderRadius.circular(10),
            border      : Border.all(color: AppTheme.border),
          ),
          child: Text(
            '${state.displayStudents.length} étudiant(s)',
            style: const TextStyle(fontSize: 13, color: AppTheme.textSecondary),
          ),
        ),
      ],
    );
  }

  // ── Table ─────────────────────────────────────────────────────────────────

  Widget _table(BuildContext ctx, List<Student> students, AppState state) {
    return DataTable2(
      columnSpacing    : 16,
      horizontalMargin : 16,
      minWidth         : 700,
      headingRowColor  : WidgetStateProperty.all(AppTheme.surfaceVariant),
      headingRowHeight : 48,
      dataRowHeight    : 52,
      dividerThickness : 0.5,
      columns          : [
        _col(ctx, 'N°',         null,          ColumnSize.S),
        _col(ctx, 'Matricule',  SortKey.matricule, ColumnSize.M),
        _col(ctx, 'Nom',        SortKey.name,  ColumnSize.L),
        _col(ctx, 'Note',       SortKey.grade, ColumnSize.S),
        _col(ctx, 'Mention',    SortKey.letter,ColumnSize.S),
        _col(ctx, 'Appréciation', null,        ColumnSize.M),
        _col(ctx, 'Résultat',   null,          ColumnSize.M),
      ],
      rows: students.asMap().entries.map((e) => _row(e.key, e.value, state)).toList(),
    );
  }

  DataColumn2 _col(
    BuildContext ctx,
    String label,
    SortKey? sortKey,
    ColumnSize size,
  ) {
    final state = ctx.read<AppState>();
    return DataColumn2(
      size : size,
      label: sortKey != null
          ? GestureDetector(
              onTap  : () => state.setSort(sortKey),
              cursor : SystemMouseCursors.click,
              child  : Row(
                children: [
                  Text(label,
                      style: const TextStyle(
                        fontWeight: FontWeight.w600,
                        color     : AppTheme.textPrimary,
                        fontSize  : 13,
                      )),
                  if (state.sortKey == sortKey)
                    Icon(
                      state.sortAscending
                          ? Icons.arrow_upward
                          : Icons.arrow_downward,
                      size : 14,
                      color: AppTheme.primary,
                    ),
                ],
              ),
            )
          : Text(label,
              style: const TextStyle(
                fontWeight: FontWeight.w600,
                color     : AppTheme.textPrimary,
                fontSize  : 13,
              )),
    );
  }

  DataRow2 _row(int index, Student s, AppState state) {
    final isEven = index.isEven;
    return DataRow2(
      color: WidgetStateProperty.all(
        isEven ? AppTheme.cardBg : AppTheme.surfaceVariant.withOpacity(0.3),
      ),
      cells: [
        DataCell(Text('${index + 1}',
            style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary))),
        DataCell(Text(s.matricule,
            style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w500))),
        DataCell(Text(s.name,
            style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w500))),
        DataCell(Container(
          padding    : const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
          decoration : BoxDecoration(
            color       : _gradeScoreColor(s.rawGrade).withOpacity(0.15),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Text(
            s.rawGrade.toStringAsFixed(1),
            style: TextStyle(
              fontSize: 13, fontWeight: FontWeight.w700,
              color: _gradeScoreColor(s.rawGrade),
            ),
          ),
        )),
        DataCell(_gradeBadge(s.letterGrade)),
        DataCell(Text(s.appreciation,
            style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary))),
        DataCell(_remarkBadge(s.remark, s.hasPassed)),
      ],
    );
  }

  Widget _gradeBadge(String letter) {
    final color = AppTheme.gradeColor(letter);
    return Container(
      padding   : const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color       : color.withOpacity(0.18),
        borderRadius: BorderRadius.circular(8),
        border      : Border.all(color: color.withOpacity(0.6)),
      ),
      child: Text(letter,
          style: TextStyle(
            fontSize: 12, fontWeight: FontWeight.w800, color: color,
          )),
    );
  }

  Widget _remarkBadge(String remark, bool passed) {
    final color = passed ? AppTheme.success : AppTheme.danger;
    return Container(
      padding   : const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color       : color.withOpacity(0.12),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(passed ? Icons.check : Icons.close,
              size: 12, color: color),
          const SizedBox(width: 4),
          Text(remark, style: TextStyle(fontSize: 12, color: color, fontWeight: FontWeight.w600)),
        ],
      ),
    );
  }

  Color _gradeScoreColor(double score) {
    if (score >= 80) return AppTheme.success;
    if (score >= 50) return AppTheme.accent;
    if (score >= 40) return AppTheme.warning;
    return AppTheme.danger;
  }
}
