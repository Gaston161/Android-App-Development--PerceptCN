<div align="center">

<img src="https://img.shields.io/badge/Kotlin-2.1.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white"/>
<img src="https://img.shields.io/badge/Compose_Desktop-1.8.1-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white"/>
<img src="https://img.shields.io/badge/Gradle-9.3.1-02303A?style=for-the-badge&logo=gradle&logoColor=white"/>
<img src="https://img.shields.io/badge/JDK-25_Temurin-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white"/>
<img src="https://img.shields.io/badge/Platform-Windows_%7C_macOS_%7C_Linux-0078D6?style=for-the-badge"/>

# 🎓 GradeCalc Pro

**Application desktop professionnelle de calcul et d'analyse de notes académiques**

Développée en Kotlin + Compose for Desktop dans le cadre du cours **SE 3242 — Android Application Development** à **ICT University**, Yaoundé, Cameroun.

[Fonctionnalités](#-fonctionnalités) · [Démarrage](#-démarrage-rapide) · [Architecture](#-architecture) · [Format Excel](#-format-du-fichier-excel) · [Barème](#-barème-de-notation) · [Concepts OOP](#-concepts-oop-illustrés)

</div>

---

## ✨ Fonctionnalités

| Fonctionnalité | Description |
|---|---|
| 📂 **Import multi-fichiers** | Sélection de plusieurs `.xlsx`/`.xls` simultanément, fusion automatique |
| ⚡ **Calcul instantané** | Traitement asynchrone via coroutines Kotlin, sans blocage de l'UI |
| 📊 **Statistiques complètes** | Moyenne, médiane, écart-type, taux de réussite, distribution par mention |
| 🔍 **Recherche & tri** | Filtrage temps réel par nom/matricule/mention, tri multi-colonnes |
| 📤 **4 formats d'export** | Excel, PDF, XML, Word — sauvegardé automatiquement dans `Downloads/` |
| 🎨 **Interface sombre** | GUI professionnelle Compose Desktop avec Material 3 |
| 🎓 **Code commenté** | Chaque concept du cours est annoté directement dans le source |

---

## 🚀 Démarrage rapide

### Prérequis

- **JDK 17+** — testé avec JDK 25 Temurin ([télécharger](https://adoptium.net/temurin/releases/?version=17))
- **Gradle 9.3.1** — ([télécharger](https://gradle.org/releases/))
- **IntelliJ IDEA** Community ou Ultimate ([télécharger](https://www.jetbrains.com/idea/download/))

> ⚠️ **Important :** Extraire dans un chemin **sans espaces** (ex: `C:\dev\GradeCalcPro\`) pour éviter les problèmes Gradle sur Windows.

### Installation

```bash
# Cloner le dépôt
git clone https://github.com/votre-user/GradeCalcPro.git
cd GradeCalcPro
```

### Lancement

**Option A — IntelliJ IDEA (recommandé)**
1. `File → Open` → sélectionner le dossier du projet
2. Cliquer **Trust Project** sur le popup Gradle
3. Attendre la synchronisation (3–5 min au 1er lancement)
4. Sélectionner la configuration **GradeCalcPro** → cliquer ▶

**Option B — Terminal**
```bash
gradle run
```

**Option C — Créer un installeur .msi autonome (JRE inclus)**
```bash
gradle packageMsi
# Sortie : build/compose/binaries/main/msi/
```

### Résolution de problèmes courants

| Problème | Solution |
|---|---|
| Timeout téléchargement Gradle | `gradle run` (utilise le Gradle installé, pas le wrapper) |
| `Timeout waiting to lock cache` | `taskkill /F /IM java.exe` puis relancer |
| IntelliJ demande JDK 23 | `Settings → Gradle → Use Gradle from → Specified location` |
| JVM target incompatible | Déjà corrigé : cible JVM 21 dans `build.gradle.kts` |

---

## 🏗️ Architecture

Architecture en **4 couches séparées** — chaque couche ne dépend que des couches inférieures.

```
src/main/kotlin/
├── Main.kt                    ← point d'entrée, application{}
├── models/
│   └── Models.kt              ← Student, GradeScale, BatchResult, sealed class...
├── services/
│   ├── GradeCalculator.kt     ← HOF, lambdas, pipeline fonctionnel
│   └── ExportServices.kt      ← Excel, PDF, XML, Word (BaseExporter)
└── ui/
    ├── AppViewModel.kt        ← état observable, coroutines, delegation
    ├── MainWindow.kt          ← interface graphique Compose Desktop
    └── Theme.kt               ← palette de couleurs, AppColors
```

```
┌─────────────────────────────────────────────────────┐
│  UI / Compose  — MainWindow.kt + Theme.kt           │
├─────────────────────────────────────────────────────┤
│  ViewModel     — AppViewModel.kt                    │
│  (mutableStateOf, coroutines, Delegates.observable) │
├─────────────────────────────────────────────────────┤
│  Services      — GradeCalculator + ExportServices   │
│  (HOF, pipeline, Apache POI, iText, XML)            │
├─────────────────────────────────────────────────────┤
│  Modèles       — Models.kt                          │
│  (Student, GradeScale, BatchStats, AppUiState)      │
└─────────────────────────────────────────────────────┘
```

---

## 📋 Format du fichier Excel

Le fichier doit avoir une **ligne d'en-tête** et **3 colonnes obligatoires**.
L'ordre des colonnes n'a pas d'importance — la détection est automatique.

| nom | matricule | note |
|-----|-----------|------|
| Alice Mbarga | ETU2024001 | 87 |
| Bob Nkeng | ETU2024002 | 55.5 |
| Charlie Fotso | ETU2024003 | 42 |
| Diana Owono | ETU2024004 | 19 |

### Noms de colonnes reconnus (insensibles à la casse)

| Colonne | Variantes acceptées |
|---------|-------------------|
| **Nom** | `nom`, `name`, `prénom`, `prenom`, `etudiant`, `student` |
| **Matricule** | `matricule`, `id`, `num`, `numero`, `code`, `immatriculation` |
| **Note** | `note`, `grade`, `score`, `mark`, `marks`, `points` |

> 💡 Les notes doivent être entre **0 et 100**. Les décimales sont acceptées (ex: `78.5`). Les lignes vides sont ignorées. Plusieurs fichiers peuvent être chargés simultanément — les données sont fusionnées.

---

## 📊 Barème de notation

| Mention | Intervalle | Appréciation | Résultat |
|---------|-----------|--------------|----------|
| 🟢 **A** | 80 – 100 | Excellent | Admis(e) |
| 🔵 **B+** | 71 – 79 | Très Bien | Admis(e) |
| 🔵 **B** | 66 – 70 | Bien | Admis(e) |
| 🟣 **C+** | 56 – 65 | Assez Bien | Admis(e) |
| 🟣 **C** | 50 – 55 | Satisfaisant | Admis(e) |
| 🟡 **D+** | 45 – 49 | Passable + | Admis(e) |
| 🟡 **D** | 40 – 44 | Passable | Admis(e) |
| 🔴 **F** | 0 – 39 | Insuffisant | Ajourné(e) |

---

## 📤 Formats d'export

| Format | Librairie | Contenu |
|--------|-----------|---------|
| 🟢 **Excel (.xlsx)** | Apache POI 5.3.0 | 2 feuilles : résultats + statistiques, styles colorés |
| 🔴 **PDF (.pdf)** | iText 5.5.13 LGPL | Rapport complet avec KPI, tableau paginé, en-tête coloré |
| 🟠 **XML (.xml)** | stdlib Kotlin | Structure hiérarchique : meta, statistiques, étudiants |
| 🔵 **Word (.docx)** | Apache POI XWPF | Document Word avec tableaux stylisés |

Les fichiers sont sauvegardés dans `~/Downloads/` avec horodatage (`resultats_20240315_1430.xlsx`).

---

## 📚 Concepts OOP illustrés (SE 3242)

### Class 01 — Kotlin Essentials

```kotlin
// val/var, null safety, Elvis operator
val name = row["name"]?.toString()?.trim() ?: ""

// Lambda stockée dans val (function type)
val validateGrade: (Any?) -> String? = { value ->
    when {
        value == null -> "Note manquante"
        else          -> null
    }
}

// Pipeline HOF chaîné : filter → map → filter
val students = rows
    .filter  { rowIsUsable(it) }
    .map     (transformer)
    .filter  { it.name.isNotEmpty() }
```

### Class 02 — OOP : Classes, Héritage, Interfaces

```kotlin
// data class — génère equals(), hashCode(), toString(), copy()
data class Student(val name: String, val matricule: String, val rawGrade: Double, ...)

// sealed class — when exhaustif, pas besoin de else
sealed class AppUiState {
    object Idle    : AppUiState()
    object Loading : AppUiState()
    data class Success(val message: String) : AppUiState()
    data class Error  (val message: String) : AppUiState()
}

// object = singleton
object GradeScale {
    fun resolve(score: Double): GradeBand = bands.firstOrNull { it.contains(score) } ?: bands.last()
}

// abstract class + interface
interface ReportExporter { fun export(result: BatchResult, destDir: String): String }
abstract class BaseExporter : ReportExporter { protected fun buildOutputPath(...) }
class ExcelService : BaseExporter() { override fun export(...) }
```

### Class 03 — Scope Functions, Generics, Delegation, Compose

```kotlin
// by lazy — calculé une seule fois au premier accès
val stats: BatchStats by lazy { /* calcul coûteux */ }

// Delegates.observable — callback à chaque modification
var stateLog: String by Delegates.observable("") { _, old, new ->
    println("$old → $new")
}

// Scope functions : apply, with, also, let, run
sb.apply {
    appendLine("<rapport>")
    appendLine("  <meta>")
}
with(stats) { addRow("Total", "$total") }

// Generics avec contrainte
fun <T : Comparable<T>> maxOf(list: List<T>): T? =
    list.fold(null as T?) { acc, item -> if (acc == null || item > acc) item else acc }

// Coroutines
launch {
    withContext(Dispatchers.IO) { /* lecture fichier */ }
    uiState = AppUiState.Success("Chargé !")
}

// Compose Desktop — @Composable + mutableStateOf + LazyColumn
@Composable
fun StudentTable(vm: AppViewModel) {
    var query by remember { mutableStateOf("") }
    LazyColumn {
        itemsIndexed(vm.displayStudents) { idx, student ->
            StudentRow(idx, student)
        }
    }
}
```

---

## 🛠️ Stack technique

| Catégorie | Technologie | Version |
|-----------|-------------|---------|
| Langage | Kotlin | 2.1.21 |
| GUI | Jetpack Compose Desktop | 1.8.1 |
| Design System | Material 3 | inclus |
| Build | Gradle | 9.3.1 |
| Runtime | JDK Temurin | 25.0.2 |
| Excel I/O | Apache POI | 5.3.0 |
| PDF | iText | 5.5.13.3 |
| Async | Kotlinx Coroutines | 1.10.2 |
| Logging | SLF4J Simple | 2.0.16 |

---

## 🗺️ Évolutions prévues (v2.0)

- [ ] Import CSV en plus d'Excel
- [ ] Graphiques avancés (courbe de Gauss, diagramme en barres natif Canvas)
- [ ] Gestion des coefficients de pondération
- [ ] Historique des sessions (SQLite via Exposed)
- [ ] Thème clair / sombre dynamique
- [ ] Tests unitaires (JUnit 5 + MockK)
- [ ] Impression directe du rapport PDF
- [ ] Internationalisation (FR / EN)

---

## 👥 Équipe

Projet réalisé en **binôme** dans le cadre du cours **SE 3242 — Android Application Development**.

| Rôle | Implémentation |
|------|---------------|
| Étudiant A | Kotlin / Compose Desktop |
| Étudiant B | Dart / Flutter (version binôme) |

---

## 📄 Licence

Ce projet est développé à des fins académiques dans le cadre du cours SE 3242 à ICT University.

---

<div align="center">

Développé avec ❤️ à **ICT University, Yaoundé, Cameroun** · 2026

**SE 3242 — Android Application Development** · Engr. Daniel MOUNE

</div>
