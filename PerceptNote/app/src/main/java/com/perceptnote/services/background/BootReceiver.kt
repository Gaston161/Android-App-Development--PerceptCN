// 📄 app/src/main/java/com/perceptnote/services/background/BootReceiver.kt
// 🔑 Permissions : RECEIVE_BOOT_COMPLETED
package com.perceptnote.services.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.perceptnote.services.overlay.OverlayService

/**
 * Relance l'overlay flottant après redémarrage du téléphone,
 * si l'utilisateur l'avait activé dans les paramètres.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!Settings.canDrawOverlays(context)) return

        val prefs = context.getSharedPreferences("perceptnote_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("overlay_enabled", false)) {
            context.startService(OverlayService.startIntent(context))
        }
    }
}
