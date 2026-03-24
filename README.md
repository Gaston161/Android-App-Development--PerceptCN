<div align="center">

<br/>

<img src="https://capsule-render.vercel.app/api?type=waving&color=4F46E5&height=200&section=header&text=PerceptNote&fontSize=80&fontColor=ffffff&animation=fadeIn&fontAlignY=38&desc=+EchoesClass&descAlignY=60&descSize=28&descColor=a5b4fc" width="100%"/>

<br/>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android"/>
  <img src="https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose"/>
  <img src="https://img.shields.io/badge/Material_3-757575?style=for-the-badge&logo=materialdesign&logoColor=white" alt="Material 3"/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Min_API-26_(Android_8.0)-orange?style=flat-square" alt="Min API"/>
  <img src="https://img.shields.io/badge/Target_API-34_(Android_14)-brightgreen?style=flat-square" alt="Target API"/>
  <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" alt="License"/>
  <img src="https://img.shields.io/badge/Status-En_Développement-yellow?style=flat-square" alt="Status"/>
  <img src="https://img.shields.io/badge/Capteurs-5_actifs-purple?style=flat-square" alt="Capteurs"/>
</p>

<br/>

<h3>
  <em>"Pose ton téléphone. Le reste est automatique."</em>
</h3>

<p align="center">
  <strong>PerceptNote</strong> est une application Android qui transforme votre smartphone en <strong>organe sensoriel intelligent</strong>.<br/>
  Elle capture, transcrit, organise et lit vos cours <strong>sans aucune saisie manuelle</strong>,<br/>
  en fusionnant <strong>5 capteurs</strong> en un pipeline de capture entièrement automatisé.
</p>

<br/>

<p align="center">
  <a href="#-pourquoi-perceptnote"><strong>Pourquoi ?</strong></a> ·
  <a href="#-fonctionnalités"><strong>Fonctionnalités</strong></a> ·
  <a href="#️-architecture"><strong>Architecture</strong></a> ·
  <a href="#-installation"><strong>Installation</strong></a> ·
  <a href="#-capteurs-utilisés"><strong>Capteurs</strong></a> ·
  <a href="#-echoesclass"><strong>EchoesClass</strong></a> ·
  <a href="#️-roadmap"><strong>Roadmap</strong></a>
</p>

<br/>

</div>

---

## 🧠 Pourquoi PerceptNote ?

> En 2026, un étudiant passe encore **40 % de son temps en cours** à recopier mécaniquement des informations — au lieu de comprendre.

Les outils existants traitent le problème de façon **fragmentée** :

| Outil | Ce qu'il fait | Ce qu'il ne fait pas |
|---|---|---|
| Adobe Scan | Scanne des documents | Transcrit, organise, contextualise |
| Otter.ai | Transcrit l'audio | Capture le visuel, détecte le contexte |
| Notion AI | Organise des notes | Capture automatiquement |
| **PerceptNote** | **Tout ça, automatiquement** | — |

**PerceptNote** n'attend pas que tu agisses. Il **perçoit** ton environnement et agit à ta place.

---

## ✨ Le Geste Fondateur

> C'est l'innovation centrale du projet. Un seul geste change tout.

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│   📱 Tu poses ton téléphone à plat sur la table                 │
│                          ↓                                      │
│   📐 L'accéléromètre détecte l'angle ≈ 0°                       │
│                          ↓                                      │
│   🎙️  L'enregistrement audio démarre AUTOMATIQUEMENT            │
│                          ↓                                      │
│   📍 Le GPS géo-indexe la session (ex: Salle B204)              │
│                          ↓                                      │
│   🔔 Mercredi, tu entres dans la même salle                     │
│                          ↓                                      │
│   💬 Notification : "Reprendre les notes de Lundi 14h ?"        │
│                                                                 │
│              ZERO bouton. ZERO saisie. ZERO friction.           │
└─────────────────────────────────────────────────────────────────┘
```

> **Comment le GPS "retrouve" tes notes ?**
> À chaque session, l'app enregistre tes coordonnées GPS exactes (latitude/longitude, rayon 20m).
> La prochaine fois que tu te trouves aux mêmes coordonnées, l'app reconnaît le lieu et remonte
> automatiquement les notes associées. Ta salle de cours devient une **étiquette invisible et permanente**.

---

## 🚀 Fonctionnalités

<details>
<summary><strong>📷 Module Vision Intelligente</strong> — Capteur : Caméra</summary>

<br/>

Basé sur **CameraX** + **Google ML Kit Text Recognition**, ce module transforme la caméra en scanner de structure temps réel.

| Fonctionnalité | Description |
|---|---|
| 🔍 **Scanner de structure** | Numérisation temps réel de tableaux blancs, schémas, notes manuscrites |
| 🌍 **OCR multilingue** | Conversion image → texte éditable avec détection automatique de langue |
| 🧹 **Nettoyage automatique** | Suppression ombres, redressement perspective, amélioration contraste |
| 📊 **Détection de schémas** | Reconnaissance des structures tabulaires pour un rendu adapté |

```kotlin
// Exemple : Démarrage du scanner OCR avec CameraX
val imageAnalysis = ImageAnalysis.Builder()
    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
    .build()
    .also {
        it.setAnalyzer(cameraExecutor) { imageProxy ->
            processImageProxy(textRecognizer, imageProxy)
        }
    }
```

</details>

<details>
<summary><strong>🎙️ Module Audio Intelligent</strong> — Capteur : Microphone</summary>

<br/>

Basé sur **MediaRecorder** + **Android SpeechRecognizer**, ce module transcrit le flux vocal en texte structuré.

| Fonctionnalité | Description |
|---|---|
| 📝 **Transcription temps réel** | Voix → texte horodaté avec segments identifiés |
| ⏱️ **Ancrage temporel** | Synchronisation automatique audio ↔ photos prises simultanément |
| 🔇 **Filtrage de bruit** | Isolation de la voix principale via traitement du signal |
| ✂️ **Découpage automatique** | Détection des silences pour structurer la transcription en sections |

```kotlin
// Exemple : Démarrage de la transcription avec SpeechRecognizer
val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
             RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
}
speechRecognizer.startListening(recognizerIntent)
```

</details>

<details>
<summary><strong>📍 Moteur Contextuel</strong> — Capteurs : GPS + Accéléromètre</summary>

<br/>

C'est le **cœur différenciateur** de PerceptNote. La fusion de deux capteurs passifs crée une intelligence contextuelle unique.

| Fonctionnalité | Capteur | Description |
|---|---|---|
| 🗺️ **Géo-indexation auto** | GPS | Chaque session est liée à ses coordonnées (rayon 20m) |
| 🔔 **Rappel contextuel** | GPS | Retour au même endroit = suggestion des notes précédentes |
| 📲 **Déclenchement par geste** | Accéléromètre | Téléphone posé à plat = enregistrement automatique |
| 🏛️ **Profils de lieu** | GPS + Accel | Comportements différenciés selon le contexte |

```kotlin
// Exemple : Détection de pose via accéléromètre
override fun onSensorChanged(event: SensorEvent) {
    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
        val z = event.values[2]
        // z ≈ 9.8 m/s² = téléphone à plat sur une surface
        if (z > 8.5f && !isRecording) {
            startAutoRecording()
        }
    }
}
```

```kotlin
// Exemple : Géo-indexation d'une session
fun saveSessionWithLocation(note: NoteEntity) {
    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        val session = SessionEntity(
            noteId = note.id,
            latitude = location.latitude,
            longitude = location.longitude,
            radius = 20f, // 20 mètres de rayon
            timestamp = System.currentTimeMillis()
        )
        sessionRepository.save(session)
    }
}
```

</details>

<details>
<summary><strong>🤖 Cerveau IA</strong> — Claude / Gemini API</summary>

<br/>

L'IA transforme les données brutes capturées en savoir exploitable via l'API Claude (Anthropic) ou Gemini (Google).

| Fonctionnalité | Description |
|---|---|
| 📋 **Résumé automatique** | 2h de cours → fiche structurée en un clic |
| 💬 **Chat sur tes notes** | *"Réexplique-moi le point 3 du schéma"* |
| ❓ **Quiz génératif** | Questions d'auto-évaluation basées sur le contenu réel capturé |
| 🌐 **Traduction contextuelle** | Notes dans la langue de ton choix, sens technique préservé |

```kotlin
// Exemple : Appel API pour résumé automatique
suspend fun generateSummary(noteContent: String): String {
    val response = apiClient.sendMessage(
        model = "claude-sonnet-4-20250514",
        systemPrompt = """Tu es un assistant académique expert.
            Génère une fiche de révision structurée à partir de ces notes de cours.
            Utilise des titres clairs, des listes à puces
            et mets en gras les concepts clés.""",
        userMessage = noteContent
    )
    return response.content
}
```

</details>

---

## ♿ EchoesClass

> Le module d'accessibilité intégré à PerceptNote — pour apprendre **sans les mains**, **sans les yeux**.

EchoesClass s'adresse aux étudiants dyslexiques, aux personnes en mobilité, et à tous ceux qui préfèrent consommer l'information **par l'écoute**.

```
┌──────────────────────────────────────────────────────────────────┐
│                      MODE ECHOESCLASS                            │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  🗣️  "Lis le résumé du cours d'aujourd'hui"                      │
│       └→ Reconnaissance vocale → TTS démarre la lecture          │
│                                                                  │
│  📱  Incline le téléphone à GAUCHE                               │
│       └→ Accéléromètre → Section précédente                      │
│                                                                  │
│  📱  Incline le téléphone à DROITE                               │
│       └→ Accéléromètre → Section suivante                        │
│                                                                  │
│  🗣️  "Stop" / "Pause"                                            │
│       └→ Reconnaissance vocale → Pause lecture                   │
│                                                                  │
│  👁️  Mode Dyslexie activé                                        │
│       └→ Police OpenDyslexic + espacement + fond teinté          │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

| Fonctionnalité | Capteur utilisé | Comportement |
|---|---|---|
| Navigation vocale | Microphone | Commandes pour parcourir les notes |
| Lecture TTS | Haut-parleur | Notes lues section par section |
| Navigation par gestes | Accéléromètre | Inclinaison = changement de section |
| Mode Dyslexie | — | Police, espacement, fond coloré adaptés |

---

## 📐 Capteurs Utilisés

> L'obligation académique requiert **2 capteurs minimum**. PerceptNote en mobilise **5**, chacun avec une justification fonctionnelle précise.

| # | Capteur | API Android | Usage PerceptNote | Usage EchoesClass |
|---|---|---|---|---|
| 1 | 📷 **Caméra** | `CameraX` | OCR tableaux & manuscrits | — |
| 2 | 🎙️ **Microphone** | `MediaRecorder` `SpeechRecognizer` | Transcription vocale | Commandes vocales |
| 3 | 📐 **Accéléromètre** | `SensorManager` `TYPE_ACCELEROMETER` | Pose → auto-record | Gestes navigation |
| 4 | 📍 **GPS** | `FusedLocationProviderClient` | Géo-indexation sessions | — |
| 5 | 🔊 **Haut-parleur** | `TextToSpeech` | — | Lecture TTS notes |

> 💡 Le microphone et l'accéléromètre sont utilisés **différemment** selon le mode actif, démontrant la richesse d'usage sans multiplication des dépendances matérielles.

---

## 🏗️ Architecture

L'application suit le patron **MVVM + Clean Architecture**, recommandé officiellement par Google pour les projets Android modernes.

```
com.perceptnote/
│
├── 📁 data/
│   ├── local/
│   │   ├── NoteEntity.kt              # Entité Room — une note
│   │   ├── SessionEntity.kt           # Entité Room — session géo-indexée
│   │   ├── AudioEntity.kt             # Entité Room — fichier audio lié
│   │   └── PerceptNoteDatabase.kt     # Classe Room DB principale
│   ├── repository/
│   │   ├── NoteRepository.kt          # Interface + implémentation notes
│   │   └── SessionRepository.kt       # Interface + implémentation sessions
│   └── models/
│       ├── Note.kt                    # Data class domaine
│       └── Session.kt                 # Data class domaine
│
├── 📁 sensors/
│   ├── CameraModule.kt                # CameraX + ML Kit OCR
│   ├── MicrophoneModule.kt            # MediaRecorder + SpeechRecognizer
│   ├── AccelerometerModule.kt         # SensorManager — détection de pose
│   └── LocationModule.kt              # FusedLocationProviderClient
│
├── 📁 ai/
│   ├── AIClient.kt                    # Retrofit → Claude API / Gemini API
│   ├── SummaryEngine.kt               # Résumé automatique de session
│   ├── QuizEngine.kt                  # Génération de quiz contextuel
│   └── TranslatorEngine.kt            # Traduction multilingue
│
├── 📁 features/
│   ├── home/
│   │   ├── HomeViewModel.kt           # Logique dashboard + géo-sessions
│   │   └── HomeScreen.kt              # UI Compose — écran principal
│   ├── capture/
│   │   ├── CaptureViewModel.kt        # Logique caméra + micro + capteurs
│   │   └── CaptureScreen.kt           # UI Compose — écran de capture
│   ├── notes/
│   │   ├── NotesViewModel.kt          # Logique liste et détail
│   │   └── NotesScreen.kt             # UI Compose — liste des notes
│   ├── chat/
│   │   ├── ChatViewModel.kt           # Logique conversation IA
│   │   └── ChatScreen.kt              # UI Compose — interface chat
│   └── echoes/
│       ├── EchoesViewModel.kt         # Logique TTS + commandes vocales
│       └── EchoesScreen.kt            # UI Compose — mode EchoesClass
│
└── 📁 ui/
    ├── theme/
    │   ├── Color.kt                   # Palette Material 3
    │   ├── Typography.kt              # Typographie
    │   └── Theme.kt                   # Theme principal
    ├── components/
    │   ├── SensorStatusBar.kt         # Barre statut capteurs actifs
    │   ├── NoteCard.kt                # Carte note réutilisable
    │   └── AudioWaveform.kt           # Visualisation audio temps réel
    └── navigation/
        └── NavGraph.kt                # Routes + NavHost
```

### Flux de données

```mermaid
graph TD
    A[📷 Caméra] -->|ImageProxy| B[CameraModule]
    C[🎙️ Microphone] -->|AudioData| D[MicrophoneModule]
    E[📐 Accéléromètre] -->|SensorEvent| F[AccelerometerModule]
    G[📍 GPS] -->|Location| H[LocationModule]

    B -->|Texte OCR| I[CaptureViewModel]
    D -->|Transcription| I
    F -->|Pose détectée| I
    H -->|Coordonnées| I

    I -->|NoteEntity| J[(Room Database)]
    I -->|Texte brut| K[🤖 AIClient]

    K -->|Résumé / Quiz / Chat| L[ChatViewModel]
    J -->|Notes géo-indexées| M[HomeViewModel]

    L --> N[ChatScreen]
    M --> O[HomeScreen]
    I --> P[CaptureScreen]
```

---

## 🔧 Tech Stack

| Couche | Technologie | Justification |
|---|---|---|
| **UI** | Jetpack Compose + Material 3 | Standard Android moderne, déclaratif |
| **Architecture** | MVVM + Clean Architecture | Séparation des responsabilités + testabilité |
| **Async** | Kotlin Coroutines + Flow | Gestion réactive des capteurs en temps réel |
| **Caméra** | CameraX | API officielle stable, lifecycle-aware |
| **OCR** | Google ML Kit | On-device, gratuit, multilingue, rapide |
| **Audio** | MediaRecorder + SpeechRecognizer | Natif Android, zéro dépendance externe |
| **Localisation** | FusedLocationProviderClient | Optimisé batterie (API Google Play Services) |
| **Accéléromètre** | SensorManager | API standard Android |
| **Base de données** | Room (SQLite) | Persistence locale robuste + migrations |
| **API IA** | Retrofit + OkHttp | Claude API (Anthropic) / Gemini API |
| **TTS** | Android TextToSpeech | Intégré Android, multilingue |
| **DI** | Hilt | Injection de dépendances recommandée par Google |
| **Navigation** | Navigation Compose | Type-safe, standard Jetpack |
| **Tests** | JUnit 4 + Mockk + Compose Test | Pyramide de tests complète |

---

## ⚙️ Installation

### Prérequis

- **Android Studio** Hedgehog (2023.1.1) ou plus récent
- **JDK** 17+
- **SDK Android** API 26 (Android 8.0) minimum
- Un **appareil physique Android** ou émulateur avec Google Play Services
- Une clé API **Claude** (Anthropic) ou **Gemini** (Google AI Studio) — gratuit pour démarrer

### Étape 1 — Cloner le projet

```bash
git clone https://github.com/TON_USERNAME/PerceptNote.git
cd PerceptNote
```

### Étape 2 — Configurer les clés API

Crée le fichier `local.properties` à la racine (déjà dans `.gitignore`) :

```properties
# local.properties — NE JAMAIS commiter ce fichier !
sdk.dir=/path/to/your/Android/sdk

# Choisir l'un ou l'autre :
CLAUDE_API_KEY=sk-ant-xxxxxxxxxxxxxxxxxxxxxxxx
# GEMINI_API_KEY=AIzaSyxxxxxxxxxxxxxxxxxxxxxxx
```

### Étape 3 — Déployer sur ton téléphone Android

> **Activer le mode développeur :**
> 1. `Paramètres` → `À propos du téléphone`
> 2. Appuyer **7 fois** sur `Numéro de build`
> 3. `Paramètres` → `Options pour les développeurs` → Activer **Débogage USB**

```bash
# Vérifier que ton téléphone est détecté
adb devices

# Compiler et installer directement
./gradlew installDebug
```

Ou depuis Android Studio : `Run ▶` → sélectionner ton appareil physique.

### Permissions requises

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
```

---

## 🗺️ Roadmap

```
✅ Terminé   🔄 En cours   📋 Planifié   💡 Idée future
```

**Phase 1 — Fondations**
- ✅ Architecture du projet (MVVM + Clean Arch)
- ✅ Documentation complète (README + Document projet)
- 📋 Structure Room Database (NoteEntity, SessionEntity)
- 📋 Navigation Compose (NavGraph + routes)
- 📋 Material 3 Theme + composants de base

**Phase 2 — Capteurs**
- 📋 Module Caméra (CameraX + ML Kit OCR)
- 📋 Module Microphone (MediaRecorder + SpeechRecognizer)
- 📋 Module Accéléromètre (détection de pose)
- 📋 Module GPS (FusedLocationProvider + géo-indexation)

**Phase 3 — Intelligence Artificielle**
- 📋 Intégration Claude API (Retrofit + OkHttp)
- 📋 Moteur de résumé automatique
- 📋 Chat interactif sur les notes
- 📋 Génération de quiz contextuels
- 📋 Traduction multilingue

**Phase 4 — EchoesClass**
- 📋 Navigation vocale (commandes micro)
- 📋 Lecture TTS section par section
- 📋 Navigation par gestes (accéléromètre)
- 📋 Mode Dyslexie (police + couleurs adaptées)

**Phase 5 — Qualité & Tests**
- 📋 Tests unitaires (JUnit + Mockk)
- 📋 Tests d'intégration — pipeline caméra → OCR → Room
- 📋 Tests UI (Compose Test)

**Phase 6 — Livraison**
- 📋 Démo live sur appareil physique
- 📋 Export PDF / Markdown
- 📋 Présentation académique finale

**Idées futures 💡**
- 💡 Synchronisation Google Drive / OneDrive
- 💡 Mode collaboratif (partage de notes)
- 💡 Analytics d'apprentissage (temps d'étude, sujets couverts)
- 💡 Widget Android pour démarrage rapide

---

## 🧪 Tests

```bash
# Lancer tous les tests unitaires
./gradlew test

# Lancer les tests instrumentés (appareil connecté requis)
./gradlew connectedAndroidTest

# Rapport de couverture
./gradlew jacocoTestReport
```

**Stratégie de tests (pyramide) :**

```
            ┌───────────────┐
            │    Tests UI    │  ← Compose Test · 5%
            └───────┬───────┘
                    │
         ┌──────────▼──────────┐
         │  Tests Intégration   │  ← Pipeline complet · 20%
         └──────────┬───────────┘
                    │
      ┌─────────────▼─────────────┐
      │      Tests Unitaires       │  ← JUnit + Mockk · 75%
      └───────────────────────────┘
```

---

## 🔐 Sécurité & Bonnes Pratiques

- ✅ Clés API dans `local.properties` — exclu de Git via `.gitignore`
- ✅ Injection via `BuildConfig` — jamais hardcodées dans le code source
- ✅ Permissions demandées au runtime avec gestion des refus (Android 6+)
- ✅ Données stockées **localement** sur l'appareil (Room DB — pas de cloud par défaut)
- ✅ Aucune donnée personnelle transmise sans action explicite de l'utilisateur

---

## 👤 Auteur

<div align="center">

**Gaston**
*Développeur principal — Architecture & Capteurs*

[![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/TON_USERNAME)

*Projet académique — Cours de Développement d'Applications Mobiles Android · 2026*

</div>

---

## 📄 Licence

```
MIT License — Copyright (c) 2026 Gaston

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```

---

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=4F46E5&height=120&section=footer" width="100%"/>

<br/>

**PerceptNote + EchoesClass** — Fait avec ☕ et Kotlin · Yaoundé · 2026

*Si ce projet t'inspire, laisse une ⭐ — ça compte vraiment !*

</div>
