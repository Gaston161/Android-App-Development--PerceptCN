// lib/models/batch_result.dart
// ─────────────────────────────────────────────────────────────────────────────
// Result of processing one or more input Excel files.
// ─────────────────────────────────────────────────────────────────────────────

import 'student.dart';

class BatchStats {
  final int    total;
  final int    passed;
  final int    failed;
  final double average;
  final double highest;
  final double lowest;
  final double median;
  final double standardDeviation;
  final Map<String, int> gradeDistribution;  // letter → count

  const BatchStats({
    required this.total,
    required this.passed,
    required this.failed,
    required this.average,
    required this.highest,
    required this.lowest,
    required this.median,
    required this.standardDeviation,
    required this.gradeDistribution,
  });

  double get passRate => total > 0 ? (passed / total) * 100 : 0;
  double get failRate => total > 0 ? (failed / total) * 100 : 0;
}

class BatchResult {
  final List<Student> students;
  final List<String>  sourceFiles;
  final DateTime      processedAt;

  BatchResult({
    required this.students,
    required this.sourceFiles,
    DateTime? processedAt,
  }) : processedAt = processedAt ?? DateTime.now();

  bool get isEmpty  => students.isEmpty;
  bool get hasData  => students.isNotEmpty;
  int  get count    => students.length;

  /// Compute statistics using higher-order functions.
  BatchStats get stats {
    if (students.isEmpty) {
      return BatchStats(
        total: 0, passed: 0, failed: 0,
        average: 0, highest: 0, lowest: 0, median: 0,
        standardDeviation: 0, gradeDistribution: {},
      );
    }

    final scores  = students.map((s) => s.rawGrade).toList();
    final passed  = students.where((s) => s.hasPassed).length;
    final sorted  = [...scores]..sort();
    final avg     = scores.reduce((a, b) => a + b) / scores.length;

    // Median — higher-order slice
    final mid    = sorted.length ~/ 2;
    final median = sorted.length.isOdd
        ? sorted[mid]
        : (sorted[mid - 1] + sorted[mid]) / 2;

    // Standard deviation
    final variance = scores
        .map((s) => (s - avg) * (s - avg))
        .reduce((a, b) => a + b) / scores.length;
    final stdDev = variance > 0 ? _sqrt(variance) : 0.0;

    // Grade distribution — HOF fold
    final dist = <String, int>{};
    for (final s in students) {
      dist[s.letterGrade] = (dist[s.letterGrade] ?? 0) + 1;
    }

    return BatchStats(
      total             : students.length,
      passed            : passed,
      failed            : students.length - passed,
      average           : _round2(avg),
      highest           : sorted.last,
      lowest            : sorted.first,
      median            : _round2(median),
      standardDeviation : _round2(stdDev),
      gradeDistribution : dist,
    );
  }

  // Private helpers
  static double _sqrt(double x) {
    if (x <= 0) return 0;
    double guess = x / 2;
    for (int i = 0; i < 50; i++) guess = (guess + x / guess) / 2;
    return guess;
  }

  static double _round2(double v) =>
      (v * 100).roundToDouble() / 100;
}
