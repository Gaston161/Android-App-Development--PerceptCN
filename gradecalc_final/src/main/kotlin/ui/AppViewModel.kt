// src/main/kotlin/ui/AppViewModel.kt
package com.gradecalc.ui

import androidx.compose.runtime.*
import com.gradecalc.models.*
import com.gradecalc.services.*
import kotlinx.coroutines.*
import kotlin.properties.Delegates
import java.io.File

class AppViewModel : CoroutineScope by CoroutineScope(Dispatchers.Main + SupervisorJob()) {

    // [C3] mutableStateOf — Compose recompose l'UI automatiquement à chaque changement
    var uiState        by mutableStateOf<AppUiState>(AppUiState.Idle)
    var loadedFiles    by mutableStateOf<List<String>>(emptyList())
    var result         by mutableStateOf<BatchResult?>(null)
    var searchQuery    by mutableStateOf("")
    var sortKey        by mutableStateOf(SortKey.MATRICULE)
    var sortAscending  by mutableStateOf(true)
    var selectedFormat by mutableStateOf(ExportFormat.EXCEL)
    var lastExportPath by mutableStateOf<String?>(null)
    var errorLog       by mutableStateOf<List<String>>(emptyList())

    // [C2] companion object — constantes de classe accessibles sans instance
    companion object {
        const val APP_NAME    = "GradeCalc Pro"
        const val APP_VERSION = "1.0.0"
        const val APP_COURSE  = "SE 3242 — ICT University"
    }

    // [C3] Delegates.observable — callback appelé à chaque modification de stateLog
    private var stateLog: String by Delegates.observable("") { _, old, new ->
        if (new.isNotBlank()) println("[AppViewModel] transition : '$old' → '$new'")
    }

    // [C3] by lazy — downloadsDir calculé une seule fois au premier accès
    private val downloadsDir: String by lazy {
        val home = System.getProperty("user.home")
        val dl   = File(home, "Downloads")
        if (dl.exists()) dl.absolutePath
        else File(home, "Documents").also { it.mkdirs() }.absolutePath
    }

    // Propriété dérivée — filtrée + triée pour l'affichage
    val displayStudents: List<Student>
        get() {
            val r = result ?: return emptyList()
            val q = searchQuery.lowercase().trim()
            val filtered = if (q.isEmpty()) r.students
            else r.students.filter { s ->
                s.name.lowercase().contains(q)      ||
                s.matricule.lowercase().contains(q) ||
                s.letterGrade.lowercase().contains(q)
            }
            return GradeCalculatorService.sortBy(filtered, sortKey, sortAscending)
        }

    val hasResult: Boolean get() = result?.hasData == true

    // ── Commandes publiques ────────────────────────────────────────────

    fun loadFiles(paths: List<String>) {
        loadedFiles = (loadedFiles + paths).distinct()
        reprocess()
    }

    fun removeFile(path: String) {
        loadedFiles = loadedFiles.filter { it != path }
        if (loadedFiles.isEmpty()) reset() else reprocess()
    }

    // [C3] launch — démarre une coroutine (opération non bloquante)
    private fun reprocess() {
        uiState  = AppUiState.Loading
        errorLog = emptyList()

        launch {
            val allRows   = mutableListOf<RawRow>()
            val fileNames = mutableListOf<String>()
            val errors    = mutableListOf<String>()

            // [C3] withContext — bascule sur le thread IO pour les opérations disque
            withContext(Dispatchers.IO) {
                val excel = ExcelService()
                for (path in loadedFiles) {
                    try {
                        allRows.addAll(excel.readFile(path))
                        fileNames.add(File(path).name)
                    } catch (e: Exception) {
                        errors.add("${File(path).name} : ${e.message}")
                    }
                }
            }
            // Retour sur Main thread pour modifier l'état UI
            errorLog = errors
            val batch = GradeCalculatorService.process(allRows, fileNames)
            result   = batch
            stateLog = "result_updated"

            // [C2] when exhaustif sur sealed class — pas besoin de else
            uiState = when {
                batch.isEmpty       -> AppUiState.Error("Aucun étudiant valide trouvé.")
                errors.isNotEmpty() -> AppUiState.Success("${batch.count} étudiant(s) — ${errors.size} avertissement(s)")
                else                -> AppUiState.Success("${batch.count} étudiant(s) depuis ${fileNames.size} fichier(s)")
            }
        }
    }

    fun export() {
        if (!hasResult) return
        uiState = AppUiState.Loading
        launch {
            try {
                val exporter = ExporterFactory.create(selectedFormat)
                val path: String = withContext(Dispatchers.IO) {
                    exporter.export(result!!, downloadsDir)
                }
                lastExportPath = path
                stateLog       = "export_done"
                uiState        = AppUiState.Success("Exporté : ${File(path).name}")
            } catch (e: Exception) {
                uiState = AppUiState.Error("Erreur export : ${e.message}")
            }
        }
    }

    fun toggleSort(key: SortKey) {
        sortAscending = if (sortKey == key) !sortAscending else true
        sortKey       = key
    }

    fun reset() {
        loadedFiles    = emptyList()
        result         = null
        uiState        = AppUiState.Idle
        searchQuery    = ""
        lastExportPath = null
        errorLog       = emptyList()
    }

    fun dispose() = cancel()
}
