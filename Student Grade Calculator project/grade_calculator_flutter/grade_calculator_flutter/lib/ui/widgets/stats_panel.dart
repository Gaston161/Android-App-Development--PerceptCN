// lib/ui/widgets/stats_panel.dart
// ─────────────────────────────────────────────────────────────────────────────
// KPI cards + pie-chart distribution panel.
// ─────────────────────────────────────────────────────────────────────────────

import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:provider/provider.dart';

import '../../core/theme.dart';
import '../../models/batch_result.dart';
import '../../models/grade_scale.dart';
import '../../services/app_state.dart';

class StatsPanel extends StatefulWidget {
  const StatsPanel({super.key});
  @override
  State<StatsPanel> createState() => _StatsPanelState();
}

class _StatsPanelState extends State<StatsPanel> {
  int _touchedIndex = -1;

  @override
  Widget build(BuildContext context) {
    final stats = context.select<AppState, BatchStats?>(
      (s) => s.result?.stats,
    );
    if (stats == null) return const SizedBox.shrink();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _sectionTitle('Vue d\'ensemble'),
        const SizedBox(height: 12),
        _kpiRow(stats),
        const SizedBox(height: 20),
        _sectionTitle('Répartition des mentions'),
        const SizedBox(height: 12),
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(child: _pieChart(stats)),
            const SizedBox(width: 20),
            Expanded(child: _legend(stats)),
          ],
        ),
      ],
    );
  }

  Widget _sectionTitle(String t) => Text(
    t,
    style: const TextStyle(
      fontSize: 15, fontWeight: FontWeight.w700,
      color: AppTheme.textPrimary,
    ),
  );

  // ── KPI row ───────────────────────────────────────────────────────────────

  Widget _kpiRow(BatchStats stats) {
    final items = [
      _KpiItem('Total',    stats.total.toString(),                Icons.people_outline,     AppTheme.primary),
      _KpiItem('Admis',    '${stats.passed}',                     Icons.check_circle_outline,AppTheme.success),
      _KpiItem('Ajournés', '${stats.failed}',                     Icons.cancel_outlined,     AppTheme.danger),
      _KpiItem('Moyenne',  stats.average.toStringAsFixed(2),      Icons.bar_chart,           AppTheme.accent),
      _KpiItem('Médiane',  stats.median.toStringAsFixed(2),       Icons.show_chart,          AppTheme.warning),
      _KpiItem('Réussite', '${stats.passRate.toStringAsFixed(1)}%',Icons.emoji_events_outlined,AppTheme.success),
    ];
    return Wrap(
      spacing: 12, runSpacing: 12,
      children: items.asMap().entries.map((e) => _kpiCard(e.value, e.key)).toList(),
    );
  }

  Widget _kpiCard(_KpiItem item, int idx) => SizedBox(
    width: 140,
    child: Container(
      padding   : const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color       : item.color.withOpacity(0.1),
        border      : Border.all(color: item.color.withOpacity(0.3)),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(item.icon, color: item.color, size: 22),
          const SizedBox(height: 10),
          Text(item.value,
              style: TextStyle(
                fontSize: 24, fontWeight: FontWeight.w800, color: item.color,
              )),
          const SizedBox(height: 4),
          Text(item.label,
              style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary)),
        ],
      ),
    ).animate(delay: Duration(milliseconds: idx * 60))
     .fadeIn(duration: 400.ms).slideY(begin: 0.2, end: 0),
  );

  // ── Pie chart ─────────────────────────────────────────────────────────────

  Widget _pieChart(BatchStats stats) {
    final sections = _buildSections(stats);
    if (sections.isEmpty) return const SizedBox.shrink();

    return SizedBox(
      height: 220,
      child: PieChart(
        PieChartData(
          sectionsSpace     : 3,
          centerSpaceRadius : 50,
          pieTouchData      : PieTouchData(
            touchCallback: (evt, res) {
              setState(() {
                _touchedIndex = res?.touchedSection?.touchedSectionIndex ?? -1;
              });
            },
          ),
          sections: sections,
        ),
      ),
    );
  }

  List<PieChartSectionData> _buildSections(BatchStats stats) {
    final dist   = stats.gradeDistribution;
    final total  = stats.total;
    int   idx    = 0;
    return dist.entries.map((e) {
      final isTouched = idx++ == _touchedIndex;
      final pct       = total > 0 ? (e.value / total) * 100 : 0;
      return PieChartSectionData(
        value      : e.value.toDouble(),
        title      : isTouched ? '${pct.toStringAsFixed(1)}%' : e.key,
        radius     : isTouched ? 72 : 60,
        color      : AppTheme.gradeColor(e.key),
        titleStyle : const TextStyle(
          fontSize: 12, fontWeight: FontWeight.bold, color: Colors.white,
        ),
      );
    }).toList();
  }

  // ── Legend ────────────────────────────────────────────────────────────────

  Widget _legend(BatchStats stats) {
    return Wrap(
      spacing   : 8,
      runSpacing: 8,
      children  : GradeScale.bands.map((band) {
        final count = stats.gradeDistribution[band.letter] ?? 0;
        return Container(
          padding   : const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          decoration: BoxDecoration(
            color       : band.color.withOpacity(0.12),
            border      : Border.all(color: band.color.withOpacity(0.4)),
            borderRadius: BorderRadius.circular(10),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 10, height: 10,
                decoration: BoxDecoration(
                  color: band.color, shape: BoxShape.circle,
                ),
              ),
              const SizedBox(width: 8),
              Text('${band.letter}  ',
                  style: TextStyle(
                    fontSize: 13, fontWeight: FontWeight.w700, color: band.color,
                  )),
              Text('$count étudiant${count != 1 ? 's' : ''}',
                  style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary)),
              const SizedBox(width: 6),
              Text(band.appreciation,
                  style: const TextStyle(fontSize: 10, color: AppTheme.textSecondary)),
            ],
          ),
        );
      }).toList(),
    );
  }
}

class _KpiItem {
  final String label;
  final String value;
  final IconData icon;
  final Color color;
  const _KpiItem(this.label, this.value, this.icon, this.color);
}
