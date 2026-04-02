// src/main/kotlin/services/GradeCalculator.kt
package com.gradecalc.services

import com.gradecalc.models.*

// [C2] typealias — noms lisibles pour les types de fonctions
typealias RawRow             = Map<String, Any?>
typealias StudentTransformer = (RawRow) -> Student

// [C2] object singleton — service stateless sans état mutable
object GradeCalculatorService {

    // [C1] lambda stockée dans val — validateur réutilisable comme valeur
    val validateGrade: (Any?) -> String? = { value ->
        when {
            value == null              -> "Note manquante"
            value.toString().isBlank() -> "Note vide"
            else -> {
                val parsed = value.toString().trim().toDoubleOrNull()
                when {
                    parsed == null             -> "Valeur non numérique : $value"
                    parsed < 0 || parsed > 100 -> "Hors intervalle [0-100] : $parsed"
                    else                       -> null   // null = valide
                }
            }
        }
    }

    // [C3] HOF factory — retourne une fonction configurée (closure)
    // buildTransformer() est une Higher-Order Function qui RETOURNE une autre fonction
    fun buildTransformer(
        nameProcessor : (String) -> String = { it.trim() },
        gradeProcessor: (Double) -> Double = { g -> Math.round(g * 10) / 10.0 }
    ): StudentTransformer = { row ->
        val name     = nameProcessor(row["name"]?.toString() ?: "")
        val mat      = row["matricule"]?.toString()?.trim() ?: ""
        val rawGrade = gradeProcessor(
            row["grade"]?.toString()?.trim()?.toDoubleOrNull() ?: 0.0
        )
        val band = GradeScale.resolve(rawGrade)
        Student(
            name         = name,
            matricule    = mat,
            rawGrade     = rawGrade,
            letterGrade  = band.letter,
            appreciation = band.appreciation,
            remark       = GradeScale.remarkFor(rawGrade)
        )
    }

    // [C1] pipeline de HOFs chaînés : filter → map → filter
    fun process(
        rows       : List<RawRow>,
        sourceFiles: List<String>,
        transformer: StudentTransformer = buildTransformer()
    ): BatchResult {
        val students = rows
            .filter  { rowIsUsable(it) }       // HOF filter : garder lignes non vides
            .map     (transformer)             // HOF map   : transformer chaque ligne
            .filter  { it.name.isNotEmpty() } // HOF filter : exclure noms vides

        // [C3] also — exécute un effet de bord sans modifier la valeur retournée
        return BatchResult(students = students, sourceFiles = sourceFiles).also {
            println("[GradeCalculatorService] ${it.count} étudiant(s) traité(s)")
        }
    }

    // [C1] HOF — retourne un Comparator selon la clé choisie
    fun sortBy(students: List<Student>, key: SortKey, ascending: Boolean = true): List<Student> {
        // [C2] when-expression retournant une valeur (Comparator)
        val comparator: Comparator<Student> = when (key) {
            SortKey.NAME      -> compareBy { it.name }
            SortKey.MATRICULE -> compareBy { it.matricule }
            SortKey.GRADE     -> compareBy { it.rawGrade }
            SortKey.LETTER    -> compareBy { it.letterGrade }
        }
        // [C1] let — transformation de la valeur intermédiaire
        return students.sortedWith(comparator).let { if (ascending) it else it.reversed() }
    }

    // [C3] fonction générique avec contrainte de type
    // T doit implémenter Comparable<T> — garantit que > est disponible
    fun <T : Comparable<T>> maxOf(list: List<T>): T? =
        list.fold(null as T?) { acc, item ->
            if (acc == null || item > acc) item else acc
        }

    // [C1] HOF — predicate en paramètre (function type)
    fun filterWhere(students: List<Student>, predicate: (Student) -> Boolean): List<Student> =
        students.filter(predicate)

    // [C1] groupBy — HOF de regroupement
    fun groupByGrade(students: List<Student>): Map<String, List<Student>> =
        students.groupBy { it.letterGrade }

    private fun rowIsUsable(row: RawRow): Boolean {
        val name = row["name"]?.toString()?.trim() ?: ""
        val mat  = row["matricule"]?.toString()?.trim() ?: ""
        return name.isNotEmpty() || mat.isNotEmpty()
    }
}
