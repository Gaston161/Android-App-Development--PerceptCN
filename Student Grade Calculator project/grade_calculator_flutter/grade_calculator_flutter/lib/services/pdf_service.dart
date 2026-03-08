// lib/services/pdf_service.dart
// ─────────────────────────────────────────────────────────────────────────────
// Generates a professional PDF report using the `pdf` package.
// ─────────────────────────────────────────────────────────────────────────────

import 'dart:io';
import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;
import 'package:path/path.dart' as p;

import '../models/batch_result.dart';
import '../models/student.dart';

class PdfService {
  const PdfService();

  // Brand colours mapped to PdfColor
  static const _primary    = PdfColor.fromInt(0xFF4F6AF5);
  static const _accent     = PdfColor.fromInt(0xFF00D4B4);
  static const _surface    = PdfColor.fromInt(0xFFF5F7FF);
  static const _danger     = PdfColor.fromInt(0xFFEF5350);
  static const _success    = PdfColor.fromInt(0xFF66BB6A);
  static const _textDark   = PdfColor.fromInt(0xFF1A1D2E);
  static const _textMid    = PdfColor.fromInt(0xFF4A4E6E);

  static PdfColor _gradeColor(String letter) => const {
    'A'  : PdfColor.fromInt(0xFF66BB6A),
    'B+' : PdfColor.fromInt(0xFF26C6DA),
    'B'  : PdfColor.fromInt(0xFF42A5F5),
    'C+' : PdfColor.fromInt(0xFFAB47BC),
    'C'  : PdfColor.fromInt(0xFF7E57C2),
    'D+' : PdfColor.fromInt(0xFFFFCA28),
    'D'  : PdfColor.fromInt(0xFFFFA726),
    'F'  : PdfColor.fromInt(0xFFEF5350),
  }[letter] ?? _textMid;

  Future<String> writeReport(BatchResult result, String destDir) async {
    final doc   = pw.Document(
      title   : 'Rapport de Notes',
      author  : 'GradeCalc Pro',
      subject : 'Résultats académiques',
    );
    final stats = result.stats;

    doc.addPage(
      pw.MultiPage(
        pageFormat  : PdfPageFormat.a4,
        margin      : const pw.EdgeInsets.symmetric(horizontal: 36, vertical: 32),
        header      : (ctx) => _buildHeader(ctx, result),
        footer      : (ctx) => _buildFooter(ctx),
        build       : (ctx) => [
          _buildSummaryCards(stats),
          pw.SizedBox(height: 20),
          _buildDistributionSection(stats),
          pw.SizedBox(height: 24),
          _buildTableSection(result.students),
        ],
      ),
    );

    final fileName = 'resultats_${_ts()}.pdf';
    final outPath  = p.join(destDir, fileName);
    await File(outPath).writeAsBytes(await doc.save());
    return outPath;
  }

  // ── Header ────────────────────────────────────────────────────────────────

  pw.Widget _buildHeader(pw.Context ctx, BatchResult result) {
    return pw.Container(
      decoration: const pw.BoxDecoration(
        color        : _primary,
        borderRadius : pw.BorderRadius.all(pw.Radius.circular(8)),
      ),
      padding  : const pw.EdgeInsets.symmetric(horizontal: 20, vertical: 14),
      margin   : const pw.EdgeInsets.only(bottom: 16),
      child    : pw.Row(
        mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
        children: [
          pw.Column(
            crossAxisAlignment: pw.CrossAxisAlignment.start,
            children: [
              pw.Text('GradeCalc Pro',
                  style: pw.TextStyle(
                    fontSize: 18, fontWeight: pw.FontWeight.bold,
                    color: PdfColors.white,
                  )),
              pw.Text('Rapport de résultats académiques',
                  style: const pw.TextStyle(fontSize: 10, color: PdfColors.white70)),
            ],
          ),
          pw.Text(
            'Généré le ${_formatDate(result.processedAt)}',
            style: const pw.TextStyle(fontSize: 9, color: PdfColors.white70),
          ),
        ],
      ),
    );
  }

  // ── Footer ────────────────────────────────────────────────────────────────

  pw.Widget _buildFooter(pw.Context ctx) => pw.Row(
    mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
    children: [
      pw.Text('GradeCalc Pro — Confidentiel',
          style: const pw.TextStyle(fontSize: 8, color: _textMid)),
      pw.Text('Page ${ctx.pageNumber} / ${ctx.pagesCount}',
          style: const pw.TextStyle(fontSize: 8, color: _textMid)),
    ],
  );

  // ── Summary cards ─────────────────────────────────────────────────────────

  pw.Widget _buildSummaryCards(BatchStats stats) {
    return pw.Row(
      children: [
        _card('Total', stats.total.toString(), PdfColors.blueGrey700),
        pw.SizedBox(width: 10),
        _card('Admis', stats.passed.toString(), _success),
        pw.SizedBox(width: 10),
        _card('Ajournés', stats.failed.toString(), _danger),
        pw.SizedBox(width: 10),
        _card('Moyenne', stats.average.toStringAsFixed(2), _primary),
        pw.SizedBox(width: 10),
        _card('Réussite', '${stats.passRate.toStringAsFixed(1)}%', _accent),
      ],
    );
  }

  pw.Expanded _card(String label, String value, PdfColor color) =>
      pw.Expanded(
        child: pw.Container(
          decoration: pw.BoxDecoration(
            color       : color.flatten(),
            borderRadius: const pw.BorderRadius.all(pw.Radius.circular(8)),
          ),
          padding: const pw.EdgeInsets.all(12),
          child  : pw.Column(
            children: [
              pw.Text(value,
                  style: pw.TextStyle(
                    fontSize: 18, fontWeight: pw.FontWeight.bold,
                    color: PdfColors.white,
                  )),
              pw.Text(label,
                  style: const pw.TextStyle(fontSize: 9, color: PdfColors.white70)),
            ],
          ),
        ),
      );

  // ── Distribution ──────────────────────────────────────────────────────────

  pw.Widget _buildDistributionSection(BatchStats stats) {
    final entries = stats.gradeDistribution.entries.toList()
      ..sort((a, b) => a.key.compareTo(b.key));

    return pw.Column(
      crossAxisAlignment: pw.CrossAxisAlignment.start,
      children: [
        pw.Text('Répartition par mention',
            style: pw.TextStyle(
              fontSize: 13, fontWeight: pw.FontWeight.bold, color: _textDark,
            )),
        pw.SizedBox(height: 8),
        pw.Wrap(
          spacing: 8,
          runSpacing: 6,
          children: entries.map((e) => pw.Container(
            padding    : const pw.EdgeInsets.symmetric(horizontal: 12, vertical: 6),
            decoration : pw.BoxDecoration(
              color       : _gradeColor(e.key).flatten().withOpacity(0.15),
              borderRadius: const pw.BorderRadius.all(pw.Radius.circular(6)),
              border      : pw.Border.all(color: _gradeColor(e.key), width: 1),
            ),
            child: pw.Text('${e.key}: ${e.value} étudiant(s)',
                style: pw.TextStyle(
                  fontSize: 10,
                  color   : _gradeColor(e.key),
                  fontWeight: pw.FontWeight.bold,
                )),
          )).toList(),
        ),
      ],
    );
  }

  // ── Table ─────────────────────────────────────────────────────────────────

  pw.Widget _buildTableSection(List<Student> students) {
    const cols = ['N°', 'Matricule', 'Nom', 'Note', 'Mention', 'Appréciation', 'Résultat'];

    return pw.Column(
      crossAxisAlignment: pw.CrossAxisAlignment.start,
      children: [
        pw.Text('Liste des étudiants',
            style: pw.TextStyle(
              fontSize: 13, fontWeight: pw.FontWeight.bold, color: _textDark,
            )),
        pw.SizedBox(height: 8),
        pw.TableHelper.fromTextArray(
          headers : cols,
          data    : students.asMap().entries.map((e) {
            final s = e.value;
            return [
              (e.key + 1).toString(),
              s.matricule,
              s.name,
              s.rawGrade.toStringAsFixed(2),
              s.letterGrade,
              s.appreciation,
              s.remark,
            ];
          }).toList(),
          headerStyle          : pw.TextStyle(
            fontWeight: pw.FontWeight.bold,
            color     : PdfColors.white,
            fontSize  : 9,
          ),
          headerDecoration     : const pw.BoxDecoration(color: _primary),
          rowDecoration        : const pw.BoxDecoration(color: _surface),
          oddRowDecoration     : const pw.BoxDecoration(color: PdfColors.white),
          cellAlignments       : {
            0: pw.Alignment.center,
            3: pw.Alignment.center,
            4: pw.Alignment.center,
            6: pw.Alignment.center,
          },
          cellStyle            : const pw.TextStyle(fontSize: 8),
          cellPadding          : const pw.EdgeInsets.symmetric(horizontal: 6, vertical: 4),
        ),
      ],
    );
  }

  // ── Utilities ─────────────────────────────────────────────────────────────

  static String _formatDate(DateTime dt) =>
      '${dt.day.toString().padLeft(2,'0')}/'
      '${dt.month.toString().padLeft(2,'0')}/'
      '${dt.year}  ${dt.hour.toString().padLeft(2,'0')}:'
      '${dt.minute.toString().padLeft(2,'0')}';

  static String _ts() {
    final n = DateTime.now();
    return '${n.year}${n.month.toString().padLeft(2,'0')}'
           '${n.day.toString().padLeft(2,'0')}_'
           '${n.hour.toString().padLeft(2,'0')}'
           '${n.minute.toString().padLeft(2,'0')}';
  }
}

extension on PdfColor {
  PdfColor flatten() => PdfColor(red, green, blue);
}
