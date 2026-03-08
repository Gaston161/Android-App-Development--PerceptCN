// lib/services/app_state.dart
// ─────────────────────────────────────────────────────────────────────────────
// Central ChangeNotifier — holds all runtime state and orchestrates services.
// ─────────────────────────────────────────────────────────────────────────────

import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:path_provider/path_provider.dart';

import '../models/batch_result.dart';
import '../models/student.dart';
import '../core/constants.dart';
import 'grade_calculator.dart';
import 'excel_service.dart';
import 'pdf_service.dart';
import 'xml_service.dart';
import 'word_service.dart';

enum AppStatus { idle, loading, done, error }

class AppState extends ChangeNotifier {
  // ── Services (injected as final fields — enables future mocking) ───────────
  final ExcelService _excel;
  final PdfService   _pdf;
  final XmlService   _xml;
  final WordService  _word;

  AppState({
    ExcelService? excel,
    PdfService?   pdf,
    XmlService?   xml,
    WordService?  word,
  })  : _excel = excel ?? const ExcelService(),
        _pdf   = pdf   ?? const PdfService(),
        _xml   = xml   ?? const XmlService(),
        _word  = word  ?? const WordService();

  // ── Observable state ───────────────────────────────────────────────────────

  AppStatus        _status          = AppStatus.idle;
  String           _statusMessage   = '';
  List<String>     _loadedFiles     = [];
  BatchResult?     _result;
  String           _searchQuery     = '';
  SortKey          _sortKey         = SortKey.matricule;
  bool             _sortAscending   = true;
  String           _selectedFormat  = AppConstants.fmtExcel;
  String?          _lastExportPath;
  List<String>     _errorLog        = [];

  // ── Getters ────────────────────────────────────────────────────────────────

  AppStatus    get status         => _status;
  String       get statusMessage  => _statusMessage;
  List<String> get loadedFiles    => _loadedFiles;
  BatchResult? get result         => _result;
  bool         get hasResult      => _result != null && _result!.hasData;
  String       get searchQuery    => _searchQuery;
  SortKey      get sortKey        => _sortKey;
  bool         get sortAscending  => _sortAscending;
  String       get selectedFormat => _selectedFormat;
  String?      get lastExportPath => _lastExportPath;
  List<String> get errorLog       => _errorLog;

  /// Derived: filtered + sorted students for display.
  List<Student> get displayStudents {
    if (_result == null) return [];
    final query   = _searchQuery.toLowerCase();
    final filtered = query.isEmpty
        ? _result!.students
        : _result!.students.where((s) =>
            s.name.toLowerCase().contains(query)       ||
            s.matricule.toLowerCase().contains(query)  ||
            s.letterGrade.toLowerCase().contains(query),
          ).toList();

    return GradeCalculatorService.sortBy(
      filtered, _sortKey, ascending: _sortAscending,
    );
  }

  // ── Commands ───────────────────────────────────────────────────────────────

  /// Load one or multiple Excel files.
  Future<void> loadFiles(List<String> paths) async {
    _setStatus(AppStatus.loading, 'Lecture des fichiers…');
    _errorLog.clear();
    _loadedFiles = [..._loadedFiles, ...paths.where((p) => !_loadedFiles.contains(p))];

    try {
      final allRows   = <RawRow>[];
      final fileNames = <String>[];

      for (final path in _loadedFiles) {
        try {
          final rows = await _excel.readFile(path);
          allRows.addAll(rows);
          fileNames.add(path.split(Platform.pathSeparator).last);
        } catch (e) {
          _errorLog.add('${path.split(Platform.pathSeparator).last}: $e');
        }
      }

      _result = GradeCalculatorService.process(
        rows       : allRows,
        sourceFiles: fileNames,
      );

      if (_result!.isEmpty) {
        _setStatus(AppStatus.error, 'Aucun étudiant valide trouvé.');
      } else {
        _setStatus(AppStatus.done,
          '${_result!.count} étudiant(s) chargé(s) depuis ${fileNames.length} fichier(s).');
      }
    } catch (e) {
      _setStatus(AppStatus.error, 'Erreur: $e');
    }
  }

  /// Remove a file and reprocess.
  Future<void> removeFile(String path) async {
    _loadedFiles.remove(path);
    if (_loadedFiles.isEmpty) {
      reset();
    } else {
      await loadFiles([]);   // reprocess existing files
    }
  }

  /// Export the current result in the selected format.
  Future<void> export() async {
    if (!hasResult) return;
    _setStatus(AppStatus.loading, 'Export en cours…');

    try {
      final destDir = await _pickDestDir();
      final path = await switch (_selectedFormat) {
        AppConstants.fmtExcel => _excel.writeReport(_result!, destDir),
        AppConstants.fmtPdf   => _pdf.writeReport(_result!, destDir),
        AppConstants.fmtXml   => _xml.writeReport(_result!, destDir),
        AppConstants.fmtWord  => _word.writeReport(_result!, destDir),
        _                     => _excel.writeReport(_result!, destDir),
      };
      _lastExportPath = path;
      _setStatus(AppStatus.done, 'Fichier exporté: ${path.split(Platform.pathSeparator).last}');
    } catch (e) {
      _setStatus(AppStatus.error, 'Échec export: $e');
    }
  }

  void setSearchQuery(String q) {
    _searchQuery = q;
    notifyListeners();
  }

  void setSort(SortKey key) {
    if (_sortKey == key) {
      _sortAscending = !_sortAscending;
    } else {
      _sortKey       = key;
      _sortAscending = true;
    }
    notifyListeners();
  }

  void setFormat(String fmt) {
    _selectedFormat = fmt;
    notifyListeners();
  }

  void reset() {
    _loadedFiles    = [];
    _result         = null;
    _status         = AppStatus.idle;
    _statusMessage  = '';
    _searchQuery    = '';
    _lastExportPath = null;
    _errorLog       = [];
    notifyListeners();
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  void _setStatus(AppStatus s, String msg) {
    _status        = s;
    _statusMessage = msg;
    notifyListeners();
  }

  Future<String> _pickDestDir() async {
    // Default: Downloads folder (or Documents)
    try {
      final dir = await getDownloadsDirectory();
      return dir?.path ?? (await getApplicationDocumentsDirectory()).path;
    } catch (_) {
      return (await getApplicationDocumentsDirectory()).path;
    }
  }
}
