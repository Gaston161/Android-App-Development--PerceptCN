// lib/core/constants.dart
// ─────────────────────────────────────────────────────────────────────────────
// Application-wide constants.
// ─────────────────────────────────────────────────────────────────────────────

class AppConstants {
  AppConstants._();

  static const String appName      = 'GradeCalc Pro';
  static const String appVersion   = '1.0.0';
  static const String appSubtitle  = 'Student Grade Calculator';

  // Expected Excel column headers (case-insensitive match)
  static const String colName       = 'nom';
  static const String colMatricule  = 'matricule';
  static const String colGrade      = 'note';

  // Export format labels
  static const String fmtExcel = 'Excel (.xlsx)';
  static const String fmtPdf   = 'PDF (.pdf)';
  static const String fmtXml   = 'XML (.xml)';
  static const String fmtWord  = 'Word (.docx)';

  static const List<String> exportFormats = [fmtExcel, fmtPdf, fmtXml, fmtWord];

  // Accepted drop/pick extensions
  static const List<String> allowedExtensions = ['xlsx', 'xls'];
}
