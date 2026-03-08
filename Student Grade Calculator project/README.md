# GradeCalc Pro — Student Grade Calculator

> Application desktop professionnelle de calcul de notes académiques.
> Disponible en **Dart/Flutter** et **Kotlin/Compose Desktop**.

---

## 🏗️ Architecture

Les deux versions suivent la même architecture OOP :

```
┌────────────────────────────────────────────────────────────────┐
│                        PRÉSENTATION (UI)                       │
│  HomeScreen / MainWindow │ DropZone │ StudentTable │ StatsPanel │
│  ExportPanel │ FileList  │ GradeScaleRef │ TopBar               │
├────────────────────────────────────────────────────────────────┤
│                         ÉTAT (State)                           │
│  AppState (ChangeNotifier / Compose State)                     │
├────────────────────────────────────────────────────────────────┤
│                        SERVICES                                │
│  GradeCalculatorService │ ExcelService │ PdfService            │
│  XmlService             │ WordService                          │
├────────────────────────────────────────────────────────────────┤
│                        MODÈLES                                 │
│  Student │ GradeBand │ GradeScale │ BatchResult │ BatchStats    │
└────────────────────────────────────────────────────────────────┘
```

---

## 📐 Barème de notation

| Mention | Appréciation   | Intervalle  |
|---------|----------------|-------------|
| **A**   | Excellent       | 80 – 100    |
| **B+**  | Très Bien       | 71 – 79     |
| **B**   | Bien            | 66 – 70     |
| **C+**  | Assez Bien      | 56 – 65     |
| **C**   | Satisfaisant    | 50 – 55     |
| **D+**  | Passable +      | 45 – 49     |
| **D**   | Passable        | 40 – 44     |
| **F**   | Insuffisant     | 0  – 39     |

---

## 📋 Format du fichier Excel d'entrée

| Colonne A | Colonne B   | Colonne C |
|-----------|-------------|-----------|
| nom       | matricule   | note      |
| Alice     | ETU2024001  | 78        |
| Bob       | ETU2024002  | 42        |

- Les en-têtes sont insensibles à la casse.
- Synonymes acceptés : `name/prénom` | `id/num/code` | `grade/score/mark`.
- Plusieurs fichiers peuvent être chargés simultanément (fusion automatique).

---

## 🎯 Concepts OOP implémentés

### Dart/Flutter
| Concept                | Où                                  |
|------------------------|-------------------------------------|
| Classes abstraites     | `_IExcelService` interface          |
| Héritage               | Tous les services implémentent `_I*`|
| Encapsulation          | Champs privés + getters dans `AppState` |
| Immutabilité           | `Student` avec `const` + `copyWith` |
| Lambda / closures      | `GradeCalculatorService.buildTransformer()` |
| Fonctions d'ordre sup. | `filterWhere`, `sortBy`, `groupByGrade` |
| Extension methods      | `PdfColor.flatten()` dans pdf_service |
| Enum                   | `SortKey`, `AppStatus`              |
| Mixin / Provider       | `ChangeNotifier` + `context.watch`  |

### Kotlin
| Concept                | Où                                  |
|------------------------|-------------------------------------|
| `object` (singleton)   | `GradeScale`, `GradeCalculatorService` |
| `data class`           | `Student`, `GradeBand`, `BatchStats`|
| Interface              | `ReportExporter`                    |
| `sealed class` / `when`| Switch exhaustif sur `SortKey`     |
| Lambda / higher-order  | `buildTransformer`, `filterWhere`   |
| Extension functions    | `Double.format2`, `String.xmlEscape`|
| `lazy`                 | `BatchResult.stats` (lazy val)      |
| Coroutines             | Toutes les opérations I/O async     |
| Companion object       | Constantes partagées                |
| `by lazy`              | Calcul des stats à la demande       |

---

## 🚀 Démarrage rapide

### Version Flutter

**Prérequis** : Flutter ≥ 3.16 avec support desktop activé.

```bash
cd grade_calculator_flutter
flutter pub get
flutter run -d windows   # ou -d macos / -d linux
flutter build windows    # produit un .exe
```

Pour créer un installeur MSIX (Windows) :
```bash
flutter pub add msix
flutter pub run msix:create
```

### Version Kotlin

**Prérequis** : JDK 17+, Gradle 8+.

```bash
cd grade_calculator_kotlin
./gradlew run              # lancer en développement
./gradlew packageMsi       # Windows installer
./gradlew packageDmg       # macOS installer
./gradlew packageDeb       # Linux Debian package
```

---

## 📤 Formats d'export

| Format  | Bibliothèque Flutter     | Bibliothèque Kotlin          | Contenu                              |
|---------|--------------------------|------------------------------|--------------------------------------|
| Excel   | `excel: ^4.0.6`          | Apache POI 5.2.5             | Feuille Résultats + Statistiques     |
| PDF     | `pdf: ^3.10.8`           | iText 7 / iText 8            | Rapport complet avec en-tête coloré  |
| XML     | `xml: ^6.5.0`            | stdlib StringBuilder         | Méta + stats + liste étudiants       |
| Word    | `archive` (DOCX manuel)  | Apache POI XWPF              | Document Word stylisé                |

---

## 🔮 Évolutions prévues (v2.0)

- [ ] **Authentification** : gestion multi-utilisateurs (admin / enseignant)
- [ ] **Pondération** : notes sur différents coefficients
- [ ] **Rattrapage** : gestion session normale / rattrapage
- [ ] **Graphiques avancés** : courbe de Gauss, box-plot
- [ ] **Import CSV** : en plus d'Excel
- [ ] **Base de données locale** : SQLite pour historique des promotions
- [ ] **API REST** : synchronisation avec un serveur central
- [ ] **Thème clair** : bascule dark/light
- [ ] **i18n** : traduction EN / FR / AR
- [ ] **Tests unitaires** : couverture 80 %+ des services

---

## 👥 Binôme

| Rôle          | Langage        |
|---------------|----------------|
| Étudiant A    | Dart / Flutter |
| Étudiant B    | Kotlin / Compose Desktop |

---

## 📄 Licence

MIT — Libre d'utilisation et de modification.
