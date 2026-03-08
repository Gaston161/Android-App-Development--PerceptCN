// lib/models/student.dart
// ─────────────────────────────────────────────────────────────────────────────
// Immutable value-object representing one student record.
// ─────────────────────────────────────────────────────────────────────────────

class Student {
  final String name;
  final String matricule;
  final double rawGrade;
  final String letterGrade;   // e.g. "B+"
  final String appreciation;  // e.g. "Bien"
  final String remark;        // pass/fail remark

  const Student({
    required this.name,
    required this.matricule,
    required this.rawGrade,
    required this.letterGrade,
    required this.appreciation,
    required this.remark,
  });

  /// Returns true if student has passed (grade ≥ D threshold).
  bool get hasPassed => rawGrade >= 40.0;

  /// Copy-with helper — useful when editing records.
  Student copyWith({
    String?  name,
    String?  matricule,
    double?  rawGrade,
    String?  letterGrade,
    String?  appreciation,
    String?  remark,
  }) {
    return Student(
      name        : name        ?? this.name,
      matricule   : matricule   ?? this.matricule,
      rawGrade    : rawGrade    ?? this.rawGrade,
      letterGrade : letterGrade ?? this.letterGrade,
      appreciation: appreciation?? this.appreciation,
      remark      : remark      ?? this.remark,
    );
  }

  /// Serialise to a plain map (useful for XML/export).
  Map<String, dynamic> toMap() => {
    'name'        : name,
    'matricule'   : matricule,
    'rawGrade'    : rawGrade,
    'letterGrade' : letterGrade,
    'appreciation': appreciation,
    'remark'      : remark,
    'passed'      : hasPassed,
  };

  @override
  String toString() =>
      'Student($matricule | $name | $rawGrade | $letterGrade)';
}
