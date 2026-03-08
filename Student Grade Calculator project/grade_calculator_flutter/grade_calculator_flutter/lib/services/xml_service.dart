// lib/services/xml_service.dart
// ─────────────────────────────────────────────────────────────────────────────

import 'dart:io';
import 'package:xml/xml.dart';
import 'package:path/path.dart' as p;

import '../models/batch_result.dart';

class XmlService {
  const XmlService();

  Future<String> writeReport(BatchResult result, String destDir) async {
    final stats   = result.stats;
    final builder = XmlBuilder();

    builder.processing('xml', 'version="1.0" encoding="UTF-8"');
    builder.element('rapport', nest: () {
      // Meta
      builder.element('meta', nest: () {
        builder.element('generePar',   nest: () => builder.text('GradeCalc Pro'));
        builder.element('dateExport',  nest: () => builder.text(DateTime.now().toIso8601String()));
        builder.element('sources',     nest: () {
          for (final f in result.sourceFiles) {
            builder.element('fichier', nest: () => builder.text(f));
          }
        });
      });

      // Statistics
      builder.element('statistiques', nest: () {
        builder.element('total',         nest: () => builder.text(stats.total.toString()));
        builder.element('admis',         nest: () => builder.text(stats.passed.toString()));
        builder.element('ajournes',      nest: () => builder.text(stats.failed.toString()));
        builder.element('tauxReussite',  nest: () => builder.text('${stats.passRate.toStringAsFixed(1)}%'));
        builder.element('moyenne',       nest: () => builder.text(stats.average.toStringAsFixed(2)));
        builder.element('noteMax',       nest: () => builder.text(stats.highest.toStringAsFixed(2)));
        builder.element('noteMin',       nest: () => builder.text(stats.lowest.toStringAsFixed(2)));
        builder.element('mediane',       nest: () => builder.text(stats.median.toStringAsFixed(2)));
        builder.element('ecartType',     nest: () => builder.text(stats.standardDeviation.toStringAsFixed(2)));
        builder.element('repartition',   nest: () {
          stats.gradeDistribution.forEach((letter, count) {
            builder.element('mention',
              attributes: {'lettre': letter},
              nest: () => builder.text(count.toString()),
            );
          });
        });
      });

      // Students
      builder.element('etudiants', nest: () {
        for (final s in result.students) {
          builder.element('etudiant',
            attributes: {'matricule': s.matricule},
            nest: () {
              builder.element('nom',          nest: () => builder.text(s.name));
              builder.element('note',         nest: () => builder.text(s.rawGrade.toString()));
              builder.element('mention',      nest: () => builder.text(s.letterGrade));
              builder.element('appreciation', nest: () => builder.text(s.appreciation));
              builder.element('resultat',     nest: () => builder.text(s.remark));
            },
          );
        }
      });
    });

    final document = builder.buildDocument();
    final xml = document.toXmlString(pretty: true, indent: '  ');

    final fileName = 'resultats_${_ts()}.xml';
    final outPath  = p.join(destDir, fileName);
    await File(outPath).writeAsString(xml, flush: true);
    return outPath;
  }

  static String _ts() {
    final n = DateTime.now();
    return '${n.year}${n.month.toString().padLeft(2,'0')}'
           '${n.day.toString().padLeft(2,'0')}_'
           '${n.hour.toString().padLeft(2,'0')}'
           '${n.minute.toString().padLeft(2,'0')}';
  }
}
