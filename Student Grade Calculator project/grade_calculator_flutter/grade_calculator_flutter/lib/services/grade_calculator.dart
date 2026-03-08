// lib/services/grade_calculator.dart
// ─────────────────────────────────────────────────────────────────────────────
// Pure-function service that transforms raw input rows into Student objects.
// Demonstrates: lambda expressions, higher-order functions, functional pipeline.
// ─────────────────────────────────────────────────────────────────────────────

import '../models/student.dart';
import '../models/grade_scale.dart';
import '../models/batch_result.dart';

/// Type alias for a raw data row from Excel.
typedef RawRow = Map<String, dynamic>;

/// A transformer function that maps a RawRow to a Student.
typedef StudentTransformer = Student Function(RawRow row);

/// A validator that checks a raw grade value.
typedef GradeValidator = String? Function(dynamic value);

class GradeCalculatorService {
  GradeCalculatorService._();

  // ── Validators (lambda-style, composable) ──────────────────────────────────

  /// Returns an error message or null if valid.
  static final GradeValidator validateGrade = (dynamic value) {
    if (value == null) return 'Note manquante';
    final parsed = double.tryParse(value.toString().trim());
    if (parsed == null) return 'Valeur non numérique: $value';
    if (parsed < 0 || parsed > 100) return 'Note hors intervalle [0–100]: $parsed';
    return null; // valid
  };

  // ── Core transformer (HOF: factory returning a configured transformer) ─────

  /// Returns a StudentTransformer configured with optional pre-/post-processors.
  static StudentTransformer buildTransformer({
    String Function(String)? nameProcessor,
    double Function(double)? gradeProcessor,
  }) {
    // Capture processors into closure
    final processName  = nameProcessor  ?? (s) => s.trim();
    final processGrade = gradeProcessor ?? (g) => (g * 10).round() / 10;  // round 1dp

    return (RawRow row) {
      final name       = processName(row['name']?.toString() ?? '');
      final matricule  = row['matricule']?.toString().trim() ?? '';
      final rawGrade   = processGrade(
        double.tryParse(row['grade']?.toString().trim() ?? '0') ?? 0.0,
      );

      final band = GradeScale.resolve(rawGrade);

      return Student(
        name        : name,
        matricule   : matricule,
        rawGrade    : rawGrade,
        letterGrade : band.letter,
        appreciation: band.appreciation,
        remark      : GradeScale.remarkFor(rawGrade),
      );
    };
  }

  // ── Public API ─────────────────────────────────────────────────────────────

  /// Process a list of raw rows into a BatchResult.
  ///
  /// [rows]         — raw data extracted from Excel
  /// [sourceFiles]  — names of the source files
  /// [transformer]  — optional custom transformer; defaults to [buildTransformer()]
  static BatchResult process({
    required List<RawRow> rows,
    required List<String> sourceFiles,
    StudentTransformer? transformer,
  }) {
    final transform = transformer ?? buildTransformer();

    // Validate → filter → transform (functional pipeline using HOFs)
    final students = rows
        .where((row) => _rowIsUsable(row))          // filter blank rows
        .map(transform)                              // transform each row
        .where((s) => s.name.isNotEmpty)            // drop empty names
        .toList();

    return BatchResult(students: students, sourceFiles: sourceFiles);
  }

  /// Sort students by a chosen key using an HOF comparator factory.
  static List<Student> sortBy(
    List<Student> students,
    SortKey key, {
    bool ascending = true,
  }) {
    final comparator = _comparatorFor(key);
    final sorted = [...students]..sort(comparator);
    return ascending ? sorted : sorted.reversed.toList();
  }

  /// Filter students using a predicate lambda.
  static List<Student> filterWhere(
    List<Student> students,
    bool Function(Student) predicate,
  ) => students.where(predicate).toList();

  /// Group students by letter grade — HOF groupBy.
  static Map<String, List<Student>> groupByGrade(
    List<Student> students,
  ) => students.fold<Map<String, List<Student>>>(
    {},
    (acc, s) {
      acc.putIfAbsent(s.letterGrade, () => []).add(s);
      return acc;
    },
  );

  // ── Private helpers ────────────────────────────────────────────────────────

  static bool _rowIsUsable(RawRow row) {
    final name = row['name']?.toString().trim() ?? '';
    final mat  = row['matricule']?.toString().trim() ?? '';
    return name.isNotEmpty || mat.isNotEmpty;
  }

  static Comparator<Student> _comparatorFor(SortKey key) {
    // HOF: return a comparator closure
    return switch (key) {
      SortKey.name       => (a, b) => a.name.compareTo(b.name),
      SortKey.matricule  => (a, b) => a.matricule.compareTo(b.matricule),
      SortKey.grade      => (a, b) => a.rawGrade.compareTo(b.rawGrade),
      SortKey.letter     => (a, b) => a.letterGrade.compareTo(b.letterGrade),
    };
  }
}

enum SortKey { name, matricule, grade, letter }
