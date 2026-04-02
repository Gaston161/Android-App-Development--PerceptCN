// build.gradle.kts
// ─────────────────────────────────────────────────────────────────────
// Versions calées sur ton environnement :
//   Gradle  9.3.1   (installé sur ta machine)
//   Kotlin  2.1.21  (dernière version compatible Compose 1.8.x)
//   JDK     25.0.2  (Temurin installé)
//   Target  JVM 21  (cible supportée par Kotlin, compatible JDK 25)
// ─────────────────────────────────────────────────────────────────────
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")                             version "2.1.21"
    id("org.jetbrains.compose")               version "1.8.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.21"
}

group   = "com.gradecalc"
version = "1.0.0"

// ── Cible JVM 21 — Kotlin ne supporte pas encore JVM 25 comme cible ──
// On compile pour JVM 21 tout en utilisant le JDK 25 pour compiler.
// Les deux blocs DOIVENT être alignés sur la même valeur.
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = "21"
    targetCompatibility = "21"
}

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    // GUI desktop — aucun Visual Studio, aucun Android SDK requis
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // Apache POI 5.3.0 — lecture et écriture Excel
    implementation("org.apache.poi:poi-ooxml:5.3.0")

    // iText 5 LGPL — génération de PDF
    implementation("com.itextpdf:itextpdf:5.5.13.3")

    // Coroutines — I/O asynchrone non bloquant
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")

    // SLF4J — masque les warnings de POI au démarrage
    implementation("org.slf4j:slf4j-simple:2.0.16")
}

compose.desktop {
    application {
        // IMPORTANT : doit correspondre exactement au package + nom du fichier
        mainClass = "com.gradecalc.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName    = "GradeCalcPro"
            packageVersion = "1.0.0"
            description    = "Student Grade Calculator — SE 3242 ICT University"
            vendor         = "ICT University"
            windows {
                menuGroup   = "GradeCalc Pro"
                upgradeUuid = "3F9A1C2D-4B5E-6F7A-8B9C-0D1E2F3A4B5C"
                shortcut    = true
            }
        }
    }
}
