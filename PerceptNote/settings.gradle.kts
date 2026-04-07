// 📄 settings.gradle.kts
// IMPORTANT : Avec AGP 8.x + Gradle 8+, gradle/libs.versions.toml est
// automatiquement détecté comme catalog "libs". Ne PAS ajouter le bloc
// versionCatalogs manuellement — c'est ce qui causait l'erreur
// "too_many_import_invocation".

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    // ⚠️ NE PAS ajouter versionCatalogs { create("libs") { from(...) } } ici
    // Le fichier gradle/libs.versions.toml est auto-détecté par Gradle 8+
}

rootProject.name = "PerceptNote"
include(":app")
