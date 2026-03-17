// src/main/kotlin/ui/MainWindow.kt
package com.gradecalc.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.gradecalc.models.*
import java.awt.Desktop
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

// ── Composable racine ─────────────────────────────────────────────────────
@Composable
fun AppRoot(vm: AppViewModel) {
    MaterialTheme(colorScheme = AppColors.colorScheme) {
        Surface(color = AppColors.Surface) {
            Row(Modifier.fillMaxSize()) {
                Sidebar(vm, Modifier.width(300.dp).fillMaxHeight())
                MainContent(vm, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// PANNEAU LATÉRAL GAUCHE
// ══════════════════════════════════════════════════════════════════════
@Composable
fun Sidebar(vm: AppViewModel, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(AppColors.SurfaceVariant)
            .border(BorderStroke(1.dp, AppColors.Border))
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Logo()
        // [C3] state hoisting — callback passé au composable enfant
        FilePickerButton { vm.loadFiles(it) }
        if (vm.loadedFiles.isNotEmpty()) FileList(vm)
        if (vm.hasResult) {
            OutlinedButton(
                onClick  = { vm.reset() },
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Danger),
                border   = BorderStroke(1.dp, AppColors.Danger)
            ) {
                Icon(Icons.Outlined.Refresh, null, Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("Réinitialiser")
            }
        }
        ExportCard(vm)
        GradeLegend()
        if (vm.errorLog.isNotEmpty()) ErrorCard(vm.errorLog)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun Logo() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(42.dp).background(
                Brush.linearGradient(listOf(AppColors.Primary, AppColors.Accent)),
                RoundedCornerShape(12.dp)
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.School, null, Modifier.size(22.dp), tint = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(AppViewModel.APP_NAME,
                fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppColors.TextPrimary)
            Text(AppViewModel.APP_COURSE, fontSize = 9.sp, color = AppColors.TextSecondary)
        }
    }
}

@Composable
fun FilePickerButton(onFiles: (List<String>) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(145.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.CardBg)
            .border(BorderStroke(1.5.dp, AppColors.Border), RoundedCornerShape(14.dp))
            .clickable { selectFiles(onFiles) },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.CloudUpload, null, Modifier.size(40.dp), tint = AppColors.Primary)
            Text("Cliquez pour ouvrir des fichiers",
                fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AppColors.TextPrimary)
            Text("Formats acceptés : .xlsx   .xls",
                fontSize = 11.sp, color = AppColors.TextSecondary)
        }
    }
}

@Composable
fun FileList(vm: AppViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("${vm.loadedFiles.size} fichier(s) chargé(s)",
            fontSize = 12.sp, color = AppColors.TextSecondary)
        vm.loadedFiles.forEach { path ->
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.CardBg)
                    .border(BorderStroke(1.dp, AppColors.Border), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.TableChart, null, Modifier.size(14.dp), tint = AppColors.Accent)
                Spacer(Modifier.width(7.dp))
                Text(File(path).name, Modifier.weight(1f), fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, color = AppColors.TextPrimary)
                IconButton(onClick = { vm.removeFile(path) }, Modifier.size(22.dp)) {
                    Icon(Icons.Outlined.Close, null, Modifier.size(13.dp), tint = AppColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
fun ExportCard(vm: AppViewModel) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(AppColors.CardBg)
            .border(BorderStroke(1.dp, AppColors.Border), RoundedCornerShape(13.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Download, null, Modifier.size(16.dp), tint = AppColors.Primary)
            Spacer(Modifier.width(6.dp))
            Text("Exporter le rapport",
                fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AppColors.TextPrimary)
        }
        Spacer(Modifier.height(4.dp))

        ExportFormat.entries.forEach { fmt ->
            val selected = fmt == vm.selectedFormat
            val color    = AppColors.forFormat(fmt.label)
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) color.copy(0.15f) else Color.Transparent)
                    .border(BorderStroke(1.dp, if (selected) color.copy(0.6f) else AppColors.Border),
                        RoundedCornerShape(8.dp))
                    .clickable { vm.selectedFormat = fmt }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(fmt.label, Modifier.weight(1f), fontSize = 12.sp,
                    color      = if (selected) color else AppColors.TextPrimary,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                if (selected) Icon(Icons.Outlined.Check, null, Modifier.size(12.dp), tint = color)
            }
        }

        Spacer(Modifier.height(6.dp))
        Divider(color = AppColors.Border)
        Spacer(Modifier.height(6.dp))

        Button(
            onClick  = { vm.export() },
            enabled  = vm.hasResult && vm.uiState !is AppUiState.Loading,
            modifier = Modifier.fillMaxWidth(),
            colors   = ButtonDefaults.buttonColors(
                containerColor = AppColors.forFormat(vm.selectedFormat.label)
            )
        ) {
            if (vm.uiState is AppUiState.Loading)
                CircularProgressIndicator(Modifier.size(15.dp), color = Color.White, strokeWidth = 2.dp)
            else
                Icon(Icons.Outlined.Download, null, Modifier.size(15.dp))
            Spacer(Modifier.width(7.dp))
            Text("Exporter en ${vm.selectedFormat.extension.uppercase()}")
        }

        vm.lastExportPath?.let { path ->
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.clip(RoundedCornerShape(7.dp))
                    .background(AppColors.Success.copy(0.1f))
                    .clickable { openFile(path) }
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.OpenInNew, null, Modifier.size(12.dp), tint = AppColors.Success)
                Spacer(Modifier.width(5.dp))
                Text(File(path).name, fontSize = 10.sp, color = AppColors.Success,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun GradeLegend() {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.CardBg)
            .border(BorderStroke(1.dp, AppColors.Border), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Grading, null, Modifier.size(14.dp), tint = AppColors.Primary)
            Spacer(Modifier.width(6.dp))
            Text("Barème de notation",
                fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AppColors.TextPrimary)
        }
        Spacer(Modifier.height(8.dp))
        GradeScale.bands.forEach { band ->
            val color = AppColors.forGrade(band.letter)
            Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(band.letter, Modifier.width(26.dp),
                    fontWeight = FontWeight.Bold, fontSize = 11.sp, color = color)
                Box(Modifier.weight(1f).height(5.dp)
                    .clip(RoundedCornerShape(3.dp)).background(AppColors.Border)) {
                    Box(Modifier.fillMaxHeight()
                        .fillMaxWidth(band.maxInclusive.toFloat() / 100f)
                        .clip(RoundedCornerShape(3.dp)).background(color.copy(0.55f)))
                }
                Spacer(Modifier.width(6.dp))
                Text("${band.minInclusive.toInt()}–${band.maxInclusive.toInt()}",
                    fontSize = 10.sp, color = AppColors.TextSecondary)
            }
        }
    }
}

@Composable
fun ErrorCard(errors: List<String>) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AppColors.Danger.copy(0.1f))
            .border(BorderStroke(1.dp, AppColors.Danger.copy(0.4f)), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Warning, null, Modifier.size(13.dp), tint = AppColors.Danger)
            Spacer(Modifier.width(5.dp))
            Text("Avertissements", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = AppColors.Danger)
        }
        errors.forEach { err ->
            Text(err, fontSize = 10.sp, color = AppColors.Danger, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// PANNEAU PRINCIPAL DROIT
// ══════════════════════════════════════════════════════════════════════
@Composable
fun MainContent(vm: AppViewModel, modifier: Modifier) {
    Column(modifier) {
        StatusBar(vm)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (vm.hasResult) ResultsView(vm) else WelcomeView()
        }
    }
}

@Composable
fun StatusBar(vm: AppViewModel) {
    Row(
        Modifier.fillMaxWidth().height(52.dp)
            .background(AppColors.SurfaceVariant)
            .border(BorderStroke(1.dp, AppColors.Border))
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // [C2] when exhaustif sur sealed class AppUiState
        when (val state = vm.uiState) {
            is AppUiState.Idle    -> {}
            is AppUiState.Loading -> {
                CircularProgressIndicator(Modifier.size(14.dp), color = AppColors.Primary, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Chargement…", fontSize = 13.sp, color = AppColors.TextSecondary)
            }
            is AppUiState.Success -> {
                Icon(Icons.Outlined.CheckCircle, null, Modifier.size(14.dp), tint = AppColors.Success)
                Spacer(Modifier.width(8.dp))
                Text(state.message, fontSize = 13.sp, color = AppColors.TextSecondary,
                    modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            is AppUiState.Error -> {
                Icon(Icons.Outlined.ErrorOutline, null, Modifier.size(14.dp), tint = AppColors.Danger)
                Spacer(Modifier.width(8.dp))
                Text(state.message, fontSize = 13.sp, color = AppColors.Danger,
                    modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.clip(RoundedCornerShape(20.dp))
                .background(AppColors.Primary.copy(0.15f))
                .padding(horizontal = 9.dp, vertical = 3.dp)
        ) {
            Text("v${AppViewModel.APP_VERSION}", fontSize = 10.sp, color = AppColors.Primary)
        }
    }
}

@Composable
fun WelcomeView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                Modifier.size(90.dp).background(
                    Brush.radialGradient(listOf(AppColors.Primary.copy(0.2f), AppColors.Accent.copy(0.04f))),
                    RoundedCornerShape(24.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.UploadFile, null, Modifier.size(44.dp), tint = AppColors.Primary)
            }
            Text("Bienvenue dans ${AppViewModel.APP_NAME}",
                fontWeight = FontWeight.Bold, fontSize = 20.sp, color = AppColors.TextPrimary)
            Text("Cliquez sur la zone à gauche pour charger\nvotre fichier Excel (.xlsx ou .xls).",
                fontSize = 14.sp, color = AppColors.TextSecondary, lineHeight = 22.sp)
            Row(
                Modifier.clip(RoundedCornerShape(12.dp))
                    .background(AppColors.SurfaceVariant.copy(0.7f))
                    .border(BorderStroke(1.dp, AppColors.Border), RoundedCornerShape(12.dp))
                    .padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                listOf("Nom" to AppColors.Primary, "Matricule" to AppColors.Accent,
                    "Note" to AppColors.Warning).forEach { (col, color) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(col, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = color)
                        Text("Colonne Excel", fontSize = 9.sp, color = AppColors.TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun ResultsView(vm: AppViewModel) {
    val stats = vm.result!!.stats
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        KpiRow(stats)
        GradeDistRow(stats)
        Divider(color = AppColors.Border)
        // [C3] state hoisting — vm passé au tableau
        StudentTable(vm, Modifier.weight(1f))
    }
}

// ── KPI cards ─────────────────────────────────────────────────────────
@Composable
fun KpiRow(stats: BatchStats) {
    data class Kpi(val label: String, val value: String, val icon: ImageVector, val color: Color)
    val items = listOf(
        Kpi("Total",    "${stats.total}",                    Icons.Outlined.People,      AppColors.Primary),
        Kpi("Admis",    "${stats.passed}",                   Icons.Outlined.CheckCircle, AppColors.Success),
        Kpi("Ajournés", "${stats.failed}",                   Icons.Outlined.Cancel,      AppColors.Danger),
        Kpi("Moyenne",  "%.2f".format(stats.average),        Icons.Outlined.BarChart,    AppColors.Accent),
        Kpi("Réussite", "${"%.1f".format(stats.passRate)}%", Icons.Outlined.EmojiEvents, AppColors.Success),
        Kpi("Médiane",  "%.2f".format(stats.median),         Icons.Outlined.ShowChart,   AppColors.Warning)
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { kpi ->
            Column(
                Modifier.weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(kpi.color.copy(0.1f))
                    .border(BorderStroke(1.dp, kpi.color.copy(0.3f)), RoundedCornerShape(11.dp))
                    .padding(14.dp)
            ) {
                Icon(kpi.icon, null, Modifier.size(18.dp), tint = kpi.color)
                Spacer(Modifier.height(6.dp))
                Text(kpi.value, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = kpi.color)
                Text(kpi.label, fontSize = 10.sp, color = AppColors.TextSecondary)
            }
        }
    }
}

@Composable
fun GradeDistRow(stats: BatchStats) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        GradeScale.bands.forEach { band ->
            val count = stats.gradeDistribution[band.letter] ?: 0
            val color = AppColors.forGrade(band.letter)
            Row(
                Modifier.clip(RoundedCornerShape(7.dp))
                    .background(color.copy(0.12f))
                    .border(BorderStroke(1.dp, color.copy(0.4f)), RoundedCornerShape(7.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(band.letter, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = color)
                Spacer(Modifier.width(4.dp))
                Text("$count", fontSize = 10.sp, color = AppColors.TextSecondary)
            }
        }
    }
}

// ── Tableau étudiants ──────────────────────────────────────────────────
@Composable
fun StudentTable(vm: AppViewModel, modifier: Modifier) {
    val headers  = listOf("N°","Matricule","Nom","Note","Mention","Appréciation","Résultat")
    val sortKeys = listOf(null, SortKey.MATRICULE, SortKey.NAME, SortKey.GRADE, SortKey.LETTER, null, null)
    val weights  = listOf(0.5f, 1.5f, 2.5f, 1f, 1f, 2f, 1.5f)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Barre de recherche
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(9.dp))
                .background(AppColors.SurfaceVariant)
                .border(BorderStroke(1.dp, AppColors.Border), RoundedCornerShape(9.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Search, null, Modifier.size(16.dp), tint = AppColors.TextSecondary)
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value         = vm.searchQuery,
                onValueChange = { vm.searchQuery = it },
                textStyle     = TextStyle(color = AppColors.TextPrimary, fontSize = 13.sp),
                singleLine    = true,
                modifier      = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (vm.searchQuery.isEmpty())
                        Text("Rechercher par nom, matricule ou mention…",
                            fontSize = 13.sp, color = AppColors.TextSecondary)
                    inner()
                }
            )
            if (vm.searchQuery.isNotEmpty()) {
                IconButton(onClick = { vm.searchQuery = "" }, Modifier.size(20.dp)) {
                    Icon(Icons.Outlined.Clear, null, Modifier.size(14.dp), tint = AppColors.TextSecondary)
                }
            }
            Spacer(Modifier.width(8.dp))
            Text("${vm.displayStudents.size} étudiant(s)", fontSize = 11.sp, color = AppColors.TextSecondary)
        }

        // En-tête colonnes (triables)
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 9.dp, topEnd = 9.dp))
                .background(AppColors.SurfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            headers.forEachIndexed { i, h ->
                val key = sortKeys[i]
                Row(
                    Modifier.weight(weights[i]).let { m ->
                        if (key != null) m.clickable { vm.toggleSort(key) } else m
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(h, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = AppColors.TextPrimary)
                    if (key != null && vm.sortKey == key) {
                        Spacer(Modifier.width(3.dp))
                        Icon(
                            if (vm.sortAscending) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                            null, Modifier.size(11.dp), tint = AppColors.Primary
                        )
                    }
                }
            }
        }

        // [C3] LazyColumn — compose uniquement les éléments visibles (efficace)
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 9.dp, bottomEnd = 9.dp))
                .border(BorderStroke(1.dp, AppColors.Border),
                    RoundedCornerShape(bottomStart = 9.dp, bottomEnd = 9.dp))
        ) {
            itemsIndexed(vm.displayStudents) { idx, student ->
                StudentRow(idx, student, weights)
                if (idx < vm.displayStudents.size - 1)
                    Divider(color = AppColors.Border.copy(0.4f), thickness = 0.5.dp)
            }
            if (vm.displayStudents.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        Text("Aucun résultat.", color = AppColors.TextSecondary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StudentRow(index: Int, s: Student, weights: List<Float>) {
    val bg         = if (index % 2 == 0) AppColors.CardBg else AppColors.SurfaceVariant.copy(0.3f)
    val gradeColor = AppColors.forGrade(s.letterGrade)
    val rkColor    = if (s.hasPassed) AppColors.Success else AppColors.Danger

    Row(
        Modifier.fillMaxWidth().background(bg).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${index+1}", Modifier.weight(weights[0]), fontSize = 11.sp, color = AppColors.TextSecondary)
        Text(s.matricule, Modifier.weight(weights[1]), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AppColors.TextPrimary)
        Text(s.name, Modifier.weight(weights[2]), fontSize = 12.sp, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)

        Box(Modifier.weight(weights[3]), contentAlignment = Alignment.CenterStart) {
            Box(Modifier.clip(RoundedCornerShape(5.dp))
                .background(AppColors.forScore(s.rawGrade).copy(0.15f))
                .padding(horizontal = 7.dp, vertical = 2.dp)) {
                Text(s.formattedGrade, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AppColors.forScore(s.rawGrade))
            }
        }
        Box(Modifier.weight(weights[4]), contentAlignment = Alignment.CenterStart) {
            Box(Modifier.clip(RoundedCornerShape(5.dp))
                .background(gradeColor.copy(0.15f))
                .border(BorderStroke(1.dp, gradeColor.copy(0.5f)), RoundedCornerShape(5.dp))
                .padding(horizontal = 7.dp, vertical = 2.dp)) {
                Text(s.letterGrade, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = gradeColor)
            }
        }
        Text(s.appreciation, Modifier.weight(weights[5]), fontSize = 11.sp, color = AppColors.TextSecondary)
        Box(Modifier.weight(weights[6]), contentAlignment = Alignment.CenterStart) {
            Row(
                Modifier.clip(RoundedCornerShape(5.dp))
                    .background(rkColor.copy(0.12f))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(if (s.hasPassed) Icons.Outlined.Check else Icons.Outlined.Close,
                    null, Modifier.size(10.dp), tint = rkColor)
                Spacer(Modifier.width(3.dp))
                Text(s.remark, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = rkColor)
            }
        }
    }
}

// ── Utilitaires ───────────────────────────────────────────────────────
fun selectFiles(onFiles: (List<String>) -> Unit) {
    val chooser = JFileChooser().apply {
        isMultiSelectionEnabled = true
        fileFilter = FileNameExtensionFilter("Fichiers Excel (*.xlsx, *.xls)", "xlsx", "xls")
        dialogTitle = "Sélectionner des fichiers Excel"
    }
    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
        onFiles(chooser.selectedFiles.map { it.absolutePath })
}

fun openFile(path: String) {
    try { Desktop.getDesktop().open(File(path)) } catch (_: Exception) {}
}
