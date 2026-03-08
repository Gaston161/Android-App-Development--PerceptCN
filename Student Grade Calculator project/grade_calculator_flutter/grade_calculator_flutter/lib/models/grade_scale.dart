// lib/models/grade_scale.dart
// ─────────────────────────────────────────────────────────────────────────────
// Encapsulates the complete grading rubric.
// Uses higher-order functions and lambdas to resolve grades.
// ─────────────────────────────────────────────────────────────────────────────

import 'package:flutter/material.dart';
import '../core/theme.dart';

/// A single band in the grading scale.
class GradeBand {
  final String letter;
  final String appreciation;
  final double minInclusive;
  final double maxInclusive;

  const GradeBand({
    required this.letter,
    required this.appreciation,
    required this.minInclusive,
    required this.maxInclusive,
  });

  /// Lambda-style predicate for whether a score falls in this band.
  bool contains(double score) =>
      score >= minInclusive && score <= maxInclusive;

  Color get color => AppTheme.gradeColor(letter);

  @override
  String toString() =>
      '$letter ($minInclusive–$maxInclusive): $appreciation';
}

/// The full grading scale — singleton, immutable.
class GradeScale {
  GradeScale._();

  /// Official bands ordered from highest to lowest.
  static const List<GradeBand> bands = [
    GradeBand(letter: 'A',  appreciation: 'Excellent',       minInclusive: 80, maxInclusive: 100),
    GradeBand(letter: 'B+', appreciation: 'Très Bien',        minInclusive: 71, maxInclusive: 79),
    GradeBand(letter: 'B',  appreciation: 'Bien',             minInclusive: 66, maxInclusive: 70),
    GradeBand(letter: 'C+', appreciation: 'Assez Bien',       minInclusive: 56, maxInclusive: 65),
    GradeBand(letter: 'C',  appreciation: 'Satisfaisant',     minInclusive: 50, maxInclusive: 55),
    GradeBand(letter: 'D+', appreciation: 'Passable +',       minInclusive: 45, maxInclusive: 49),
    GradeBand(letter: 'D',  appreciation: 'Passable',         minInclusive: 40, maxInclusive: 44),
    GradeBand(letter: 'F',  appreciation: 'Insuffisant',      minInclusive: 0,  maxInclusive: 39),
  ];

  /// Higher-order lookup: find first band whose predicate matches.
  static GradeBand resolve(double score) {
    // Clamp to valid range
    final clamped = score.clamp(0.0, 100.0);
    return bands.firstWhere(
      (band) => band.contains(clamped),
      orElse: () => bands.last,   // fallback → F
    );
  }

  /// Letter grade string for a raw score.
  static String letterFor(double score) => resolve(score).letter;

  /// Appreciation string for a raw score.
  static String appreciationFor(double score) => resolve(score).appreciation;

  /// Remark — pass / fail.
  static String remarkFor(double score) =>
      score >= 40 ? 'Admis(e)' : 'Ajourné(e)';

  /// HOF: filter a list of grades down to a single band.
  static List<double> scoresInBand(
    List<double> scores,
    String letter,
  ) => scores.where((s) => letterFor(s) == letter).toList();

  /// HOF: transform raw scores into letter-grade pairs.
  static List<MapEntry<double, String>> mapToLetters(
    List<double> scores,
  ) => scores.map((s) => MapEntry(s, letterFor(s))).toList();
}
