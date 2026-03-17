// src/main/kotlin/Main.kt
package com.gradecalc

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.gradecalc.ui.AppRoot
import com.gradecalc.ui.AppViewModel

fun main() {
    // AppViewModel créé avant application{} — évite les problèmes de scope Compose
    val viewModel = AppViewModel()

    application {
        Window(
            onCloseRequest = {
                viewModel.dispose()
                exitApplication()
            },
            title = "GradeCalc Pro  |  Student Grade Calculator  |  SE 3242",
            state = rememberWindowState(width = 1300.dp, height = 820.dp)
        ) {
            AppRoot(viewModel)
        }
    }
}
