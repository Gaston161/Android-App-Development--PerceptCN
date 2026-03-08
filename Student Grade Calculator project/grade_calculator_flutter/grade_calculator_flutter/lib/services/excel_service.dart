// lib/services/excel_service.dart
// ─────────────────────────────────────────────────────────────────────────────
// Reads input Excel files and writes output Excel reports.
// ─────────────────────────────────────────────────────────────────────────────

import 'dart:io';
import 'package:excel/excel.dart';
import 'package:path/path.dart' as p;

import '../models/student.dart';
import '../models/batch_result.dart';
import '../core/constants.dart';
import 'grade_calculator.dart';

class ExcelParseException implements Exception {
  final String message;
  const ExcelParseException(this.message);
  @override String toString() => 'ExcelParseException: $message';
}

abstract class _IExcelService {
  Future<List<RawRow>> readFile(String path);
  Future<String>       writeReport(BatchResult result, String destDir);
}

class ExcelService implements _IExcelService {
  const ExcelService();

  // ── Read ──────────────────────────────────────────────────────────────────

  @override
  Future<List<RawRow>> readFile(String filePath) async {
    final bytes = await File(filePath).readAsBytes();
    final excel = Excel.decodeBytes(bytes);

    // Pick first non-empty sheet
    final sheet = excel.sheets.values.firstWhere(
      (s) => s.rows.isNotEmpty,
      orElse: () => throw const ExcelParseException(
        'Aucune feuille non-vide trouvée.',
      ),
    );

    if (sheet.rows.isEmpty) {
      throw const ExcelParseException('Feuille vide.');
    }

    // Parse header row — map column index → canonical key
    final header = sheet.rows.first;
    final colMap  = _parseHeader(header);

    // Validate required columns
    for (final required in ['name', 'matricule', 'grade']) {
      if (!colMap.containsValue(required)) {
        throw ExcelParseException(
          'Colonne requise absente. Attendues: '
          '"${AppConstants.colName}", "${AppConstants.colMatricule}", '
          '"${AppConstants.colGrade}".',
        );
      }
    }

    // Build rows — skip header (index 0)
    final rows = <RawRow>[];
    for (int r = 1; r < sheet.rows.length; r++) {
      final cells = sheet.rows[r];
      final row   = <String, dynamic>{};
      colMap.forEach((colIdx, key) {
        final cell = colIdx < cells.length ? cells[colIdx] : null;
        row[key]   = cell?.value?.toString() ?? '';
      });
      rows.add(row);
    }
    return rows;
  }

  // ── Write ─────────────────────────────────────────────────────────────────

  @override
  Future<String> writeReport(BatchResult result, String destDir) async {
    final excel    = Excel.createExcel();
    final sheet    = excel['Résultats'];
    excel.setDefaultSheet('Résultats');

    // ── Header row ──
    final headers = [
      'N°', 'Matricule', 'Nom', 'Note (/100)',
      'Mention', 'Appréciation', 'Résultat',
    ];
    sheet.appendRow(
      headers.map<CellValue>((h) => TextCellValue(h)).toList(),
    );
    _styleHeader(sheet, headers.length);

    // ── Data rows ──
    for (int i = 0; i < result.students.length; i++) {
      final s = result.students[i];
      sheet.appendRow([
        IntCellValue(i + 1),
        TextCellValue(s.matricule),
        TextCellValue(s.name),
        DoubleCellValue(s.rawGrade),
        TextCellValue(s.letterGrade),
        TextCellValue(s.appreciation),
        TextCellValue(s.remark),
      ]);
    }

    // ── Stats sheet ──
    _appendStatsSheet(excel, result);

    final fileName = 'resultats_${_timestamp()}.xlsx';
    final outPath  = p.join(destDir, fileName);
    final bytes    = excel.encode()!;
    await File(outPath).writeAsBytes(bytes);
    return outPath;
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  Map<int, String> _parseHeader(List<Data?> header) {
    final map = <int, String>{};
    for (int i = 0; i < header.length; i++) {
      final raw = header[i]?.value?.toString().trim().toLowerCase() ?? '';
      if (raw == AppConstants.colName || raw == 'name' || raw == 'prénom' || raw == 'prenom') {
        map[i] = 'name';
      } else if (raw == AppConstants.colMatricule || raw == 'id' || raw == 'num') {
        map[i] = 'matricule';
      } else if (raw == AppConstants.colGrade || raw == 'grade' || raw == 'score' || raw == 'mark') {
        map[i] = 'grade';
      }
    }
    return map;
  }

  void _styleHeader(Sheet sheet, int colCount) {
    for (int c = 0; c < colCount; c++) {
      final cell = sheet.cell(CellIndex.indexByColumnRow(columnIndex: c, rowIndex: 0));
      cell.cellStyle = CellStyle(
        bold            : true,
        backgroundColorHex: ExcelColor.fromHexString('#4F6AF5'),
        fontColorHex    : ExcelColor.fromHexString('#FFFFFF'),
        horizontalAlign : HorizontalAlign.Center,
      );
    }
  }

  void _appendStatsSheet(Excel excel, BatchResult result) {
    final sheet = excel['Statistiques'];
    final stats = result.stats;

    void addRow(String label, String value) =>
        sheet.appendRow([TextCellValue(label), TextCellValue(value)]);

    addRow('Total étudiants',   stats.total.toString());
    addRow('Admis',             stats.passed.toString());
    addRow('Ajournés',          stats.failed.toString());
    addRow('Taux de réussite',  '${stats.passRate.toStringAsFixed(1)} %');
    addRow('Moyenne',           stats.average.toStringAsFixed(2));
    addRow('Note la plus haute',stats.highest.toStringAsFixed(2));
    addRow('Note la plus basse',stats.lowest.toStringAsFixed(2));
    addRow('Médiane',           stats.median.toStringAsFixed(2));
    addRow('Écart-type',        stats.standardDeviation.toStringAsFixed(2));

    sheet.appendRow([]);
    sheet.appendRow([TextCellValue('Répartition par mention')]);
    stats.gradeDistribution.forEach((letter, count) {
      addRow(letter, count.toString());
    });
  }

  static String _timestamp() {
    final now = DateTime.now();
    return '${now.year}${now.month.toString().padLeft(2,'0')}'
           '${now.day.toString().padLeft(2,'0')}_'
           '${now.hour.toString().padLeft(2,'0')}'
           '${now.minute.toString().padLeft(2,'0')}';
  }
}
