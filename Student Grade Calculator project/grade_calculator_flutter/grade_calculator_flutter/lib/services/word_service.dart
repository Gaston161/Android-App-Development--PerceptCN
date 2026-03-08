// lib/services/word_service.dart
// ─────────────────────────────────────────────────────────────────────────────
// Creates a DOCX file (Open XML format) without external Word library.
// A .docx is a ZIP archive containing XML parts.
// ─────────────────────────────────────────────────────────────────────────────

import 'dart:convert';
import 'dart:io';
import 'package:archive/archive_io.dart';
import 'package:path/path.dart' as p;

import '../models/batch_result.dart';
import '../models/student.dart';

class WordService {
  const WordService();

  Future<String> writeReport(BatchResult result, String destDir) async {
    final archive = Archive();

    // Required DOCX parts
    _addUtf8(archive, '[Content_Types].xml', _contentTypes());
    _addUtf8(archive, '_rels/.rels',          _rootRels());
    _addUtf8(archive, 'word/document.xml',    _document(result));
    _addUtf8(archive, 'word/styles.xml',      _styles());
    _addUtf8(archive, 'word/_rels/document.xml.rels', _docRels());
    _addUtf8(archive, 'docProps/core.xml',    _coreProps(result.processedAt));
    _addUtf8(archive, 'docProps/app.xml',     _appProps());

    final fileName = 'resultats_${_ts()}.docx';
    final outPath  = p.join(destDir, fileName);
    final encoder  = ZipEncoder();
    final bytes    = encoder.encode(archive)!;
    await File(outPath).writeAsBytes(bytes);
    return outPath;
  }

  // ── XML parts ─────────────────────────────────────────────────────────────

  String _contentTypes() => '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml"  ContentType="application/xml"/>
  <Override PartName="/word/document.xml"
    ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml"
    ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
  <Override PartName="/docProps/core.xml"
    ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
  <Override PartName="/docProps/app.xml"
    ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
</Types>''';

  String _rootRels() => '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument"
    Target="word/document.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties"
    Target="docProps/core.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties"
    Target="docProps/app.xml"/>
</Relationships>''';

  String _docRels() => '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles"
    Target="styles.xml"/>
</Relationships>''';

  String _coreProps(DateTime dt) => '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties
  xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties"
  xmlns:dc="http://purl.org/dc/elements/1.1/"
  xmlns:dcterms="http://purl.org/dc/terms/"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <dc:title>Rapport de Résultats</dc:title>
  <dc:creator>GradeCalc Pro</dc:creator>
  <dcterms:created xsi:type="dcterms:W3CDTF">${dt.toUtc().toIso8601String()}</dcterms:created>
</cp:coreProperties>''';

  String _appProps() => '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties">
  <Application>GradeCalc Pro 1.0</Application>
</Properties>''';

  String _styles() => '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:style w:type="paragraph" w:styleId="Heading1">
    <w:name w:val="heading 1"/>
    <w:rPr>
      <w:b/><w:sz w:val="36"/><w:color w:val="4F6AF5"/>
    </w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Normal">
    <w:name w:val="Normal"/>
    <w:rPr><w:sz w:val="20"/></w:rPr>
  </w:style>
  <w:style w:type="table" w:styleId="TableGrid">
    <w:name w:val="Table Grid"/>
    <w:tblPr>
      <w:tblBorders>
        <w:top    w:val="single" w:sz="4" w:color="CCCCCC"/>
        <w:left   w:val="single" w:sz="4" w:color="CCCCCC"/>
        <w:bottom w:val="single" w:sz="4" w:color="CCCCCC"/>
        <w:right  w:val="single" w:sz="4" w:color="CCCCCC"/>
        <w:insideH w:val="single" w:sz="4" w:color="CCCCCC"/>
        <w:insideV w:val="single" w:sz="4" w:color="CCCCCC"/>
      </w:tblBorders>
    </w:tblPr>
  </w:style>
</w:styles>''';

  String _document(BatchResult result) {
    final stats = result.stats;
    final sb    = StringBuffer();
    const ns    = 'xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"';

    sb.writeln('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>');
    sb.writeln('<w:document $ns>');
    sb.writeln('<w:body>');

    // ── Title ──
    sb.writeln(_para('GradeCalc Pro — Rapport de Résultats',
        bold: true, size: 36, color: '4F6AF5', center: true));
    sb.writeln(_para(
      'Généré le ${_fmtDate(result.processedAt)}  |  Sources: ${result.sourceFiles.join(", ")}',
      color: '888888', size: 18, center: true,
    ));
    sb.writeln(_para(''));

    // ── Stats summary ──
    sb.writeln(_para('Statistiques Générales', bold: true, size: 28, color: '1A1D2E'));
    sb.writeln(_statsTable(stats));
    sb.writeln(_para(''));

    // ── Distribution ──
    sb.writeln(_para('Répartition par Mention', bold: true, size: 28, color: '1A1D2E'));
    sb.writeln(_distributionTable(stats));
    sb.writeln(_para(''));

    // ── Students table ──
    sb.writeln(_para('Liste des Étudiants', bold: true, size: 28, color: '1A1D2E'));
    sb.writeln(_studentsTable(result.students));

    // Required page-break / section
    sb.writeln('<w:sectPr><w:pgSz w:w="12240" w:h="15840"/></w:sectPr>');
    sb.writeln('</w:body>');
    sb.writeln('</w:document>');

    return sb.toString();
  }

  // ── Paragraph helper ──────────────────────────────────────────────────────

  String _para(
    String text, {
    bool   bold   = false,
    int    size   = 20,
    String color  = '000000',
    bool   center = false,
  }) {
    final xml = text.isEmpty ? '' :
      '<w:r><w:rPr>'
      '${bold ? "<w:b/>" : ""}'
      '<w:sz w:val="$size"/>'
      '<w:color w:val="$color"/>'
      '</w:rPr>'
      '<w:t xml:space="preserve">${_esc(text)}</w:t>'
      '</w:r>';

    final align = center ? '<w:jc w:val="center"/>' : '';
    return '<w:p><w:pPr>$align</w:pPr>$xml</w:p>';
  }

  // ── Table helpers ─────────────────────────────────────────────────────────

  String _statsTable(BatchStats stats) => _table([
    ['Indicateur', 'Valeur'],
    ['Total étudiants',    stats.total.toString()],
    ['Admis',              '${stats.passed} (${stats.passRate.toStringAsFixed(1)}%)'],
    ['Ajournés',           '${stats.failed} (${stats.failRate.toStringAsFixed(1)}%)'],
    ['Moyenne générale',   stats.average.toStringAsFixed(2)],
    ['Note la plus haute', stats.highest.toStringAsFixed(2)],
    ['Note la plus basse', stats.lowest.toStringAsFixed(2)],
    ['Médiane',            stats.median.toStringAsFixed(2)],
    ['Écart-type',         stats.standardDeviation.toStringAsFixed(2)],
  ]);

  String _distributionTable(BatchStats stats) {
    final rows = [['Mention', 'Nombre', 'Pourcentage']];
    stats.gradeDistribution.forEach((letter, count) {
      final pct = stats.total > 0
          ? (count / stats.total * 100).toStringAsFixed(1)
          : '0.0';
      rows.add([letter, count.toString(), '$pct%']);
    });
    return _table(rows);
  }

  String _studentsTable(List<Student> students) {
    final rows = [
      ['N°', 'Matricule', 'Nom', 'Note', 'Mention', 'Appréciation', 'Résultat']
    ];
    for (int i = 0; i < students.length; i++) {
      final s = students[i];
      rows.add([
        (i + 1).toString(),
        s.matricule,
        s.name,
        s.rawGrade.toStringAsFixed(2),
        s.letterGrade,
        s.appreciation,
        s.remark,
      ]);
    }
    return _table(rows, firstRowHeader: true);
  }

  String _table(List<List<String>> rows, {bool firstRowHeader = true}) {
    final sb = StringBuffer();
    sb.write('<w:tbl>');
    sb.write('<w:tblPr>'
        '<w:tblStyle w:val="TableGrid"/>'
        '<w:tblW w:w="9072" w:type="dxa"/>'
        '</w:tblPr>');

    for (int r = 0; r < rows.length; r++) {
      final isHeader = (r == 0 && firstRowHeader);
      sb.write('<w:tr>');
      for (final cell in rows[r]) {
        sb.write('<w:tc>');
        if (isHeader) {
          sb.write('<w:tcPr><w:shd w:val="clear" w:color="auto" w:fill="4F6AF5"/></w:tcPr>');
        } else if (r.isEven) {
          sb.write('<w:tcPr><w:shd w:val="clear" w:color="auto" w:fill="F5F7FF"/></w:tcPr>');
        }
        sb.write('<w:p><w:r><w:rPr>');
        if (isHeader) sb.write('<w:b/><w:color w:val="FFFFFF"/>');
        sb.write('<w:sz w:val="18"/></w:rPr>');
        sb.write('<w:t>${_esc(cell)}</w:t></w:r></w:p>');
        sb.write('</w:tc>');
      }
      sb.write('</w:tr>');
    }
    sb.write('</w:tbl>');
    return sb.toString();
  }

  // ── Utilities ─────────────────────────────────────────────────────────────

  String _esc(String s) => s
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;');

  static String _fmtDate(DateTime dt) =>
      '${dt.day.toString().padLeft(2,'0')}/'
      '${dt.month.toString().padLeft(2,'0')}/'
      '${dt.year}';

  static String _ts() {
    final n = DateTime.now();
    return '${n.year}${n.month.toString().padLeft(2,'0')}'
           '${n.day.toString().padLeft(2,'0')}_'
           '${n.hour.toString().padLeft(2,'0')}'
           '${n.minute.toString().padLeft(2,'0')}';
  }

  void _addUtf8(Archive arc, String name, String content) {
    final bytes = utf8.encode(content);
    arc.addFile(ArchiveFile(name, bytes.length, bytes));
  }
}
