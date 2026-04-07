// 📄 app/src/main/java/com/perceptnote/ui/navigation/NavGraph.kt — VERSION CLOUD
package com.perceptnote.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.perceptnote.admin.AdminDashboardScreen
import com.perceptnote.auth.LoginScreen
import com.perceptnote.features.capture.CaptureScreen
import com.perceptnote.features.chat.ChatScreen
import com.perceptnote.features.echoes.EchoesScreen
import com.perceptnote.features.feedback.FeedbackScreen
import com.perceptnote.features.home.HomeScreen
import com.perceptnote.features.notes.NoteDetailScreen
import com.perceptnote.features.notes.NotesScreen
import com.perceptnote.features.profile.ProfileScreen
import com.perceptnote.features.settings.SettingsScreen

object Routes {
    const val LOGIN       = "login"
    const val HOME        = "home"
    const val CAPTURE     = "capture"
    const val NOTES       = "notes"
    const val NOTE_DETAIL = "note_detail/{sessionId}"
    const val CHAT        = "chat/{sessionId}"
    const val ECHOES      = "echoes/{sessionId}"
    const val PROFILE     = "profile"
    const val SETTINGS    = "settings"
    const val FEEDBACK    = "feedback"
    const val ADMIN       = "admin"

    fun noteDetail(id: Long) = "note_detail/$id"
    fun chat(id: Long)       = "chat/$id"
    fun echoes(id: Long)     = "echoes/$id"
}

@Composable
fun NavGraph(startDestination: String = Routes.LOGIN) {
    val nav = rememberNavController()
    val spec = tween<androidx.compose.ui.unit.IntOffset>(280)

    NavHost(
        navController = nav,
        startDestination = startDestination,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, spec) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, spec) },
        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, spec) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, spec) }
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(onLoginSuccess = {
                nav.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
            })
        }
        composable(Routes.HOME) {
            HomeScreen(
                onStartCapture = { nav.navigate(Routes.CAPTURE) },
                onOpenSession = { nav.navigate(Routes.noteDetail(it)) },
                onOpenProfile = { nav.navigate(Routes.PROFILE) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                onOpenAdmin = { nav.navigate(Routes.ADMIN) }
            )
        }
        composable(Routes.CAPTURE) {
            CaptureScreen(
                onNavigateBack = { nav.popBackStack() },
                onSessionSaved = { id -> nav.navigate(Routes.noteDetail(id)) { popUpTo(Routes.HOME) } }
            )
        }
        composable(Routes.NOTES) {
            NotesScreen(
                onSessionClick = { nav.navigate(Routes.noteDetail(it)) },
                onNavigateBack = { nav.popBackStack() }
            )
        }
        composable(Routes.NOTE_DETAIL,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })) { back ->
            val id = back.arguments?.getLong("sessionId") ?: return@composable
            NoteDetailScreen(sessionId = id,
                onOpenChat = { nav.navigate(Routes.chat(id)) },
                onOpenEchoes = { nav.navigate(Routes.echoes(id)) },
                onNavigateBack = { nav.popBackStack() })
        }
        composable(Routes.CHAT,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })) { back ->
            val id = back.arguments?.getLong("sessionId") ?: return@composable
            ChatScreen(sessionId = id, onNavigateBack = { nav.popBackStack() })
        }
        composable(Routes.ECHOES,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })) { back ->
            val id = back.arguments?.getLong("sessionId") ?: return@composable
            EchoesScreen(sessionId = id, onNavigateBack = { nav.popBackStack() })
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                onNavigateBack = { nav.popBackStack() },
                onOpenFeedback = { nav.navigate(Routes.FEEDBACK) },
                onLogout = { nav.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } } }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onNavigateBack = { nav.popBackStack() })
        }
        composable(Routes.FEEDBACK) {
            FeedbackScreen(onNavigateBack = { nav.popBackStack() })
        }
        composable(Routes.ADMIN) {
            AdminDashboardScreen(onNavigateBack = { nav.popBackStack() })
        }
    }
}
