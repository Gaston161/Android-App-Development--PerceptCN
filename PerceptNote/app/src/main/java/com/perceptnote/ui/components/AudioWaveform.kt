// 📄 app/src/main/java/com/perceptnote/ui/components/AudioWaveform.kt
package com.perceptnote.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.perceptnote.ui.theme.RecordingRed
import kotlin.math.sin

/**
 * Visualisation d'onde audio animée — affiché pendant l'enregistrement.
 * Animation sinusoïdale simulant l'activité du microphone.
 */
@Composable
fun AudioWaveform(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 20
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isActive) (2 * Math.PI).toFloat() else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveProgress"
    )

    val waveColor = RecordingRed
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)

    Canvas(modifier = modifier
        .fillMaxWidth()
        .height(48.dp)
    ) {
        val barWidth = size.width / (barCount * 2f)
        val maxHeight = size.height
        val centerY = size.height / 2f

        for (i in 0 until barCount) {
            val normalizedPos = i.toFloat() / barCount
            val height = if (isActive) {
                val wave = sin(animationProgress + normalizedPos * 2 * Math.PI).toFloat()
                val baseHeight = maxHeight * 0.15f
                val amplitude = maxHeight * 0.4f * ((i % 5 + 1) / 5f)
                baseHeight + amplitude * (wave + 1f) / 2f
            } else {
                maxHeight * 0.1f
            }

            val x = i * (barWidth * 2f) + barWidth / 2f
            drawRoundRect(
                color = if (isActive) waveColor else inactiveColor,
                topLeft = Offset(x, centerY - height / 2f),
                size = Size(barWidth * 0.8f, height),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}
