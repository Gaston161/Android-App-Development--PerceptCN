// src/main/kotlin/models/Models.kt
package com.gradecalc.models

import java.time.LocalDateTime

// ════════════════════════════════════════════════════════════════════
// MODÈLES DE DONNÉES — GradeCalc Pro
//
// Concepts SE 3242 illustrés :
//  [C1] = Class01 : val/var, null safety, when-expression, lambda, HOF
//  [C2] = Class02 : data class, sealed class, enum, object singleton,
//                   companion object, abstract class, interface,
//                   init block, custom getter, open/override, visibility
//  [C3] = Class03 : by lazy, Delegates.observable, generics
// ════════════════════════════════════════════════════════════════════

// [C2] data class — génère equals(), hashCode(), toString(), copy()
data class Student(
    val name        : String,
    val matricule   : String,
    val rawGrade    : Double,
    val letterGrade : String,
    val appreciation: String,
    val remark      : String
) {
    // [C2] custom getter — calculé à la demande, pas stocké
    val hasPassed: Boolean
        get() = rawGrade >= 40.0

    val formattedGrade: String
        get() = "%.2f".format(rawGrade)
}

// [C2] data class pour une tranche du barème
data class GradeBand(
    val letter      : String,
    val appreciation: String,
    val minInclusive: Double,
    val maxInclusive: Double
) {
    // [C1] lambda stockée dans une propriété val (function type)
    val contains: (Double) -> Boolean = { score ->
        score >= minInclusive && score <= maxInclusive
    }
}

// [C2] object declaration = SINGLETON
// Une seule instance partagée dans tout le programme, sans instanciation
object GradeScale {

    val bands: List<GradeBand> = listOf(
        GradeBand("A",  "Excellent",     80.0, 100.0),
        GradeBand("B+", "Très Bien",     71.0,  79.0),
        GradeBand("B",  "Bien",          66.0,  70.0),
        GradeBand("C+", "Assez Bien",    56.0,  65.0),
        GradeBand("C",  "Satisfaisant",  50.0,  55.0),
        GradeBand("D+", "Passable +",    45.0,  49.0),
        GradeBand("D",  "Passable",      40.0,  44.0),
        GradeBand("F",  "Insuffisant",    0.0,  39.0)
    )

    // [C1] firstOrNull (HOF) + ?: (Elvis) comme fallback null-safe
    fun resolve(score: Double): GradeBand {
        val clamped = score.coerceIn(0.0, 100.0)
        return bands.firstOrNull { it.contains(clamped) } ?: bands.last()
    }

    // [C1] expression body (= au lieu de bloc {})
    fun letterFor(score: Double)      : String = resolve(score).letter
    fun appreciationFor(score: Double): String = resolve(score).appreciation

    // [C1] if comme expression (retourne une valeur)
    fun remarkFor(score: Double): String =
        if (score >= 40.0) "Admis(e)" else "Ajourné(e)"
}

// [C2] data class — statistiques calculées
data class BatchStats(
    val total            : Int,
    val passed           : Int,
    val failed           : Int,
    val average          : Double,
    val highest          : Double,
    val lowest           : Double,
    val median           : Double,
    val standardDeviation: Double,
    val gradeDistribution: Map<String, Int>
) {
    val passRate: Double get() = if (total > 0) passed.toDouble() / total * 100.0 else 0.0
    val failRate: Double get() = 100.0 - passRate
}

// [C2] classe normale avec init block
// [C3] by lazy : stats calculées seulement au premier accès
class BatchResult(
    val students   : List<Student>,
    val sourceFiles: List<String>,
    val processedAt: LocalDateTime = LocalDateTime.now()
) {
    // [C2] init block — s'exécute à chaque création d'instance
    init {
        println("[BatchResult] Initialisé avec ${students.size} étudiant(s)")
    }

    val isEmpty: Boolean get() = students.isEmpty()
    val hasData: Boolean get() = students.isNotEmpty()
    val count  : Int     get() = students.size

    // [C3] by lazy — évalué une seule fois, mis en cache ensuite (thread-safe)
    val stats: BatchStats by lazy {
        if (students.isEmpty()) {
            return@lazy BatchStats(0, 0, 0, 0.0, 0.0, 0.0, 0.0, 0.0, emptyMap())
        }
        val scores = students.map { it.rawGrade }
        val passed = students.count { it.hasPassed }
        val sorted = scores.sorted()
        val avg    = scores.average()

        val mid    = sorted.size / 2
        val median = if (sorted.size % 2 == 1) sorted[mid]
                     else (sorted[mid - 1] + sorted[mid]) / 2.0

        val variance = scores.map { (it - avg) * (it - avg) }.average()
        val stdDev   = if (variance > 0) Math.sqrt(variance) else 0.0

        // [C1] fold — HOF d'accumulation (réduction)
        val dist = students.fold(mutableMapOf<String, Int>()) { acc, s ->
            acc[s.letterGrade] = (acc[s.letterGrade] ?: 0) + 1
            acc
        }.toMap()

        BatchStats(
            total             = students.size,
            passed            = passed,
            failed            = students.size - passed,
            average           = "%.2f".format(avg).toDouble(),
            highest           = sorted.last(),
            lowest            = sorted.first(),
            median            = "%.2f".format(median).toDouble(),
            standardDeviation = "%.2f".format(stdDev).toDouble(),
            gradeDistribution = dist
        )
    }
}

// [C2] sealed class — toutes les sous-classes connues à la compilation
// when devient exhaustif : pas besoin de else
sealed class AppUiState {
    object Idle    : AppUiState()
    object Loading : AppUiState()
    data class Success(val message: String) : AppUiState()
    data class Error  (val message: String) : AppUiState()
}

// [C2] enum class — valeurs nommées avec propriétés
enum class ExportFormat(val label: String, val extension: String) {
    EXCEL("Excel (.xlsx)", "xlsx"),
    PDF  ("PDF (.pdf)",    "pdf"),
    XML  ("XML (.xml)",    "xml"),
    WORD ("Word (.docx)",  "docx")
}

enum class SortKey { NAME, MATRICULE, GRADE, LETTER }

// [C2] interface — contrat avec méthode abstraite + méthode par défaut
interface ReportExporter {
    // Méthode abstraite — DOIT être implémentée par toute classe concrète
    fun export(result: BatchResult, destDir: String): String

    // Méthode par défaut — utilisable sans override
    fun timestamp(): String {
        val n = LocalDateTime.now()
        return "%04d%02d%02d_%02d%02d".format(n.year, n.monthValue, n.dayOfMonth, n.hour, n.minute)
    }
}

// [C2] abstract class — base commune, ne peut pas être instanciée directement
abstract class BaseExporter : ReportExporter {

    // [C2] protected — visible dans cette classe et ses sous-classes uniquement
    protected fun buildOutputPath(destDir: String, format: ExportFormat): String =
        java.io.File(destDir, "resultats_${timestamp()}.${format.extension}").absolutePath

    protected fun formatDate(dt: LocalDateTime): String =
        "%02d/%02d/%04d %02d:%02d".format(dt.dayOfMonth, dt.monthValue, dt.year, dt.hour, dt.minute)
}

// [C2] companion object — factory (méthode statique Kotlin)
class ExporterFactory private constructor() {
    companion object {
        fun create(format: ExportFormat): ReportExporter = when (format) {
            ExportFormat.EXCEL -> com.gradecalc.services.ExcelService()
            ExportFormat.PDF   -> com.gradecalc.services.PdfService()
            ExportFormat.XML   -> com.gradecalc.services.XmlService()
            ExportFormat.WORD  -> com.gradecalc.services.WordService()
        }
    }
}
