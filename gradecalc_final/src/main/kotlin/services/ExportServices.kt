// src/main/kotlin/services/ExportServices.kt
package com.gradecalc.services

import com.gradecalc.models.*
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

// ── ExcelService ─────────────────────────────────────────────────────────
class ExcelService : BaseExporter() {

    // Lit un fichier Excel et retourne les lignes sous forme de Map
    fun readFile(filePath: String): List<RawRow> {
        val wb    = FileInputStream(File(filePath)).use { XSSFWorkbook(it) }
        val sheet = wb.sheetIterator().asSequence()
            .firstOrNull { it.physicalNumberOfRows > 0 }
            ?: throw Exception("Aucune feuille non-vide trouvée dans $filePath")

        val rows = sheet.iterator().asSequence().toList()
        if (rows.isEmpty()) throw Exception("Feuille vide.")

        // Analyser la ligne d'en-tête → trouver les colonnes requises
        val colMap = detectColumns(rows.first())
        for (required in listOf("name", "matricule", "grade")) {
            if (required !in colMap.values)
                throw Exception(
                    "Colonne '$required' manquante.\n" +
                    "En-têtes reconnus : 'nom/name', 'matricule/id', 'note/grade/score'"
                )
        }

        // Convertir les lignes (en ignorant l'en-tête)
        return rows.drop(1).map { row ->
            colMap.entries.associate { (colIdx, key) ->
                val cell = if (colIdx < row.lastCellNum) row.getCell(colIdx) else null
                key to cellToValue(cell)
            }
        }
    }

    // [C2] override de la méthode abstraite de l'interface ReportExporter
    override fun export(result: BatchResult, destDir: String): String {
        val wb = XSSFWorkbook()
        buildResultSheet(wb, result)
        buildStatsSheet(wb, result.stats)
        val outPath = buildOutputPath(destDir, ExportFormat.EXCEL)
        FileOutputStream(outPath).use { wb.write(it) }
        return outPath
    }

    private fun buildResultSheet(wb: XSSFWorkbook, result: BatchResult) {
        val sheet   = wb.createSheet("Résultats")
        val headers = listOf("N°", "Matricule", "Nom", "Note (/100)", "Mention", "Appréciation", "Résultat")

        // Style en-tête : fond bleu, texte blanc, gras
        val headFont  = wb.createFont().also { it.bold = true; it.color = IndexedColors.WHITE.index }
        val headStyle = wb.createCellStyle().also { s ->
            s.setFont(headFont)
            s.setFillForegroundColor(IndexedColors.ROYAL_BLUE.index)
            s.fillPattern = FillPatternType.SOLID_FOREGROUND
            s.alignment   = HorizontalAlignment.CENTER
        }
        // Style lignes paires : fond gris clair
        val evenStyle = wb.createCellStyle().also { s ->
            s.setFillForegroundColor(IndexedColors.LAVENDER.index)
            s.fillPattern = FillPatternType.SOLID_FOREGROUND
        }

        val hRow = sheet.createRow(0)
        headers.forEachIndexed { i, h ->
            hRow.createCell(i).also { cell -> cell.setCellValue(h); cell.cellStyle = headStyle }
        }

        result.students.forEachIndexed { idx, s ->
            val row = sheet.createRow(idx + 1)
            if (idx % 2 == 0) row.rowStyle = evenStyle
            row.createCell(0).setCellValue((idx + 1).toDouble())
            row.createCell(1).setCellValue(s.matricule)
            row.createCell(2).setCellValue(s.name)
            row.createCell(3).setCellValue(s.rawGrade)
            row.createCell(4).setCellValue(s.letterGrade)
            row.createCell(5).setCellValue(s.appreciation)
            row.createCell(6).setCellValue(s.remark)
        }
        (0..6).forEach { sheet.autoSizeColumn(it) }
    }

    private fun buildStatsSheet(wb: XSSFWorkbook, stats: BatchStats) {
        val sheet  = wb.createSheet("Statistiques")
        var rowIdx = 0

        fun addRow(label: String, value: String) {
            sheet.createRow(rowIdx++).also { row ->
                row.createCell(0).setCellValue(label)
                row.createCell(1).setCellValue(value)
            }
        }

        // [C3] with — opère sur stats sans répéter le nom de la variable
        with(stats) {
            addRow("Total étudiants",    "$total")
            addRow("Admis",              "$passed (${"%.1f".format(passRate)}%)")
            addRow("Ajournés",           "$failed (${"%.1f".format(failRate)}%)")
            addRow("Moyenne générale",   "%.2f".format(average))
            addRow("Note la plus haute", "%.2f".format(highest))
            addRow("Note la plus basse", "%.2f".format(lowest))
            addRow("Médiane",            "%.2f".format(median))
            addRow("Écart-type",         "%.2f".format(standardDeviation))
            sheet.createRow(rowIdx++)
            sheet.createRow(rowIdx++).createCell(0).setCellValue("Répartition par mention")
            gradeDistribution.forEach { (letter, count) -> addRow(letter, "$count") }
        }
        (0..1).forEach { sheet.autoSizeColumn(it) }
    }

    private fun detectColumns(headerRow: Row): Map<Int, String> {
        val map = mutableMapOf<Int, String>()
        headerRow.cellIterator().asSequence().forEach { cell ->
            if (cell.cellType == CellType.STRING) {
                val normalized = cell.stringCellValue.trim().lowercase()
                val key = when (normalized) {
                    "nom", "name", "prénom", "prenom", "etudiant", "student" -> "name"
                    "matricule", "id", "num", "numero", "code", "immatriculation" -> "matricule"
                    "note", "grade", "score", "mark", "marks", "points", "résultat" -> "grade"
                    else -> null
                }
                if (key != null) map[cell.columnIndex] = key
            }
        }
        return map
    }

    private fun cellToValue(cell: Cell?): Any? = when (cell?.cellType) {
        CellType.NUMERIC -> cell.numericCellValue
        CellType.STRING  -> cell.stringCellValue
        CellType.FORMULA -> try { cell.numericCellValue } catch (_: Exception) { cell.stringCellValue }
        else             -> null
    }
}

// ── PdfService ────────────────────────────────────────────────────────────
class PdfService : BaseExporter() {

    override fun export(result: BatchResult, destDir: String): String {
        val outPath  = buildOutputPath(destDir, ExportFormat.PDF)
        val document = com.itextpdf.text.Document(com.itextpdf.text.PageSize.A4)
        com.itextpdf.text.pdf.PdfWriter.getInstance(document, FileOutputStream(outPath))
        document.open()

        val stats = result.stats

        // Couleurs
        val blue  = com.itextpdf.text.BaseColor(79, 106, 245)
        val light = com.itextpdf.text.BaseColor(245, 247, 255)
        val white = com.itextpdf.text.BaseColor.WHITE
        val gray  = com.itextpdf.text.BaseColor(100, 100, 130)

        // Polices (iText 5 : constructeur Font direct)
        fun font(size: Float, bold: Boolean, color: com.itextpdf.text.BaseColor) =
            com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA,
                size,
                if (bold) com.itextpdf.text.Font.BOLD else com.itextpdf.text.Font.NORMAL,
                color
            )

        val fTitle  = font(16f, true,  blue)
        val fSub    = font(9f,  false, gray)
        val fBold   = font(11f, true,  com.itextpdf.text.BaseColor.BLACK)
        val fSmall  = font(8f,  false, com.itextpdf.text.BaseColor.BLACK)
        val fWhite  = font(8f,  true,  white)

        // Titre
        document.add(com.itextpdf.text.Paragraph("GradeCalc Pro — Rapport de Résultats", fTitle))
        document.add(com.itextpdf.text.Paragraph(
            "Généré le ${formatDate(result.processedAt)}  |  ${result.sourceFiles.joinToString(", ")}", fSub))
        document.add(com.itextpdf.text.Paragraph("\n"))

        // KPI
        document.add(com.itextpdf.text.Paragraph("Statistiques Générales", fBold))
        document.add(com.itextpdf.text.Paragraph("\n"))

        val kpiTable = com.itextpdf.text.pdf.PdfPTable(5).apply { widthPercentage = 100f }
        with(stats) {
            listOf(
                "Total"    to "$total",
                "Admis"    to "$passed",
                "Ajournés" to "$failed",
                "Moyenne"  to "%.2f".format(average),
                "Réussite" to "${"%.1f".format(passRate)}%"
            ).forEach { (label, value) ->
                kpiTable.addCell(
                    com.itextpdf.text.pdf.PdfPCell().apply {
                        addElement(com.itextpdf.text.Paragraph("$value\n$label", fSmall))
                        backgroundColor = light
                        horizontalAlignment = com.itextpdf.text.Element.ALIGN_CENTER
                        setPadding(8f)
                    }
                )
            }
        }
        document.add(kpiTable)
        document.add(com.itextpdf.text.Paragraph("\n"))

        // Tableau étudiants
        document.add(com.itextpdf.text.Paragraph("Liste des Étudiants", fBold))
        document.add(com.itextpdf.text.Paragraph("\n"))

        val widths = floatArrayOf(4f, 14f, 25f, 9f, 9f, 20f, 15f)
        val table  = com.itextpdf.text.pdf.PdfPTable(widths.size).apply {
            widthPercentage = 100f
            setWidths(widths)
        }

        fun pdfCell(text: String, bg: com.itextpdf.text.BaseColor, f: com.itextpdf.text.Font, pad: Float) =
            com.itextpdf.text.pdf.PdfPCell().apply {
                addElement(com.itextpdf.text.Paragraph(text, f))
                backgroundColor = bg
                setPadding(pad)
            }

        listOf("N°", "Matricule", "Nom", "Note", "Mention", "Appréciation", "Résultat").forEach { h ->
            table.addCell(pdfCell(h, blue, fWhite, 5f))
        }
        result.students.forEachIndexed { idx, s ->
            val bg = if (idx % 2 == 0) light else white
            listOf((idx + 1).toString(), s.matricule, s.name,
                   s.formattedGrade, s.letterGrade, s.appreciation, s.remark).forEach { v ->
                table.addCell(pdfCell(v, bg, fSmall, 4f))
            }
        }
        document.add(table)
        document.close()
        return outPath
    }
}

// ── XmlService ────────────────────────────────────────────────────────────
class XmlService : BaseExporter() {

    override fun export(result: BatchResult, destDir: String): String {
        val outPath = buildOutputPath(destDir, ExportFormat.XML)
        val stats   = result.stats
        val sb      = StringBuilder()

        // [C3] apply — configure le StringBuilder sans répéter son nom
        sb.apply {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("<rapport>")
            appendLine("  <meta>")
            appendLine("    <generePar>GradeCalc Pro 1.0 — Kotlin</generePar>")
            appendLine("    <dateExport>${result.processedAt}</dateExport>")
            appendLine("    <sources>")
            result.sourceFiles.forEach { appendLine("      <fichier>${it.esc()}</fichier>") }
            appendLine("    </sources>")
            appendLine("  </meta>")

            appendLine("  <statistiques>")
            // [C3] with — opère sur stats
            with(stats) {
                appendLine("    <total>$total</total>")
                appendLine("    <admis>$passed</admis>")
                appendLine("    <ajournes>$failed</ajournes>")
                appendLine("    <tauxReussite>${"%.1f".format(passRate)}%</tauxReussite>")
                appendLine("    <moyenne>${"%.2f".format(average)}</moyenne>")
                appendLine("    <noteMax>${"%.2f".format(highest)}</noteMax>")
                appendLine("    <noteMin>${"%.2f".format(lowest)}</noteMin>")
                appendLine("    <mediane>${"%.2f".format(median)}</mediane>")
                appendLine("    <ecartType>${"%.2f".format(standardDeviation)}</ecartType>")
                appendLine("    <repartition>")
                gradeDistribution.forEach { (letter, count) ->
                    appendLine("""      <mention lettre="$letter">$count</mention>""")
                }
                appendLine("    </repartition>")
            }
            appendLine("  </statistiques>")

            appendLine("  <etudiants>")
            result.students.forEach { s ->
                appendLine("""    <etudiant matricule="${s.matricule.esc()}">""")
                appendLine("      <nom>${s.name.esc()}</nom>")
                appendLine("      <note>${s.rawGrade}</note>")
                appendLine("      <mention>${s.letterGrade}</mention>")
                appendLine("      <appreciation>${s.appreciation.esc()}</appreciation>")
                appendLine("      <resultat>${s.remark.esc()}</resultat>")
                appendLine("    </etudiant>")
            }
            appendLine("  </etudiants>")
            appendLine("</rapport>")
        }

        File(outPath).writeText(sb.toString(), Charsets.UTF_8)
        return outPath
    }

    // [C2] extension function sur String
    private fun String.esc(): String = replace("&", "&amp;")
        .replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}

// ── WordService ───────────────────────────────────────────────────────────
class WordService : BaseExporter() {

    override fun export(result: BatchResult, destDir: String): String {
        val outPath = buildOutputPath(destDir, ExportFormat.WORD)
        val doc     = org.apache.poi.xwpf.usermodel.XWPFDocument()

        addHeading(doc, "GradeCalc Pro — Rapport de Résultats", 18, "4F6AF5")
        addPara   (doc, "Généré le ${formatDate(result.processedAt)}")
        addPara   (doc, "Sources : ${result.sourceFiles.joinToString(", ")}")
        addPara   (doc, "")

        addHeading(doc, "Statistiques Générales", 13, "1A1D2E")
        val statsRows = mutableListOf(listOf("Indicateur", "Valeur"))
        with(result.stats) {
            statsRows += listOf("Total étudiants",    "$total")
            statsRows += listOf("Admis",              "$passed (${"%.1f".format(passRate)}%)")
            statsRows += listOf("Ajournés",           "$failed (${"%.1f".format(failRate)}%)")
            statsRows += listOf("Moyenne générale",   "%.2f".format(average))
            statsRows += listOf("Note la plus haute", "%.2f".format(highest))
            statsRows += listOf("Note la plus basse", "%.2f".format(lowest))
            statsRows += listOf("Médiane",            "%.2f".format(median))
            statsRows += listOf("Écart-type",         "%.2f".format(standardDeviation))
        }
        addTable(doc, statsRows)
        addPara(doc, "")

        addHeading(doc, "Liste des Étudiants", 13, "1A1D2E")
        val dataRows = mutableListOf(listOf("N°", "Matricule", "Nom", "Note", "Mention", "Appréciation", "Résultat"))
        result.students.forEachIndexed { i, s ->
            dataRows += listOf("${i + 1}", s.matricule, s.name,
                s.formattedGrade, s.letterGrade, s.appreciation, s.remark)
        }
        addTable(doc, dataRows)

        FileOutputStream(outPath).use { doc.write(it) }
        return outPath
    }

    private fun addHeading(doc: org.apache.poi.xwpf.usermodel.XWPFDocument,
                            text: String, size: Int, colorHex: String) {
        doc.createParagraph().createRun().also { run ->
            run.setText(text); run.isBold = true; run.fontSize = size; run.setColor(colorHex)
        }
    }

    private fun addPara(doc: org.apache.poi.xwpf.usermodel.XWPFDocument, text: String) {
        doc.createParagraph().createRun().setText(text)
    }

    private fun addTable(doc: org.apache.poi.xwpf.usermodel.XWPFDocument, data: List<List<String>>) {
        if (data.isEmpty()) return
        val table = doc.createTable(data.size, data[0].size)
        data.forEachIndexed { rowIdx, cols ->
            cols.forEachIndexed { colIdx, value ->
                val cell = table.getRow(rowIdx).getCell(colIdx) ?: table.getRow(rowIdx).addNewTableCell()
                cell.removeParagraph(0)
                cell.addParagraph().createRun().also { run ->
                    run.setText(value)
                    run.fontSize = 9
                    if (rowIdx == 0) { run.isBold = true; run.setColor("FFFFFF") }
                }
                if (rowIdx == 0) cell.setColor("4F6AF5")
                else if (rowIdx % 2 == 0) cell.setColor("F5F7FF")
            }
        }
    }
}
