// 📄 app/src/main/java/com/perceptnote/services/overlay/OverlayService.kt
// 🔑 Permissions : SYSTEM_ALERT_WINDOW (l'utilisateur doit l'accorder manuellement dans Paramètres)
package com.perceptnote.services.overlay

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleService
import com.perceptnote.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs

/**
 * Service overlay — bulle flottante visible au-dessus de toutes les applications.
 *
 * IMPORTANT : Android exige que l'utilisateur autorise manuellement ce type d'overlay
 * via Paramètres > Applications > PerceptNote > Apparaître au-dessus des autres apps.
 * Utiliser Settings.canDrawOverlays(context) pour vérifier avant de démarrer ce service.
 *
 * La bulle permet :
 * - Voir si l'enregistrement est actif (point rouge clignotant)
 * - Ouvrir PerceptNote depuis n'importe quelle app
 * - Arrêter l'enregistrement sans revenir dans l'app
 * - Draggable : repositionnable par l'utilisateur
 */
@AndroidEntryPoint
class OverlayService : LifecycleService() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isRecording = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createOverlayView()
    }

    private fun createOverlayView() {
        val inflater = LayoutInflater.from(this)

        // Vue bubble flottante (layout programmatique simple)
        overlayView = buildBubbleView()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 300
        }

        windowManager?.addView(overlayView, params)
        setupDragBehavior(overlayView!!, params)
    }

    private fun buildBubbleView(): View {
        // LinearLayout programmatique pour la bulle
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = resources.getDrawable(
                android.R.drawable.dialog_holo_light_frame,
                null
            )
            setPadding(16, 12, 16, 12)
        }

        // Indicateur statut
        val statusText = TextView(this).apply {
            id = android.R.id.text1
            text = "● PerceptNote"
            textSize = 11f
            setTextColor(0xFF888888.toInt())
        }

        // Bouton ouvrir
        val openButton = ImageButton(this).apply {
            setImageDrawable(resources.getDrawable(android.R.drawable.ic_menu_view, null))
            background = null
            setOnClickListener {
                val intent = Intent(this@OverlayService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(intent)
            }
        }

        layout.addView(statusText)
        layout.addView(openButton)
        return layout
    }

    /** Gestion drag & drop de la bulle */
    private fun setupDragBehavior(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0; var initialY = 0
        var touchX = 0f; var touchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x; initialY = params.y
                    touchX = event.rawX; touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - touchX).toInt()
                    params.y = initialY + (event.rawY - touchY).toInt()
                    windowManager?.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Si c'était un tap (pas un drag)
                    if (abs(event.rawX - touchX) < 10 && abs(event.rawY - touchY) < 10) {
                        view.performClick()
                    }
                    true
                }
                else -> false
            }
        }
    }

    fun updateRecordingStatus(recording: Boolean) {
        isRecording = recording
        val statusText = overlayView?.findViewById<TextView>(android.R.id.text1)
        statusText?.apply {
            text = if (recording) "● REC" else "○ PerceptNote"
            setTextColor(if (recording) 0xFFE53935.toInt() else 0xFF888888.toInt())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let { windowManager?.removeView(it) }
        overlayView = null
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    companion object {
        fun startIntent(context: Context) = Intent(context, OverlayService::class.java)
        fun stopIntent(context: Context) = Intent(context, OverlayService::class.java)
    }
}
